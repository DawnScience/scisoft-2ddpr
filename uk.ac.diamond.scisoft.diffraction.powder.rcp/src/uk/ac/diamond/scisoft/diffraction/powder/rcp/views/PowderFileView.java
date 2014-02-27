package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.io.File;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.sda.navigator.views.FileView;

public class PowderFileView extends FileView {

public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.views.PowderFileView";
	
	private static final Logger logger = LoggerFactory.getLogger(PowderFileView.class);
	
	@Override
	public void openSelectedFile() {
		final File file = getSelectedFile();
		if (file==null) return;
		
		if (!file.isDirectory()) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart view = page.findView(DiffractionCalibrationView.ID);
			if (view==null) return;
			
			final DiffractionDataManager manager = (DiffractionDataManager)view.getAdapter(DiffractionDataManager.class);
			if (manager != null) {
				manager.loadData(file.getAbsolutePath(), null);
			} else {
				logger.error("Could not get file manager");
			}
		}
	}

}
