package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.HashMap;
import java.util.Map;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;

/**
 * Brute force approximate matching of a powder diffraction calibrant trace to d-space values 
 * <p>
 * Used to find the approximate radius (in pixels) for the d-space values
 * 
 */
public class BruteStandardMatcher {
	
	private static final double minDistance = 100;
	private static final double maxDistance = 1200;
	private static final double distanceStep = 2;
	
	private static final double defaultWidth = 5;
	
	private static final double[] energies = new double[] {10, 13, 15, 20, 25, 30, 40, 50, 70, 90, 120, 170};
	
	/**
	 * Match a radial profile to a set of d-space values, returns a map contain the d-space values as keys and the radius as values
	 * @param radius
	 * @param integrated
	 * @param dSpace
	 * @param pixelSize
	 * @return dSpaceRadiusMap
	 */
	public static Map<Double,Double> bruteForceMatchStandards(AbstractDataset radius, AbstractDataset integrated, double[] dSpace, double pixelSize) {
		
		double bestssq = Double.MIN_VALUE;
		double[] bestValues = new double[] {energies[0],minDistance};
		
		DetectorProperties dp = new DetectorProperties(minDistance, 0, 0, radius.getSize(), radius.getSize(), pixelSize, pixelSize);
		DiffractionCrystalEnvironment ce = new DiffractionCrystalEnvironment();
		ce.setWavelengthFromEnergykeV(10);
		
		AbstractDataset clean = cleanUpData(radius, integrated);
				
		for (double en : energies) {
			for (double dist = minDistance; dist <= maxDistance ; dist += distanceStep) {
				dp.setDetectorDistance(dist);
				ce.setWavelengthFromEnergykeV(en);

				double[] radii = new double[dSpace.length];
				for (int i = 0; i< radii.length; i++) radii[i] = DSpacing.radiusFromDSpacing(dp, ce, dSpace[i]);
 				
				AbstractDataset filter = getFilterDataset(radius,radii);
				AbstractDataset out = Maths.multiply(filter, clean);
				double ssq = (double)out.sum();
				
				//if (en == 13 && dist == 174) ssq = Double.MAX_VALUE;
				
				if (ssq > bestssq) {
					bestssq = ssq;
					bestValues[0] = en;
					bestValues[1] = dist;
				}
			}
		}
		
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
	public static AbstractDataset getFilterDataset(AbstractDataset pixelRadius, double[] peakPositions) {
		
		AbstractDataset filter = AbstractDataset.zeros(pixelRadius.getShape(), AbstractDataset.FLOAT64);
		for (double r : peakPositions) {
			int rmin = (int)Math.round(r) - (int)defaultWidth;
			rmin = rmin < 0 ? 0 : rmin;
			int rmax = (int)Math.round(r) + (int)defaultWidth;
			int max = filter.getSize()-1;
			rmax = rmax > max ? max : rmax;
			
			for (int i = rmin; i < rmax; i++) {
				filter.set(1, i);
			}
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
	public static AbstractDataset cleanUpData(AbstractDataset radius, AbstractDataset data) {
		
		AbstractDataset d = Maths.derivative(radius, data, 7);
		d = Maths.derivative(radius, data, 7);
		d.imultiply(-1);
		
		for (int i = 0; i < d.getSize(); i++) {
			if (d.getDouble(i) < 0) {
				d.set(0, i);
			}
		}
		return d;
	}
}
