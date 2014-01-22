package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.wizard.persistence.PersistenceExportWizard;
import org.dawb.common.ui.wizard.persistence.PersistenceImportWizard;
import org.dawb.workbench.ui.diffraction.CalibrantPositioningWidget;
import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.table.DiffCalTableViewer;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawb.workbench.ui.diffraction.table.TableChangedEvent;
import org.dawb.workbench.ui.diffraction.table.TableChangedListener;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.preference.diffraction.DiffractionPreferencePage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.nebula.widgets.formattedtext.FormattedText;
import org.eclipse.nebula.widgets.formattedtext.NumberFormatter;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;
import org.mihalis.opal.checkBoxGroup.CheckBoxGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectedListener;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectionEvent;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.io.ILoaderService;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePointsParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromPointsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.FromRingsCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;

public class DiffractionCalibrationView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionCalibrationView";
	private final Logger logger = LoggerFactory.getLogger(DiffractionCalibrationView.class);

	public static final String FORMAT_MASK = "##,##0.##########";

	private final String DATA_PATH = "DataPath";
	private final String CALIBRANT = "Calibrant";

	private DiffractionTableData currentData;
	private List<DiffractionTableData> model;
	private List<String> pathsList = new ArrayList<String>();
	private ILoaderService service;
	private CalibrationStandards standards;

	private Composite parent;
	private DiffCalTableViewer diffractionTableViewer;
	private Button calibrateImagesButton;
	private Combo calibrantCombo;
	private Spinner ringNumberSpinner;
	private FormattedText wavelengthFormattedText;
	private FormattedText energyFormattedText;
	private CalibrantPositioningWidget calibrantPositioning;
	private CheckBoxGroup xRayGroup;
	private Label residualLabel;
	private Button usePointCalibration;
	private POIFindingRun ringFindJob;
	private DiffractionImageAugmenter augmenter;
	
	private static final String RESIDUAL = "Residual: ";

	private IPlottingSystem plottingSystem;

	private ISelectionChangedListener selectionChangeListener;
	private CalibrantSelectedListener calibrantChangeListener;
	private TableChangedListener tableChangedListener;

	private boolean checked = true;
	private String calibrantName;

	public DiffractionCalibrationView() {
		service = (ILoaderService) PlatformUI.getWorkbench().getService(ILoaderService.class);
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
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		if (memento != null) {
			int i = 0;
			for (TableItem t : diffractionTableViewer.getTable().getItems()) {
				DiffractionTableData data = (DiffractionTableData) t.getData();
				memento.putString(DATA_PATH + String.valueOf(i++), data.path);
			}
			memento.putString(CALIBRANT, calibrantCombo.getItem(calibrantCombo.getSelectionIndex()));
		}
	}

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());

		this.parent = parent;
		final Display display = parent.getDisplay();
		
		initializeListeners();
		standards = CalibrationFactory.getCalibrationStandards();

		Composite controlComp = new Composite(parent, SWT.NONE);
		controlComp.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(controlComp);
		createToolbarActions(controlComp);

		Label instructionLabel = new Label(controlComp, SWT.WRAP);
		instructionLabel.setText("Drag/drop a file/data to the table below, " +
				"choose a type of calibrant, " +
				"select the auto mode and the number of rings through the settings tab " +
				"and finally run the auto calibration.");
		instructionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		Point pt = instructionLabel.getSize(); pt.x +=4; pt.y += 4; instructionLabel.setSize(pt);

		// make a scrolled composite
		ScrolledComposite scrollComposite = new ScrolledComposite(controlComp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrollComposite.setLayout(new GridLayout(1, false));
		scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite scrollHolder = new Composite(scrollComposite, SWT.NONE);
		scrollHolder.setLayout(new GridLayout(1, false));

		// table of images and found rings
		diffractionTableViewer = new DiffCalTableViewer(scrollHolder, pathsList, service);
		diffractionTableViewer.addSelectionChangedListener(selectionChangeListener);
		diffractionTableViewer.addTableChangedListener(tableChangedListener);
		model = diffractionTableViewer.getModel();

		Composite mainHolder = new Composite(scrollHolder, SWT.NONE);
		mainHolder.setLayout(new GridLayout(1, false));
		mainHolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		// create calibrant combo
		Composite topComp = new Composite(mainHolder, SWT.NONE);
		topComp.setLayout(new GridLayout(2, false));
		topComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createCalibrantGroup(topComp);
		//Xrays text fields
		xRayGroup = createXRayGroup(topComp);
		xRayGroup.deactivate(); //deactivate by default
		// Enable/disable the modifiers
		setXRaysModifiersEnabled(false);

		//TabFolder
		TabFolder tabFolder = new TabFolder(mainHolder, SWT.BORDER | SWT.FILL);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		TabItem autoTabItem = new TabItem(tabFolder, SWT.FILL);
		autoTabItem.setText("Auto");
		autoTabItem.setToolTipText("Automatic calibration");
		autoTabItem.setControl(getAutoTabControl(tabFolder));
		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				diffractionTableViewer.updateTableColumnsAndLayout();
			}
		});
		diffractionTableViewer.setTabFolder(tabFolder);

		TabItem manualTabItem = new TabItem(tabFolder, SWT.FILL);
		manualTabItem.setText("Manual");
		manualTabItem.setToolTipText("Manual calibration");
		manualTabItem.setControl(getManualTabControl(tabFolder));

		TabItem settingTabItem = new TabItem(tabFolder, SWT.FILL);
		settingTabItem.setText("Settings");
		settingTabItem.setToolTipText("Calibration settings");
		settingTabItem.setControl(getSettingTabControl(tabFolder, standards));

		scrollHolder.layout();
		scrollComposite.setContent(scrollHolder);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.setExpandVertical(true);
		Rectangle r = scrollHolder.getClientArea();
		scrollComposite.setMinSize(scrollHolder.computeSize(r.width, SWT.DEFAULT));
		scrollComposite.layout();
		// end of Diffraction Calibration controls
		
		residualLabel = new Label(mainHolder, SWT.NONE);
		residualLabel.setText(RESIDUAL);
		residualLabel.setLayoutData(new GridData());

		// try to load the previous data saved in the memento
		DiffractionTableData good = null;
		for (String p : pathsList) {
			if (!p.endsWith(".nxs")) {
				DiffractionTableData d = diffractionTableViewer.createData(p, null);
				if (good == null && d != null) {
					good = d;
					setWavelength(d);
					setCalibrant();
				}
			}
		}
		diffractionTableViewer.refresh();
		if (good != null) {
			final DiffractionTableData g = good;
			display.asyncExec(new Runnable() { // this is necessary to give the plotting system time to lay out itself
				@Override
				public void run() {
					diffractionTableViewer.setSelection(new StructuredSelection(g));
				}
			});
		}
		if (model.size() > 0)
			setXRaysModifiersEnabled(true);

		CalibrationFactory.addCalibrantSelectionListener(calibrantChangeListener);

		calibrantPositioning.setControlsToUpdate(calibrateImagesButton);
		calibrantPositioning.setTableViewerToUpdate(diffractionTableViewer);
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
		plottingSystem.setFocus();
		augmenter = new DiffractionImageAugmenter(plottingSystem);
		augmenter.activate();
		ringFindJob = new POIFindingRun(plottingSystem, currentData, ringNumberSpinner.getSelection());
		calibrantPositioning.setRingFinder(ringFindJob);
	}

	private void initializeListeners(){
		// selection change listener for table viewer
		selectionChangeListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(false);
			}
		};

		tableChangedListener = new TableChangedListener() {
			@Override
			public void tableChanged(TableChangedEvent event) {
				setWavelength(currentData);
				if (!model.isEmpty()) {
					setXRaysModifiersEnabled(true);
					if (event == TableChangedEvent.REMOVED)
						drawSelectedData((DiffractionTableData) diffractionTableViewer
							.getElementAt(0));
					else if (event == TableChangedEvent.ADDED)
						getView(DiffractionPlotView.ID).setFocus();
				} else {
					updateCurrentData(null); // need to reset this
					plottingSystem.clear();
					setXRaysModifiersEnabled(false);
					calibrateImagesButton.setEnabled(false);
					residualLabel.setText(RESIDUAL);
					residualLabel.getParent().layout();
				}
			}
		};

		calibrantChangeListener = new CalibrantSelectedListener() {
			@Override
			public void calibrantSelectionChanged(CalibrantSelectionEvent evt) {
				calibrantCombo.select(calibrantCombo.indexOf(evt.getCalibrant()));
				
				ringNumberSpinner.setMaximum(CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size());
				
				if (currentData != null)
					showCalibrantAndBeamCentre(checked);
			}
		};

	}

	protected void updateSelection(boolean force) {
		ISelection is = diffractionTableViewer.getSelection();
		if (is instanceof StructuredSelection) {
			StructuredSelection structSelection = (StructuredSelection) is;
			DiffractionTableData selectedData = (DiffractionTableData) structSelection.getFirstElement();
			
			if (augmenter != null) {
				if (structSelection.isEmpty()) 
					augmenter.deactivate(false);
				else
					augmenter.activate();
			}
			
			if (selectedData == null || (!force && selectedData == currentData)) {
				return;
			}
			drawSelectedData(selectedData);
			showCalibrantAndBeamCentre(checked);
		}
	}

	/**
	 * Shows/Hides the calibrant and beam centre
	 * @param show
	 * @param currentData
	 */
	private void showCalibrantAndBeamCentre(boolean show) {
		
		if (show) {
			augmenter.activate();
			augmenter.drawBeamCentre(show);
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

	private void updateWavelengthAfterCalibration(){
		double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
		wavelength = DiffractionCalibrationUtils.setPrecision(wavelength, 5);
		String newFormat = DiffractionCalibrationUtils.getFormatMask(wavelength, wavelength);
		wavelengthFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
		wavelengthFormattedText.setValue(wavelength);
		double energy = DiffractionCalibrationUtils.getWavelengthEnergy(wavelength);
		newFormat = DiffractionCalibrationUtils.getFormatMask(energy, energy);
		energyFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
		energyFormattedText.setValue(energy);
	}

	private void createCalibrantGroup(Composite composite) {
		Group selectCalibComp = new Group(composite, SWT.NONE);
		selectCalibComp.setText("Calibrant");
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
				if (currentData == null)
					return;
				String calibrantName = calibrantCombo.getItem(calibrantCombo.getSelectionIndex());
				// update the calibrant in diffraction tool
				standards.setSelectedCalibrant(calibrantName, true);
				// set the maximum number of rings
				ringNumberSpinner.setMaximum(standards.getCalibrant().getHKLs().size());
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

	private CheckBoxGroup createXRayGroup(Composite composite) {
		CheckBoxGroup xRayGroup = new CheckBoxGroup(composite, SWT.FILL);
		xRayGroup.setText("Fix X-Ray Wavelength");
		xRayGroup.setToolTipText("Set the wavelength / energy");
		xRayGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Composite checkComp = xRayGroup.getContent();
		checkComp.setLayout(new GridLayout(3, false));
		checkComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label wavelengthLabel = new Label(checkComp, SWT.NONE);
		wavelengthLabel.setText("Wavelength");

		wavelengthFormattedText = new FormattedText(checkComp, SWT.SINGLE | SWT.BORDER);
		wavelengthFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		wavelengthFormattedText.getControl().setToolTipText("Set the wavelength in Angstrom");
		wavelengthFormattedText.getControl().addListener(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// update wavelength of each image
				double distance = 0;
				Object obj = wavelengthFormattedText.getValue();
				if (obj instanceof Long)
					distance = ((Long) obj).doubleValue();
				else if (obj instanceof Double)
					distance = (Double) obj;
				for (int i = 0; i < model.size(); i++) {
					model.get(i).md.getDiffractionCrystalEnvironment().setWavelength(distance);
				}
				// update wavelength in keV
				double energy = DiffractionCalibrationUtils.getWavelengthEnergy(distance);
				if (energy != Double.POSITIVE_INFINITY) {
					String newFormat = DiffractionCalibrationUtils.getFormatMask(distance, energy);
					energyFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
				}
				energyFormattedText.setValue(energy);
			}
		});
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.widthHint = 150;
		wavelengthFormattedText.getControl().setLayoutData(data);
		Label unitDistanceLabel = new Label(checkComp, SWT.NONE);
		unitDistanceLabel.setText(NonSI.ANGSTROM.toString());

		Label energyLabel = new Label(checkComp, SWT.NONE);
		energyLabel.setText("Energy");

		energyFormattedText = new FormattedText(checkComp, SWT.SINGLE | SWT.BORDER);
		energyFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		energyFormattedText.getControl().setToolTipText("Set the wavelength in keV");
		energyFormattedText.getControl().addListener(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// update wavelength of each image
				double energy = 0;
				Object obj = energyFormattedText.getValue();
				if (obj instanceof Long)
					energy = ((Long) obj).doubleValue();
				else if (obj instanceof Double)
					energy = (Double) obj;
				for (int i = 0; i < model.size(); i++) {
					model.get(i).md.getDiffractionCrystalEnvironment().setWavelength(DiffractionCalibrationUtils.getWavelengthEnergy(energy));
				}
				// update wavelength in Angstrom
				double distance = DiffractionCalibrationUtils.getWavelengthEnergy(energy);
				if (distance != Double.POSITIVE_INFINITY) {
					String newFormat = DiffractionCalibrationUtils.getFormatMask(energy, distance);
					wavelengthFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
				}
				wavelengthFormattedText.setValue(distance);
			}
		});
		energyFormattedText.getControl().setLayoutData(data);
		Label unitEnergyLabel = new Label(checkComp, SWT.NONE);
		unitEnergyLabel.setText(SI.KILO(NonSI.ELECTRON_VOLT).toString());
		return xRayGroup;
	}

	private void setXRaysModifiersEnabled(boolean b) {
		wavelengthFormattedText.getControl().setEnabled(b);
		energyFormattedText.getControl().setEnabled(b);
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
					DiffractionCalibrationUtils.saveModelToCSVFile(model, savedFilePath);
				}
			}
		};
		exportToXLSAction.setImageDescriptor(Activator.getImageDescriptor("icons/page_white_excel.png"));

		IAction resetRingsAction = new Action("Remove found rings") {
			@Override
			public void run() {
				DiffractionCalibrationUtils.hideFoundRings(plottingSystem);
			}
		};
		resetRingsAction.setImageDescriptor(Activator.getImageDescriptor("icons/reset_rings.png"));

		IAction resetTableAction = new Action("Reset metadata") {
			@Override
			public void run() {
				// select last item in table
				if (model != null && model.size() > 0) {
					diffractionTableViewer.setSelection(new StructuredSelection(model.get(model.size() - 1)));
					for (int i = 0; i < model.size(); i++) {
						// Restore original metadata
						DetectorProperties originalProps = model.get(i).md.getOriginalDetector2DProperties();
						DiffractionCrystalEnvironment originalEnvironment = model.get(i).md.getOriginalDiffractionCrystalEnvironment();
						model.get(i).md.getDetector2DProperties().restore(originalProps);
						model.get(i).md.getDiffractionCrystalEnvironment().restore(originalEnvironment);
					}
					// update wavelength
					double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
					energyFormattedText.setValue(DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
					wavelengthFormattedText.setValue(wavelength);
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
				
				SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
				params.setFloatEnergy(!xRayGroup.isActivated());
				params.setNumberOfRings(ringNumberSpinner.getSelection());
				IRunnableWithProgress job = new AutoCalibrationRun(Display.getDefault(), plottingSystem, model, currentData, params);

				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				try {
					dia.run(true, true, job);
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				updateAfterCalibration();
				
			}
		});
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
		ringFindJob = new POIFindingRun(null, null, 0);
		calibrantPositioning = new CalibrantPositioningWidget(composite, model);

		calibrateImagesButton = new Button(composite, SWT.PUSH);
		calibrateImagesButton.setText("Run Calibration Process");
		calibrateImagesButton.setToolTipText("Calibrate detector in chosen images");
		calibrateImagesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		calibrateImagesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				IRunnableWithProgress job = null;
				
				CalibratePointsParameterModel params = new CalibratePointsParameterModel();
				params.setNumberOfRings(ringNumberSpinner.getSelection());
				params.setFloatEnergy(!xRayGroup.isActivated());
				
				if (usePointCalibration.getSelection()) {
					
					
					job = new FromPointsCalibrationRun(Display.getDefault(), plottingSystem, model, currentData, params);
				} else {
					job = new FromRingsCalibrationRun(Display.getDefault(), plottingSystem, model, currentData, params);
				}
				
				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
				try {
					dia.run(true, true, job);
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				updateAfterCalibration();
			}
		});
		calibrateImagesButton.setEnabled(false);
		return composite;
	}
	
	private void updateAfterCalibration() {
		updateScrolledComposite();
		updateWavelengthAfterCalibration();
		updateSelection(true);
		
		residualLabel.setText(RESIDUAL + currentData.residual);
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

		Label ringNumberLabel = new Label(composite, SWT.NONE);
		ringNumberLabel.setText("No. of Rings to Use:");
		ringNumberSpinner = new Spinner(composite, SWT.BORDER);
		ringNumberSpinner.setMaximum(standards.getCalibrant().getHKLs().size());
		ringNumberSpinner.setMinimum(2);
		ringNumberSpinner.setSelection(100);
		
		ringNumberSpinner.addSelectionListener(new SelectionAdapter() {
		
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ringFindJob != null) ringFindJob.setNumberOfRingsToFind(ringNumberSpinner.getSelection());
			}
		});
		
		usePointCalibration = new Button(composite, SWT.CHECK);
		usePointCalibration.setText("Manual calibration uses points not ellipse parameters");
		usePointCalibration.setSelection(false);

		return composite;
	}

	private void setWavelength(DiffractionTableData data) {
		// set the wavelength
		if (data != null) {
			double wavelength = data.md.getOriginalDiffractionCrystalEnvironment().getWavelength();
			wavelengthFormattedText.setValue(wavelength);
			double energy = DiffractionCalibrationUtils.getWavelengthEnergy(wavelength);
			if (energy != Double.POSITIVE_INFINITY) {
				energyFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, 
						DiffractionCalibrationUtils.getFormatMask(wavelength, energy), Locale.UK));
			}
			energyFormattedText.setValue(energy);
		}
	}

	private void setCalibrant() {
		// set the calibrant
		CalibrationStandards standard = CalibrationFactory.getCalibrationStandards();
		if (calibrantName != null) {
			calibrantCombo.select(calibrantCombo.indexOf(calibrantName));
			standard.setSelectedCalibrant(calibrantName, true);
		} else {
			calibrantCombo.select(calibrantCombo.indexOf(standard.getSelectedCalibrant()));
		}
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

		calibrantPositioning.setDiffractionData(currentData);
		if (data.md != null) {
		augmenter.setDiffractionMetadata(currentData.md);
		diffractionTableViewer.addDetectorPropertyListener(data);
		}
		
		DiffractionCalibrationUtils.hideFoundRings(plottingSystem);
		
	}
	
	private void updateCurrentData(DiffractionTableData data) {
		currentData = data;
		if (ringFindJob != null) ringFindJob.setCurrentData(currentData);
		
	}

	@SuppressWarnings("unused")
	private void setCalibrateButtons() {
		// enable/disable calibrate button according to use column
		int used = 0;
		for (DiffractionTableData d : model) {
			if (d.use && d.nrois > 0) {
				used++;
			}
		}
		calibrateImagesButton.setEnabled(used > 0);
	}

	private void removeListeners() {
		if(diffractionTableViewer != null) {
			diffractionTableViewer.removeSelectionChangedListener(selectionChangeListener);
			diffractionTableViewer.removeTableChangedListener(tableChangedListener);
		}
		CalibrationFactory.removeCalibrantSelectionListener(calibrantChangeListener);

		augmenter.deactivate(false);
		
		if (model != null) {
			for (DiffractionTableData d : model) {
				diffractionTableViewer.removeDetectorPropertyListener(d);
			}
			model.clear();
		}
		logger.debug("model emptied");
	}

	@Override
	public void dispose() {
		super.dispose();
		removeListeners();
		// FIXME Clear
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
		return null;
	}
}
