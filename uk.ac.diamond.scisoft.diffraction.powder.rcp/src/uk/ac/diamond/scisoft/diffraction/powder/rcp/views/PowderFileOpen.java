package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dawb.common.util.io.IOpenFileAction;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderFileOpen implements IOpenFileAction {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.views.PowderFileView";

	private static final Logger logger = LoggerFactory.getLogger(PowderFileOpen.class);

	@Override
	public void openFile(Path file) {

		if (file==null) return;

		if (!Files.isDirectory(file)) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart view = page.findView(DiffractionCalibrationView.ID);
			if (view==null) return;

			final DiffractionDataManager manager = (DiffractionDataManager)view.getAdapter(DiffractionDataManager.class);
			if (manager != null) {
				manager.loadData(file.toAbsolutePath().toString(), null);
			} else {
				logger.error("Could not get file manager");
			}
		}
	}

}
