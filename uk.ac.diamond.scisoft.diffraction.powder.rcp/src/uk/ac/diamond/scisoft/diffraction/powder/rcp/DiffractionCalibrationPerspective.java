package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IViewLayout;

public class DiffractionCalibrationPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		layout.setFixed(true);
		
		IFolderLayout navigatorFolder = layout.createFolder("navigator-folder", IPageLayout.LEFT, 0.15f, editorArea);
		navigatorFolder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		navigatorFolder.addView("uk.ac.diamond.sda.navigator.views.FileView");
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.LEFT, 0.8f, IPageLayout.ID_EDITOR_AREA);
			folderLayout.addView("uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationview");
		}
		
		
		
		
//		{
//			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.LEFT, 0.4f, IPageLayout.ID_EDITOR_AREA);
//			folderLayout.addView("uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationplot");
//			IViewLayout vLayout = layout.getViewLayout("uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationplot");
//			vLayout.setCloseable(false);
//		}
//		
//		IFolderLayout toolPageLayout = layout.createFolder("toolPageFolder", IPageLayout.RIGHT, 0.4f, "uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationplot");
//		toolPageLayout.addView("org.dawb.workbench.plotting.views.toolPageView.fixed:org.dawb.workbench.plotting.tools.diffraction.Diffraction");
//		toolPageLayout.addPlaceholder("*");
//		
//		toolPageLayout = layout.createFolder("toolPageFolder2", IPageLayout.BOTTOM, 0.4f, "org.dawb.workbench.plotting.views.toolPageView.fixed:org.dawb.workbench.plotting.tools.diffraction.Diffraction");
//		toolPageLayout.addView("org.dawb.workbench.plotting.views.toolPageView.fixed:org.dawnsci.plotting.tools.powdercheck");
//		toolPageLayout.addPlaceholder("*");
		
	}

}
