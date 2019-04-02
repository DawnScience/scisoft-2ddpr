package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.ProgressMonitorWrapper;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class FromPointsCalibrationRun extends AbstractCalibrationRun {

	
	public FromPointsCalibrationRun(Display display,
			IPlottingSystem<?> plottingSystem, DiffractionDataManager manager, SimpleCalibrationParameterModel params, ICalibrationUIProgressUpdate uiUpdater) {
		super( manager, params, uiUpdater);

	}

	@Override
	public void run(IProgressMonitor monitor) {
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		CalibrationOutput output = PowderCalibration.manualCalibrateMultipleImagesPoints(currentData, spacings, params, new ProgressMonitorWrapper(monitor), uiUpdater);

		currentData.setCalibrationInfo(output.getCalibrationInfo()[0]);

		uiUpdater.removeRings();
		
		return;
	}

}
