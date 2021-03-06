package uk.ac.diamond.scisoft.diffraction.powder.test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.Maths;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.PixelIntegrationUtils;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;


@RunWith(Parameterized.class)
public class SimulatedDataTest {
	
	double distance;
	double bx;
	double by;
	double yaw;
	double roll;
	double wavelength;
	
	public SimulatedDataTest(double distance, double bx, double by, double yaw, double roll, double wavelength) {
		this.distance = distance;
		this.bx=bx;
		this.by = by;
		this.yaw = yaw;
		this.roll = roll;
		this.wavelength = wavelength;
	}
	
	@Parameterized.Parameters
	   public static Collection<?> params() {
	      return Arrays.asList(new Object[][] {
	         { 300,999,1001,3,45,1}
	         
	      });
	   }

	
	@Test
	public void fixedWavelength(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
		DiffractionCrystalEnvironment ce = meta.getDiffractionCrystalEnvironment();
		
		Dataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		Dataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		Dataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, ce.getWavelength(),
				dp.getHPxSize(), ceO2.getHKLs(),10);
		
		Assert.assertEquals(distance, result.getDistance().getDouble(0), 0.1);
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.1);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.1);
		
	}
	
	@Test
	public void fixedWavelengthCheckWavelength(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
		DiffractionCrystalEnvironment ce = meta.getDiffractionCrystalEnvironment();
		
		Dataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		Dataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		Dataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		ce.setWavelength(1.012345);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		double w = ce.getWavelength();
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, ce.getWavelength(),
				dp.getHPxSize(), ceO2.getHKLs(),10);
		
		
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.1);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.1);
		Assert.assertEquals(1.012345, result.getWavelength(), 0.000001);
		
	}
	
	@Test
	public void floatAll(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
//		DiffractionCrystalEnvironment ce = meta.getDiffractionCrystalEnvironment();
		
		Dataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		Dataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		Dataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateSingleImage(image,
				dp.getHPxSize(), ceO2.getHKLs(),10);
		
		Assert.assertEquals(distance, result.getDistance().getDouble(0), 0.1);
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.1);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.1);
		Assert.assertEquals(wavelength, result.getWavelength(), 0.0001);
		
	}
	
	@Test
	public void manualPointTest(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
		
		Dataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		Dataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		Dataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		
		IDiffractionMetadata m = meta.clone();
		
		m.getDetector2DProperties().setBeamCentreDistance(m.getDetector2DProperties().getBeamCentreDistance()*1.005);
		
		
		CalibrationOutput result = PowderCalibration.calibrateSingleImageManualPoint(image, ceO2.getHKLs(),10,m,false);
		
		Assert.assertEquals(distance, result.getDistance().getDouble(0), 0.1);
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.1);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.1);
		Assert.assertEquals(wavelength, result.getWavelength(), 0.0001);
		
		double w = m.getDiffractionCrystalEnvironment().getWavelength();
		w *= 1.01;
		m.getDiffractionCrystalEnvironment().setWavelength(w);
		
		result = PowderCalibration.calibrateSingleImageManualPoint(image, ceO2.getHKLs(),10,m,true);
		
		Assert.assertEquals(w, result.getWavelength(), 0.0001);
		Assert.assertNotEquals(distance, result.getDistance().getDouble(0), 0.1);
		
	}
	
	private IDiffractionMetadata getPerkinElmerDiffractionMetadata() {
		
		DetectorProperties dp = new DetectorProperties(100, 0, 0, 2048, 2048, 0.2, 0.2);
		DiffractionCrystalEnvironment ce = new DiffractionCrystalEnvironment(wavelength);
		
		dp.setBeamCentreCoords(new double[]{bx,by});
		dp.setBeamCentreDistance(distance);
		dp.setNormalAnglesInDegrees(yaw, 0, roll);
		
		return new DiffractionMetadata("test",dp, ce);
	}
	
	private Dataset[] getSimulated1D() {
		final File file = new File("testfiles/Sim_CeO2_SRM674b.dat");
		Dataset x;
		Dataset y;
		try {
			x = DatasetUtils.convertToDataset(LoaderFactory.getDataSet(file.getAbsolutePath(),"Column_1",null));
			y = DatasetUtils.convertToDataset(LoaderFactory.getDataSet(file.getAbsolutePath(),"Column_2",null));
		} catch (Exception e) {
			return null;
		}
		
		return new Dataset[]{x,Maths.add(y,100)};
	}
	
}
