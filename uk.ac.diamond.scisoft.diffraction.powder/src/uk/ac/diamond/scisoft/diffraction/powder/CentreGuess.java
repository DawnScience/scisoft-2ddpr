package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.Image;
import uk.ac.diamond.scisoft.analysis.dataset.Signal;
import uk.ac.diamond.scisoft.analysis.dataset.function.Downsample;
import uk.ac.diamond.scisoft.analysis.dataset.function.DownsampleMode;
import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Gaussian;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheNelderMead;

public class CentreGuess {
	
	public static double[] guessCentre(AbstractDataset image) {
		
		int downSample = 5;
		int samFW = 36;
		int samW = (samFW-1)/2;
		
		Downsample ds = new Downsample(DownsampleMode.MEAN, new int[]{downSample, downSample});
		
		AbstractDataset small = ds.value(image).get(0);
		
		small = DatasetUtils.cast(small, AbstractDataset.FLOAT64);
		
		AbstractDataset conv = Signal.convolve(small, small, new int[]{0,1});
		
		conv = Image.medianFilter(conv, new int[]{3,3});
		
		int[] maxPos = conv.maxPos();

		AbstractDataset patch = conv.getSlice(new int[] {maxPos[0] - samW-1, maxPos[1] - samW-1}, new int[] {maxPos[0] + samW, maxPos[1] + samW}, null);
		
		AbstractDataset my = patch.sum(1);
		AbstractDataset mx = patch.sum(0);
		
		my.isubtract(my.min().doubleValue());
		mx.isubtract(mx.min().doubleValue());
		
		AbstractDataset axis = AbstractDataset.arange(samFW, AbstractDataset.FLOAT64);
		
		Gaussian g = new Gaussian(samFW/2, samFW/4, my.max().doubleValue());
		double yfound;
		try {
			CompositeFunction out = Fitter.fit(axis, my, new ApacheNelderMead(), g);
			yfound = out.getFunction(0).getParameterValue(0);
		} catch (Exception e) {
			yfound = maxPos[1];
			e.printStackTrace();
		}
		
		g = new Gaussian(samFW/2, samFW/4, mx.max().doubleValue());
		double xfound;
		try {
			CompositeFunction out = Fitter.fit(axis, mx, new ApacheNelderMead(), g);
			xfound = out.getFunction(0).getParameterValue(0);
		} catch (Exception e) {
			xfound = maxPos[1];
			e.printStackTrace();
		}
		
		double convCenX = maxPos[1] - samW -1 + xfound;
		double convCenY = maxPos[0] - samW -1 + yfound;
		
		double[] coOrds = new double[2];
		
		coOrds[0] = ((convCenX +1)/2) * downSample - (downSample-1)/2;		
		coOrds[1] = ((convCenY +1)/2) * downSample - (downSample-1)/2;	
		
		return coOrds;
	}

}
