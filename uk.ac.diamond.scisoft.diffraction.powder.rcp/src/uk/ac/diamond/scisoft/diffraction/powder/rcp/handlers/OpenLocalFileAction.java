package uk.ac.diamond.scisoft.diffraction.powder.rcp.handlers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import uk.ac.diamond.osgi.services.ServiceProvider;

public class OpenLocalFileAction extends Action implements
		IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private String filterPath;

	@Override
	public void run(IAction action) {
		run();
	}
	
	@Override
	public void run() {
		FileDialog dialog =  new FileDialog(window.getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setText("Open file");
		dialog.setFilterPath(filterPath);
		dialog.open();
		String[] names =  dialog.getFileNames();
		
		
		if (names != null) {
			String[] fullNames = new String[names.length];
			for (int i = 0; i < names.length; i++) fullNames[i] = dialog.getFilterPath() + File.separator + names[i];
			
			EventAdmin eventAdmin = ServiceProvider.getService(EventAdmin.class);
			Map<String,String[]> props = new HashMap<>();
			props.put("paths", fullNames);
			eventAdmin.postEvent(new Event("org/dawnsci/events/file/powder/OPEN", props));
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		window = null;
		filterPath = null;
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window =  window;
		filterPath =  System.getProperty("user.home");
	}

}
