package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;

/**
 * Brute force approximate matching of a powder diffraction calibrant trace to d-space values 
 * <p>
 * Used to find the approximate radius (in pixels) for the d-space values
 * 
 */
public class BruteStandardMatcher {
	
	private static Logger logger = LoggerFactory.getLogger(BruteStandardMatcher.class);
	
	private static final double minDistance = 100;
	private static final double maxDistance = 2000;
	private static final double distanceStep = 2;
	
	private static final double defaultWidth = 5;
	
	private static final double[] energies = new double[] {10,11,12,13,14,15,17,20,25,30,40,50,70,90,120,150,170};
	
	/**
	 * Match a radial profile to a set of d-space values, returns a map contain the d-space values as keys and the radius as values
	 * @param radius
	 * @param integrated
	 * @param dSpace
	 * @param pixelSize
	 * @return dSpaceRadiusMap
	 */
	public static Map<Double,Double> bruteForceMatchStandards(Dataset radius, Dataset integrated, double[] dSpace, double pixelSize) {
		
		double bestssq = Double.NEGATIVE_INFINITY;
		double[] bestValues = new double[] {energies[0],minDistance};
		
		DetectorProperties dp = new DetectorProperties(minDistance, 0, 0, radius.getSize(), radius.getSize(), pixelSize, pixelSize);
		DiffractionCrystalEnvironment ce = new DiffractionCrystalEnvironment();
		ce.setWavelengthFromEnergykeV(10);
		
		Dataset clean = cleanUpData(radius, integrated);
				
		for (double en : energies) {
			for (double dist = minDistance; dist <= maxDistance ; dist += distanceStep) {
				dp.setDetectorDistance(dist);
				ce.setWavelengthFromEnergykeV(en);
				double w = ce.getWavelength();
				double[] radii = new double[dSpace.length];
				for (int i = 0; i< radii.length; i++) {
					if (w < 2*dSpace[i]) radii[i] = DSpacing.radiusFromDSpacing(dp, ce, dSpace[i]);
				}
 				
				Dataset filter = getFilterDataset(radius,radii);
				Dataset out = Maths.multiply(filter, clean);
				double ssq = ((Number) out.sum()).doubleValue();
				
				if (ssq > bestssq) {
					bestssq = ssq;
					bestValues[0] = en;
					bestValues[1] = dist;
				}
			}
		}
		
		logger.debug("Guess distance: {}", bestValues[1]);
		logger.debug("Guess energy: {}", bestValues[0]);
		
		dp.setDetectorDistance(bestValues[1]);
		ce.setWavelengthFromEnergykeV(bestValues[0]);
		
		Map<Double,Double> dSpaceRadiusMap = new HashMap<Double,Double>();
		
			for (double d : dSpace) {
				double r = DSpacing.radiusFromDSpacing(dp, ce, d);
				dSpaceRadiusMap.put(d, r);
			}
		
		return dSpaceRadiusMap;
		
	}
	
	/**
	 * Method to get a dataset containing ones around the peak positions and zeros elsewhere
	 * @param pixelRadius
	 * @param peakPositions
	 * @return filter
	 */
	public static Dataset getFilterDataset(Dataset pixelRadius, double[] peakPositions) {
		
		Dataset filter = DatasetFactory.zeros(pixelRadius.getShape());
		double val = 1;
		for (double r : peakPositions) {
			int rmin = (int)Math.round(r) - (int)defaultWidth;
			rmin = rmin < 0 ? 0 : rmin;
			int rmax = (int)Math.round(r) + (int)defaultWidth;
			int max = filter.getSize()-1;
			rmax = rmax > max ? max : rmax;
			
			for (int i = rmin; i < rmax; i++) {
				filter.set(val, i);
			}
			val *=0.9;
		}
		
		return filter;
	}
	
	/**
	 * Cleans the data for matching. Takes the 2nd derivative, flips the data and sets anything < 0 to 0
	 * <p>
	 * Artificially sharpens peaks and reduces sloping backgrounds
	 * 
	 * @param radius
	 * @param data
	 * @return d
	 */
	public static Dataset cleanUpData(Dataset radius, Dataset data) {
		
		return rollingBallBaselineCorrection(data,10);
	}
	
	private static Dataset rollingBallBaselineCorrection(Dataset y, int width) {
		
		Dataset t1 = DatasetFactory.zeros(y);
		Dataset t2 = DatasetFactory.zeros(y);
		
		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = y.getSlice(new int[]{start}, new int[]{end}, null).min().doubleValue();
			t1.set(val, i);
		}
		
		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = t1.getSlice(new int[]{start}, new int[]{end}, null).max().doubleValue();
			t2.set(val, i);
		}
		
		//t1 = DatasetFactory.zeros(y);
		
		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = (double)t2.getSlice(new int[]{start}, new int[]{end}, null).mean();
			t1.set(val, i);
		}
		
		return Maths.subtract(y, t1);
	}
}
