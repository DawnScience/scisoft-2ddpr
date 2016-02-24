package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.downsample.DownsampleMode;
import org.eclipse.dawnsci.analysis.dataset.function.Downsample;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.analysis.dataset.impl.Image;
import org.eclipse.dawnsci.analysis.dataset.impl.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Gaussian;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheOptimizer;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheOptimizer.Optimizer;

public class CentreGuess {
	
	private static Logger logger = LoggerFactory.getLogger(CentreGuess.class);
	
	public static double[] guessCentre(Dataset image) {
		
		int downSample = 5;
		int samFW = 18;
		int samW = (samFW)/2;
		
		Downsample ds = new Downsample(DownsampleMode.MEAN, new int[]{downSample, downSample});
		
		Dataset small = ds.value(image).get(0);
		
		small = DatasetUtils.cast(small, Dataset.FLOAT64);
		
		Dataset conv = Signal.convolve(small, small, new int[]{0,1});
		
		conv = Image.medianFilter(conv, new int[]{3,3});
		
		int[] maxPos = conv.maxPos();

		Dataset patch = conv.getSlice(new int[] {maxPos[0] - samW, maxPos[1] - samW}, new int[] {maxPos[0] + samW, maxPos[1] + samW}, null);
		
		Dataset my = patch.sum(1);
		Dataset mx = patch.sum(0);
		
		my.isubtract(my.min().doubleValue());
		mx.isubtract(mx.min().doubleValue());
		
		Dataset axis = DatasetFactory.createRange(samFW, Dataset.FLOAT64);
		
		Gaussian g = new Gaussian(samFW/2, samFW/4, my.max().doubleValue());
		double yfound;
		try {
			CompositeFunction out = Fitter.fit(axis, my, new ApacheOptimizer(Optimizer.SIMPLEX_MD), g);
			yfound = out.getFunction(0).getParameterValue(0);
		} catch (Exception e) {
			yfound = my.maxPos()[0];
			logger.warn("y fit failed");
		}
		
		g = new Gaussian(samFW/2, samFW/4, mx.max().doubleValue());
		double xfound;
		try {
			CompositeFunction out = Fitter.fit(axis, mx, new ApacheOptimizer(Optimizer.SIMPLEX_MD), g);
			xfound = out.getFunction(0).getParameterValue(0);
		} catch (Exception e) {
			xfound = mx.maxPos()[0];
			logger.warn("x fit failed");
		}
		
		double convCenX = maxPos[1] - samW + xfound;
		double convCenY = maxPos[0] - samW + yfound;
		
		double[] coOrds = new double[2];
		
		coOrds[0] = ((convCenX +1)/2) * downSample - (downSample-1)/2;		
		coOrds[1] = ((convCenY +1)/2) * downSample - (downSample-1)/2;	
		
		return coOrds;
	}

}
