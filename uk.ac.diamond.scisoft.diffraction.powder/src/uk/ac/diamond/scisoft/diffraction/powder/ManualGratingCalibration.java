/*-
 * Copyright 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IPeak;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.BooleanDataset;
import org.eclipse.january.dataset.Comparisons;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.IndexIterator;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.metadata.AxesMetadata;

import uk.ac.diamond.scisoft.analysis.fitting.Generic1DFitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.PseudoVoigt;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;

public class ManualGratingCalibration {

	private double[] beamCentre; // in pixels
	private double gratingspacing; // in nm
	private double energy; // in keV
	private double pixelPitch; // in mm
	private double patternAngle; // in °
	private double fringeSpacing; // in pixels
	private double detectorDistance; // in m
	private SectorROI regionOfInterest;

	/**
	 * Set of keys for the results of the fit.
	 * @author Timothy Spain timothy.spain@diamond.ac.uk & Tim Snow tim.snow@diamond.ac.uk
	 *
	 */
	private enum FitKeys {
		FRINGE_SPACING,
		BEAM_CENTRE_X,
		BEAM_CENTRE_Y,
		PATTERN_ANGLE;
	}

	
	public ManualGratingCalibration() {
	}

	public double[] getBeamCentre() {
		return beamCentre;
	}

	public void setBeamCentre(double[] beamCentre) {
		this.beamCentre = beamCentre;
	}

	public void setGratingspacing(double gratingspacing) {
		this.gratingspacing = gratingspacing;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public void setPixelPitch(double pixelPitch) {
		this.pixelPitch = pixelPitch;
	}
	
	public double getPatternAngle() {
		return patternAngle;
	}

	public double getFringeSpacing() {
		return fringeSpacing;
	}
	
	public SectorROI getSectorROI() {
		return this.regionOfInterest;
	}

	public double getDetectorDistance(Dataset input, Dataset mask, SectorROI regionOfInterest, boolean beamCentreRefinement) {
		// First and foremost, update the internal region of interest
		this.regionOfInterest = regionOfInterest;
		
		Map<FitKeys, Double> fitResults;
		try {
			fitResults = fitGrating(input, mask, beamCentre, regionOfInterest, beamCentreRefinement);
		} catch (Exception e) {
			// try again calculating the beam centre this time...
			beamCentre = estimateBeamCentre(input, mask);
			fitResults = fitGrating(input, mask, beamCentre, regionOfInterest, beamCentreRefinement);
		}
		fringeSpacing = fitResults.get(FitKeys.FRINGE_SPACING);
		patternAngle = fitResults.get(FitKeys.PATTERN_ANGLE);
		beamCentre = new double[] {fitResults.get(FitKeys.BEAM_CENTRE_X), fitResults.get(FitKeys.BEAM_CENTRE_Y)};
		
		final double hc = 1.2398419738620932; // keV nm 
		
		detectorDistance = fringeSpacing * pixelPitch * gratingspacing * energy/hc;
		
		return detectorDistance;
	}

	
	/**
	 * Fits the beam centre and fringe spacing given an I22 grating calibration dataset.
	 * @param input
	 * 				the dataset containing the grating integration
	 * @param beamCentre
	 * 					the manually assigned beam centre, if appropriate
	 * @return a map of the double result values, encapsulated in a map. The Map is keyed by the GratingFitKeys enum.
	 */
	public static Map<FitKeys, Double> fitGrating(Dataset input, Dataset mask, double[] beamCentre, SectorROI regionOfInterest, boolean beamCentreRefinement) {
		// First let's refine the beam centre from the bounding box that the operator supplied
		double[] sectorLength = regionOfInterest.getRadii();
		double[] sectorAngles = regionOfInterest.getAngles();
		Arrays.sort(sectorAngles);

		// Now work out the angle that the sector is pointing at
		double sectorAngle = (sectorAngles[1] - sectorAngles[0]) + sectorAngles[0];
		
		// If requested, refine the beam centre
		if (beamCentreRefinement == true) {
			beamCentre = refineBeamCentre(input, mask, beamCentre, sectorAngle);	
		}
		
		// Update the ROI before going on
		regionOfInterest.setPoint(beamCentre[0], beamCentre[1]);
		regionOfInterest.setRadii(sectorLength);
		regionOfInterest.setAngles(sectorAngles);
		// Get the integrated form of the boxplot 
		Dataset[] integratedRegion = ROIProfile.sector(input, mask, regionOfInterest);
		// Now get the vertical region, down the image
		Dataset alignedIntegral = integratedRegion[0];
		// Get the log
		Dataset alignedLog = Maths.log10(alignedIntegral);
		// Interpolate the missing data
		alignedLog = interpolateMissingData(alignedLog, null);

		// Fit 10 pseudo-Voigt peaks, and eliminate any with a FWHM greater
		// than 10 pixels, since these will not be diffraction fringes
		List<IPeak> allPeaks = Generic1DFitter.fitPeaks(DatasetFactory.createRange(alignedLog.getSize()), alignedLog, PseudoVoigt.class, 10);
		
		// List, then remove all peaks with FWHM > 10 pixels
		List<IPeak> fatPeaks = new ArrayList<IPeak>();
		
		for (IPeak peak : allPeaks)
			if (peak.getFWHM() > 10)
				fatPeaks.add(peak);
		allPeaks.removeAll(fatPeaks);
		
		// Create an array for these values
		double[] peakPositions = new double[allPeaks.size()];
		
		// Find all their locations
		for (int loopIter = 0; loopIter < allPeaks.size(); loopIter ++) {
			peakPositions[loopIter] = allPeaks.get(loopIter).getPosition();
		}
		
		// Sort the entries for subtraction
		Arrays.sort(peakPositions);		
		double[] peakSpaces = new double[peakPositions.length - 1];
		
		// Work out the spaces between the peaks
		for (int loopIter = 1; loopIter < peakPositions.length; loopIter ++) {
			peakSpaces[loopIter - 1] = peakPositions[loopIter] - peakPositions[loopIter - 1];
		}

		// Set up some statistics bits
		SummaryStatistics stats;
		double peakSpaceMean = 0.00;
		double peakSpaceStandardDeviation = 2.00;
		
		// Now go through the peak spacings until the std. dev. is less than one pixel
		while (peakSpaceStandardDeviation >= 1.00) {
			if (peakSpaces.length > 2) {
				// Calculate the peak position statistics
				stats = addToStats(peakSpaces);
				
				// Retrieve useful values
				peakSpaceMean = stats.getMean();
				peakSpaceStandardDeviation = stats.getStandardDeviation();
				
				// Delete values over one standard deviation of the mean
				peakSpaces = evaluateArray(peakSpaces, peakSpaceMean, peakSpaceStandardDeviation);
			}
			// Unless there's only two peaks
			else if (peakPositions.length == 2) {
				peakSpaceMean = (peakSpaces[1] - peakSpaces[0]) / 2;
				peakSpaceStandardDeviation = 0.00;
			}
			// Or, if there's nothing, give a zero result back
			else {
				peakSpaceMean = 0.00;
				peakSpaceStandardDeviation = 0.00;
			}
			
		}
		
		// Set up a map to return the results
		Map<FitKeys, Double> results = null;

		// Fill the map of the results
		results = new HashMap<FitKeys, Double>(4);
		results.put(FitKeys.FRINGE_SPACING, peakSpaceMean);
		results.put(FitKeys.BEAM_CENTRE_X, beamCentre[0]);
		results.put(FitKeys.BEAM_CENTRE_Y, beamCentre[1]);
		results.put(FitKeys.PATTERN_ANGLE, Math.toDegrees(sectorAngle));
		
	return results;
	}
	
	
	private static SummaryStatistics addToStats(double[] inputArray) {
		// Create a new statistics object
		SummaryStatistics stats = new SummaryStatistics();
		
		// Add the contents of the array to it, element by element
		for (int loopIter = 0; loopIter < inputArray.length; loopIter ++) {
			stats.addValue(inputArray[loopIter]);
		}
		
		// Return the array
		return stats;
	}
	

	private static double[] evaluateArray(double[] peakSpaces, double peakMean, double peakStandardDeviation) {
		// First a sanity check, if the standard deviation is less than one, then we'll just return the array
		if (peakStandardDeviation <= 1.00) {
			return peakSpaces;
		}

		// Use nice Java 8 syntax to filter through the array and remove spaces that are larger than the
		// mean plus the standard deviation
		peakSpaces = Arrays.stream(peakSpaces).filter(spaceStream -> spaceStream < (peakMean + peakStandardDeviation)).toArray();
		peakSpaces = Arrays.stream(peakSpaces).filter(spaceStream -> spaceStream > (peakMean - peakStandardDeviation)).toArray();

		// Return the array
		return peakSpaces;
	}
	
	
	// Estimate the beam centre by fitting peaks in each dimension to the entire dataset
	// Not currently using but keeping for now
	private static double[] estimateBeamCentre(Dataset input, Dataset mask) {
		RectangularROI integralBox = new RectangularROI(input.getShape()[0], input.getShape()[1], 0.0);
		Dataset[] profiles = ROIProfile.box(input, mask, integralBox);
		
		int nDim = 2;
		double[] centre = new double[nDim];
		for (int i = 0; i < nDim; i++) {
			Dataset profile = profiles[i];
			List<IPeak> allPeaks = Generic1DFitter.fitPeaks(DatasetFactory.createRange(profile.getSize()), profile, PseudoVoigt.class, 1);
			centre[i] = allPeaks.get(0).getPosition();
		}
		
		return centre;
	}
	
	// Corrects the position of the beam to account for the beam stop blocking the maximum intensity
	private static double[] refineBeamCentre(Dataset input, Dataset mask, double[] beamCentre, double angle) {
		// Define a region of interest, integrate its box
		double angleOffset = angle - (Math.PI / 2);
		double pixelShift = 15.0;
		
		double yBoxX = (beamCentre[0] - (Math.sin(angleOffset) * pixelShift)) + pixelShift, yBoxY = beamCentre[1] - (Math.cos(angleOffset) * pixelShift);
		double xBoxX = beamCentre[0] - (Math.cos(angleOffset) * pixelShift), xBoxY = (beamCentre[1] + (Math.sin(angleOffset) * pixelShift)) + pixelShift;
		
		RectangularROI yIntegralBox = new RectangularROI(yBoxX, yBoxY, input.getShape()[1], pixelShift * 2, angleOffset);
		RectangularROI xIntegralBox = new RectangularROI(xBoxX, xBoxY, pixelShift * 2, input.getShape()[0], angleOffset);
//		new RectangularROI(ptx, pty, width, height, angle, clip)
		Dataset[] yIntegralBoxes = ROIProfile.box(input, mask, yIntegralBox);
		Dataset[] xIntegralBoxes = ROIProfile.box(input, mask, xIntegralBox);
		// Of the integrals calculated pick the one along the box
		Dataset yBoxIntegral = yIntegralBoxes[1];
		Dataset xBoxIntegral = xIntegralBoxes[0];
		// Log this dataset and fill in any blanks
		Dataset yBoxLog = Maths.log10(yBoxIntegral);
		Dataset xBoxLog = Maths.log10(xBoxIntegral);
		yBoxLog = interpolateMissingData(yBoxLog, null);
		xBoxLog = interpolateMissingData(xBoxLog, null);
		// Do a peak fit on this dataset
		List<IPeak> yPeaks = Generic1DFitter.fitPeaks(DatasetFactory.createRange(yBoxLog.getSize()), yBoxLog, PseudoVoigt.class, 1);
		List<IPeak> xPeaks = Generic1DFitter.fitPeaks(DatasetFactory.createRange(xBoxLog.getSize()), xBoxLog, PseudoVoigt.class, 1);
		// And find out where the maxima is
//		double yPeakShift = (beamCentre[1] - yPeaks.get(0).getPosition()) / Math.cos(angleOffset);
//		double xPeakShift = (beamCentre[0] - xPeaks.get(0).getPosition()) / Math.cos(angleOffset);

		double yPeakShift = (yPeaks.get(0).getPosition() / Math.cos(angleOffset)) - pixelShift;
		double xPeakShift = (xPeaks.get(0).getPosition() / Math.cos(angleOffset)) - pixelShift;
		
		double xPosition = beamCentre[0] + xPeakShift;
		double yPosition = beamCentre[1] + yPeakShift;

		return new double[] {xPosition, yPosition};
	}
	
	
	private static Dataset interpolateMissingData(Dataset inputData, Double mdi) {
		BooleanDataset isMissing = Comparisons.logicalNot(Comparisons.isFinite(inputData));
		// Also add missing data values, if defined
		if (mdi != null) {
			isMissing = Comparisons.logicalOr(isMissing, Comparisons.equalTo(inputData, mdi));
		}
		// RLE the missing data blocks. Stored in a map of first element to length of block
		Map<Integer, Integer> missingDataBlocks = new TreeMap<Integer, Integer>();
		IndexIterator iter = isMissing.getIterator();
		while (iter.hasNext()) {
			if (isMissing.getAbs(iter.index)) {
				int missingBlockStart = iter.index;
				int missingBlockLength = 1;
				while(iter.hasNext()) {
					if (isMissing.getAbs(iter.index))
						missingBlockLength++;
					else
						break;
				}
				missingDataBlocks.put(missingBlockStart, missingBlockLength);
			}
		}
		// Get the abscissae of the data, or build one
		AxesMetadata aM = inputData.getFirstMetadata(AxesMetadata.class);
		ILazyDataset[] abscissae = (aM != null) ? inputData.getFirstMetadata(AxesMetadata.class).getAxis(0) : null;
		Dataset abscissa = null;
		if (abscissae != null && abscissae[0] != null) {
			try {
				abscissa = DatasetUtils.sliceAndConvertLazyDataset(abscissae[0]);
			} catch (DatasetException e) {
				// do nothing
			}
		}
		if (abscissa == null) {
			abscissa = DatasetFactory.createRange(inputData.getSize());
		}

		// To interpolate the missing data, we can assume an ordered grid,
		// since missing data can be just left out of a collection of points
		for (Entry<Integer, Integer> entry : missingDataBlocks.entrySet()) {
			int nextFiniteIndex = entry.getKey()+entry.getValue();
			int lastFiniteIndex = entry.getKey()-1;
			// Deal with end points
			if (entry.getKey() == 0) {
				inputData.setSlice(inputData.getObjectAbs(nextFiniteIndex), new int[]{0}, new int[]{nextFiniteIndex}, new int[]{1});
			} else if (entry.getKey() + entry.getValue() == inputData.getSize()) {
				inputData.setSlice(inputData.getObjectAbs(lastFiniteIndex), new int[]{lastFiniteIndex+1}, new int[]{inputData.getSize()}, new int[]{1});
			} else {
				// Construct the parameters of the interpolation y(x) = a·x + b
				double a = (inputData.getElementDoubleAbs(nextFiniteIndex) - inputData.getElementDoubleAbs(lastFiniteIndex))/(abscissa.getElementLongAbs(nextFiniteIndex) - abscissa.getElementDoubleAbs(lastFiniteIndex));
				double b = inputData.getElementDoubleAbs(lastFiniteIndex) - a * abscissa.getElementDoubleAbs(lastFiniteIndex);
				
				for (int index = entry.getKey(); index < entry.getKey() + entry.getValue(); index++) {
					inputData.setObjectAbs(index, a*abscissa.getElementDoubleAbs(index)+b);
				}
			}
		}
		return inputData;
	}

}
