package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.views.DiffractionCalibrationView;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.views.DiffractionPlotView;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.views.DiffractionToolView;

public class DiffractionCalibrationMultipleViewsPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
	
		IFolderLayout navigatorFolder = layout.createFolder("navigator-folder", IPageLayout.LEFT, 0.15f, editorArea);
		navigatorFolder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		navigatorFolder.addView("uk.ac.diamond.sda.navigator.views.FileView");

		String fixed = "org.dawb.workbench.plotting.views.toolPageView.fixed:";
		// Top left: Diffraction calibration view
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.RIGHT, 0.25f, editorArea);
		topLeft.addView(DiffractionCalibrationView.ID);

		// Top right: Diffraction plotting view
		IFolderLayout topRight = layout.createFolder("topRight", IPageLayout.RIGHT, 0.50f, "topLeft");
		topRight.addView(DiffractionPlotView.ID);

		// Bottom left: Diffraction tool view
		IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f, "topLeft");
//		bottomLeft.addView(fixed+DiffractionCalibrationView.DIFFRACTION_ID);
		//open the tool within an empty view
		bottomLeft.addView(DiffractionToolView.ID);

		// Bottom Right: Powder diffraction tool
		IFolderLayout bottomRight = layout.createFolder("bottomRight", IPageLayout.BOTTOM, 0.50f, "topRight");
		// open the tool as a fixed view
		bottomRight.addView(fixed+DiffractionCalibrationView.POWDERCHECK_ID);
//		bottomRight.addView(fixed+DiffractionPowderCalibCheckView.ID);

		layout.getViewLayout(DiffractionPlotView.ID).setCloseable(false);
		layout.getViewLayout(DiffractionCalibrationView.ID).setCloseable(false);
		layout.getViewLayout(DiffractionToolView.ID).setCloseable(false);
		layout.getViewLayout(fixed+DiffractionCalibrationView.POWDERCHECK_ID).setCloseable(false);

	}

}
