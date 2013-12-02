package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.Image;
import uk.ac.diamond.scisoft.analysis.dataset.Signal;
import uk.ac.diamond.scisoft.analysis.dataset.function.Downsample;
import uk.ac.diamond.scisoft.analysis.dataset.function.DownsampleMode;

public class CentreGuess {
	
	public static double[] guessCentre(AbstractDataset image) {
		
		int downSample = 5;
		//int samFW = 9;
		//int samW = (samFW-1)/2;
		
		Downsample ds = new Downsample(DownsampleMode.MEAN, new int[]{downSample, downSample});
		
		AbstractDataset small = ds.value(image).get(0);
		//AbstractDataset small = image.getSlice(null,new int[] {h,w},new int[] {5,5});
		
		small = DatasetUtils.cast(small, AbstractDataset.FLOAT64);
		
		AbstractDataset conv = Signal.convolve(small, small, new int[]{0,1});
		
		conv = Image.medianFilter(conv, new int[]{3,3});
		
		int[] maxPos = conv.maxPos();

		//TODO robustly fit position - fitter doesnt seem to do this
//		AbstractDataset patch = conv.getSlice(new int[] {maxPos[0] - samW-1, maxPos[1] - samW-1}, new int[] {maxPos[0] + samW, maxPos[1] + samW}, null);
//		
//		int[] shape = patch.getShape();
		
		//NDGaussianFitResult res = Fitter.NDGaussianSimpleFit(patch, AbstractDataset.arange(shape[0], AbstractDataset.FLOAT64),AbstractDataset.arange(shape[1], AbstractDataset.FLOAT64));
		//logger.info("max centre, x: " + res.getPos()[0] +" y: " + res.getPos()[1]);
		
//		AbstractDataset my = patch.sum(1);
//		AbstractDataset mx = patch.sum(0);
//		
//		AbstractDataset axis = AbstractDataset.arange(samFW, AbstractDataset.FLOAT64);
		
//		Gaussian g = new Gaussian(samFW/2, samFW/2, 1000);
//		Offset off = new Offset(new double[]{my.min(true).doubleValue()});
//		CompositeFunction func = new CompositeFunction();
//		func.addFunction(g);
//		func.addFunction(off);
//		
//		try {
//			CompositeFunction out = Fitter.fit(axis, my, new ApacheNelderMead(), func);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		AFunction yGauss = Fitter.GaussianFit(my, AbstractDataset.arange(samFW, AbstractDataset.FLOAT64));
//		AFunction xGauss = Fitter.GaussianFit(mx, AbstractDataset.arange(samFW, AbstractDataset.FLOAT64));
//		
//		double convCenX = maxPos[1] - samW -1 + xGauss.getParameter(0).getValue();
//		double convCenY = maxPos[0] - samW -1 + yGauss.getParameter(0).getValue();
		
//		double convCenX = maxPos[1] - samW -1 + res.getPos()[1];
//		double convCenY = maxPos[0] - samW -1 + res.getPos()[0];
		
		double convCenX = maxPos[1];
		double convCenY = maxPos[0];
		
		double[] coOrds = new double[2];
		
		coOrds[0] = ((convCenX +1)/2) * downSample - (downSample-1)/2;		
		coOrds[1] = ((convCenY +1)/2) * downSample - (downSample-1)/2;	
		
		return coOrds;
	}

}
