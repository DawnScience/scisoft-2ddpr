package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.lang.reflect.InvocationTargetException;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.plotting.tools.diffraction.DiffractionDefaultMetadata;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.tool.ToolPageFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
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

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.CalibrationUIProgressUpdateImpl;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.CalibrantSelectionGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.CalibrationOptionsGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.IRunner;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.PowderCalibrationSetupWidget;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.RingSelectionGroup;

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
				
			}
		};
		
		
		PowderCalibrationSetupWidget widget = new PowderCalibrationSetupWidget(manager, model, augmenter, system, runner);
		widget.createControl(left);
		
//		model.setNumberOfRings(CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size());
//		
//		final CalibrantSelectionGroup group = new CalibrantSelectionGroup(left);
//		group.addCalibrantSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if (manager.getCurrentData() == null)
//					return;
//				String calibrantName = group.getCalibrant();
//				// update the calibrant in diffraction tool
//				CalibrationFactory.getCalibrationStandards().setSelectedCalibrant(calibrantName, true);
//				// set the maximum number of rings
//				int ringMaxNumber = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size();
//				
//			}
//		});
//		
//		group.addDisplaySelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				showCalibrantAndBeamCentre(group.getShowRings());
//			}
//		});
//		
//		final RingSelectionGroup ringSelection = new RingSelectionGroup(left, CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size(), model);
//		ringSelection.addRingNumberSpinnerListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				int ringNumber = ringSelection.getRingSpinnerSelection();
//				model.setNumberOfRings(ringNumber);
//				augmenter.setMaxCalibrantRings(ringNumber);
//				augmenter.activate();
////				calibrantRingsMap.put(calibrantName, ringNumber);
//			}
//		});
//		
//		final Button autoRadio = new Button(left, SWT.RADIO);
//		autoRadio.setText("Automatic");
//		autoRadio.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
//
//		final Button manualRadio = new Button(left, SWT.RADIO);
//		manualRadio.setText("Manual");
//		manualRadio.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
//		manualRadio.setSelection(false);
//		
//		final Composite autoManStack = new Composite(left, SWT.NONE);
//		final StackLayout stackLayout = new StackLayout();
//		autoManStack.setLayout(stackLayout);
//		final Label auto = new Label(autoManStack,SWT.WRAP);
//		auto.setText("Automatic Calibration");
//		stackLayout.topControl = auto;
//		autoManStack.layout();
//		final Button findRings = new Button(autoManStack, SWT.PUSH);
//		findRings.setText("Find Rings");
//
//		final POIFindingRun poiFindingRun = new POIFindingRun(new CalibrationUIProgressUpdateImpl(system, Display.getCurrent()), manager.getCurrentData(), model);
//		findRings.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				poiFindingRun.updateData(manager.getCurrentData());
//				try {
//					getContainer().run(true, true, poiFindingRun);
//				} catch (InvocationTargetException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			}
//			
//		});
//		
//		
//		final Button usePoints = new Button(left, SWT.CHECK);
//		usePoints.setText("Point calibration");
//		usePoints.setSelection(true);
//		usePoints.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				boolean s = ((Button)e.getSource()).getSelection();
//				model.setIsPointCalibration(s);
//				left.layout();
//			}
//		});
//		
//		final CalibrationOptionsGroup options = new CalibrationOptionsGroup(left, model, true,true);
//		
//		autoRadio.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				boolean s = ((Button)e.getSource()).getSelection();
//				options.showOptions(s, !s);
//				stackLayout.topControl = s ? auto : findRings;
//				autoManStack.layout();
//				
//			}
//		});
//		
//		autoRadio.setSelection(true);
		sashForm.setWeights(new int[]{20,40,40});
	}
	
	private void showCalibrantAndBeamCentre(boolean show) {
		if (augmenter == null) return;
		if (show && !augmenter.isActive()) {
			augmenter.activate();
			augmenter.drawBeamCentre(true);
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			augmenter.drawCalibrantRings(true, standards.getCalibrant());
		} else {
			augmenter.deactivate(false);
		}
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
