package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public abstract class AbstractCalibrationRun implements IRunnableWithProgress {

	Display display;
	IPlottingSystem plottingSystem;
	DiffractionDataManager manager;
	DiffractionTableData currentData;
	SimpleCalibrationParameterModel params;

	private static String REGION_PREFIX = "Pixel peaks";
	
	private static Logger logger = LoggerFactory.getLogger(AbstractCalibrationRun.class);
	
	public AbstractCalibrationRun(Display display,
			IPlottingSystem plottingSystem,
			DiffractionDataManager manager,
			DiffractionTableData currentData,
			SimpleCalibrationParameterModel params) {
		
		this.display = display;
		this.plottingSystem = plottingSystem;
		this.manager = manager;
		this.params = params;
		this.currentData = currentData;
	}
	
	/**
	 * 
	 * @param plottingSystem
	 */
	public void removeFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
				}
		}
	}
	
	
	protected void updateOnFinish(final CalibrationOutput output) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				for (DiffractionTableData data : manager.iterable()) {
					updateMetaData(data.getMetaData(), output, i);
					data.setResidual(output.getResidual());
					i++;
				}

				removeFoundRings(plottingSystem);
			}
		});
	}
	
	protected void updateMetaData(final IDiffractionMetadata md , final CalibrationOutput output, final int i) {
		
		if (display.getThread()!=Thread.currentThread()) {
			display.syncExec(new Runnable() {

				@Override
				public void run() {
					updateMetaData(md, output, i);

				}
			});
		} else {
			PowderCalibration.updateMetadataFromOutput(md, output, i);
		}
		
	}

}
