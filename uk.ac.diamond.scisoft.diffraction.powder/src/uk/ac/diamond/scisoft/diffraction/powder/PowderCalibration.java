package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.BooleanDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;

public class PowderCalibration {
	
	private static final int MAX_RINGS = 10;
	private static final int CENTRE_MASK_RADIUS = 50;
	private static final double ARC_LENGTH = 8;
	//private static final double RADIAL_DELTA = 10;
	private static final int MAX_POINTS = 200;
	
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
		
		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		
		EllipticalROI roi = null;
		double lastMajor = -1;
		double lastAspect = -1;
		int i = 0;
		for (EllipticalROI e : ellipses) {
			
			double major = e.getSemiAxis(0);
			double delta = lastMajor < 0 ? 0.1*major : 0.2*(major - lastMajor);
			if (delta > 50)
				delta = 50;
			lastMajor = major;

			try {
				roi = ellipseFit(image, null, e, delta);
			} catch (Exception ex) {
				roi = null;
			} 
			
			if (roi != null) {
				foundEllipses.add(new ResolutionEllipseROI(roi, dSpace[i]));
				lastAspect = roi instanceof EllipticalROI ? ((EllipticalROI) roi).getAspectRatio() : 1.;
			}
			i++;
		}
		
		return foundEllipses;
		
	}
	
	public static EllipticalROI ellipseFit(AbstractDataset image, BooleanDataset mask, EllipticalROI roi, double radialDelta) {
		
		PolylineROI points;
		EllipticalFitROI efroi;
		
		points = PowderRingsUtils.findPOIsNearEllipse(null, image, mask, (EllipticalROI) roi, ARC_LENGTH, radialDelta, MAX_POINTS);
		
		if (points.getNumberOfPoints() < 3) {
			throw new IllegalArgumentException("Could not find enough points to trim");
		}
		
		efroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 2, false);

		int npts = efroi.getPoints().getNumberOfPoints();
		int lpts;
		do {
			lpts = npts;
			points = PowderRingsUtils.findPOIsNearEllipse(null, image, mask, (EllipticalROI) efroi);

			efroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 2, false);
			npts = efroi.getPoints().getNumberOfPoints(); 
		} while (lpts > npts);

		return efroi;
	}
	

}
