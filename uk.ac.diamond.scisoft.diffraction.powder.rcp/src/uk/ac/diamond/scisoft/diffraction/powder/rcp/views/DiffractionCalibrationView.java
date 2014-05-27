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
import org.dawb.workbench.ui.diffraction.table.DiffractionDataChanged;
import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionDelegate;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawb.workbench.ui.diffraction.table.IDiffractionDataListener;
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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
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
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePointsParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromPointsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromRingsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;
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
	private DiffractionDelegate diffractionTableViewer;
	private Button calibrateImagesButton;
	private Combo calibrantCombo;
	private CalibrantPositioningWidget calibrantPositioning;
	private Label residualLabel;
	private Button usePointCalibration;
	private Button optimiseAfter;
	private POIFindingRun ringFindJob;
	private DiffractionImageAugmenter augmenter;
	
	// Actual data
	private CalibratePointsParameterModel   pointParameters   = new CalibratePointsParameterModel();
	private SimpleCalibrationParameterModel ellipseParameters = new SimpleCalibrationParameterModel();
	
	private static final String RESIDUAL = "Residual: ";

	private IPlottingSystem plottingSystem;

	private ISelectionChangedListener selectionChangeListener;
	private CalibrantSelectedListener calibrantChangeListener;
	private IPartListener2 partListener;

	private boolean checked = true;
	private String calibrantName;
	private int ringNumberSaved;
	private RingSelectionGroup ringSelection;

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
				memento.putString(DATA_PATH + String.valueOf(i++), data.getPath());
			}
			memento.putString(CALIBRANT, calibrantCombo.getItem(calibrantCombo.getSelectionIndex()));
			memento.putInteger(RINGS, ringSelection.getRingSpinnerSelection());
		}
	}

	@Override
	public void createPartControl(final Composite parent) {

		final ScrolledComposite scrollComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);

		final Composite content = new Composite(scrollComposite, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initializeListeners();
		standards = CalibrationFactory.getCalibrationStandards();	
		manager = new DiffractionDataManager();

		// table of images and found rings
		diffractionTableViewer = new DiffractionDelegate(content, pathsList, manager);
		diffractionTableViewer.addSelectionChangedListener(selectionChangeListener);

		// create calibrant combo
		createCalibrantGroup(content);

		ringSelection = new RingSelectionGroup(content, standards.getCalibrant().getHKLs().size());
		ringSelection.addRingNumberSpinnerListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ringNumber = ringSelection.getRingSpinnerSelection();
				// Fill the Map with ring number for the selected calibrant
				calibrantRingsMap.put(calibrantName, ringNumber);
			}
		});
		
		new Label(content, SWT.NONE);
		
		final Group run  = new Group(content, SWT.NONE);
		run.setText("Run Calibration");
		run.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		run.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(run);
		
		final Composite choiceLine = new Composite(run, SWT.NONE);
		choiceLine.setLayout(new GridLayout(2, false));
		choiceLine.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
        final Button autoRadio = new Button(choiceLine, SWT.RADIO);
        autoRadio.setText("Automatic");
        autoRadio.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        autoRadio.setSelection(true);
        
        final Button manualRadio = new Button(choiceLine, SWT.RADIO);
        manualRadio.setText("Manual");
        manualRadio.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
        manualRadio.setSelection(false);
		
		final Composite choiceContent = new Composite(run, SWT.NONE);
		final StackLayout stackLayout = new StackLayout();
		choiceContent.setLayout(stackLayout);
		choiceContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Composite auto   = getAutoControl(choiceContent);
		stackLayout.topControl = auto; // TODO Remember...
		final Composite manual = getManualControl(choiceContent);

		scrollComposite.setContent(content);
		scrollComposite.setExpandVertical(true);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle r = scrollComposite.getClientArea();
				int height = content.computeSize(r.width, SWT.DEFAULT).y;
				scrollComposite.setMinHeight(height);
				scrollComposite.setMinWidth(content.computeSize(SWT.DEFAULT, r.height).x);
			}
		});

        autoRadio.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		autoRadio.setSelection(true);
                manualRadio.setSelection(false);
                setCalibrationChoice(auto, 0, stackLayout);
        	}
		});
        manualRadio.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
                autoRadio.setSelection(false);
                manualRadio.setSelection(true);
                setCalibrationChoice(manual, 1, stackLayout);
        	}
		});
		manager.addFileListener(new IDiffractionDataListener() {
			
			@Override
			public void dataChanged(final DiffractionDataChanged event) {
				
				Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
						diffractionTableViewer.refresh();
						diffractionTableViewer.updateTableColumnsAndLayout(calibrationTypeIndex);
						diffractionTableViewer.setSelection(new StructuredSelection(event.getSource()),true);
					}
				});
			}
		});
		
		final Link settings = new Link(content, SWT.RIGHT);
		settings.setText("<a>Calibration Settings</a>");
		settings.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		settings.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DiffractionCalibrationSettings dialog = new DiffractionCalibrationSettings(settings.getShell(),
						                                                                   pointParameters, 
						                                                                   ellipseParameters,
						                                                                   standards,
						                                                                   calibrationTypeIndex,
						                                                                   usePointCalibration.getSelection());
				dialog.open();
			}
		});
		
		residualLabel = new Label(content, SWT.NONE);
		residualLabel.setText(RESIDUAL);
		residualLabel.setLayoutData(new GridData());

		// try to load the previous data saved in the memento
		for (String p : pathsList) {
			if (!p.endsWith(".nxs")) {
				manager.loadData(p, null);
			}
		}

		CalibrationFactory.addCalibrantSelectionListener(calibrantChangeListener);

		calibrantPositioning.setControlsToUpdate(calibrateImagesButton);
		calibrantPositioning.setRefreshable(diffractionTableViewer);

		//initialize the calibrant ring Map
		calibrantRingsMap.put(calibrantName, ringNumberSaved);
		
		createToolbarActions();
	}
	private int calibrationTypeIndex = 0;
	public void setCalibrationChoice(Composite comp, int index, StackLayout stackLayout) {
		calibrationTypeIndex = index;
		stackLayout.topControl = comp;	
		comp.getParent().layout();
		diffractionTableViewer.updateTableColumnsAndLayout(index);
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

	private void createToolbarActions() {
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
	 * @param content
	 *            the parent tab folder
	 * @return Control
	 */
	private Composite getAutoControl(Composite content) {
		
		Composite composite = new Composite(content, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
				
		new Label(composite, SWT.NONE);

		optimiseAfter = new Button(composite, SWT.CHECK);
		optimiseAfter.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		optimiseAfter.setText("Finish with point calibration optimisation");
		
		new Label(composite, SWT.NONE);
	
		Button goBabyGoButton = new Button(composite, SWT.PUSH);
		goBabyGoButton.setImage(Activator.getImage("icons/CalibrationRun.png"));
		goBabyGoButton.setText("Run Calibration");
		goBabyGoButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
		goBabyGoButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (manager.isEmpty()) {
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Load Data", "Please load some data!");
				}
				setUpCalbrationModel(ellipseParameters);
				IRunnableWithProgress job = new AutoCalibrationRun(Display.getDefault(), plottingSystem, manager, manager.getCurrentData(), ellipseParameters);

				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
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

		return composite;
	}

	/**
	 * Gets the control for the manual tab
	 * 
	 * @param content
	 *            the parent tab folder
	 * @param model
	 * @return Control
	 */
	private Composite getManualControl(Composite content) {
		
		Composite composite = new Composite(content, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		calibrantPositioning = new CalibrantPositioningWidget(composite, manager);

		new Label(composite, SWT.NONE);

		usePointCalibration = new Button(composite, SWT.CHECK | SWT.WRAP);
		usePointCalibration.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 2, 1));
		usePointCalibration.setText("Use point parameters");
		usePointCalibration.setToolTipText("Tick to use point instead of ellipse parameters");
		usePointCalibration.setSelection(false);
		
		new Label(composite, SWT.NONE);

		calibrateImagesButton = new Button(composite, SWT.PUSH);
		calibrateImagesButton.setImage(Activator.getImage("icons/CalibrationRun.png"));
		calibrateImagesButton.setText("Run Calibration");
		calibrateImagesButton.setToolTipText("Calibrate detector in chosen images");
		calibrateImagesButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
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
		
		return composite;
	}
	
	private void updateAfterCalibration() {
		if (manager.isEmpty()) return;
		updateSelection(true);
		residualLabel.setText(RESIDUAL + manager.getCurrentData().getResidual());
		residualLabel.getParent().layout();
	}

	private void enableControl(Group group, boolean enabled) {
		for (Control child : group.getChildren())
			  child.setEnabled(enabled);
		
		group.setEnabled(enabled);
	}
 	

	private void drawSelectedData(DiffractionTableData data) {

		if (data.getImage() == null)
			return;

		if (plottingSystem == null){
			initializeSystems();
		}
		plottingSystem.clear();
		plottingSystem.updatePlot2D(data.getImage(), null, null);
		plottingSystem.setTitle(data.getName());
		plottingSystem.getAxes().get(0).setTitle("");
		plottingSystem.getAxes().get(1).setTitle("");
		plottingSystem.setKeepAspect(true);
		//plottingSystem.setShowIntensity(false);

		updateCurrentData(data);

		calibrantPositioning.setDiffractionData(manager.getCurrentData());
		if (data.getMetaData() != null) {
		augmenter.setDiffractionMetadata(manager.getCurrentData().getMetaData());
		
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
		if (diffractionTableViewer != null && !diffractionTableViewer.isDisposed())
			diffractionTableViewer.setFocus();
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
