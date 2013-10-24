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

public class LambdaFitter {

	private static final double REL_TOL = 1e-10;
	private static final double ABS_TOL = 1e-10;
	private static final int MAX_EVAL = 100000;
	
	public static double[] fit(final AbstractDataset major, final AbstractDataset distance, final AbstractDataset dspace,  final AbstractDataset sint, final double dApprox, final double lApprox) {

		MultivariateOptimizer opt = new SimplexOptimizer(REL_TOL,ABS_TOL);
		MultivariateFunction fun = new MultivariateFunction() {

			@Override
			public double value(double[] arg0) {

				AbstractDataset ddif = Maths.subtract(arg0[0], distance);
				AbstractDataset ld = Maths.multiply(dspace, 2);
				ld = Maths.divide(arg0[1], ld);
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

				return major.residual(numer);
				
//				D0 = params(1);
//		        lambda = params(2);
//		        
//		        
//		        FittedLine = ((D0 - deltaDdata) .* tan(2 * asin(lambda./(2 * ddata))) .* (1 - ((D0 - deltaDdata) ./ D0_sint_data).^2).^0.5) ./ (1 - ((D0 - deltaDdata) ./ D0_sint_data).^2 .* (1 + tan(2 * asin(lambda./(2 * ddata))).^2));
//		        
//		        ErrorVector = FittedLine - adata;
//		        sse = sum(ErrorVector .^ 2);

			}
		};

		PointValuePair result = opt.optimize(new InitialGuess(new double[]{dApprox,lApprox}), GoalType.MINIMIZE,
				new ObjectiveFunction(fun), new MaxEval(MAX_EVAL),
				new NelderMeadSimplex(2));	

		return result.getPointRef();
	}
}
