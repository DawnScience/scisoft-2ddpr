package uk.ac.diamond.scisoft.diffraction.powder.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
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
