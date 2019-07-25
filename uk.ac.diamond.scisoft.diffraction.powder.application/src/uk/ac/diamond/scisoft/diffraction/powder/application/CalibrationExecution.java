package uk.ac.diamond.scisoft.diffraction.powder.application;

import java.util.List;

import org.dawnsci.plotting.tools.preference.detector.DiffractionDetector;
import org.dawnsci.plotting.tools.preference.detector.DiffractionDetectorHelper;
import org.dawnsci.plotting.tools.preference.detector.DiffractionDetectorPreferenceInitializer;
import org.dawnsci.plotting.tools.preference.detector.DiffractionDetectors;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.NexusDiffractionCalibrationReader;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.NexusCalibrationExportUtils;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationConfig;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public class CalibrationExecution {
	
	private static final Logger logger = LoggerFactory.getLogger(CalibrationExecution.class);
	
	private PowderCalibrationConfig config;

	public CalibrationExecution(PowderCalibrationConfig config) {
		this.config = config;
	}
	
	public void run() throws Exception {
		
		BundleContext bundleContext =
                FrameworkUtil.
                getBundle(this.getClass()).
                getBundleContext();
		
		ILoaderService loaderService = bundleContext.getService(bundleContext.getServiceReference(ILoaderService.class));
		
		IDataset[] images = getImages(config.getInputPath(), config.getDatasetPath(), loaderService);
		
		SimpleCalibrationParameterModel params = config.getModel();
		
		double fixedWavelengthOrDistance = 0;
		
		DiffractionDetector dd = DiffractionDetectorHelper.getMatchingDefaultDetector(images[0].getShape());
		
		int[] options = new int[]{params.getnIgnoreCentre(),params.getMinimumSpacing(),params.getnPointsPerRing()};
		
		CalibrantSpacing cs = CalibrationFactory.getCalibrationStandards().getCalibrationPeakMap(config.getStandard());
		
		CalibrationOutput output = null;
		
		if (config.getInitialCalibration() != null && !config.getModel().isAutomaticCalibration()) {
			logger.info("Running manual calibration using seed from " + config.getInitialCalibration());
			IDiffractionMetadata md = NexusDiffractionCalibrationReader.getDiffractionMetadataFromNexus(config.getInitialCalibration(), null);
			
			DiffractionImageData imdata = new DiffractionImageData();
			imdata.setImage(images[0]);
			imdata.setMetaData(md);
			output = PowderCalibration.calibrateSingleImageManualPoint(DatasetUtils.convertToDataset(images[0]), cs.getHKLs(), md,config.getModel());
		} else {
			output = PowderCalibration.calibrateMultipleImages(images,
					null, dd.getXPixelMM(), cs.getHKLs(), fixedWavelengthOrDistance, options, params, null, null, null);
		}
		
	    if (output == null) {
	    	return;
	    }
		
	    System.out.println("");
	    System.out.println("************************");
	    System.out.println("*********OUTPUT*********");
		System.out.println(output.getCalibrationOutputDescription());
		System.out.println("************************");
		System.out.println("************************");
		IDiffractionMetadata md = images[0].getFirstMetadata(IDiffractionMetadata.class);
		IDiffractionMetadata mdnew;
		if (md == null) {
			DetectorProperties dp = DetectorProperties.getDefaultDetectorProperties(images[0].getShape());
			dp.setHPxSize(dd.getYPixelMM());
			dp.setVPxSize(dd.getXPixelMM());
			DiffractionCrystalEnvironment dce = DiffractionCrystalEnvironment.getDefaultDiffractionCrystalEnvironment();
			dce.setWavelength(output.getWavelength());
			mdnew = new DiffractionMetadata("",dp,dce);
			
		} else {
			mdnew = md.clone();
		}
		
		PowderCalibration.updateMetadataFromOutput(mdnew,output,0,null);
		System.out.println("");
		System.out.println("************************");
		System.out.println("*******CALIBRATION******");
		System.out.println("Detector " + mdnew.getDetector2DProperties());
		System.out.println("Wavelength /A: " +mdnew.getDiffractionCrystalEnvironment().getWavelength());
		System.out.println("Energy /Kev: " +mdnew.getDiffractionCrystalEnvironment().getEnergy());
	    System.out.println("************************");
	    System.out.println("************************");
		
		NexusCalibrationExportUtils.saveToNexusFile(images[0], mdnew, output.getCalibrationInfo()[0], config.getOutputPath());
		
	}
	
	private IDataset[] getImages(String path, String datasetPath, ILoaderService service) throws Exception {
		
		IDataHolder dh = service.getData(path, null);
		
		if (dh.size() == 1 && dh.getDataset(0).getRank() == 2) {
			return new IDataset[] {dh.getDataset(0).getSlice()};
		}
		
		return null;
	}

}
