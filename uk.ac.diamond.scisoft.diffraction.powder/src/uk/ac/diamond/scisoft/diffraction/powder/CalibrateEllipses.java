package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetFactory;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;

/**
 * Direct conversion of I12 Matlab calibrateD_forDAWN.m
 * <p>
 * Used to calibrate a powder diffraction experiment from a set of ellipse parameters matched
 * to d-space values
 * 
 */
public class CalibrateEllipses {
	
	private final static Logger logger = LoggerFactory.getLogger(CalibrateEllipses.class);
	
	/**
	 * Calibrate a set of images at a unknown distance and wavelength, but known delta distance
	 * <p>
	 * Returns the wavelength, distances, detector tilts and beam centres for each image
	 * <p>
	 * @param allEllipses - list of ellipses for each image
	 * @param allDSpacings - array of d-spacings for each image
	 * @param deltaDistance - approximate distances between each image with accurate differences between distances
	 * @param pixel size in mm
	 * @param wavelength in angstroms
	 * @return calibrationOutput
	 */
	public static CalibrationOutput run(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings, Dataset deltaDistance, double pixel) {
		return run(allEllipses, allDSpacings, deltaDistance,pixel, -1, false);
	}
	
	/**
	 * Calibrate a single image at a known wavelength
	 * <p>
	 * Returns distance, detector tilt and beam centre
	 * <p>
	 * @param allEllipses - list of ellipses for each image (should only be one image)
	 * @param allDSpacings - array of d-spacings for each image (should only be one image)
	 * @param pixel size in mm
	 * @param wavelength in angstroms
	 * @return calibrationOutput
	 */
	public static CalibrationOutput runKnownDistance(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings,double pixel,double knownDistance) {
		Dataset deltaDistance = DatasetFactory.zeros(new int[]{1}, Dataset.FLOAT64);
		return run(allEllipses, allDSpacings, deltaDistance,pixel, knownDistance, false);
	}
	
	/**
	 * Calibrate a single image at a known wavelength
	 * <p>
	 * Returns distance, detector tilt and beam centre
	 * <p>
	 * @param allEllipses - list of ellipses for each image (should only be one image)
	 * @param allDSpacings - array of d-spacings for each image (should only be one image)
	 * @param pixel size in mm
	 * @param wavelength in angstroms
	 * @return calibrationOutput
	 */
	public static CalibrationOutput runKnownWavelength(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings,double pixel,double knownWavelength) {
		Dataset deltaDistance = DatasetFactory.zeros(new int[]{1}, Dataset.FLOAT64);
		return run(allEllipses, allDSpacings, deltaDistance,pixel, knownWavelength, true);
	}
	
	
	/**
	 * Calibrate images with various options
	 * <p>
	 * Returns the wavelength, distances, detector tilts and beam centres for each image
	 * <p>
	 * @param allEllipses - list of ellipses for each image
	 * @param allDSpacings - array of d-spacings for each image
	 * @param deltaDistance - approximate distances between each image with accurate differences between distances
	 * @param md - diffraction metadata object for the image
	 * @param params - parameter model for calibration process
	 * @return calibrationOutput
	 */
	public static CalibrationOutput run(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings,
			Dataset deltaDistance, double pixelSize, double fixedValue, SimpleCalibrationParameterModel params) {
		
		if (params.isFloatDistance() && !params.isFloatEnergy()) {
			return runKnownWavelength(allEllipses, allDSpacings, pixelSize, fixedValue);
		} else if (!params.isFloatDistance() && params.isFloatEnergy()) {
			return runKnownDistance(allEllipses, allDSpacings, pixelSize, fixedValue);
		} else {
			return run(allEllipses, allDSpacings, deltaDistance,pixelSize, -1, false);
		}
	}
	
	
	public static CalibrationOutput run(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings, Dataset deltaDistance,double pixel, double knownValue, boolean isWavelength){
		
		if (allEllipses.isEmpty() || allEllipses.get(0).size() < 2) throw new IllegalArgumentException("Need more than 1 ellipse");
		if (allDSpacings.isEmpty() || allEllipses.get(0).size() != allDSpacings.get(0).length) throw new IllegalArgumentException("Number of ellipses must equal number of d-spacings");
		
		//double w = 2048, h = 2048;
		//double pixel = 0.2;
		Dataset normDist = Maths.subtract(deltaDistance.getDouble(deltaDistance.getSize()-1),deltaDistance);
		
		List<EllipseParameters> ellipseParams = getParametersOfEllipses(allEllipses);
		
		//Fitting for line beam centre lie on, then beam centre
		List<double[]> mcs = new ArrayList<double[]>(ellipseParams.size());
		double[][] beamcentres = new double[2][ellipseParams.size()];
//		beamcentres.add(new double[ellipseParams.size()]);
//		beamcentres.add(new double[ellipseParams.size()]);
		
		Dataset[] allMajor = new Dataset[ellipseParams.size()];
		Dataset[] allDeltaDist= new Dataset[ellipseParams.size()];
		Dataset[] allD= new Dataset[ellipseParams.size()];
		Dataset[] allDSin= new Dataset[ellipseParams.size()];
		Dataset dSint = DatasetFactory.zeros(new int[]{ellipseParams.size()}, Dataset.FLOAT64);
		
		for (int i = 0; i<ellipseParams.size(); i++) {
			EllipseParameters params = ellipseParams.get(i);
			int size = params.xCentres.getSize();
			double approxAngle = calculateApproximateAngle(params.xCentres.getDouble(0),params.xCentres.getDouble(size-1),
					params.yCentres.getDouble(0),params.yCentres.getDouble(size-1));
			
			//Repetition here - could be made more robust?
			double[] beamC;
			if(Math.abs(approxAngle) > 85) {
				double[] mc = CentreFitter.fit(params.yCentres, params.xCentres);
				beamC = ImageFitter.fit(params.majorAxes, params.yCentres, params.xCentres, mc, pixel);
				beamcentres[1][i] = beamC[1];
				beamcentres[0][i] = mc[0]*beamC[1]+mc[1];
				mc = CentreFitter.fit(params.xCentres, params.yCentres);
				mcs.add(mc);
			} else {
				double[] mc = CentreFitter.fit(params.xCentres, params.yCentres);
				mcs.add(mc);
				beamC = ImageFitter.fit(params.majorAxes, params.xCentres, params.yCentres, mc, pixel);
				beamcentres[0][i] = beamC[1];
				beamcentres[1][i] = mc[0]*beamC[1]+mc[1];
			}
			
			allMajor[i] = ellipseParams.get(i).majorAxes;
			allD[i] = new DoubleDataset(allDSpacings.get(i), allDSpacings.get(i).length);
			allDeltaDist[i] = DatasetFactory.ones(new int[]{size},Dataset.FLOAT64).imultiply(normDist.getDouble(i));
			allDSin[i] = DatasetFactory.ones(new int[]{size},Dataset.FLOAT64).imultiply(beamC[0]);
			dSint.set(beamC[0], i);
			
		}
		
		Dataset allMajorD = DatasetUtils.concatenate(allMajor, 0);
		Dataset allNormDistD = DatasetUtils.concatenate(allDeltaDist, 0);
		Dataset allDD = DatasetUtils.concatenate(allD, 0);
		Dataset allDSinD = DatasetUtils.concatenate(allDSin, 0);
		
		double[] out;
		double wavelength;
		double dist = 0;
		double distFactor = 0;
		Dataset calculatedMajors;
		
		if (knownValue == -1) {
			Dataset xcen = new DoubleDataset(beamcentres[0].clone(), beamcentres[0].length);
			Dataset ycen = new DoubleDataset(beamcentres[1].clone(), beamcentres[1].length);
			
			Dataset hyp  = Maths.hypot(xcen.isubtract(xcen.getDouble(xcen.getSize()-1)),ycen.isubtract(ycen.getDouble(ycen.getSize()-1)));
			
			double[] mc = CentreFitter.fit(deltaDistance, hyp);
			
			distFactor = 1/Math.cos(Math.atan(Math.abs(mc[0])));
			
			out = LambdaFitter.fit(Maths.multiply(allMajorD, pixel), Maths.multiply(allNormDistD, distFactor), allDD, Maths.multiply(allDSinD, pixel), deltaDistance.getDouble(deltaDistance.getSize()-1), 0.14);
			wavelength = out[1];
			dist = out[0];
			calculatedMajors = LambdaFitter.calculateMajorAxesfinal(Maths.multiply(allNormDistD, distFactor), allDD, Maths.multiply(allDSinD, pixel), out[0], wavelength);
			calculatedMajors.imultiply(1/pixel);
			
		} else if (isWavelength){
			wavelength = knownValue;
			out = LambdaFitter.fitKnownWavelength(Maths.multiply(allMajorD, pixel), allDD, Maths.multiply(allDSinD, pixel), deltaDistance.getDouble(deltaDistance.getSize()-1), wavelength);
			dist = out[0];
			Dataset d0 = DatasetFactory.zeros(allNormDistD);
			calculatedMajors = LambdaFitter.calculateMajorAxesfinal(d0, allDD, Maths.multiply(allDSinD, pixel), dist, wavelength);
			calculatedMajors.imultiply(1/pixel);
		} else {
			dist = knownValue;
			calculatedMajors = null;
			out = LambdaFitter.fitKnownDistance(Maths.multiply(allMajorD, pixel), allDD, Maths.multiply(allDSinD, pixel), deltaDistance.getDouble(deltaDistance.getSize()-1), dist);
			wavelength = out[0];
			Dataset d0 = DatasetFactory.zeros(allNormDistD);
			calculatedMajors = LambdaFitter.calculateMajorAxesfinal(d0, allDD, Maths.multiply(allDSinD, pixel), dist, wavelength);
			calculatedMajors.imultiply(1/pixel);
		}
		
		double ssTot = (double)Maths.power(Maths.subtract(allMajorD, allMajorD.mean()),2).sum();
		double ssRes = (double)Maths.power(Maths.subtract(allMajorD, calculatedMajors),2).sum();
		double rCoeff = 1 - ssRes/ssTot;
		
		logger.debug("R2 value: " + rCoeff);
		
		Dataset tilts = getFittedTilts(dist, Maths.multiply(normDist, distFactor), dSint,pixel);
		
		Dataset distances = Maths.subtract(dist, Maths.multiply(normDist, distFactor));
		
		Dataset tiltAngles = getTiltAngles(ellipseParams, mcs);
		
		Dataset beamCentreX = new DoubleDataset(beamcentres[0], new int[]{beamcentres[0].length});
		Dataset beamCentreY = new DoubleDataset(beamcentres[1], new int[]{beamcentres[1].length});
		
		return new CalibrationOutput(wavelength, beamCentreX, beamCentreY, tilts, tiltAngles, distances,ssRes/ssTot);
	}
	
	private static double calculateApproximateAngle(double x1, double x2, double y1, double y2) {
		return Math.atan((y2-y1)/(x2-x1));
	}
	
	private static List<EllipseParameters> getParametersOfEllipses(List<List<EllipticalROI>> allEllipses) {
		List<EllipseParameters> ellipseParams = new ArrayList<EllipseParameters>(allEllipses.size());
		
		for (int i = 0; i < allEllipses.size(); i++) ellipseParams.add(getParametersOfEllipse(allEllipses.get(i)));
		
		return ellipseParams;
	}
	
	private static EllipseParameters getParametersOfEllipse(List<EllipticalROI> rois) {
		EllipseParameters els = new EllipseParameters();
		Dataset major = DatasetFactory.zeros(new int[]{rois.size()}, Dataset.FLOAT64);
		Dataset xc = DatasetFactory.zeros(new int[]{rois.size()}, Dataset.FLOAT64);
		Dataset yc = DatasetFactory.zeros(new int[]{rois.size()}, Dataset.FLOAT64);
		Dataset angle = DatasetFactory.zeros(new int[]{rois.size()}, Dataset.FLOAT64);
		
		for (int i = 0; i < rois.size();i++) {
			EllipticalROI el = rois.get(i);
			major.set(el.getSemiAxis(0), i);
			xc.set(el.getPointX(), i);
			yc.set(el.getPointY(), i);
			angle.set(el.getAngleDegrees(), i);
		}
		
		els.majorAxes = major;
		els.xCentres = xc;
		els.yCentres = yc;
		els.anglesDegrees = angle;
		
		return els;
	}
	
	private static Dataset getFittedTilts(double d0_lambda, Dataset normDist, Dataset dSint, double pixel) {
		
		Dataset var = Maths.subtract(d0_lambda, normDist);
		Dataset denom = Maths.multiply(dSint, pixel);
		var.idivide(denom);
		Dataset var2 = Maths.arcsin(var);
		var2.idivide(Math.PI);
		var2.imultiply(180);
		
		//fitted_tilt = asin((fit_D0_lambda_e(1) - para_nom_dist_diff) ./ (array_D0_sint'*pixel_dim)) /pi * 180;
		return var2;
	}
	
	private static Dataset getTiltAngles(List<EllipseParameters> ellipseParams, List<double[]> mcs) {
		
		Dataset tiltAngles = DatasetFactory.zeros(new int[]{ellipseParams.size()}, Dataset.FLOAT64);
		
		for (int i = 0; i<ellipseParams.size(); i++) {
			EllipseParameters params = ellipseParams.get(i);
			int size = params.xCentres.getSize();
			double dir = Math.signum(params.xCentres.getDouble(size-1)-params.xCentres.getDouble(0));
			
			//av_real_phi_det_centers(k) = -atan(center_mc(k,1))/pi*180 - ((xdir(k)-1)/2)*180;
			double  val =-Math.atan(mcs.get(i)[0])/Math.PI*180;
			tiltAngles.set(val - ((dir-1)/2)*180, i);
		}
		
		
		return tiltAngles;
	}
	
	private static class EllipseParameters {
		public Dataset majorAxes;
		public Dataset xCentres;
		public Dataset yCentres;
		public Dataset anglesDegrees;
	}

}
