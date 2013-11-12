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
 * Direct conversion of I12 Matlab fit_DO_lambda.m
 * <p>
 * Used to find the distance from the sample to the beam centre on the detector, and the wavelength if unknown.
 */
public class LambdaFitter {

	private static final double REL_TOL = 1e-10;
	private static final double ABS_TOL = 1e-10;
	private static final int MAX_EVAL = 100000;
	
	/**
	 * Uses a NelderMeadSimplex to fit calculated major axis values against measured, to determine distance
	 * and wavelength.
	 * <p>
	 * Returns a double array containing distance at 0 and wavelength in angstroms at [1]
	 * <p>
	 * @param major
	 * @param distance
	 * @param dspace
	 * @param sint
	 * @param dApprox
	 * @param lApprox
	 * @return distance
	 */
	public static double[] fit(final AbstractDataset major, final AbstractDataset distance, final AbstractDataset dspace,  final AbstractDataset sint, final double dApprox, final double lApprox) {

		MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
		MultivariateFunction fun = new MultivariateFunction() {

			@Override
			public double value(double[] arg0) {

				return calculateResidual(major, distance, dspace, sint, arg0[0], arg0[1]);
			}
		};

		PointValuePair result = opt.optimize(new InitialGuess(new double[]{dApprox,lApprox}), GoalType.MINIMIZE,
				new ObjectiveFunction(fun), new MaxEval(MAX_EVAL),
				new NelderMeadSimplex(2));	

		return result.getPointRef();
	}
	
	/**
	 * Uses a NelderMeadSimplex to fit calculated major axis values against measured, to determine distance
	 * when wavelength is known.
	 * <p>
	 * Returns a double array containing distance
	 * <p>
	 * @param major
	 * @param dspace
	 * @param sint
	 * @param dApprox
	 * @param wavelength
	 * @return distance
	 */
	public static double[] fit(final AbstractDataset major, final AbstractDataset dspace,  final AbstractDataset sint, final double dApprox, final double wavelength) {
	
		final AbstractDataset distance = AbstractDataset.zeros(major);
		
		MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
		MultivariateFunction fun = new MultivariateFunction() {

			@Override
			public double value(double[] arg0) {

				return calculateResidual(major, distance, dspace, sint, arg0[0], wavelength);
			}
		};

		PointValuePair result = opt.optimize(new InitialGuess(new double[]{dApprox}), GoalType.MINIMIZE,
				new ObjectiveFunction(fun), new MaxEval(MAX_EVAL),
				new NelderMeadSimplex(1));	

		return result.getPointRef();
		
	}
	
	private static double calculateResidual(final AbstractDataset major, final AbstractDataset distance, final AbstractDataset dspace,  final AbstractDataset sint, final double dApprox, final double lApprox) {
		
		AbstractDataset calcMajor= calculateMajorAxesfinal(distance, dspace, sint, dApprox, lApprox);
		return major.residual(calcMajor);
	}
	
	public static AbstractDataset calculateMajorAxesfinal(final AbstractDataset distance, final AbstractDataset dspace,  final AbstractDataset sint, final double d0, final double wavelength) {
		AbstractDataset ddif = Maths.subtract(d0, distance);
		AbstractDataset ld = Maths.multiply(dspace, 2);
		ld = Maths.divide(wavelength, ld);
		ld = Maths.arcsin(ld);
		ld.imultiply(2);
		ld = Maths.tan(ld);// tan(2 * asin(lambda./(2 * ddata)))
		
		AbstractDataset var = Maths.divide(ddif, sint); //((D0 - deltaDdata) ./ D0_sint_data).^2
		var.ipower(2);

		AbstractDataset denom = Maths.power(ld, 2);
		denom.iadd(1);
		denom.imultiply(var);
		denom = Maths.subtract(1, denom);
		
		AbstractDataset numer = Maths.subtract(1, var);
		numer.ipower(0.5);
		numer.imultiply(ld);
		numer.imultiply(ddif);
		
		return numer;
	}
}
