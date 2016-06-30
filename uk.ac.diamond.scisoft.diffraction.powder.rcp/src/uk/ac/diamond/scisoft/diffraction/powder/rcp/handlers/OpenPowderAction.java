package uk.ac.diamond.scisoft.diffraction.powder.rcp.handlers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.LocalServiceManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class OpenPowderAction extends Action {
	
	private ISelectionProvider provider;

	public OpenPowderAction(IWorkbenchPage p, ISelectionProvider selectionProvider) {
		setText("Open Property"); //$NON-NLS-1$
		provider = selectionProvider;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		ISelection selection = provider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1 && sSelection.getFirstElement() instanceof IFile) {

				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		ISelection selection = provider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1 && sSelection.getFirstElement() instanceof IFile) {

				IFile file = (IFile)sSelection.getFirstElement();
				String loc = file.getRawLocation().toOSString();
				
				String[] fullNames = {loc};
				
				EventAdmin eventAdmin = LocalServiceManager.getEventAdmin();
				Map<String,String[]> props = new HashMap<>();
				props.put("paths", fullNames);
				eventAdmin.postEvent(new Event("org/dawnsci/events/file/powder/OPEN", props));
				
			}
		}
	}
	
}
