package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.List;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;

public class CalibrateEllipses {
	
	public static CalibrationOutput run(List<List<EllipticalROI>> allEllipses, List<double[]> allDSpacings, AbstractDataset deltaDistance,double pixel){
		
		//double w = 2048, h = 2048;
		//double pixel = 0.2;
		AbstractDataset normDist = Maths.subtract(deltaDistance.getDouble(deltaDistance.getSize()-1),deltaDistance);
		
		List<EllipseParameters> ellipseParams = getParametersOfEllipses(allEllipses);
		
		//Fitting for line beam centre lie on, then beam centre
		List<double[]> mcs = new ArrayList<double[]>(ellipseParams.size());
		double[][] beamcentres = new double[2][ellipseParams.size()];
//		beamcentres.add(new double[ellipseParams.size()]);
//		beamcentres.add(new double[ellipseParams.size()]);
		
		AbstractDataset[] allMajor = new AbstractDataset[ellipseParams.size()];
		AbstractDataset[] allDeltaDist= new AbstractDataset[ellipseParams.size()];
		AbstractDataset[] allD= new AbstractDataset[ellipseParams.size()];
		AbstractDataset[] allDSin= new AbstractDataset[ellipseParams.size()];
		AbstractDataset dSint = AbstractDataset.zeros(new int[]{ellipseParams.size()}, AbstractDataset.FLOAT64);
		
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
			allDeltaDist[i] = AbstractDataset.ones(new int[]{size},AbstractDataset.FLOAT64).imultiply(normDist.getDouble(i));
			allDSin[i] = AbstractDataset.ones(new int[]{size},AbstractDataset.FLOAT64).imultiply(beamC[0]);
			dSint.set(beamC[0], i);
			
		}
		
		AbstractDataset allMajorD = DatasetUtils.concatenate(allMajor, 0);
		AbstractDataset allNormDistD = DatasetUtils.concatenate(allDeltaDist, 0);
		AbstractDataset allDD = DatasetUtils.concatenate(allD, 0);
		AbstractDataset allDSinD = DatasetUtils.concatenate(allDSin, 0);
		
		AbstractDataset xcen = new DoubleDataset(beamcentres[0].clone(), beamcentres[0].length);
		AbstractDataset ycen = new DoubleDataset(beamcentres[1].clone(), beamcentres[1].length);
		
		AbstractDataset hyp  = Maths.hypot(xcen.isubtract(xcen.getDouble(xcen.getSize()-1)),ycen.isubtract(ycen.getDouble(ycen.getSize()-1)));
		
		double[] mc = CentreFitter.fit(deltaDistance, hyp);
		
		double distFactor = 1/Math.cos(Math.atan(Math.abs(mc[0])));
		
		double[] out = LambdaFitter.fit(Maths.multiply(allMajorD, pixel), Maths.multiply(allNormDistD, distFactor), allDD, Maths.multiply(allDSinD, pixel), deltaDistance.getDouble(deltaDistance.getSize()-1), 0.14);
		
		AbstractDataset tilts = getFittedTilts(out[0], Maths.multiply(normDist, distFactor), dSint,pixel);
		
		AbstractDataset distances = Maths.subtract(out[0], Maths.multiply(normDist, distFactor));
		
		double wavelength = out[1] / 10;
		
		AbstractDataset tiltAngles = getTiltAngles(ellipseParams, mcs);
		
		AbstractDataset beamCentreX = new DoubleDataset(beamcentres[0], new int[]{beamcentres[0].length});
		AbstractDataset beamCentreY = new DoubleDataset(beamcentres[1], new int[]{beamcentres[1].length});
		
		return new CalibrationOutput(wavelength, beamCentreX, beamCentreY, tilts, tiltAngles, distances);
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
		AbstractDataset major = AbstractDataset.zeros(new int[]{rois.size()}, AbstractDataset.FLOAT64);
		AbstractDataset xc = AbstractDataset.zeros(new int[]{rois.size()}, AbstractDataset.FLOAT64);
		AbstractDataset yc = AbstractDataset.zeros(new int[]{rois.size()}, AbstractDataset.FLOAT64);
		AbstractDataset angle = AbstractDataset.zeros(new int[]{rois.size()}, AbstractDataset.FLOAT64);
		
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
	
	private static AbstractDataset getFittedTilts(double d0_lambda, AbstractDataset normDist, AbstractDataset dSint, double pixel) {
		
		AbstractDataset var = Maths.subtract(d0_lambda, normDist);
		AbstractDataset denom = Maths.multiply(dSint, pixel);
		var.idivide(denom);
		AbstractDataset var2 = Maths.arcsin(var);
		var2.idivide(Math.PI);
		var2.imultiply(180);
		
		//fitted_tilt = asin((fit_D0_lambda_e(1) - para_nom_dist_diff) ./ (array_D0_sint'*pixel_dim)) /pi * 180;
		return var2;
	}
	
	private static AbstractDataset getTiltAngles(List<EllipseParameters> ellipseParams, List<double[]> mcs) {
		
		AbstractDataset tiltAngles = AbstractDataset.zeros(new int[]{ellipseParams.size()}, AbstractDataset.FLOAT64);
		
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
		public AbstractDataset majorAxes;
		public AbstractDataset xCentres;
		public AbstractDataset yCentres;
		public AbstractDataset anglesDegrees;
	}

}
