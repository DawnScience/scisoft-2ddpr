package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.List;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.crystallography.IHKL;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;

public class AutoCalibrationRun extends AbstractCalibrationRun {


	public AutoCalibrationRun(Display display, IPlottingSystem<?> plottingSystem,
			DiffractionDataManager manager, SimpleCalibrationParameterModel param) {
		super(display, plottingSystem, manager, param);
	}
	
	public AutoCalibrationRun(Display display, IPlottingSystem<?> plottingSystem,
			DiffractionDataManager manager, DiffractionTableData currentData, SimpleCalibrationParameterModel param) {
		super(display, plottingSystem, manager, param);
		this.currentData = currentData;
	}

	@Override
	public void run(final IProgressMonitor monitor) {
		
		//Unpack all ui stuff to go into maths level since the automatic routine should be scriptable
		ICalibrationUIProgressUpdate uiUpdate = new ICalibrationUIProgressUpdate() {
			
			@Override
			public void updatePlotData(IDataset data) {
				plottingSystem.updatePlot2D(data, null, monitor);
				
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
				DiffractionCalibrationUtils.drawFoundRing(monitor, display, plottingSystem, roi, false);
			}
			
			@Override
			public void completed() {
			}
		};
		
		monitor.beginTask("Calibrating...", IProgressMonitor.UNKNOWN);
		int centreMaskRadius = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS);
		int minSpacing = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
		int nPoints = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
		final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		List<IHKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		if (manager.isEmpty()) return;
		
		Dataset ddist = manager.getDistances();
		double pxSize = manager.getCurrentData() != null ? manager.getCurrentData().getMetaData().getDetector2DProperties().getHPxSize() : 0;
		
		double fixed = 0;
		if (params.isFloatDistance() && !params.isFloatEnergy()) {
			fixed = currentData.getMetaData().getDiffractionCrystalEnvironment().getWavelength();
		} else if (!params.isFloatDistance() && params.isFloatEnergy()) {
			fixed = currentData.getMetaData().getDetector2DProperties().getBeamCentreDistance();
		}
		
		
		IDataset[] images = new IDataset[manager.getSize()];
		
		PowderCalibrationInfoImpl[] info = new PowderCalibrationInfoImpl[manager.getSize()];
		
		int count = 0;
		for (DiffractionTableData data : manager.iterable()) {
			try {
				images[count] = DatasetUtils.sliceAndConvertLazyDataset(data.getImage());
			} catch (DatasetException e) {
				continue;
			}
			//TODO use better detector name for nexus paths
			info[count++] = createPowderCalibrationInfo(data, true);
		}
		
		CalibrationOutput output = PowderCalibration.calibrateMultipleImages(images, ddist, pxSize, spacings, fixed, new int[]{centreMaskRadius,minSpacing,nPoints}, params, mon, uiUpdate, info);
		
		updateOnFinish(output);

		return;
	}
	
	public void runForRunnable(final IProgressMonitor monitor) {
		run(monitor);
	}

}
