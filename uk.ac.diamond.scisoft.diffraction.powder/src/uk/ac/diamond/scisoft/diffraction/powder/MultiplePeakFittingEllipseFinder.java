package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.BooleanDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetFactory;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Stats;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.Generic1DFitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Gaussian;
import uk.ac.diamond.scisoft.analysis.optimize.GeneticAlg;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.PointROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;

public class MultiplePeakFittingEllipseFinder {
	
	private static Logger logger = LoggerFactory.getLogger(MultiplePeakFittingEllipseFinder.class);

	private static final double FULL_CIRCLE = 2.0*Math.PI;

	private static final int MAX_POSITION_SHIFT = 20;
	private static final double ANGULAR_STEP = Math.PI/32;
	
	public static List<EllipticalROI> findEllipses(Dataset image, double[] approxCentre) {
		//TODO check image 2D
		final int[] shape = image.getShape();
		final int h = shape[0];
		final int w = shape[1];
		
		double maxDist = Math.hypot(approxCentre[0], approxCentre[1]);
		double dist =  Math.hypot(w - approxCentre[0], approxCentre[1]);
		maxDist = maxDist < dist ? dist : maxDist;
		dist =  Math.hypot(w - approxCentre[0],h - approxCentre[1]);
		maxDist = maxDist < dist ? dist : maxDist;
		dist =  Math.hypot(approxCentre[0],h - approxCentre[1]);
		maxDist = maxDist < dist ? dist : maxDist;
		
		
		//First quadrant
		
		double currentAngle = -Math.PI;
		
		List<TreeSet<double[]>> foundParams = new ArrayList<TreeSet<double[]>>();
		List<Double> angles = new ArrayList<Double>();
		
		while (currentAngle < Math.PI) {
			Dataset profile;
			List<CompositeFunction> peaks;
			
			profile = getProfile(image, approxCentre, currentAngle, 0, maxDist);
			Dataset x = DatasetFactory.createRange(profile.getSize(), Dataset.INT32);
			
			try {
				peaks = Generic1DFitter.fitPeakFunctions(x, profile, Gaussian.class, new GeneticAlg(0.0001),
						10,3);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			
			TreeSet<double[]> vals = new TreeSet<double[]>(new Comparator<double[]>() {

				@Override
				public int compare(double[] o1, double[] o2) {
					return Double.compare(o1[0], o2[0]);
				}
			});
			
			if (peaks == null) continue;
			
			for (CompositeFunction func : peaks) {
				vals.add(new double[] {func.getParameter(0).getValue(), func.val(func.getParameter(0).getValue()),func.getParameter(1).getValue()});
			}
			
			foundParams.add(vals);
			angles.add(currentAngle);
			
			currentAngle += ANGULAR_STEP;
		}
		
		cleanFoundPeaks(foundParams);
		
		List<Dataset> goodPoints = ellipsesFromFoundPeaks(foundParams, angles);
		
		return ellipsesFromGoodPoints(goodPoints,angles,approxCentre);
		
	}
	
	private static void cleanFoundPeaks(List<TreeSet<double[]>> foundParams) {
		
		
		
		for (TreeSet<double[]> peaks : foundParams) {
			
			Dataset widths = DatasetFactory.zeros(new int[] {peaks.size()}, Dataset.FLOAT64);
			int i = 0;
			
			Iterator<double[]> iter = peaks.iterator();
			
			while(iter.hasNext()) {
				double[] param = iter.next();
				if (param[0] < 0 || param[1] < 0) iter.remove();
				widths.set(param[2], i++);
			}
			
			double threshold = (double)Stats.median(widths) + widths.stdDeviation().doubleValue()*2;
			
			iter = peaks.iterator();
			
			while(iter.hasNext()) {
				double[] param = iter.next();
				if (param[2] > threshold) iter.remove();
			}
		}
		
	}
	
	private static List<Dataset> ellipsesFromFoundPeaks(List<TreeSet<double[]>> foundParams, List<Double> angles) {
		
		List<Dataset> goodPoints = new ArrayList<Dataset>();
		
		int maxPeaks = 0;
		for (TreeSet<double[]> peaks : foundParams) {
			maxPeaks = maxPeaks > peaks.size() ? maxPeaks : peaks.size(); 
		}
		
		for (int j = 0; j < maxPeaks; j++) {
			int i = 0;
			Dataset first = DatasetFactory.zeros(new int[] {foundParams.size()}, Dataset.FLOAT64);
			
			for (TreeSet<double[]> peaks : foundParams) {
				if (!peaks.isEmpty()) first.set(peaks.first()[0], i++);
				else first.set(Double.NaN, i++);
			}
			
			double min = first.min(true).doubleValue();
			i=0;
			for (TreeSet<double[]> peaks : foundParams) {
				if (!peaks.isEmpty() && peaks.first()[0] < (min + MAX_POSITION_SHIFT)) {
					first.set(peaks.pollFirst()[0], i++);
				}
				else {
					first.set(Double.NaN, i++);
				}
			}
			
			goodPoints.add(first);
		}
		
		return goodPoints;
		
	}
	
	private static List<EllipticalROI> ellipsesFromGoodPoints(List<Dataset> goodPoints,List<Double> angles,double[] approxCentre){
		List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();

		for (Dataset points : goodPoints) {
			PolylineROI polyline = new PolylineROI();
			for (int i =0 ;  i< angles.size(); i++) {
				double r = points.getDouble(i);
				if (Double.isNaN(r)) continue;
				polyline.insertPoint(r*Math.cos(angles.get(i)) +approxCentre[0],r*Math.sin(angles.get(i))+approxCentre[1]);
			}

			if (polyline.getNumberOfPoints() > 7) {
				ellipses.add(PowderRingsUtils.fitAndTrimOutliers(null, polyline,1000, false));
			}
		}

		return ellipses;
	}
	
	private static Dataset getProfile(Dataset image, double[] centre, double angle, double inner, double outer) {
		
		LinearROI roi = new LinearROI();
		roi.setPoint(centre);
		roi.setAngle(angle);
		roi.setLength(outer-inner);
		roi.translateAlongLength(inner);
		
		return ROIProfile.line(image, roi, 1)[0];
	}
	
	/**
	 * Find a set of points of interests near given ellipse from an image.
	 * <p>
	 * The ellipse is divided into sub-areas and these POIs are considered to
	 * be the locations of maximum pixel intensities found within those sub-areas.
	 * @param mon
	 * @param image
	 * @param mask (can be null)
	 * @param ellipse
	 * @param arcLength step size along arc in pixels
	 * @param radialDelta +/- value to define area to search
	 * @param maxPoints maximum number of points to return
	 * @return polyline ROI
	 */
	public static PolylineROI fitPOIsNearEllipse(Dataset image, BooleanDataset mask, EllipticalROI ellipse,
			double arcLength, double radialDelta, int maxPoints) {
		if (image.getRank() != 2) {
			logger.error("Dataset must have two dimensions");
			throw new IllegalArgumentException("Dataset must have two dimensions");
		}
		if (mask != null && !image.isCompatibleWith(mask)) {
			logger.error("Mask must match image shape");
			throw new IllegalArgumentException("Mask must match image shape");
		}

		final int[] shape = image.getShape();
		final int h = shape[0];
		final int w = shape[1];
		if (ellipse.containsPoint(-1,-1) && ellipse.containsPoint(-1,h+1) && ellipse.containsPoint(w+1,h+1) && ellipse.containsPoint(w+1,-1)) {
			throw new IllegalArgumentException("Ellipse does not intersect image!");
		}
		
		final double aj = ellipse.getSemiAxis(0);
		final double an = ellipse.getSemiAxis(1);
		if (an < arcLength) {
			logger.error("Ellipse/circle is too small");
			throw new IllegalArgumentException("Ellipse/circle is too small");
		}

		final double xc = ellipse.getPointX();
		final double yc = ellipse.getPointY();
		final double ang = ellipse.getAngle();
		final double ca = Math.cos(ang);
		final double sa = Math.sin(ang);

		final double pdelta = (arcLength*8) / aj; // change in angle
		double rdelta = radialDelta; // semi-width of annulus of interest
		if (rdelta < 1) {
			logger.warn("Radial delta was set too low: setting to 1");
			rdelta = 1;
		}
		final double rsj = aj - rdelta;
		final double rej = aj + rdelta;
		final double rsn = an - rdelta;
		final double ren = an + rdelta;

		final int imax = (int) Math.ceil(FULL_CIRCLE / pdelta);

		logger.debug("Major semi-axis = [{}, {}]; {}", new Object[] { rsj, rej, imax });
		PolylineROI polyline = new PolylineROI();
		LinearROI roi = new LinearROI();
		CompositeFunction cf = null;
		for (int i = 0; i < imax; i++) {
			double p = i * pdelta;
			double cp = Math.cos(p);
			double sp = Math.sin(p);
			Dataset sub;
			final double[] beg = new double[] { (yc + rsj * sa * cp + rsn * ca * sp),
					 (xc + rsj * ca * cp - rsn * sa * sp) };
			final double[] end = new double[] { (yc + rej * sa * cp + ren * ca * sp),
					 (xc + rej * ca * cp - ren * sa * sp) };
			
			roi.setPoint(beg[1], beg[0]);
			roi.setEndPoint(end[1], end[0]);

			sub = ROIProfile.line(image,mask,roi,1,false)[0];
			Dataset xAx = DatasetFactory.createRange(sub.getSize(), Dataset.INT32);

			List<CompositeFunction> peaks= null;
			
			try {
				if (cf == null) {
					peaks = Generic1DFitter.fitPeakFunctions(xAx, sub, Gaussian.class, new GeneticAlg(0.0001),
							1,3);
					cf = peaks.get(0);
				} else {
					DoubleDataset xData = DoubleDataset.createRange(sub.getSize());
						int maxPos = sub.maxPos()[0];
						cf.getFunction(0).getParameter(1).setValue(maxPos);
					Fitter.ApacheNelderMeadFit(new Dataset[]{xData}, sub, cf);
					
					if (cf.getFunction(0).getParameter(1).getValue() > 10 || cf.getFunction(0).getParameter(1).getValue() < 0.1 || cf.getFunction(0).getParameter(2).getValue() < 0) {
						peaks = Generic1DFitter.fitPeakFunctions(xAx, sub, Gaussian.class, new GeneticAlg(0.0001),
								1,3);
						cf = peaks.get(0);
					} else {
						peaks = new ArrayList<CompositeFunction>(1);
						peaks.add(cf);
					}
				}
				
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
			
			if (peaks == null || peaks.isEmpty()) continue;
			
			if (peaks.get(0).getFunction(0).getParameter(1).getValue() > 10 || peaks.get(0).getFunction(0).getParameter(1).getValue() < 0.1 || peaks.get(0).getFunction(0).getParameter(2).getValue() < 0) continue;
			
			double r = peaks.get(0).getParameter(0).getValue();
			double x = r*Math.cos(p+ang)+beg[1];
			double y = r*Math.sin(p+ang)+beg[0];
			polyline.insertPoint(new PointROI(x,y));
		}
		
		return polyline;
	}

}
