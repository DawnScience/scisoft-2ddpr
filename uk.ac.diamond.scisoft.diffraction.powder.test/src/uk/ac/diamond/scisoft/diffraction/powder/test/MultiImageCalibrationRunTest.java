package uk.ac.diamond.scisoft.diffraction.powder.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;

public class MultiImageCalibrationRunTest {
	
	
	@Test
	public void setDetectorFastAxisTestInPlane() {
		DetectorProperties dp = getModelDetectorGeometry();
		dp.setNormalAnglesInDegrees(0., 0., 45.); //set this detector such that it's got a 45 degree roll
		
		
		double rollDeg = 0.; //I want the equivalent representation of this rolled detector for a zero roll angle
		PowderCalibration.setDetectorFastAxisAngle(dp, rollDeg);
	
		Vector3d origin = dp.getOrigin();
		Assert.assertArrayEquals(new double[] {0.5, 1.0,100.}, new double[] {origin.getX(),origin.getY(),origin.getZ()},1e-6 );
		Assert.assertEquals(rollDeg, dp.getNormalAnglesInDegrees()[2],1e-6);
		
		//now get the equivalent representation of 90 deg roll angle (if the detector is kept still this 
		//is like rotating the DLS system 90deg clockwise around the z-axis so x+ becomes -Y, and
		// y becomes X) 
		rollDeg = 90.;  
		
		PowderCalibration.setDetectorFastAxisAngle(dp, rollDeg);
		origin =dp.getOrigin();
		Assert.assertEquals(90., dp.getNormalAnglesInDegrees()[2],1e-6);
		Assert.assertArrayEquals(new double[] {1.0, -0.5,100.}, new double[] {origin.getX(),origin.getY(),origin.getZ()},1e-6 );
		
	}
	
	@Test
	public void setDetectorFastAxisTestTwoAngles() {
		
		DetectorProperties dp = getModelDetectorGeometry();
		
		//combined yaw and roll- the new yaw and pitch provided should be corrected 
		//45 deg yaw with a 90 deg roll should have an equivalent of a 45 deg pitch when the reference space has zero roll;
		
		dp.setNormalAnglesInDegrees(45,0.,90.);
		double rollDeg = 0.;
		PowderCalibration.setDetectorFastAxisAngle(dp, rollDeg);
		double[] newAngles = dp.getNormalAnglesInDegrees();
		Vector3d origin = dp.getOrigin();
		Assert.assertArrayEquals(new double[] {0.,45.,0.},newAngles,1e-6);
		//origin should be [(w/2), cos(45 deg)*(h/2), z0 + cos(45 deg)*(h/2)]
		Assert.assertArrayEquals(new double[] {0.5,0.707107,100.707107}, new double[] {origin.getX(),origin.getY(),origin.getZ()},1e-5);
		
		
		//combined pitch and roll- the new yaw and pitch provided should be corrected 
		//45 deg pitch with a -90 deg roll should have an equivalent of a 45 deg yaw, when the reference space has zero roll;
		dp = getModelDetectorGeometry(); //reset the detector
		dp.setNormalAnglesInDegrees(0.,45.,-90.); 
		PowderCalibration.setDetectorFastAxisAngle(dp, rollDeg);
		origin = dp.getOrigin();
		newAngles = dp.getNormalAnglesInDegrees();
		Assert.assertArrayEquals(new double[] {45.,0.,0.},newAngles,1e-6);
		//origin should be [cos(45 deg)*(w/2), (h/2), z0 + cos(45 deg)*(w/2)]
		Assert.assertArrayEquals(new double[] {0.707107*0.5,1.0,100+(0.707107*0.5)}, new double[] {origin.getX(),origin.getY(),origin.getZ()},1e-5);
		
	}
	
	
	private DetectorProperties getModelDetectorGeometry() {
		//set up a fake detector with a pixel size of 1 um and 2000 pixels in height and 1000 pixels in width
		Matrix3d ori = new  Matrix3d();
		ori.setIdentity();
		Vector3d origin = new Vector3d(0.5,1.,100.);
		DetectorProperties dp = new DetectorProperties(origin, 2000, 1000, 1e-3, 1e-3, ori );
		return dp;
		
	}
	
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void MultiImageTestPixium(){
		
		Dataset image;
		try {
			double pixel = 0.2;
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00000.tif","image-01",null));
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2");
			List<ResolutionEllipseROI> first = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			//assertEquals(197.97, first.get(0).getSemiAxis(0), 0.1);
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00001.tif","image-01",null));
			List<ResolutionEllipseROI> second = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00003.tif","image-01",null));
			List<ResolutionEllipseROI> third = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i12/AlgorithmTesting/Circular1/pixi_00004.tif","image-01",null));
			List<ResolutionEllipseROI> forth = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			double[] dDist = new double[]{300,400,600,700};
			Dataset deltaDistance = DatasetFactory.createFromObject(dDist);
			
			List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
			allEllipses.add(new ArrayList<EllipticalROI>(first));
			allEllipses.add(new ArrayList<EllipticalROI>(second));
			allEllipses.add(new ArrayList<EllipticalROI>(third));
			allEllipses.add(new ArrayList<EllipticalROI>(forth));
			
			List<double[]> allDSpacings = new ArrayList<double[]>();
			
			double[] fd = new double[first.size()];
			int i = 0;
			for (ResolutionEllipseROI r : first) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[second.size()];
			i = 0;
			for (ResolutionEllipseROI r : second) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[third.size()];
			i = 0;
			for (ResolutionEllipseROI r : third) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[forth.size()];
			i = 0;
			for (ResolutionEllipseROI r : forth) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);

			CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings, deltaDistance, pixel);
			
			assertEquals(4.250447566358648e-01, output.getWavelength(), 0.0009);
			
		} catch (Exception e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void MultiImageTestPilatus(){
		
	
		Dataset image;
		try {
			double pixel = 0.172;
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_170.cbf","image-01",null));
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2Minimal");
			List<ResolutionEllipseROI> first = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_210.cbf","image-01",null));
			List<ResolutionEllipseROI> second = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_250.cbf","image-01",null));
			List<ResolutionEllipseROI> third = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i04/CeO2/ceo2_12kev_290.cbf","image-01",null));
			List<ResolutionEllipseROI> forth = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			double[] dDist = new double[]{170,210,250,290};
			Dataset deltaDistance = DatasetFactory.createFromObject(dDist);
			
			List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
			allEllipses.add(new ArrayList<EllipticalROI>(first));
			allEllipses.add(new ArrayList<EllipticalROI>(second));
			allEllipses.add(new ArrayList<EllipticalROI>(third));
			allEllipses.add(new ArrayList<EllipticalROI>(forth));
			
			List<double[]> allDSpacings = new ArrayList<double[]>();
			
			double[] fd = new double[first.size()];
			int i = 0;
			for (ResolutionEllipseROI r : first) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[second.size()];
			i = 0;
			for (ResolutionEllipseROI r : second) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[third.size()];
			i = 0;
			for (ResolutionEllipseROI r : third) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[forth.size()];
			i = 0;
			for (ResolutionEllipseROI r : forth) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);

			CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings, deltaDistance, pixel);
			
			assertEquals(0.97886, output.getWavelength(), 0.0009);
			
		} catch (Exception e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void MultiImageTestPerkin(){
		
	
		Dataset image;
		try {
			double pixel = 0.2;
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d259-00017.tif","image-01",null));
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("CeO2Minimal");
			List<ResolutionEllipseROI> first = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d359-00016.tif","image-01",null));
			List<ResolutionEllipseROI> second = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d459-00015.tif","image-01",null));
			List<ResolutionEllipseROI> third = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			image = DatasetUtils.convertToDataset(LoaderFactory.getDataSet("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d559-00014.tif","image-01",null));
			List<ResolutionEllipseROI> forth = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
			
			double[] dDist = new double[]{250,350,450,550};
			Dataset deltaDistance = DatasetFactory.createFromObject(dDist);
			
			List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
			allEllipses.add(new ArrayList<EllipticalROI>(first));
			allEllipses.add(new ArrayList<EllipticalROI>(second));
			allEllipses.add(new ArrayList<EllipticalROI>(third));
			allEllipses.add(new ArrayList<EllipticalROI>(forth));
			
			List<double[]> allDSpacings = new ArrayList<double[]>();
			
			double[] fd = new double[first.size()];
			int i = 0;
			for (ResolutionEllipseROI r : first) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[second.size()];
			i = 0;
			for (ResolutionEllipseROI r : second) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[third.size()];
			i = 0;
			for (ResolutionEllipseROI r : third) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);
			
			fd = new double[forth.size()];
			i = 0;
			for (ResolutionEllipseROI r : forth) {
				fd[i++] = r.getResolution();
			}
			allDSpacings.add(fd);

			CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings, deltaDistance, pixel);
			
			assertEquals(0.425, output.getWavelength(), 0.0009);
			
		} catch (Exception e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore("Hard coded paths, currently for TDD, will be a valid test when we have a location for the files.")
	@Test
	public void MultiImageTestPerkinLab629k(){
		
		double pixel = 0.2;
		Dataset image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d359-00021.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		CalibrantSpacing ceO2 = standards.getCalibrationPeakMap("Lab6Minimal6");
		List<ResolutionEllipseROI> first = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
		
		
		image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d459-00022.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		List<ResolutionEllipseROI> second = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
		
		image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d559-00023.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		List<ResolutionEllipseROI> third = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());
		
		image = getImage("/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/LaB6_29p2keV_d659-00024.tif");
		if (image == null) {
			Assert.fail("Image loading failed");
			return;
		}
		List<ResolutionEllipseROI> forth = PowderCalibration.findMatchedEllipses(image, pixel, ceO2.getHKLs());

		double[] dDist = new double[]{350,450,550,650};
		Dataset deltaDistance = DatasetFactory.createFromObject(dDist);

		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		allEllipses.add(new ArrayList<EllipticalROI>(first));
		allEllipses.add(new ArrayList<EllipticalROI>(second));
		allEllipses.add(new ArrayList<EllipticalROI>(third));
		allEllipses.add(new ArrayList<EllipticalROI>(forth));

		List<double[]> allDSpacings = new ArrayList<double[]>();

		double[] fd = new double[first.size()];
		int i = 0;
		for (ResolutionEllipseROI r : first) {
			fd[i++] = r.getResolution();
		}
		allDSpacings.add(fd);

		fd = new double[second.size()];
		i = 0;
		for (ResolutionEllipseROI r : second) {
			fd[i++] = r.getResolution();
		}
		allDSpacings.add(fd);

		fd = new double[third.size()];
		i = 0;
		for (ResolutionEllipseROI r : third) {
			fd[i++] = r.getResolution();
		}
		allDSpacings.add(fd);

		fd = new double[forth.size()];
		i = 0;
		for (ResolutionEllipseROI r : forth) {
			fd[i++] = r.getResolution();
		}
		allDSpacings.add(fd);

		CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings, deltaDistance, pixel);

		assertEquals(0.425, output.getWavelength(), 0.0009);

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
