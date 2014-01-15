package uk.ac.diamond.scisoft.diffraction.powder.rcp.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.api.trace.ITrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionTool;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationJob;

public class DiffractionToolAutoCalHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (!(event.getApplicationContext() instanceof DiffractionTool)) return null;
		
		DiffractionTool dtool = (DiffractionTool)event.getApplicationContext();
		
		DiffractionTableData dtd = new DiffractionTableData();
		dtd.augmenter  = dtool.getAugmenter();
		
		IPlottingSystem system = dtool.getPlottingSystem();
		
		if (system == null) return null;
		
		Collection<ITrace> traces = system.getTraces(IImageTrace.class);
		
		if (traces.isEmpty()) return null;
		
		IImageTrace imTrace = (IImageTrace)traces.iterator().next();
		
		AbstractDataset ds = (AbstractDataset)imTrace.getData();
		
		if (ds == null) return null;
		
		IDiffractionMetadata dm = (IDiffractionMetadata)ds.getMetadata();
		
		if (dm == null) return null;
		
		dtd.image = ds;
		dtd.md = dm;
		
		final Display display = Display.getDefault();
		List<DiffractionTableData> model = new ArrayList<DiffractionTableData>();
		model.add(dtd);
		
		AutoCalibrationJob job = new AutoCalibrationJob(Display.getDefault(), system, model, dtd, 10);
		
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						//TODO show dialog with reduced data
						//updateIntegrated(currentData);
					}
				});
			}
		});
		job.setUser(true);
		job.schedule();
		
		return null;
	}

}
