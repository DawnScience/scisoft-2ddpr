package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.lang.reflect.InvocationTargetException;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.tool.ToolPageFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.ICalibrationStateListener;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.IRunner;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.PowderCalibrationSetupWidget;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.StateChangedEvent;

public class PowderSetupWizardPage extends WizardPage {

	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	private DiffractionImageAugmenter augmenter;
	private SimpleCalibrationParameterModel model;
	private SimpleCalibrationParameterModel lastRunModel = null;
	private IToolPage toolPage;
	
	protected PowderSetupWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Set-Up");
		setTitle("Powder XRD/SAX Calibration - Set-Up");
		setDescription("Select the calibration standard, rings to use and whether to perform automatic or manual calibration, click next to run calibration.");
		this.manager = manager;
		model = new SimpleCalibrationParameterModel();
		model.setIsPointCalibration(true);
		setPageComplete(true);
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
			IDataset ds = manager.getCurrentData().getImage();
			ds.setMetadata(manager.getCurrentData().getMetaData());
			system.createPlot2D(ds, null,null);
			augmenter = new DiffractionImageAugmenter(system);
			augmenter.setDiffractionMetadata(manager.getCurrentData().getMetaData());
			augmenter.activate();
			augmenter.drawBeamCentre(true);
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			augmenter.drawCalibrantRings(true, standards.getCalibrant());
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		IToolPageSystem tps = (IToolPageSystem)system.getAdapter(IToolPageSystem.class);
		try {
			toolPage = ToolPageFactory.getToolPage("uk.ac.diamond.scisoft.diffraction.powder.rcp.powderDiffractionTool");
			toolPage.setPlottingSystem(system);
			toolPage.setToolSystem(tps);
			toolPage.setTitle("Powder wizard tool");
			toolPage.setToolId(String.valueOf(toolPage.hashCode()));
			toolPage.createControl(right);
			toolPage.activate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IRunner runner = new IRunner() {
			
			@Override
			public void run(IRunnableWithProgress runnable) {
				try {
					getContainer().run(true, true, runnable);
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				getContainer().updateButtons();
				
			}
		};
		
		
		PowderCalibrationSetupWidget widget = new PowderCalibrationSetupWidget(manager, model, augmenter, system, runner);
		widget.createControl(left);
		widget.addCalibrationStateListener(new ICalibrationStateListener() {
			
			@Override
			public void calibrationStateChanged(StateChangedEvent event) {
						
				threadSafeFlip();
				
			}
		});

		sashForm.setWeights(new int[]{20,40,40});
	}
	
	private void threadSafeFlip(){
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					threadSafeFlip();
				}
			});
			return;
		}
		
		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
	}
	
	@Override
	public boolean canFlipToNextPage() {
		if (model.isAutomaticCalibration()) return true;
		if (manager.getCurrentData().getNonNullROISize() > 0) return true;
		return false;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) toolPage.activate();
		else toolPage.deactivate();
		
		super.setVisible(visible);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		toolPage.deactivate();
		toolPage.dispose();
	}
	
	public IPlottingSystem<?> getPlottingSystem(){
		return system;
	}
	
	public void setLastRunModel(SimpleCalibrationParameterModel model) {
		lastRunModel = new SimpleCalibrationParameterModel(model);
	}

	public boolean hasModelChanged(){
		return !model.equals(lastRunModel);
	}
	
	public SimpleCalibrationParameterModel getModel(){
		return model;
	}
	
}
