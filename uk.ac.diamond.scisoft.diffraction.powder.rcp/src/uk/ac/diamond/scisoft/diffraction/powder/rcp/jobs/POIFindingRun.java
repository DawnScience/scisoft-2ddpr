package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;

public class POIFindingRun implements IRunnableWithProgress {

	DiffractionImageData currentData;
	SimpleCalibrationParameterModel model;
	ICalibrationUIProgressUpdate uiUpdater;
	
	private static Logger logger = LoggerFactory.getLogger(POIFindingRun.class);
	
	static String REGION_PREFIX = "Pixel peaks";
	
	public POIFindingRun(ICalibrationUIProgressUpdate uiUpdater,
			final DiffractionImageData currentData,
			final SimpleCalibrationParameterModel model) {
		this.uiUpdater =  uiUpdater;
		this.currentData = currentData;
		this.model = model;
	}
	
	@Override
	public void run(IProgressMonitor monitor) {
		
		monitor.beginTask("Finding Points of Interest...", IProgressMonitor.UNKNOWN);
		
		if (currentData == null && currentData.getMetaData() == null) {
			logger.debug("Data or metadata is null");
			return;
		}

		CalibrantSpacing cs = CalibrationFactory.getCalibrationStandards().getCalibrant();
		
		int minSpacing = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
		int nPoints = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
		
		PowderCalibration.findPointsOfInterest(currentData, model, uiUpdater, cs, new ProgressMonitorWrapper(monitor), minSpacing, nPoints);
				
		return;
	}
	
	
	public void updateData(DiffractionImageData data) {
		this.currentData = data;
	}
}
