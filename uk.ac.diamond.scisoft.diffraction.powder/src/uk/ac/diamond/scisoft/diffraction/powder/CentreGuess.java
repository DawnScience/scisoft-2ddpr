package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.Signal;
import uk.ac.diamond.scisoft.analysis.dataset.function.Downsample;
import uk.ac.diamond.scisoft.analysis.dataset.function.DownsampleMode;
import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;

public class CentreGuess {
	
	public static double[] guessCentre(AbstractDataset image) {
		
		int downSample = 5;
		int samFW = 9;
		int samW = (samFW-1)/2;
		
		Downsample ds = new Downsample(DownsampleMode.MEAN, new int[]{downSample, downSample});
		
		AbstractDataset small = ds.value(image).get(0);
		//AbstractDataset small = image.getSlice(null,new int[] {h,w},new int[] {5,5});
		
		small = DatasetUtils.cast(small, AbstractDataset.FLOAT64);
		
		AbstractDataset conv = Signal.convolve(small, small, new int[]{0,1});
		
		int[] maxPos = conv.maxPos();
		
		AbstractDataset patch = conv.getSlice(new int[] {maxPos[0] - samW-1, maxPos[1] - samW-1}, new int[] {maxPos[0] + samW, maxPos[1] + samW}, null);
		
		AbstractDataset my = patch.mean(1);
		AbstractDataset mx = patch.mean(0);
		
		AFunction yGauss = Fitter.GaussianFit(my, AbstractDataset.arange(samFW, AbstractDataset.FLOAT64));
		AFunction xGauss = Fitter.GaussianFit(mx, AbstractDataset.arange(samFW, AbstractDataset.FLOAT64));
		
		
		double convCenX = maxPos[1] - samW -1 + xGauss.getParameter(0).getValue();
		double convCenY = maxPos[0] - samW -1 + yGauss.getParameter(0).getValue();
		
		double[] coOrds = new double[2];
		
		coOrds[0] = ((convCenX +1)/2) * downSample - (downSample-1)/2;		
		coOrds[1] = ((convCenY +1)/2) * downSample - (downSample-1)/2;	
		
		return coOrds;
	}

}
