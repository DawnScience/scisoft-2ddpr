package uk.ac.diamond.scisoft.diffraction.powder;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;

/**
 * Direct conversion of I12 Matlab fit_centres.m
 * <p>
 * Used to find the equation of the line the ellipse beam centres lie along.
 * 
 */
public class CentreFitter {
	
	private static final double REL_TOL = 1e-10;
	private static final double ABS_TOL = 1e-10;
	private static final int MAX_EVAL = 100000;
	
	/**
	 * Uses a NelderMeadSimplex to fit a straight line to x,y. Initial parameters are
	 * calculated using the first and last values in the array.
	 * <p>
	 * Returns a double array containing m at 0 and c at 1.
	 * <p>
	 * @param x
	 * @param y
	 * @return mc
	 */
	public static double[] fit(final AbstractDataset x, final AbstractDataset y) {
		
		//TODO check same length
		
		int last = y.getSize()-1;
		
		double m_approx = (y.getDouble(last) - y.getDouble(0)) / (x.getDouble(last) - x.getDouble(0));
	    double c_approx = y.getDouble(last) - x.getDouble(last) * m_approx;
		
	    MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
	    
	    MultivariateFunction fun = new MultivariateFunction() {
			
			@Override
			public double value(double[] arg0) {
				
				AbstractDataset out = Maths.multiply(x, arg0[0]);
				out.iadd(arg0[1]);
				return out.residual(y);

			}
		};
	    
		PointValuePair result = opt.optimize(new InitialGuess(new double[]{m_approx,c_approx}), GoalType.MINIMIZE,
				new ObjectiveFunction(fun), new MaxEval(MAX_EVAL),
				new NelderMeadSimplex(2));	
		
		return result.getPointRef();
	}

}
