package uk.ac.diamond.scisoft.diffraction.powder.test;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.BruteStandardMatcher;

public class BruteMatcherTest {

	@Test
	public void BruteMatcherTestRun(){
		
		double[] dSpace = new double[] {3.124417584000000e+00,
				2.705825000000000e+00,
				1.913307206000000e+00,
				1.631673868000000e+00,
				1.562,
				1.353,
				1.241,
				1.21,
				1.105,
				1.104648431000000e+00,
				1.041472528000000e+00,
				0.957};
		
		AbstractDataset x;
		AbstractDataset trace;
		try {
			x = (AbstractDataset)LoaderFactory.getDataSet(getTestFilePath("TestTrace.dat"),"x",null);
			trace = (AbstractDataset)LoaderFactory.getDataSet(getTestFilePath("TestTrace.dat"),"dataset_0",null);
			
			Map<Double,Double> out = BruteStandardMatcher.bruteForceMatchStandards(x, trace, dSpace, 0.148);
			Assert.assertEquals(197.5, out.get(dSpace[0]), 5);

			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		
	}
	
	private String getTestFilePath(String fileName) {
		final File test = new File("testfiles/"+fileName);
		return test.getAbsolutePath();
	}
	
	
}
