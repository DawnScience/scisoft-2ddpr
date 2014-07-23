package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.views.DiffractionCalibrationView;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.views.DiffractionPlotView;

public class DiffractionCalibrationPerspective implements IPerspectiveFactory {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationPerspective";
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
	
		IFolderLayout navigatorFolder = layout.createFolder("navigator-folder", IPageLayout.LEFT, 0.15f, editorArea);
		navigatorFolder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		navigatorFolder.addView("uk.ac.diamond.sda.navigator.views.FileView");

		String fixed = "org.dawb.workbench.plotting.views.toolPageView.fixed:";
		String powderCheckID = "org.dawnsci.plotting.tools.powdercheck";
		String diffractionID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.powderDiffractionTool";

		// Top left: Diffraction calibration view
		IFolderLayout left = layout.createFolder("diffCalibrationView", IPageLayout.LEFT, 0.30f, editorArea);
		left.addView(DiffractionCalibrationView.ID);
		
		// Top right: Diffraction plotting view
		IFolderLayout top = layout.createFolder("diffractionPlotting", IPageLayout.LEFT, 0.50f, editorArea);
		top.addView(DiffractionPlotView.ID);

		// Bottom Right: Powder diffraction tool
		IFolderLayout bottomRight = layout.createFolder("powderCalibration", IPageLayout.BOTTOM, 0.50f, "diffractionPlotting");
		// open the tool as a fixed view
		bottomRight.addView(fixed + powderCheckID);

		

		// Bottom left: Diffraction tool view
		IFolderLayout topRight = layout.createFolder("powderDiffraction", IPageLayout.RIGHT, 0.50f, "diffractionPlotting");
		// open the tool as a fixed view
		topRight.addView(fixed + diffractionID);

		layout.getViewLayout(DiffractionPlotView.ID).setCloseable(false);
		layout.getViewLayout(DiffractionCalibrationView.ID).setCloseable(false);
		layout.getViewLayout(fixed + diffractionID).setCloseable(false);
		layout.getViewLayout(fixed + powderCheckID).setCloseable(false);

	}

}
