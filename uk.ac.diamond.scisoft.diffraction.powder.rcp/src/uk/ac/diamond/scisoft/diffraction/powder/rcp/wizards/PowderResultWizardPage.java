package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.plaf.metal.MetalBorders.TextFieldBorder;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.common.widgets.dialog.FileSelectionDialog;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.tool.ToolPageFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDelegate;

public class PowderResultWizardPage extends WizardPage {

	
	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	private DiffractionImageAugmenter augmenter;
	private IToolPage toolPage;
	private StyledText resultText;

	protected PowderResultWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Set-Up");
		setTitle("Powder XRD/SAX Calibration - Results");
		setDescription("Review calibration results, export to NeXus file.");
		this.manager = manager;
		
	}

	@Override
	public void createControl(Composite parent) {
		SashForm sashForm= new SashForm(parent, SWT.HORIZONTAL);
		setControl(sashForm);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,3,1));
		
		final Composite left = new Composite(sashForm, SWT.NONE);
		left.setLayout(new GridLayout());
		final Composite centre = new Composite(sashForm, SWT.NONE);
		centre.setLayout(new GridLayout());
		Composite right = new Composite(sashForm, SWT.NONE);
		right.setLayout(new FillLayout());	
		
		Composite plotComp = new Composite(centre, SWT.NONE);
		plotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		plotComp.setLayout(new GridLayout());
		ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(plotComp, null);
		Composite displayPlotComp  = new Composite(plotComp, SWT.BORDER);
		displayPlotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		displayPlotComp.setLayout(new FillLayout());
		
		
		try {
			system = PlottingFactory.createPlottingSystem();
			system.createPlotPart(displayPlotComp, "PlotDataWizard", actionBarWrapper, PlotType.IMAGE, null);
			Dataset ds = DatasetUtils.sliceAndConvertLazyDataset(manager.getCurrentData().getImage());
			ds.setMetadata(manager.getCurrentData().getMetaData());
			system.createPlot2D(ds, null,null);
			augmenter = new DiffractionImageAugmenter(system);
			augmenter.setDiffractionMetadata(manager.getCurrentData().getMetaData());
			augmenter.activate();
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			augmenter.drawCalibrantRings(true, standards.getCalibrant());
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		IToolPageSystem tps = (IToolPageSystem)system.getAdapter(IToolPageSystem.class);
		
		try {
			toolPage = ToolPageFactory.getToolPage("org.dawnsci.plotting.tools.powdercheck");
			toolPage.setPlottingSystem(system);
			toolPage.setToolSystem(tps);
			toolPage.setTitle("Powder check tool");
			toolPage.setToolId(String.valueOf(toolPage.hashCode()));
			toolPage.createControl(right);
			toolPage.activate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (manager.getSize() > 1){
			DiffractionDelegate diffractionTableViewer = new DiffractionDelegate(left, manager);
			diffractionTableViewer.updateTableColumnsAndLayout(0);
		}
		
		resultText = new StyledText(left, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY |SWT.V_SCROLL);
		resultText.setAlwaysShowScrollBars(false);
		resultText.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Button export = new Button(left, SWT.PUSH);
		export.setText("Save Calibration...");
		export.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					FileSelectionDialog dialog = new FileSelectionDialog(Display.getDefault().getActiveShell());
//					FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
					dialog.setNewFile(true);
					dialog.setFolderSelector(false);

					dialog.setPath(System.getProperty("user.home")+ File.separator + "calibration_output.nxs");
					
					
					dialog.create();
					if (dialog.open() == Dialog.OK) {
						DiffractionCalibrationUtils.saveToNexusFile(manager, dialog.getPath());
					}

				} catch (Exception ex) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "File save error!", "Could not save calibration file! (Do you have write access to this folder?)");
//					logger.error("Problem opening export!", e);
				}
				
				
			}
		});
		
	}

	@Override
	public boolean canFlipToNextPage() {
		IPowderCalibrationInfo calInfo = manager.getCurrentData().getCalibrationInfo();
		if (calInfo != null) {
			String resultDescription = calInfo.getResultDescription();
			if (resultDescription != null) resultText.setText(resultDescription);
			
		}
        return super.canFlipToNextPage();
    }
	
	
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			
			toolPage.activate();
			augmenter.activate();
			augmenter.drawBeamCentre(false);
			
		}
		else {
			toolPage.deactivate();
			augmenter.deactivate(false);
		}
		
		
//		if (!visible) {
//			SimpleCalibrationParameterModel model = new SimpleCalibrationParameterModel();
//			model.setNumberOfRings(6);
//			DiffractionDataManager m = new DiffractionDataManager();
//
//			IRunnableWithProgress job = new AutoCalibrationRun(Display.getDefault(), system,manager , manager.getCurrentData(), model);
//			try {
//				getContainer().run(true, true, job);
//			} catch (InvocationTargetException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
        super.setVisible(visible);
    }

}
