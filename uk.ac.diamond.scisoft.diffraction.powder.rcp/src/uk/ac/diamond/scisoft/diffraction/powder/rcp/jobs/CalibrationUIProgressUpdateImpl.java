package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils;

public class CalibrationUIProgressUpdateImpl implements ICalibrationUIProgressUpdate{
	
	
	private IPlottingSystem<?> plottingSystem;
	private Display display;

	public CalibrationUIProgressUpdateImpl(IPlottingSystem<?> plottingSystem, Display display){
		this.plottingSystem = plottingSystem;
		this.display = display;
	}
	
	@Override
	public void updatePlotData(IDataset data) {
		plottingSystem.updatePlot2D(data, null, null);
		
	}
	
	@Override
	public void removeRings() {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				PowderCalibrationUtils.clearFoundRings(plottingSystem);
			}
		});
		
	}
	
	@Override
	public void drawFoundRing(IROI roi) {
		DiffractionCalibrationUtils.drawFoundRing(null, display, plottingSystem, roi, false);
	}
	
	@Override
	public void completed() {
		
	}

}
