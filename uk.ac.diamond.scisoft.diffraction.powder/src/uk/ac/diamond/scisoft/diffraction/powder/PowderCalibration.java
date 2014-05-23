package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.BooleanDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.PeakFittingEllipseFinder;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IParametricROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;

public class PowderCalibration {
	
	private static final int MAX_RINGS = 10;
	private static final int CENTRE_MASK_RADIUS = 50;
	
	private final static Logger logger = LoggerFactory.getLogger(PowderCalibration.class);
	
	public static CalibrationOutput calibrateKnownWavelength(AbstractDataset image, double wavelength, double pixel, List<HKL> spacings) {
		
		List<ResolutionEllipseROI> found = findMatchedEllipses(image, pixel, spacings);
		
		List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>(found);
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		allEllipses.add(ellipses);
		
		List<double[]> allDSpacings = new ArrayList<double[]>();
		double[] dSpaceArray = new double[found.size()];
		
		for (int j = 0; j < dSpaceArray.length;j++) {
			dSpaceArray[j] = found.get(j).getResolution();
		}
		
		allDSpacings.add(dSpaceArray);
		
		return CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixel, wavelength);
	}
	
	public static CalibrationOutput calibrateSingleImage(AbstractDataset image, double pixel, List<HKL> spacings) {
		
		List<ResolutionEllipseROI> found = findMatchedEllipses(image, pixel, spacings);
		
		List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>(found);
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		allEllipses.add(ellipses);
		
		List<double[]> allDSpacings = new ArrayList<double[]>();
		double[] dSpaceArray = new double[found.size()];
		
		for (int j = 0; j < dSpaceArray.length;j++) {
			dSpaceArray[j] = found.get(j).getResolution();
		}
		
		allDSpacings.add(dSpaceArray);
		
		return CalibrateEllipses.run(allEllipses, allDSpacings, new DoubleDataset(new double[]{0}, new int[]{1}), pixel);
	}
	
	public static List<ResolutionEllipseROI> findMatchedEllipses(AbstractDataset image, double pixel, List<HKL> spacings) {

		double[] approxCentre = CentreGuess.guessCentre(image);

		logger.info("centre, x: " + approxCentre[0] +" y: " + approxCentre[1]);

		int[] shape = image.getShape();

		double[] farCorner = new double[]{0,0};
		if (approxCentre[0] < shape[0]/2.0) farCorner[0] = shape[0];
		if (approxCentre[1] < shape[1]/2.0) farCorner[1] = shape[1];
		double maxDistance = Math.sqrt(Math.pow(approxCentre[0]-farCorner[0],2)+Math.pow(approxCentre[1]-farCorner[1],2));
		SectorROI sector = new SectorROI(approxCentre[0], approxCentre[1], 0, maxDistance, 0, 2*Math.PI);

		AbstractDataset[] profile = ROIProfile.sector(image, null, sector, true, false, false, null, XAxis.PIXEL, false);

		final AbstractDataset y = profile[0];

		for (int i = 0 ; i < CENTRE_MASK_RADIUS ; i++) {
			y.set(0, i);
		}

		final AbstractDataset x = AbstractDataset.arange(y.getSize(), AbstractDataset.INT32);

		int max = spacings.size() > MAX_RINGS ? MAX_RINGS : spacings.size();

		double[] dSpace = new double[max];

		for (int i = 0; i < max; i++) dSpace[i] = spacings.get(i).getDNano()*10;

		final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, dSpace, pixel);

		final List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();

		for (int i = 0; i < dSpace.length; i++) {
			ellipses.add(new EllipticalROI(dSpaceRadiusMap.get(dSpace[i]), approxCentre[0],approxCentre[1]));
		}

		double[] inner = new double[dSpace.length];
		double[] outer = new double[dSpace.length];

		for (int i = 0; i < dSpace.length; i++) {
			//TODO if dSpace == 1;
			double dVal = dSpaceRadiusMap.get(dSpace[i]);
			ellipses.add(new EllipticalROI(dVal, approxCentre[0],approxCentre[1]));
			inner.toString();
			if (i == 0) {
				double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
				inner[i] = out > 50 ? 50 : out;
				outer[i] = out > 50 ? 50 : out;

			} else if (i == dSpace.length -1) {
				double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
				inner[i] = in > 50 ? 50 : in;
				outer[i] = in > 50 ? 50 : in;
			} else {
				double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
				double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
				inner[i] = in > 50 ? 50 : in;;
				outer[i] = out > 50 ? 50 : out;;
			}
		}

		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		EllipticalROI roi = null;
		int i = 0;
		double corFact = 0;
		double lastAspect = 1;
		double lastAngle = 0;
		for (EllipticalROI e : ellipses) {
			double startSemi = e.getSemiAxis(0);
			e.setSemiAxis(0, startSemi+corFact);
			e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
			e.setAngle(lastAngle);

			try {
				roi = ellipsePeakFit(image, null, e, inner[i], outer[i]);
			} catch (Exception ex) {
				roi = null;
			} 

			if (roi != null) {
				foundEllipses.add(new ResolutionEllipseROI(roi, dSpace[i]));
				corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
				lastAspect = ((EllipticalROI) roi).getAspectRatio();
				lastAngle = ((EllipticalROI) roi).getAngle();
			}
			i++;
		}

		return foundEllipses;

	}
	
	public static EllipticalROI ellipsePeakFit(AbstractDataset image, BooleanDataset mask, EllipticalROI roi, double innerDelta, double outerDelta) {
		
		PolylineROI points;
		EllipticalFitROI efroi;
		
		EllipticalROI[] inOut = new EllipticalROI[2];

		inOut[0] = roi.copy();
		inOut[0].setSemiAxis(0, roi.getSemiAxis(0)-innerDelta);
		inOut[0].setSemiAxis(1, roi.getSemiAxis(1)-innerDelta);

		inOut[1] = roi.copy();
		inOut[1].setSemiAxis(0, roi.getSemiAxis(0)+outerDelta);
		inOut[1].setSemiAxis(1, roi.getSemiAxis(1)+outerDelta);
		
		points = PeakFittingEllipseFinder.findPointsOnConic(image, mask, roi, inOut,386,null);
		
		if (points.getNumberOfPoints() < 3) {
			throw new IllegalArgumentException("Could not find enough points to trim");
		}
		
		efroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 2, false);

		return efroi;
	}
	

}
