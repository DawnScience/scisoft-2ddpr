package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.wizard.persistence.PersistenceImportWizard;
import org.dawnsci.common.widgets.dialog.FileSelectionDialog;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.preference.diffraction.DiffractionPreferencePage;
import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectedListener;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectionEvent;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.CalibrantPositioningWidget;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.CalibrationUIProgressUpdateImpl;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromPointsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromRingsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataChanged;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDelegate;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.IDiffractionDataListener;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.CalibrantSelectionGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.ICalibrationStateListener;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.IRunner;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.PowderCalibrationSetupWidget;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.RingSelectionGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.StateChangedEvent;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards.PowderCalibrationWizard;

public class DiffractionCalibrationView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionCalibrationView";
	
	private final Logger logger = LoggerFactory.getLogger(DiffractionCalibrationView.class);

	private DiffractionDelegate diffractionTableViewer;
	private DiffractionImageAugmenter augmenter;
	private PowderCalibrationSetupWidget widget;
	private StyledText resultText;
	private IPlottingSystem<Composite> plottingSystem;
	private IPartListener2 partListener;

	private DiffractionDataManager manager;
	private SimpleCalibrationParameterModel calibrationParameters = new SimpleCalibrationParameterModel();

	private String lastPath = null;
	


	@Override
	public void createPartControl(final Composite parent) {

//		final ScrolledComposite scrollComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		IViewPart plotView = getView(DiffractionPlotView.ID);
		plottingSystem = (IPlottingSystem<Composite>)plotView.getAdapter(IPlottingSystem.class);
		if (plottingSystem != null && plottingSystem.isDisposed()) { // if we close the perspective then reopen it
			plottingSystem = PlottingFactory.getPlottingSystem(DiffractionPlotView.DIFFRACTION_PLOT_TITLE);
		}

		// set the focus on the plotting system to trigger the tools (powder tool)
		IWorkbenchPart part = plottingSystem.getPart();
		if (part != null) {
			part.getSite().getPage().activate(part);
		}
		augmenter = new DiffractionImageAugmenter(plottingSystem);
		
		
		final Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initializeListeners();
		manager = new DiffractionDataManager();

		// table of images and found rings
		diffractionTableViewer = new DiffractionDelegate(content, manager);
		

		
		IRunner runner = new IRunner() {
			@Override
			public void run(IRunnableWithProgress runnable) {
				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				try {
					dia.run(true, true, runnable);
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Calibration Error", "An error occured during ring finding, please contact your support representative: "+ e1.getTargetException().getLocalizedMessage());
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
		};
		
		
		widget = new PowderCalibrationSetupWidget(manager, calibrationParameters, augmenter, plottingSystem, runner);
		widget.setShowSteering(true);
		widget.createControl(content);

		manager.addFileListener(new IDiffractionDataListener() {
			
			@Override
			public void dataChanged(final DiffractionDataChanged event) {
				
				Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
						diffractionTableViewer.refresh();
						diffractionTableViewer.updateTableColumnsAndLayout(0);
						if (event == null) return;
						diffractionTableViewer.setSelection(new StructuredSelection(event.getSource()),true);
					}
				});
			}
		});

		resultText = new StyledText(content, SWT.BORDER);
		resultText.setAlwaysShowScrollBars(false);
		resultText.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Button goBabyGoButton = new Button(content, SWT.PUSH);
		goBabyGoButton.setImage(Activator.getImage("icons/CalibrationRun.png"));
		goBabyGoButton.setText("Run Calibration");
		goBabyGoButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		goBabyGoButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (manager.isEmpty()) {
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Load Data", "Please load some data!");
				}
//				setUpCalbrationModel(calibrationParameters);

				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				
				IRunnableWithProgress job = DiffractionCalibrationUtils.getCalibrationRunnable(calibrationParameters, manager, plottingSystem);
				
				try {
					dia.run(true, true, job);
				} catch (InvocationTargetException e1) {
					logger.error(e1.getMessage());
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Calibration Error", "An error occured during auto calibration!" + System.lineSeparator() +
							"Check the correct calibrant is selected, try changing the number of rings or use manual calibration." + System.lineSeparator()
							 + "Specific error :" + e1.getTargetException().getMessage());
					
				} catch (InterruptedException e1) {
					logger.error("Error running Job:" + e1.getMessage());
				}
				
				updateAfterCalibration();
				
			}
		});
		
		goBabyGoButton.setEnabled(false);
		
		diffractionTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(false);
				if (manager.isEmpty()) {
					diffractionTableViewer.refresh();
					updateCurrentData(null); // need to reset this
					plottingSystem.clear();
					PowderCalibrationUtils.clearFoundRings(plottingSystem);
					enableActions(false);
					goBabyGoButton.setEnabled(false);
				} else {
					enableActions(true);
					if (calibrationParameters.isAutomaticCalibration()) {
						goBabyGoButton.setEnabled(true);
					}
				}
				widget.update();
			}
		});
		
		widget.addCalibrationStateListener(new ICalibrationStateListener() {
			
			@Override
			public void calibrationStateChanged(final StateChangedEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						goBabyGoButton.setEnabled(event.isCanRun());
					}
				});
				
			}
		});
		
		createToolbarActions();
		enableActions(false);
	}
	
	private void enableActions(boolean enable) {
		IToolBarManager toolBarMan = this.getViewSite().getActionBars().getToolBarManager();
		for (IContributionItem item : toolBarMan.getItems()){
			if (item instanceof ActionContributionItem) ((ActionContributionItem)item).getAction().setEnabled(enable);
		}
	}
	
	private IViewPart getView(String viewID) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IViewPart view = page.findView(viewID);
		if (view != null) {
			return view;
		} else {
			try {
				view = page.showView(DiffractionPlotView.ID, null, IWorkbenchPage.VIEW_CREATE);
				return view;
			} catch (PartInitException e) {
				logger.error("Could not retrieve the Diffraction Plot View:"+e);
				e.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Initialise the plottingSystem and the tools (diffraction and powder)
	 */
	private void initializeSystems() {

		augmenter.activate();
		augmenter.drawBeamCentre(true);
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		augmenter.drawCalibrantRings(true, standards.getCalibrant());
//		calibrantPositioning.setRingFinder(ringFindJob);
	}

	private void initializeListeners(){

		partListener = new PartListener2Stub() {
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == DiffractionCalibrationView.this) {
					if (plottingSystem != null) {
						IWorkbenchPart part = plottingSystem.getPart();
						if (part != null) {
							part.getSite().getPage().activate(part);
						}
						if (augmenter != null && !augmenter.isActive()) augmenter.activate();
					}
				}
			}
		};

		this.getViewSite().getPage().addPartListener(partListener);
	}
	
	protected void updateSelection(boolean force) {
		ISelection is = diffractionTableViewer.getSelection();
		if (is instanceof StructuredSelection) {
			StructuredSelection structSelection = (StructuredSelection) is;
			DiffractionTableData selectedData = (DiffractionTableData) structSelection.getFirstElement();
			
			if (augmenter != null) {
				if (structSelection.isEmpty()) 
					augmenter.deactivate(false);
			}
			
			if (selectedData == null || (!force && selectedData == manager.getCurrentData())) {
				return;
			}
			drawSelectedData(selectedData);
			//showCalibrantAndBeamCentre(checked);
		}
	}

	private void createToolbarActions() {
		IToolBarManager toolBarMan = this.getViewSite().getActionBars().getToolBarManager();
		
		IAction wizAction = new Action("Wizard") {
			@Override
			public void run() {
				try {
					PowderCalibrationWizard wiz = new PowderCalibrationWizard(manager);
					wiz.setNeedsProgressMonitor(true);
					final WizardDialog wd = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),wiz);
					
					wd.setPageSize(new Point(900, 500));
					wd.create();
					wd.open();
				} catch (Exception e) {
					logger.error("Problem opening import!", e);
				}
			}
		};
		wizAction.setImageDescriptor(Activator.getImageDescriptor("icons/wand.png"));

		IAction importAction = new Action("Import metadata from file") {
			@Override
			public void run() {
				try {
					IWizard wiz = EclipseUtils.openWizard(PersistenceImportWizard.ID, false);
					WizardDialog wd = new  WizardDialog(Display.getCurrent().getActiveShell(), wiz);
					wd.setTitle(wiz.getWindowTitle());
					wd.open();
				} catch (Exception e) {
					logger.error("Problem opening import!", e);
				}
			}
		};
		importAction.setImageDescriptor(Activator.getImageDescriptor("icons/mask-import-wiz.png"));

		IAction exportAction = new Action("Export metadata to file") {
			@Override
			public void run() {
				try {
					FileSelectionDialog dialog = new FileSelectionDialog(Display.getDefault().getActiveShell());
//					FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
					dialog.setNewFile(true);
					dialog.setFolderSelector(false);
					if (lastPath != null) {
						File f = new File(lastPath);
						if (!f.isDirectory()) {
							lastPath = f.getParent();
						}
						dialog.setPath(lastPath + File.separator + "calibration_output.nxs");
					} else {
						dialog.setPath(System.getProperty("user.home")+ File.separator + "calibration_output.nxs");
					}
					
					dialog.create();
					if (dialog.open() == Dialog.OK) {
						lastPath = dialog.getPath();
						DiffractionCalibrationUtils.saveToNexusFile(manager, lastPath);
					}

				} catch (Exception e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "File save error!", "Could not save calibration file! (Do you have write access to this folder?)");
					logger.error("Problem opening export!", e);
				}
			}
		};
		exportAction.setImageDescriptor(Activator.getImageDescriptor("icons/mask-export-wiz.png"));

		IAction exportToXLSAction = new Action("Export metadata to XLS") {
			@Override
			public void run() {
				FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
				dialog.setText("Save metadata to Comma Separated Value file");
				dialog.setFilterNames(new String[] { "CSV Files", "All Files (*.*)" });
				dialog.setFilterExtensions(new String[] { "*.csv", "*.*" });
				//dialog.setFilterPath("c:\\"); // Windows path
				dialog.setFileName("metadata.csv");
				dialog.setOverwrite(true);
				String savedFilePath = dialog.open();
				if (savedFilePath != null) {
					DiffractionCalibrationUtils.saveModelToCSVFile(manager, savedFilePath);
				}
			}
		};
		exportToXLSAction.setImageDescriptor(Activator.getImageDescriptor("icons/page_white_excel.png"));

		IAction resetRingsAction = new Action("Remove found rings") {
			@Override
			public void run() {
				PowderCalibrationUtils.clearFoundRings(plottingSystem);
				Iterable<DiffractionTableData> iterable = manager.iterable();
				for (DiffractionTableData d : iterable) d.clearROIs();
			}
		};
		resetRingsAction.setImageDescriptor(Activator.getImageDescriptor("icons/reset_rings.png"));

		IAction resetTableAction = new Action("Reset metadata") {
			@Override
			public void run() {
				// select last item in table
				if (manager.isValidModel()) {
					diffractionTableViewer.setSelection(new StructuredSelection(manager.getLast()));
					manager.reset();
					diffractionTableViewer.refresh();
				}
			}
		};
		resetTableAction.setImageDescriptor(Activator.getImageDescriptor("icons/table_delete.png"));

		toolBarMan.add(wizAction);
		toolBarMan.add(importAction);
		toolBarMan.add(exportAction);
		toolBarMan.add(exportToXLSAction);
		toolBarMan.add(resetRingsAction);
		toolBarMan.add(resetTableAction);
	}

	private void updateAfterCalibration() {
		if (manager.isEmpty()) return;
		updateSelection(true);
		IPowderCalibrationInfo calInfo = manager.getCurrentData().getCalibrationInfo();
		if (calInfo != null) {
			String resultDescription = calInfo.getResultDescription();
			resultText.setText(resultDescription);
		}
	}

//	private void enableControl(Group group, boolean enabled) {
//		for (Control child : group.getChildren())
//			  child.setEnabled(enabled);
//		
//		group.setEnabled(enabled);
//	}
 	

	private void drawSelectedData(DiffractionTableData data) {

		if (data.getImage() == null)
			return;

		if (plottingSystem == null){
			initializeSystems();
		}
		plottingSystem.clear();
		plottingSystem.updatePlot2D(data.getImage(), null, null);
		plottingSystem.setTitle(data.getName() == null ? "Data" : data.getName());
		plottingSystem.getAxes().get(0).setTitle("");
		plottingSystem.getAxes().get(1).setTitle("");
		plottingSystem.setKeepAspect(true);
		//plottingSystem.setShowIntensity(false);

		updateCurrentData(data);

//		calibrantPositioning.setDiffractionData(manager.getCurrentData());
//		calibrantPositioning.setRingFinder(ringFindJob);
		if (data.getMetaData() != null) {
		augmenter.setDiffractionMetadata(manager.getCurrentData().getMetaData());
		
//		if (!augmenter.isActive() && checked) augmenter.activate();
		
		diffractionTableViewer.addDetectorPropertyListener(data);
		}
		
		PowderCalibrationUtils.clearFoundRings(plottingSystem);
		
	}
	
	private void updateCurrentData(DiffractionTableData data) {
		manager.setCurrentData(data);
		widget.update();
		
	}

	private void removeListeners() {
//		CalibrationFactory.removeCalibrantSelectionListener(calibrantChangeListener);

		if (augmenter != null)
			augmenter.deactivate(false);
		
		manager.clear(diffractionTableViewer.getDetectorPropertyListener());
		logger.debug("model emptied");
		
		this.getViewSite().getPage().removePartListener(partListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		manager.dispose();
		removeListeners();
	}

	@Override
	public void setFocus() {
		if (diffractionTableViewer != null && !diffractionTableViewer.isDisposed())
			diffractionTableViewer.setFocus();
	}
}
