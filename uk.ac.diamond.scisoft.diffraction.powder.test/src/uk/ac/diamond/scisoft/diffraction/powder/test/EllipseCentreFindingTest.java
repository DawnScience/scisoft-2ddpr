package uk.ac.diamond.scisoft.diffraction.powder.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CentreGuess;

public class EllipseCentreFindingTest {
	
	@Test
	public void CentreFitterTestRun(){
		
		AbstractDataset image;
		try {
			image = (AbstractDataset)LoaderFactory.getDataSet(getTestFilePath("centre_pixi_00001.tif"),"image-01",null);
			double[] centre = CentreGuess.guessCentre(image);
			
			double[] expected = new double[]{ 1387.1, 1361.5};
			
			Assert.assertArrayEquals(expected, centre, 2);
			
			for (int i = 0; i < 10; i++) {
				image = (AbstractDataset)LoaderFactory.getDataSet(getTestFilePath("centre_pixi_00001.tif"),"image-01",null);
				double[] centre2 = CentreGuess.guessCentre(image);
				
				Assert.assertArrayEquals(centre, centre2, 0.001);
			}
			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		
	}
	
	private String getTestFilePath(String fileName) {
		final File test = new File("testfiles/"+fileName);
		return test.getAbsolutePath();
	}
}
