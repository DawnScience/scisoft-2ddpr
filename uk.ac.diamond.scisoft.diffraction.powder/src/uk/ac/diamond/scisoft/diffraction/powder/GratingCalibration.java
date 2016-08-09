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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IPeak;
import org.eclipse.dawnsci.analysis.api.processing.OperationException;
import org.eclipse.dawnsci.analysis.dataset.impl.FFT;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.DatasetException;
import org.eclipse.january.MetadataException;
import org.eclipse.january.dataset.BooleanDataset;
import org.eclipse.january.dataset.Comparisons;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.IndexIterator;
import org.eclipse.january.dataset.LinearAlgebra;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.metadata.AxesMetadata;
import org.eclipse.january.metadata.MetadataFactory;

import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.Generic1DFitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.PseudoVoigt;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;

public class GratingCalibration {

	private double[] beamCentre; // in pixels
	private double gratingspacing; // in nm
	private double energy; // in keV
	private double pixelPitch; // in mm
	private double patternAngle; // in °
	private double fringeSpacing; // in pixels
	private double detectorDistance; // in m
	
	/**
	 * Set of keys for the results of the fit.
	 * @author Timothy Spain timothy.spain@diamond.ac.uk
	 *
	 */
	private enum FitKeys {
		FRINGE_SPACING,
		BEAM_CENTRE_X,
		BEAM_CENTRE_Y,
		PATTERN_ANGLE;
	}

	
	public GratingCalibration() {
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

	public double getDetectorDistance(Dataset input, Dataset mask) {
		
		Map<FitKeys, Double> fitResults = fitGrating(input, mask, beamCentre);
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
	 * @return a map of the double result values, encapsulated in a map. The Map is keyed by the GratingFitKeys enum.
	 */
	public static Map<FitKeys, Double> fitGrating(Dataset input) {
		return fitGrating(input, null, null);
	}
	
	/**
	 * Fits the beam centre and fringe spacing given an I22 grating calibration dataset.
	 * @param input
	 * 				the dataset containing the grating integration
	 * @param beamCentre
	 * 					the manually assigned beam centre, if appropriate
	 * @return a map of the double result values, encapsulated in a map. The Map is keyed by the GratingFitKeys enum.
	 */
	public static Map<FitKeys, Double> fitGrating(Dataset input, Dataset mask, double[] beamCentre) {
		Map<FitKeys, Double> results = null;
		
		boolean calculateBeamCentre = (beamCentre == null);
		if (calculateBeamCentre) {
			beamCentre = estimateBeamCentre(input, mask);
		}
		// only proceed if beamCentre has received a valid automatic estimate; it is not null.
		if (beamCentre != null) {
		
			int boxHalfWidth = 50;
			int boxHalfLength = Collections.max(Arrays.asList(ArrayUtils.toObject(input.getShape())))/4; // maximum dimension of the image

			// Make the parameters of the integration box
			Dataset boxCentre = DatasetFactory.createFromObject(new double[] {beamCentre[0], beamCentre[1]});
			Dataset boxShape = DatasetFactory.createFromObject(new double[] {boxHalfLength*2, boxHalfWidth*2});
			double[] bounds = new double[] {input.getShape()[0], input.getShape()[1]};

			// box profiles taken across the short edge, running along the long edge
			List<Dataset> longIntegrals = new ArrayList<Dataset>();

			int idTheta = 10;
			for (int iTheta = 0; iTheta < 180; iTheta += idTheta)
				longIntegrals.add(boxIntegrationAtDegreeAngle(input, mask, iTheta, boxShape, boxCentre, bounds));

			// Make longIntegrals into a Dataset, so that I can see it
			int maxLong = 0;
			for (Dataset longProfile : longIntegrals)
				if (longProfile.getSize() > maxLong) maxLong  = longProfile.getSize();

			// allIntegrals makes sure the box profiles all have the same size. It is a roundabout way of padding the data.
			Dataset allIntegrals = DatasetFactory.zeros(DoubleDataset.class, longIntegrals.size(), maxLong);
			for (int i = 0; i < longIntegrals.size(); i++) {
				int offset = (maxLong - longIntegrals.get(i).getSize())/2; 
				for (int j = 0; j < longIntegrals.get(i).getSize(); j++) {
					allIntegrals.set(longIntegrals.get(i).getDouble(j), i, j+offset);
				}
			}
			double[] angleSpacing = getFourierAngleSpacing(allIntegrals, idTheta, boxHalfLength);
			double optimumAngle = angleSpacing[0];
			double firstFringeSpacing = angleSpacing[1];
			Dataset alignedIntegral = boxIntegrationAtDegreeAngle(input, mask, optimumAngle, boxShape, boxCentre, bounds);		
			Dataset alignedLog = Maths.log10(alignedIntegral);

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

			// Check there are at least 2 peaks, otherwise set the spacing to that
			// determined from getFourierAngleSpacing()
			double fringeSpacing = firstFringeSpacing;
			int minPeaks = 2;
			if (allPeaks.size() >= minPeaks) {
				// Get all the peak centres
				double[] peakLocations = new double[allPeaks.size()];
				for (int i = 0; i < allPeaks.size(); i++)
					peakLocations[i] = allPeaks.get(i).getPosition();

				Dataset peakLocationData = DatasetFactory.createFromObject(peakLocations);

				double span = ((double) peakLocationData.max() - (double) peakLocationData.min());
				double fourierDerivedMultiple = span/fringeSpacing;
				double roundedMultiple = Math.floor(fourierDerivedMultiple+0.5);
				fringeSpacing = span/roundedMultiple;
			}
			
			if (calculateBeamCentre)
				beamCentre = refineBeamCentre(input, mask, beamCentre, boxShape, optimumAngle);
			
			// Fill the map of the results
			results = new HashMap<FitKeys, Double>(4);
			results.put(FitKeys.FRINGE_SPACING, fringeSpacing);
			results.put(FitKeys.BEAM_CENTRE_X, beamCentre[0]);
			results.put(FitKeys.BEAM_CENTRE_Y, beamCentre[1]);
			results.put(FitKeys.PATTERN_ANGLE, optimumAngle);
			
		}
		return results;
	}
	
	// Estimate the beam centre by fitting peaks in each dimension to the entire dataset
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
	private static double[] refineBeamCentre(Dataset input, Dataset mask, double[] beamCentre, Dataset boxShape, double angle) {
		
		double[] bounds = new double[] {input.getShape()[0], input.getShape()[1]};
		// Move the centre by the full box width at 90° to the grating pattern. This is the ±y direction
		Dataset rotationMatrix = rotationMatrix(Math.toRadians(angle));
		// Basis vectors of the box
		Dataset yDash = LinearAlgebra.dotProduct(rotationMatrix, DatasetFactory.createFromObject(new double[]{0,1}));
		Dataset xDash = LinearAlgebra.dotProduct(rotationMatrix, DatasetFactory.createFromObject(new double[]{1,0}));
		
		// vector from the centre of the image to the centre of the old box
		Dataset xCentre = Maths.subtract(DatasetFactory.createFromObject(beamCentre), Maths.divide(DatasetFactory.createFromObject(bounds), 2));
		// Determine the direction to move by taking the dot product of the
		// rotated y coordinate with the displacement vector of the box
		// (centre) from the image centre. The sign of the displacement is the
		// opposite of this sign. This moves the box towards the centre of the
		// image.
		Dataset rotateXCentre = LinearAlgebra.dotProduct(yDash.reshape(1,2), xCentre);
		double shiftSign = -1*Math.signum(rotateXCentre.getDouble(0));
		// direction vector in which to shift the box
		Dataset offAxisShiftVector = Maths.multiply(shiftSign, yDash);
		// displacement vector by which to shift the box
		Dataset offAxisShift = Maths.multiply(boxShape.getDouble(1), offAxisShiftVector);
		// the old (incorrect) beam centre shifted by the box shift
		Dataset shiftedBeamCentre = Maths.add(DatasetFactory.createFromObject(beamCentre), offAxisShift);
		
		// Get the fitted box parameters of the shifted box for our own use
		double theta = Math.toRadians(angle);
		Dataset thisBoxShape = boxShape.copy(DoubleDataset.class);
		Dataset newBoxCentre = fitInBounds(shiftedBeamCentre, theta, bounds, thisBoxShape);
		Dataset newBoxOrigin = originFromCentre(newBoxCentre, thisBoxShape, theta);

		Dataset alignedIntegral = boxIntegrationAtDegreeAngle(input, mask, angle, boxShape, shiftedBeamCentre, bounds);		
		// Fit a single peak to the data
		List<IPeak> thePeaks = Generic1DFitter.fitPeaks(DatasetFactory.createRange(alignedIntegral.getSize()), alignedIntegral, PseudoVoigt.class, 1);
		// This is the distance from the new origin along the shifted box edge at which the peak occurs
		double peakLocation = thePeaks.get(0).getPosition();

		// Get the distance from the new origin along the shifted box at which the shifted unrefined beam centre occurs
		double shiftedBeamLocation = LinearAlgebra.dotProduct(xDash.reshape(1,2), Maths.subtract(shiftedBeamCentre, newBoxOrigin)).getDouble(0);
		// the distance by which the beam centre estimate has shifted
		double centreShift = peakLocation - shiftedBeamLocation;
		Dataset beamCentreShift = Maths.multiply(centreShift, xDash);
		
		// Shift the beam
		for (int i=0; i<2; i++)
			beamCentre[i] += beamCentreShift.getDouble(i);
		
		return beamCentre;
	}
	
	// Perform a box integration at the specified angle
	private static Dataset boxIntegrationAtDegreeAngle(Dataset input, Dataset mask, double angle, Dataset boxShape, Dataset boxCentre, double[] bounds) {
		double theta = Math.toRadians(angle);
		Dataset thisBoxShape = boxShape.copy(DoubleDataset.class);
		// Get the centre and possibly altered shape of the box at this angle.
		Dataset newBoxCentre = fitInBounds(boxCentre, theta, bounds, thisBoxShape);
		Dataset newBoxOrigin = originFromCentre(newBoxCentre, thisBoxShape, theta);
		// Create the ROI covering this box 
		RectangularROI integralBox = new RectangularROI(newBoxOrigin.getDouble(0), newBoxOrigin.getDouble(1), thisBoxShape.getDouble(0), thisBoxShape.getDouble(1), theta);
		//	System.out.println("ROI:" + newBoxOrigin.getDouble(0) + ", " + newBoxOrigin.getDouble(1) + ", " + thisBoxShape.getDouble(0) + ", " + thisBoxShape.getDouble(1) + ", " + (double) iTheta);
		// box profiles from this ROI
		Dataset[] boxes = ROIProfile.box(input, mask, integralBox);
		// Get only the ROI in the first dimension
		return boxes[0];
	}
	
	// Using Fourier transforms, get the angle of the grating pattern. An
	// estimate of the spacing is also returned, but is not terribly accurate
	private static double[] getFourierAngleSpacing(Dataset allIntegrals, double idTheta, double boxHalfLength) {
		Dataset allFourier = allIntegrals.copy(DoubleDataset.class);
		int nangles = allIntegrals.getShape()[0];
		int nData = allIntegrals.getShape()[1];
		for (int i = 0; i < nangles; i++) {
			Dataset fft = Maths.abs(FFT.fft(allIntegrals.getSlice(new int[]{i,  0}, new int[] {i+1, nData}, new int[]{1,1})));
			fft.squeeze();
			for (int j = 0;  j < nData; j++){
				allFourier.set(fft.getDouble(j), i, j);
			}
		}
		
		Dataset firstACPeak = DatasetFactory.zeros(DoubleDataset.class, nangles);
		
		// Take the first derivative of the first half of the FT'd data
		for (int i = 0; i < nangles; i++) {
			Dataset x = DatasetFactory.createRange(nData/2+1);
			Dataset y =  allFourier.getSlice(new int[]{i, 0},  new int[]{i+1, nData/2+1}, new int[]{1, 1}).squeeze();
			Dataset ftDerivative = Maths.derivative(x, y, 2);
			// find the maxima and minima of the power spectrum.
			List<Double> zeroes = findDatasetZeros(ftDerivative);
			// Ignore the first zero if it is very small, corresponding to a zero found in the DC component
			if (zeroes.get(0) < 2.0)
				zeroes.remove(0);
			// The first zero will be the minimum power after the DC. Look, therefore, for the second zero.
			firstACPeak.set(zeroes.get(1), i);
		}
		// Determine if the minimum is too close to the zero
		int mindex = firstACPeak.minPos()[0];
		boolean doShiftData = (Math.abs(mindex - nangles/2) > nangles/4);
		Dataset shiftedData = firstACPeak.clone();
		if (doShiftData) {
			for (int i = 0; i < nangles; i++)
			shiftedData.set(firstACPeak.getDouble(i), (i+nangles/2) % nangles);
		}
		
		mindex = shiftedData.minPos()[0];
//		Dataset parabolaX = DatasetFactory.createRange(mindex-1, mindex+2, 1);
		Dataset parabolaX = DatasetFactory.createRange(mindex-1., mindex+2., 1.0, Dataset.FLOAT64);
		Dataset parabolaY = shiftedData.getSlice(new int[]{mindex-1}, new int[]{mindex+2}, new int[]{1});
		
		AxesMetadata parabolaAxes;
		try {
			parabolaAxes = MetadataFactory.createMetadata(AxesMetadata.class, 1);
			parabolaAxes.addAxis(0, parabolaX);
			parabolaY.addMetadata(parabolaAxes);
		} catch (MetadataException e) {
			throw new OperationException(null,  e);
		}
		
		double[] params = Fitter.polyFit(new Dataset[]{parabolaX}, parabolaY, 1e-15, 2).getParameterValues();
		double[] derivParams = Arrays.copyOf(params, 3);
		for (int i = 0; i < params.length; i++)
			derivParams[i] *= 2-i;
		double xMin = -derivParams[1]/derivParams[0];

		// The angle of best alignment of the grating
		double alignmentAngle = xMin*idTheta;
		alignmentAngle -= (doShiftData) ? nangles/2*idTheta : 0;
		alignmentAngle %= 180.0;
		
		// The wavenumber of the grating in the image
		double wavenumberGrating = params[2] - params[1]*params[1]/4/params[0];
		wavenumberGrating -= 0.5;
		double pixelSpacing = boxHalfLength/wavenumberGrating;
		
		//System.out.println("Alignment = " + alignmentAngle + "°, fringe spacing = " + pixelSpacing + "px");
		
		return new double[] {alignmentAngle, pixelSpacing};
	}
	
	// Fit a box of shape (length, breadth) centred at xo, at angle theta to
	// the coordinates within the limits 0<=x,y<=xyLimits. Return the centre of
	// the shifted box
	private static Dataset fitInBounds(Dataset xo, double theta, double[] xyLimits, Dataset shape) {
		
		double[] tRange = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
		double[] signs = new double[] {-1, +1};
		// Rotation matrix
		Dataset rotation = rotationMatrix(theta);
		
		// The vector of the shift
		Dataset u = DatasetFactory.createFromObject(new double[]{1, 0});
		u = LinearAlgebra.dotProduct(rotation, u);
		// Iterate over corners, by the signs of the translation
		for (double cornerX : signs) {
			for (double cornerY : signs) {
				Dataset cornerSigns = DatasetFactory.createFromObject(new double[] {cornerX, cornerY});
				Dataset offset = Maths.multiply(0.5, Maths.multiply(shape, cornerSigns));
				offset = LinearAlgebra.dotProduct(rotation, offset);
				Dataset xc = Maths.add(xo, offset);
				// Now solve the offsets required to keep that corner in the bounds from 0 to [x|y]Limit
				for (int dimension = 0; dimension < 2; dimension++) {
					double[] localTRange = new double[2];
					localTRange[0] = (xc.getDouble(dimension) != 0.0) ? (0 - xc.getDouble(dimension))/u.getDouble(dimension) : 0.0;
					localTRange[1] = (xc.getDouble(dimension) != xyLimits[dimension]) ? (xyLimits[dimension]- xc.getDouble(dimension))/u.getDouble(dimension) : 0.0;
					// Sort the array. The range of the t shift parameter is
					// then the lower and upper values that keep this corner
					// within the bounds of this dimension. 
					Arrays.sort(localTRange);
					// Update the global t range
					tRange[0] = Math.max(tRange[0], localTRange[0]);
					tRange[1] = Math.min(tRange[1], localTRange[1]);
				}
			}
		}
		// Calculate the value it needs to move to get within bounds.
		double tShifted = 0.0;

		double deltaLength = Math.min(tRange[1] - tRange[0], 0.0); // non-positive value
		if (deltaLength < 0.0) {
			tShifted = (tRange[0] + tRange[1])/2;
			// alter the shape of the box in this case
			shape.set(shape.getDouble(0) + deltaLength, 0);
		} else {
			tShifted = Math.max(tShifted, tRange[0]);
			tShifted = Math.min(tShifted, tRange[1]);
		}		
		Dataset xoShifted = Maths.add(xo, Maths.multiply(tShifted, u));
		return xoShifted;
	}
	
	private static Dataset rotationMatrix(double theta) {
		double cTheta = Math.cos(theta), sTheta = Math.sin(theta);
		return DatasetFactory.createFromObject(new double[] {cTheta,  -sTheta, sTheta, cTheta}, 2, 2);
	}

	private static Dataset originFromCentre(Dataset centre, Dataset shape, double theta) {
		return Maths.subtract(centre, LinearAlgebra.dotProduct(rotationMatrix(theta), Maths.multiply(0.5, shape)));
	}

	/**
	 * Returns the floating point indices of the zeros of a Dataset
	 * @param y
	 * 			dependent variable of the data
	 * @return List of values of the independent variable at the zeros of the data
	 */
	private static List<Double> findDatasetZeros(Dataset y) {
		List<Double> zeros = new ArrayList<>(); 
		IndexIterator searchStartIterator = y.getIterator(), searchingIterator = searchStartIterator;
		if (!searchStartIterator.hasNext())
			return zeros;
		double startValue = y.getElementDoubleAbs(searchStartIterator.index);
		
		while(searchingIterator.hasNext()) {
			double searchValue = y.getElementDoubleAbs(searchingIterator.index);
			if (searchValue == 0) {
				// restart the search from the next point
				if (!searchingIterator.hasNext()) break;
				searchStartIterator = searchingIterator;
				startValue = y.getElementDoubleAbs(searchStartIterator.index);
			}
			if (Math.signum(searchValue) != Math.signum(startValue)) {
				// linear interpolation to get the zero
				double y1 = y.getElementDoubleAbs(searchingIterator.index-1),
						y2 = y.getElementDoubleAbs(searchingIterator.index);
				zeros.add(searchingIterator.index - y2/(y2-y1));
				
				// restart the search from the searchValue point
				searchStartIterator = searchingIterator;
				startValue = searchValue;
			}
		}
		return zeros;
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
