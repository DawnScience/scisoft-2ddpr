package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.wizard.persistence.PersistenceExportWizard;
import org.dawb.common.ui.wizard.persistence.PersistenceImportWizard;
import org.dawb.workbench.ui.diffraction.CalibrantPositioningWidget;
import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.table.DiffCalTableViewer;
import org.dawb.workbench.ui.diffraction.table.DiffractionDataChanged;
import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawb.workbench.ui.diffraction.table.IDiffractionDataListener;
import org.dawnsci.common.widgets.radio.RadioGroupWidget;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.preference.diffraction.DiffractionPreferencePage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
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
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePointsParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromPointsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromRingsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationPreferencePage;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.RingSelectionGroup;

public class DiffractionCalibrationView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionCalibrationView";
	private final Logger logger = LoggerFactory.getLogger(DiffractionCalibrationView.class);

	private final String DATA_PATH = "DataPath";
	private final String CALIBRANT = "Calibrant";
	private final String RINGS = "Rings";

	private DiffractionDataManager manager;
	
	private List<String> pathsList = new ArrayList<String>();
	private CalibrationStandards standards;
	private Map<String, Integer> calibrantRingsMap = new HashMap<String, Integer>();

	// TODO FIXME - So much member data can lead to bugs
	private Composite parent;
	private DiffCalTableViewer diffractionTableViewer;
	private Button calibrateImagesButton;
	private Combo calibrantCombo;
	private CalibrantPositioningWidget calibrantPositioning;
	private Label residualLabel;
	private Button usePointCalibration;
	private Button optimiseAfter;
	private Group pointCalibrateGroup;
	private Group ellipseParamGroup;
	private RadioGroupWidget calibEllipseParamRadios;
	private POIFindingRun ringFindJob;
	private DiffractionImageAugmenter augmenter;
	CalibratePointsParameterModel pointParameters = new CalibratePointsParameterModel();
	SimpleCalibrationParameterModel ellipseParameters = new SimpleCalibrationParameterModel();
	
	private static final String RESIDUAL = "Residual: ";

	private IPlottingSystem plottingSystem;

	private ISelectionChangedListener selectionChangeListener;
	private CalibrantSelectedListener calibrantChangeListener;
	private IPartListener2 partListener;

	private boolean checked = true;
	private String calibrantName;
	private int ringNumberSaved;
	private RingSelectionGroup ringSelection;

	public DiffractionCalibrationView() {
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		init(site);
		setSite(site);
		setPartName("Diffraction Calibration View");

		if (memento != null) {
			for (String k : memento.getAttributeKeys()) {
				if (k.startsWith(DATA_PATH)) {
					int i = Integer.parseInt(k.substring(DATA_PATH.length()));
					pathsList.add(i, memento.getString(k));
				}
				if (k.startsWith(CALIBRANT)) {
					calibrantName = memento.getString(k);
				}
				if (k.startsWith(RINGS)) {
					ringNumberSaved = memento.getInteger(k);
				}
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		if (memento != null) {
			int i = 0;
			for (DiffractionTableData data : manager.iterable()) {
				memento.putString(DATA_PATH + String.valueOf(i++), data.path);
			}
			memento.putString(CALIBRANT, calibrantCombo.getItem(calibrantCombo.getSelectionIndex()));
			memento.putInteger(RINGS, ringSelection.getRingSpinnerSelection());
		}
	}

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());

		this.parent = parent;
		
		initializeListeners();
		standards = CalibrationFactory.getCalibrationStandards();

		Composite controlComp = new Composite(parent, SWT.NONE);
		controlComp.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(controlComp);
		createToolbarActions(controlComp);

		Label instructionLabel = new Label(controlComp, SWT.WRAP);
		instructionLabel.setText("Drag/drop a file/data to the table below, " +
				"choose the calibrant, " +
				"select the rings to use " +
				"and finally run the calibration.");
		instructionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		Point pt = instructionLabel.getSize(); pt.x +=4; pt.y += 4; instructionLabel.setSize(pt);

		// make a scrolled composite
		ScrolledComposite scrollComposite = new ScrolledComposite(controlComp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrollComposite.setLayout(new GridLayout(1, false));
		scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite scrollHolder = new Composite(scrollComposite, SWT.NONE);
		scrollHolder.setLayout(new GridLayout(1, false));
		
		manager = new DiffractionDataManager();
		
		// table of images and found rings
		diffractionTableViewer = new DiffCalTableViewer(scrollHolder, pathsList, manager);
		diffractionTableViewer.addSelectionChangedListener(selectionChangeListener);
		

		Composite mainHolder = new Composite(scrollHolder, SWT.NONE);
		mainHolder.setLayout(new GridLayout(1, false));
		mainHolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		// create calibrant combo
		Composite topComp = new Composite(mainHolder, SWT.NONE);
		topComp.setLayout(new GridLayout(2, false));
		topComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createCalibrantGroup(topComp);

		ringSelection = new RingSelectionGroup(mainHolder, standards.getCalibrant().getHKLs().size());
		ringSelection.addRingNumberSpinnerListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ringNumber = ringSelection.getRingSpinnerSelection();
				// Fill the Map with ring number for the selected calibrant
				calibrantRingsMap.put(calibrantName, ringNumber);
			}
		});
		
//		instructionLabel = new Label(mainHolder, SWT.WRAP);
//		instructionLabel.setText("Use auto calibration when images have complete rings" +
//				" and only small tilts - for all other data use manual");
//		instructionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		//TabFolder
		final TabFolder tabFolder = new TabFolder(mainHolder, SWT.BORDER | SWT.FILL);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		TabItem autoTabItem = new TabItem(tabFolder, SWT.FILL);
		autoTabItem.setText("Auto");
		autoTabItem.setToolTipText("Automatic calibration");
		autoTabItem.setControl(getAutoTabControl(tabFolder));
		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				
				diffractionTableViewer.updateTableColumnsAndLayout(tabFolder.getSelectionIndex());
			}
		});
		
		manager.addFileListener(new IDiffractionDataListener() {
			
			@Override
			public void dataChanged(final DiffractionDataChanged event) {
				
				Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
						diffractionTableViewer.refresh();
						diffractionTableViewer.updateTableColumnsAndLayout(tabFolder.getSelectionIndex());
						diffractionTableViewer.setSelection(new StructuredSelection(event.getSource()),true);
					}
				});
			}
		});

		TabItem manualTabItem = new TabItem(tabFolder, SWT.FILL);
		manualTabItem.setText("Manual");
		manualTabItem.setToolTipText("Manual calibration");
		manualTabItem.setControl(getManualTabControl(tabFolder));

		TabItem settingTabItem = new TabItem(tabFolder, SWT.FILL);
		settingTabItem.setText("Settings");
		settingTabItem.setToolTipText("Calibration settings");
		settingTabItem.setControl(getSettingTabControl(tabFolder, standards));

		residualLabel = new Label(mainHolder, SWT.NONE);
		residualLabel.setText(RESIDUAL);
		residualLabel.setLayoutData(new GridData());

		scrollHolder.layout();
		scrollComposite.setContent(scrollHolder);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.setExpandVertical(true);
		Rectangle r = scrollHolder.getClientArea();
		scrollComposite.setMinSize(scrollHolder.computeSize(r.width, SWT.DEFAULT));
		scrollComposite.layout();
		// end of Diffraction Calibration controls

		// try to load the previous data saved in the memento
		for (String p : pathsList) {
			if (!p.endsWith(".nxs")) {
				manager.loadData(p, null);
			}
		}

		CalibrationFactory.addCalibrantSelectionListener(calibrantChangeListener);

		calibrantPositioning.setControlsToUpdate(calibrateImagesButton);
		calibrantPositioning.setTableViewerToUpdate(diffractionTableViewer);

		//initialize the calibrant ring Map
		calibrantRingsMap.put(calibrantName, ringNumberSaved);
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

		IViewPart plotView = getView(DiffractionPlotView.ID);
		plottingSystem = (IPlottingSystem)plotView.getAdapter(IPlottingSystem.class);
		if (plottingSystem != null && plottingSystem.isDisposed()) { // if we close the perspective then reopen it
			plottingSystem = PlottingFactory.getPlottingSystem(DiffractionPlotView.DIFFRACTION_PLOT_TITLE);
		}

		// set the focus on the plotting system to trigger the tools (powder tool)
		IWorkbenchPart part = plottingSystem.getPart();
		if (part != null) {
			part.getSite().getPage().activate(part);
		}
		augmenter = new DiffractionImageAugmenter(plottingSystem);
		augmenter.activate();
		augmenter.drawBeamCentre(true);
		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		augmenter.drawCalibrantRings(true, standards.getCalibrant());
		
		ringFindJob = new POIFindingRun(plottingSystem, manager.getCurrentData(), ringSelection);
		calibrantPositioning.setRingFinder(ringFindJob);
	}

	private void initializeListeners(){
		// selection change listener for table viewer
		selectionChangeListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(false);
				if (manager.isEmpty()) {
					diffractionTableViewer.refresh();
					updateCurrentData(null); // need to reset this
					plottingSystem.clear();
					PowderCalibrationUtils.clearFoundRings(plottingSystem);
					calibrateImagesButton.setEnabled(false);
					residualLabel.setText(RESIDUAL);
					residualLabel.getParent().layout();
				}
				
				setSingleImageOptionsEnabled(manager.getSize() < 2);
			}
		};

		calibrantChangeListener = new CalibrantSelectedListener() {
			@Override
			public void calibrantSelectionChanged(CalibrantSelectionEvent evt) {
				final int index = calibrantCombo.getSelectionIndex();
				if (index>-1 && calibrantCombo.getItems()[index].equals(evt.getCalibrant())) return;
				setCalibrantChoice();
				ringSelection.setMaximumRingNumber(CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size());

				if (manager.getCurrentData() != null)
					showCalibrantAndBeamCentre(checked);
			}
		};

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

	private void setSingleImageOptionsEnabled(boolean enabled) {
		usePointCalibration.setEnabled(enabled);
		optimiseAfter.setEnabled(enabled);
		enableControl(ellipseParamGroup, enabled);
		
		if (usePointCalibration.getSelection()) enableControl(pointCalibrateGroup, enabled);
	}
	
	private void setCalibrantChoice() {
		final List<String> cl = standards.getCalibrantList();
		calibrantCombo.setItems(cl.toArray(new String[cl.size()]));
		String selCalib = standards.getSelectedCalibrant();
		final int index = cl.indexOf(selCalib);
		calibrantCombo.select(index); 
	}

	private void setUpCalbrationModel(SimpleCalibrationParameterModel model) {
		model.setNumberOfRings(ringSelection.getRingSpinnerSelection());
		model.setRingSet(ringSelection.getRingSelectionText().getUniqueRingNumbers());
		model.setUseRingSet(!ringSelection.isUsingRingSpinner());
		model.setFinalGlobalOptimisation(optimiseAfter.getSelection());
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

	/**
	 * Shows/Hides the calibrant and beam centre
	 * @param show
	 * @param currentData
	 */
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

	private void updateScrolledComposite() {
		// reset the scroll composite
		Rectangle r = parent.getClientArea();
		if (parent.getParent() instanceof ScrolledComposite) {
			ScrolledComposite scrollHolder = (ScrolledComposite) parent
					.getParent();
			scrollHolder.setMinSize(parent.computeSize(r.width, SWT.DEFAULT));
			scrollHolder.layout();
		}
	}

	private void createCalibrantGroup(Composite composite) {
		Group selectCalibComp = new Group(composite, SWT.FILL);
		selectCalibComp.setText("Select calibrant:");
		selectCalibComp.setLayout(new GridLayout(1, false));
		selectCalibComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite comp = new Composite(selectCalibComp, SWT.NONE);
		comp.setLayout(new GridLayout(2, true));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		calibrantCombo = new Combo(comp, SWT.READ_ONLY);
		calibrantCombo.setToolTipText("Select a type of calibrant");
		calibrantCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		calibrantCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (manager.getCurrentData() == null)
					return;
				int index = calibrantCombo.getSelectionIndex();
				calibrantName = calibrantCombo.getItem(index);
				// update the calibrant in diffraction tool
				standards.setSelectedCalibrant(calibrantName, true);
				// set the maximum number of rings
				int ringMaxNumber = standards.getCalibrant().getHKLs().size();
				ringSelection.setMaximumRingNumber(ringMaxNumber);
				// Set the calibrant ring number
				if (calibrantRingsMap.containsKey(calibrantName)) {
					ringSelection.setRingSpinnerSelection(calibrantRingsMap.get(calibrantName));
				} else {
					calibrantRingsMap.put(calibrantName, ringMaxNumber);
					ringSelection.setRingSpinnerSelection(ringMaxNumber);
				}
				// Tell the ring selection field about the maximum number allowed
				
			}
		});
		for (String c : standards.getCalibrantList()) {
			calibrantCombo.add(c);
		}
		String s = standards.getSelectedCalibrant();
		if (s != null) {
			calibrantCombo.setText(s);
		}

		Button configCalibrantButton = new Button(comp, SWT.NONE);
		configCalibrantButton.setText("Configure...");
		configCalibrantButton.setToolTipText("Open Calibrant configuration page");
		configCalibrantButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		configCalibrantButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil
						.createPreferenceDialogOn(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								DiffractionPreferencePage.ID, null, null);
				if (pref != null)
					pref.open();
			}
		});

		final Button showCalibAndBeamCtrCheckBox = new Button(selectCalibComp, SWT.CHECK);
		showCalibAndBeamCtrCheckBox.setText("Show Calibrant and Beam Centre");
		showCalibAndBeamCtrCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checked = showCalibAndBeamCtrCheckBox.getSelection();
				showCalibrantAndBeamCentre(checked);
			}
		});
		showCalibAndBeamCtrCheckBox.setSelection(true);
	}

	private void createToolbarActions(Composite parent) {
		IToolBarManager toolBarMan = this.getViewSite().getActionBars().getToolBarManager();

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
					IWizard wiz = EclipseUtils.openWizard(PersistenceExportWizard.ID, false);
					WizardDialog wd = new  WizardDialog(Display.getCurrent().getActiveShell(), wiz);
					wd.setTitle(wiz.getWindowTitle());
					wd.open();
				} catch (Exception e) {
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
				//DiffractionCalibrationUtils.hideFoundRings(plottingSystem);
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

		toolBarMan.add(importAction);
		toolBarMan.add(exportAction);
		toolBarMan.add(exportToXLSAction);
		toolBarMan.add(resetRingsAction);
		toolBarMan.add(resetTableAction);
	}

	/**
	 * Gets the control for the automatic tab
	 * 
	 * @param tabFolder
	 *            the parent tab folder
	 * @return Control
	 */
	private Control getAutoTabControl(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.FILL);
		composite.setLayout(new GridLayout(1, false));
		
		Button goBabyGoButton = new Button(composite, SWT.PUSH);
		goBabyGoButton.setImage(Activator.getImage("icons/CalibrationRun.png"));
		goBabyGoButton.setText("Run Auto Calibration");
		goBabyGoButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		goBabyGoButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setUpCalbrationModel(ellipseParameters);
				IRunnableWithProgress job = new AutoCalibrationRun(Display.getDefault(), plottingSystem, manager, manager.getCurrentData(), ellipseParameters);

				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				try {
					dia.run(true, true, job);
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Calibration Error", "An error occured: " + e1.getTargetException().getMessage());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					logger.error("Error running Job:" + e1.getMessage());
				}
				
				updateAfterCalibration();
				
			}
		});
		
		optimiseAfter = new Button(composite, SWT.CHECK);
		optimiseAfter.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		optimiseAfter.setText("Finish with point calibration optimisation");
		
		return composite;
	}

	/**
	 * Gets the control for the manual tab
	 * 
	 * @param tabFolder
	 *            the parent tab folder
	 * @param model
	 * @return Control
	 */
	private Control getManualTabControl(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		calibrantPositioning = new CalibrantPositioningWidget(composite, manager);

		calibrateImagesButton = new Button(composite, SWT.PUSH);
		calibrateImagesButton.setText("Run Calibration Process");
		calibrateImagesButton.setToolTipText("Calibrate detector in chosen images");
		calibrateImagesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		calibrateImagesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				IRunnableWithProgress job = null;
				
				if (usePointCalibration.getSelection() && manager.getSize() == 1) {
					setUpCalbrationModel(pointParameters);
					job = new FromPointsCalibrationRun(Display.getDefault(), plottingSystem, manager, manager.getCurrentData(), pointParameters);
				} else {
					setUpCalbrationModel(ellipseParameters);
					job = new FromRingsCalibrationRun(Display.getDefault(), plottingSystem, manager, manager.getCurrentData(), ellipseParameters);
				}
				
				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				try {
					dia.run(true, true, job);
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Calibration Error", "An error occured: " + e1.getTargetException().getMessage());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					logger.error("Error running Job:" + e1.getMessage());
				}
				updateAfterCalibration();
			}
		});
		calibrateImagesButton.setEnabled(false);
		
		usePointCalibration = new Button(composite, SWT.CHECK | SWT.WRAP);
		usePointCalibration.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 2, 1));
		usePointCalibration.setText("Manual calibration uses points not ellipse parameters");
		usePointCalibration.setSelection(false);
		
		return composite;
	}
	
	private void updateAfterCalibration() {
		updateScrolledComposite();
		updateSelection(true);
		
		residualLabel.setText(RESIDUAL + manager.getCurrentData().residual);
		residualLabel.getParent().layout();
	}

	/**
	 * Gets the control for the setting tab
	 * 
	 * @param tabFolder
	 *            the parent tab folder
	 * @return Control
	 */
	private Control getSettingTabControl(TabFolder tabFolder, CalibrationStandards standards) {
		Composite composite = new Composite(tabFolder, SWT.FILL);
		composite.setLayout(new GridLayout(2, false));

		ellipseParamGroup = new Group(composite, SWT.FILL);
		ellipseParamGroup.setText("Ellipse Parameters");
		ellipseParamGroup.setToolTipText("Set the Ellipse Parameters");
		ellipseParamGroup.setLayout(new GridLayout());
		ellipseParamGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		calibEllipseParamRadios = new RadioGroupWidget(ellipseParamGroup);
		calibEllipseParamRadios.setActions(getEllipseParamActions());

		pointCalibrateGroup = new Group(composite, SWT.FILL);
		pointCalibrateGroup.setText("Point Calibrate");
		pointCalibrateGroup.setToolTipText("Set the Point Parameters");
		pointCalibrateGroup.setLayout(new GridLayout());
		pointCalibrateGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Button fixEnergyButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixEnergyButton.setText("Fix Energy");
		fixEnergyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pointParameters.setFloatEnergy(!((Button)e.getSource()).getSelection());
			}
		});
		Button fixDistanceButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixDistanceButton.setText("Fix Distance");
		fixDistanceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pointParameters.setFloatDistance(!((Button)e.getSource()).getSelection());
			}
		});
		Button fixBeamCentreButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixBeamCentreButton.setText("Fix Beam Centre");
		fixBeamCentreButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pointParameters.setFloatBeamCentre(!((Button)e.getSource()).getSelection());
			}
		});
		Button fixTiltButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixTiltButton.setText("Fix Tilt");
		fixTiltButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pointParameters.setFloatTilt(!((Button)e.getSource()).getSelection());
			}
		});
		
		usePointCalibration.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isManual = usePointCalibration.getSelection();
				enableControl(pointCalibrateGroup, isManual);
			}
		});
		
		Button advanced = new Button(composite, SWT.NONE);
		advanced.setText("Advanced...");
		advanced.setToolTipText("Open preference page for advanced settings");
		advanced.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
		advanced.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil
						.createPreferenceDialogOn(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								DiffractionCalibrationPreferencePage.ID, null, null);
				if (pref != null)
					pref.open();
			}
		});
		
		enableControl(pointCalibrateGroup, false);

		return composite;
	}

	private void enableControl(Group group, boolean enabled) {
		for (Control child : group.getChildren())
			  child.setEnabled(enabled);
		
		group.setEnabled(enabled);
	}
 	

	private List<Action> getEllipseParamActions() {
		List<Action> radioActions = new ArrayList<Action>();
		Action fixNoneAction = new Action() {
			@Override
			public void run() {
				ellipseParameters.setFloatDistance(true);
				ellipseParameters.setFloatEnergy(true);
			}
		};
		fixNoneAction.setText("Fix None");
		fixNoneAction.setToolTipText("No parameter is fixed");

		Action fixEnergyAction = new Action() {
			@Override
			public void run() {
				ellipseParameters.setFloatDistance(true);
				ellipseParameters.setFloatEnergy(false);
			}
		};
		fixEnergyAction.setText("Fix Energy");
		fixEnergyAction.setToolTipText("Energy parameter is fixed");

		Action fixDistanceAction = new Action() {
			@Override
			public void run() {
				ellipseParameters.setFloatDistance(false);
				ellipseParameters.setFloatEnergy(true);
			}
		};
		fixDistanceAction.setText("Fix Distance");
		fixDistanceAction.setToolTipText("Distance parameter is fixed");

		radioActions.add(fixNoneAction);
		radioActions.add(fixEnergyAction);
		radioActions.add(fixDistanceAction);
		return radioActions;
	}

	private void drawSelectedData(DiffractionTableData data) {

		if (data.image == null)
			return;

		if (plottingSystem == null){
			initializeSystems();
		}
		plottingSystem.clear();
		plottingSystem.updatePlot2D(data.image, null, null);
		plottingSystem.setTitle(data.name);
		plottingSystem.getAxes().get(0).setTitle("");
		plottingSystem.getAxes().get(1).setTitle("");
		plottingSystem.setKeepAspect(true);
		plottingSystem.setShowIntensity(false);

		updateCurrentData(data);

		calibrantPositioning.setDiffractionData(manager.getCurrentData());
		if (data.md != null) {
		augmenter.setDiffractionMetadata(manager.getCurrentData().md);
		
		if (!augmenter.isActive() && checked) augmenter.activate();
		
		diffractionTableViewer.addDetectorPropertyListener(data);
		}
		
		PowderCalibrationUtils.clearFoundRings(plottingSystem);
		
	}
	
	private void updateCurrentData(DiffractionTableData data) {
		manager.setCurrentData(data);
		if (ringFindJob != null) ringFindJob.setCurrentData(manager.getCurrentData());
		
	}

	private void removeListeners() {
		if(diffractionTableViewer != null) {
			diffractionTableViewer.removeSelectionChangedListener(selectionChangeListener);
		}
		CalibrationFactory.removeCalibrantSelectionListener(calibrantChangeListener);

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
		if (parent != null && !parent.isDisposed())
			parent.setFocus();
	}

	/**
	 * Needed to retrieve the plotting system
	 */
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
		if (key == IPlottingSystem.class)
			return plottingSystem;
		if (key == DiffractionDataManager.class)
			return manager;
		return null;
	}
}
