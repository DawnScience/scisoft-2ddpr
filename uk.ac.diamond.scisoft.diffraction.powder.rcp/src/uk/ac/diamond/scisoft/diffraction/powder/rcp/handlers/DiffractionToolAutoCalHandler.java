package uk.ac.diamond.scisoft.diffraction.powder.rcp.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawnsci.plotting.tools.diffraction.DiffractionTool;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;

public class DiffractionToolAutoCalHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (!(event.getApplicationContext() instanceof DiffractionTool)) return null;
		
		DiffractionTool dtool = (DiffractionTool)event.getApplicationContext();
		
		DiffractionTableData dtd = new DiffractionTableData();
		
		IPlottingSystem<?> system = dtool.getPlottingSystem();
		
		if (system == null) return null;
		
		Collection<ITrace> traces = system.getTraces(IImageTrace.class);
		
		if (traces.isEmpty()) return null;
		
		IImageTrace imTrace = (IImageTrace)traces.iterator().next();
		
		IDataset ds = imTrace.getData();
		
		if (ds == null) return null;
		
		IDiffractionMetadata dm = ds.getFirstMetadata(IDiffractionMetadata.class);
		
		if (dm == null) return null;
		
		dtd.setImage(ds);
		dtd.setMetaData(dm);
		
		List<DiffractionTableData> model = new ArrayList<DiffractionTableData>();
		model.add(dtd);
		
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(10);
		
		AutoCalibrationRun job = new AutoCalibrationRun(Display.getDefault(), system, new DiffractionDataManager(model), dtd, params);
		
		ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			dia.run(true, true, job);
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return null;
	}

}
