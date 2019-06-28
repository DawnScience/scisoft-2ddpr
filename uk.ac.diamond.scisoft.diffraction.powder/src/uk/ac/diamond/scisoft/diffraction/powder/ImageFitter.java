package uk.ac.diamond.scisoft.diffraction.powder;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.Maths;

/**
 * Direct conversion of I12 Matlab fit_image8.m
 * <p>
 * Used to find the beam centre from the ellipse major axis length and centre co-ordinates (equations 14 and 16
 * in J Appl Cryst (2013) 46 1249)
 */
public class ImageFitter {
	
	private static final double REL_TOL = 1e-21;
	private static final double ABS_TOL = 1e-21;
	private static final int MAX_EVAL = 100000;
	
	/**
	 * Uses a NelderMeadSimplex with bounds on the beam centre position. Best initial parameters are
	 * calculated fitting a range of x-centre values.
	 * <p>
	 * Returns a double array containing the x and y positions, the x used to calculate y using the equation fitted earlier
	 * <p>
	 * @param major
	 * @param x
	 * @param y
	 * @param line
	 * @param pixel
	 * @return mc
	 */
	public static double[] fit(final Dataset major, final Dataset x, final Dataset y, final double[] line, double pixel){

		//TODO check same length
		int last = major.getSize()-1;
		double xRange = Math.abs(x.getDouble(last)-x.getDouble(0))/10;
		
		if (xRange == 0) {
			xRange = Math.abs(x.max().doubleValue() - x.min().doubleValue())/10;
		}
		
		double xDir = Math.signum(x.getDouble(last)-x.getDouble(0));
		
		if (xDir == 0) {
			Math.signum(x.getDouble(last)-x.getDouble(last-1)-x.getDouble(0)-x.getDouble(1));
		}
		
		double xApprox = x.getDouble(0);
		
		Dataset xApproxGuess = DatasetFactory.createRange(xApprox, xApprox-(xDir*xRange), -xDir*xRange/100);
		
		MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
		MultivariateFunction fun = new MultivariateFunction() {

			@Override
			public double value(double[] arg0) {
				
				Dataset sqrtxval =  calculateLine(x, y, line, arg0);
				double res =  major.residual(sqrtxval);
				return res;
			}
		};
		
		Dataset errors = DatasetFactory.zeros(xApproxGuess);
		errors.iadd(Double.MAX_VALUE);
		PointValuePair result;
		double offset = 1e12;
		double[] scale = new double[]{offset*0.25,offset*0.25};
		for (int i = 0; i < xApproxGuess.count(); i++) {
			
			double[] initParam = new double[]{Math.pow(major.getDouble(last),2),xApproxGuess.getDouble(i)};
			
			try {
				
				if (x.getDouble(0) < x.getDouble(last)) {
					double[] lowerb = new double[]{Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
					double[] upperb = new double[]{Double.POSITIVE_INFINITY,x.getDouble(0)};
					MultivariateFunctionPenaltyAdapter of = new MultivariateFunctionPenaltyAdapter(fun, lowerb, upperb, offset, scale);
					
					result = opt.optimize(new InitialGuess(initParam), GoalType.MINIMIZE,
							new ObjectiveFunction(of), new MaxEval(MAX_EVAL),
							new NelderMeadSimplex(2));	
				} else {
					double[] lowerb = new double[]{Double.NEGATIVE_INFINITY,x.getDouble(0)};
					double[] upperb = new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
					MultivariateFunctionPenaltyAdapter of = new MultivariateFunctionPenaltyAdapter(fun, lowerb, upperb, offset, scale);
					
					result = opt.optimize(new InitialGuess(initParam), GoalType.MINIMIZE,
							new ObjectiveFunction(of), new MaxEval(MAX_EVAL),
							new NelderMeadSimplex(2));	
				}
				
				double[] estimates = result.getPointRef();
				Dataset lineData = calculateLine(x, y, line, estimates);
				errors.set(major.residual(lineData), i);
				
			} catch (Exception e) {
				//to log
			}
		}
		
		double[] initParam = new double[]{Math.pow(major.getDouble(last),2),xApproxGuess.getDouble(errors.minPos())};
		
		if (x.getDouble(0) < x.getDouble(last)) {
			double[] lowerb = new double[]{Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
			double[] upperb = new double[]{Double.POSITIVE_INFINITY,x.getDouble(0)};
			MultivariateFunctionPenaltyAdapter of = new MultivariateFunctionPenaltyAdapter(fun, lowerb, upperb, offset, scale);
			
			result = opt.optimize(new InitialGuess(initParam), GoalType.MINIMIZE,
					new ObjectiveFunction(of), new MaxEval(MAX_EVAL),
					new NelderMeadSimplex(2));	
		} else {
			double[] lowerb = new double[]{Double.NEGATIVE_INFINITY,x.getDouble(0)};
			double[] upperb = new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
			MultivariateFunctionPenaltyAdapter of = new MultivariateFunctionPenaltyAdapter(fun, lowerb, upperb, offset, scale);
			
			result = opt.optimize(new InitialGuess(initParam), GoalType.MINIMIZE,
					new ObjectiveFunction(of), new MaxEval(MAX_EVAL),
					new NelderMeadSimplex(2));	
		}
		
		return result.getPointRef();
	}
	
	private static Dataset calculateLine(Dataset x, Dataset y, double[] line, double[] arg0) {
		Dataset xval = Maths.subtract(x, arg0[1]);
		xval.ipower(2);
		double lval = line[0]* arg0[1]+line[1];
		
		Dataset yval = Maths.subtract(y, lval);
		yval.ipower(2);
		
		xval.iadd(yval);
		
		Dataset sqrtxval = Maths.power(xval, 0.5);
		
		sqrtxval.imultiply(arg0[0]);
		sqrtxval.iadd(xval);
		sqrtxval.ipower(0.5);
		
		return sqrtxval;
	}
	
}
