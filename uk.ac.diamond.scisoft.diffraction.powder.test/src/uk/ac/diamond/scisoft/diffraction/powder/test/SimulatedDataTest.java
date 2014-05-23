package uk.ac.diamond.scisoft.diffraction.powder.test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.PixelIntegrationUtils;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
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
	   public static Collection params() {
	      return Arrays.asList(new Object[][] {
	         { 300,999,1001,3,45,1}
	         
	      });
	   }

	
	@Test
	public void fixedWavelength(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
		DiffractionCrystalEnvironment ce = meta.getDiffractionCrystalEnvironment();
		
		AbstractDataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		AbstractDataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		AbstractDataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateKnownWavelength(image, ce.getWavelength(),
				dp.getHPxSize(), ceO2.getHKLs(),10);
		
		Assert.assertEquals(distance, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.9);
		
	}
	
	@Test
	public void floatAll(){

		IDiffractionMetadata meta = getPerkinElmerDiffractionMetadata();
		DetectorProperties dp = meta.getDetector2DProperties();
		DiffractionCrystalEnvironment ce = meta.getDiffractionCrystalEnvironment();
		
		AbstractDataset[] s1d = getSimulated1D();
		int[] shape = new int[]{dp.getPy(),dp.getPx()};
		
		AbstractDataset q2D = PixelIntegrationUtils.generateQArray(shape, meta); 
		AbstractDataset image = PixelIntegrationUtils.generate2Dfrom1D(s1d, q2D);
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
		CalibrationOutput result = PowderCalibration.calibrateSingleImage(image,
				dp.getHPxSize(), ceO2.getHKLs(),10);
		
		Assert.assertEquals(distance, result.getDistance().getDouble(0), 0.9);
		Assert.assertEquals(bx, result.getBeamCentreX().getDouble(0), 0.9);
		Assert.assertEquals(by, result.getBeamCentreY().getDouble(0), 0.9);
		Assert.assertEquals(wavelength, result.getWavelength(), 0.0001);
		
	}
	
	private IDiffractionMetadata getPerkinElmerDiffractionMetadata() {
		
		DetectorProperties dp = new DetectorProperties(100, 0, 0, 2048, 2048, 0.2, 0.2);
		DiffractionCrystalEnvironment ce = new DiffractionCrystalEnvironment(wavelength);
		
		dp.setBeamCentreCoords(new double[]{bx,by});
		dp.setDetectorDistance(distance);
		dp.setNormalAnglesInDegrees(yaw, 0, roll);
		
		return new DiffractionMetadata("test",dp, ce);
	}
	
	private AbstractDataset[] getSimulated1D() {
		final File file = new File("testfiles/Sim_CeO2_SRM674b.dat");
		AbstractDataset x;
		AbstractDataset y;
		try {
			x = (AbstractDataset)LoaderFactory.getDataSet(file.getAbsolutePath(),"Column_1",null);
			y = (AbstractDataset)LoaderFactory.getDataSet(file.getAbsolutePath(),"Column_2",null);
		} catch (Exception e) {
			return null;
		}
		
		return new AbstractDataset[]{x,Maths.add(y,100)};
	}
	
}
