package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.QSpace;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.IPolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

public class CalibratePoints {

	private static final double REL_TOL = 1e-14;
	private static final double ABS_TOL = 1e-14;
	private static final int MAX_EVAL = 100000;
	
	public static CalibrationOutput run(List<IPolylineROI> allEllipses, double[] allDSpacings, final IDiffractionMetadata md, final CalibratePointsParameterModel paramModel) {
		
		if (allEllipses.size() < 2) throw new IllegalArgumentException("Need more than 1 ellipse");
		if (allDSpacings.length ==  0 || allEllipses.size() != allDSpacings.length) throw new IllegalArgumentException("Number of ellipses must equal number of d-spacings");
		
		
		int total = 0;
		
		for (IPolylineROI roi : allEllipses) {
			total += roi.getNumberOfPoints();
		}
		
		final DoubleDataset qd = new DoubleDataset(new int[]{total});
		final DoubleDataset xd = new DoubleDataset(new int[]{total});
		final DoubleDataset yd = new DoubleDataset(new int[]{total});
		int k = 0;
		for (int i = 0; i < allEllipses.size(); i++) {
			IPolylineROI roi = allEllipses.get(i);
			double q = (2*Math.PI)/allDSpacings[i];
			for (int j = 0; j < roi.getNumberOfPoints(); j++) {
				IROI p = roi.getPoint(j);
				xd.set(p.getPointX(), k);
				yd.set(p.getPointY(), k);
				qd.set(q, k);
				k++;
			}
		}
		
		MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
		MultivariateFunction fun = new MultivariateFunction() {

			@Override
			public double value(double[] arg0) {
				
				IDiffractionMetadata argMd = paramModel.getMetadata(arg0, md);
				
				QSpace q = new QSpace(argMd.getDetector2DProperties(), argMd.getDiffractionCrystalEnvironment());
				
				DoubleDataset qOut = new DoubleDataset(qd);
				
				for (int i = 0 ; i < qOut.getSize(); i++) {
					qOut.set(q.qFromPixelPosition(xd.getDouble(i), yd.getDouble(i)).length(), i);
				}
				
				double res = qd.residual(qOut)/qd.getSize();
				
				return res;
			}
		};
		
		double[] initParam = paramModel.getInitialParams(md);
		
		PointValuePair result = opt.optimize(new InitialGuess(initParam), GoalType.MINIMIZE,
				new ObjectiveFunction(fun), new MaxEval(MAX_EVAL),
				new NelderMeadSimplex(paramModel.getNumberOfFloatingParameters()));
		
		double[] point = result.getPointRef();
		
		IDiffractionMetadata outMd = paramModel.getMetadata(point, md);
		
		return new CalibrationOutput(outMd.getDiffractionCrystalEnvironment().getWavelength(),
									outMd.getDetector2DProperties().getBeamCentreCoords()[0],
									outMd.getDetector2DProperties().getBeamCentreCoords()[1],
									outMd.getDetector2DProperties().getNormalAnglesInDegrees()[0]*-1,
									outMd.getDetector2DProperties().getNormalAnglesInDegrees()[2]*-1,
									outMd.getDetector2DProperties().getBeamCentreDistance(),
									result.getValue());
	}
}
