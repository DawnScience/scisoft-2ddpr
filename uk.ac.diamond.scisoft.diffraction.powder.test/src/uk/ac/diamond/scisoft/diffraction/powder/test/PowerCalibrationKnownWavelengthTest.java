package uk.ac.diamond.scisoft.diffraction.powder.test;

import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;



public class PowerCalibrationKnownWavelengthTest {
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO212kevPilatus170(){
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_170.cbf");

		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.97949, 0.172, ceO2.getHKLs(),10);
		
		Assert.assertEquals(169.5, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1210, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1219.3, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO212kevPilatus370(){
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_370.cbf");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.97949, 0.172, ceO2.getHKLs(),10);
		
		Assert.assertEquals(369.52, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1214, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1212.3, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO216kevPilatus170(){
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_16kev_170.cbf");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.7749, 0.172, ceO2.getHKLs(),10);
		
		Assert.assertEquals(169.5, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1211.19, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1219.15, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO216kevPilatus370(){
		//TODO needs ring rejection from fit to work
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_16kev_370.cbf");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.7749, 0.172, ceO2.getHKLs(),10);
		
		Assert.assertEquals(369.52, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1215.38, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1211.94, result.getBeamCentreY().getDouble(0), 0.9);
	}

	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO229kevPerkin259(){
		//TODO needs ring rejection from fit to work
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d259-00017.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, ceO2.getHKLs(),10);
		
		Assert.assertEquals(289, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1050.7, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1016.7, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO229kevPerkin659(){
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d659-00013.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, ceO2.getHKLs(),10);
		
		Assert.assertEquals(689, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1048, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1015, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
//	@Test
//	public void LaB6229kevPerkin259(){
//		//TODO needs ring rejection from fit to work
//		try {
//			Dataset image = (Dataset)LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d259-00018.tif","image-01",null);
//			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
//			CalibrantSpacing std = standards.getCalibrationPeakMap("LaB6");
//			CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, std.getHKLs());
//			
//			Assert.assertEquals(290, result.getDistance().getDouble(0), 0.9);
//			Assert.assertEquals(1055, result.getBeamCentreX().getDouble(0), 0.9);
//			Assert.assertEquals(1017, result.getBeamCentreY().getDouble(0), 0.9);
//		
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void LaB629kevPerkin359(){
		//TODO needs ring rejection from fit to work
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d359-00021.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing std = standards.getCalibrationPeakMap("LaB6");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, std.getHKLs(),10);
		
		Assert.assertEquals(389.5, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1050, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1016, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void LaB629kevPerkin659(){
		
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d659-00024.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing std = standards.getCalibrationPeakMap("LaB6");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, std.getHKLs(),10);
		
		Assert.assertEquals(689, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1048, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1016, result.getBeamCentreY().getDouble(0), 0.9);

	}
	
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO2Pixium788(){
		//TODO needs ring rejection from fit to work
		Dataset image = getImage("/dls/science/groups/das/ExampleData/powder/ceopowder.tiff");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.2068, 0.148, ceO2.getHKLs(),10);
		
		Assert.assertEquals(788.8, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1441, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1442.8, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO2Pixium289(){
		//TODO needs ring rejection from fit to work
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00000.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}

		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, ceO2.getHKLs(),10);
		
		Assert.assertEquals(289, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1050.7, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1016.7, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void CeO2Pixium689(){
		//TODO needs ring rejection from fit to work
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00004.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}

		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, 0.425, 0.2, ceO2.getHKLs(),10);
		
		Assert.assertEquals(689, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(1048, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(1015.7, result.getBeamCentreY().getDouble(0), 0.9);
	}
	
	private Dataset getImage(String path) {
		
		Dataset image = null;
		
		try {
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet(path,"image-01",null));
		} catch (Exception e) {
			//fail silently, just return null to show it didnt work
		}
		
		return image;
	}
}
