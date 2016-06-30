package uk.ac.diamond.scisoft.diffraction.powder.rcp.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

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
				
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart view = page.findView("uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionCalibrationView");
				if (view==null) return;
				
				//FIXME table viewer should not be doing the data loading!
				final DiffractionDataManager manager = (DiffractionDataManager)view.getAdapter(DiffractionDataManager.class);
				if (manager != null) {
					manager.loadData(loc, null);
				}
				
			}
		}
	}
	
}
