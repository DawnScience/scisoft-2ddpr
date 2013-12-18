package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.mihalis.opal.checkBoxGroup.CheckBoxGroup;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.wizard.persistence.PersistenceExportWizard;
import org.dawb.common.ui.wizard.persistence.PersistenceImportWizard;
import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.CalibrantPositioningWidget;
import org.dawb.workbench.ui.diffraction.table.DiffCalTableViewer;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawb.workbench.ui.diffraction.table.TableChangedEvent;
import org.dawb.workbench.ui.diffraction.table.TableChangedListener;
import org.dawnsci.common.widgets.tree.NumericNode;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.api.tool.IToolPage.ToolPageRole;
import org.dawnsci.plotting.api.tool.IToolPageSystem;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionTool;
import org.dawnsci.plotting.tools.diffraction.DiffractionTreeModel;
import org.dawnsci.plotting.tools.powdercheck.PowderCheckTool;
import org.dawnsci.plotting.tools.preference.diffraction.DiffractionPreferencePage;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.swt.custom.StackLayout;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectedListener;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectionEvent;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.io.ILoaderService;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;

public class DiffractionCalibrationView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionCalibrationView";
	private final Logger logger = LoggerFactory.getLogger(DiffractionCalibrationView.class);

	public static final String FIXED = "org.dawb.workbench.plotting.views.toolPageView.fixed:";
	public static final String DIFFRACTION_ID = "org.dawb.workbench.plotting.tools.diffraction.Diffraction";
	public static final String POWDERCHECK_ID = "org.dawnsci.plotting.tools.powdercheck";

	private final String WAVELENGTH_NODE_PATH = "/Experimental Information/Wavelength";
	private final String BEAM_CENTRE_XPATH = "/Detector/Beam Centre/X";
	private final String BEAM_CENTRE_YPATH = "/Detector/Beam Centre/Y";
	private final String DISTANCE_NODE_PATH = "/Experimental Information/Distance";
	public static final String FORMAT_MASK = "##,##0.##########";

	private final String DATA_PATH = "DataPath";
	private final String CALIBRANT = "Calibrant";

	private DiffractionTableData currentData;
	private List<DiffractionTableData> model;
	private List<String> pathsList = new ArrayList<String>();
	private ILoaderService service;
	private CalibrationStandards standards;

	private Composite parent;
	private ScrolledComposite scrollComposite;
	private Composite scrollHolder;
	private DiffCalTableViewer diffractionTableViewer;
	private Button calibrateImagesButton;
	private Combo calibrantCombo;
	private TabFolder tabFolder;
	private Spinner ringNumberSpinner;
	private FormattedText wavelengthFormattedText;
	private FormattedText energyFormattedText;
	private CalibrantPositioningWidget calibrantPositioning;
	private CheckBoxGroup xRayGroup;

	private IPlottingSystem plottingSystem;
	private IToolPageSystem toolSystem;

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
		scrollComposite = new ScrolledComposite(controlComp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrollComposite.setLayout(new GridLayout(1, false));
		scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scrollHolder = new Composite(scrollComposite, SWT.NONE);
		scrollHolder.setLayout(new GridLayout(1, false));
		scrollHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// table of images and found rings
		diffractionTableViewer = new DiffCalTableViewer(scrollHolder, pathsList, service);
		diffractionTableViewer.addSelectionChangedListener(selectionChangeListener);
		diffractionTableViewer.addTableChangedListener(tableChangedListener);
		model = diffractionTableViewer.getModel();

		Composite mainHolder = new Composite(scrollHolder, SWT.NONE);
		mainHolder.setLayout(new GridLayout(2, false));
		mainHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tabFolder = new TabFolder(mainHolder, SWT.BORDER | SWT.FILL);
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

		standards = CalibrationFactory.getCalibrationStandards();

		TabItem manualTabItem = new TabItem(tabFolder, SWT.FILL);
		manualTabItem.setText("Manual");
		manualTabItem.setToolTipText("Manual calibration");
		manualTabItem.setControl(getManualTabControl(tabFolder));

		TabItem settingTabItem = new TabItem(tabFolder, SWT.FILL);
		settingTabItem.setText("Settings");
		settingTabItem.setToolTipText("Calibration settings");
		settingTabItem.setControl(getSettingTabControl(tabFolder, standards));

		// create calibrant combo
		Composite rightComp = new Composite(mainHolder, SWT.NONE);
		rightComp.setLayout(new GridLayout(1, false));
		rightComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group selectCalibComp = new Group(rightComp, SWT.NONE);
		selectCalibComp.setText("Calibrant");
		selectCalibComp.setLayout(new GridLayout(1, false));
		selectCalibComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite comp = new Composite(selectCalibComp, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		calibrantCombo = new Combo(comp, SWT.READ_ONLY);
		calibrantCombo.setToolTipText("Select a type of calibrant");
		calibrantCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (currentData == null)
					return;
				String calibrantName = calibrantCombo.getItem(calibrantCombo
						.getSelectionIndex());
				// update the calibrant in diffraction tool
				standards.setSelectedCalibrant(calibrantName, true);
				DiffractionCalibrationUtils.drawCalibrantRings(currentData.augmenter);
				// set the maximum number of rings
				ringNumberSpinner.setMaximum(standards.getCalibrant().getHKLs().size());
				PowderCheckTool powderTool = (PowderCheckTool)toolSystem.getToolPage(POWDERCHECK_ID);
				if (powderTool != null) powderTool.updateCalibrantLines();
				showCalibrantAndBeamCentre(checked, currentData);
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
		configCalibrantButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DiffractionPreferencePage.ID, null, null);
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
				showCalibrantAndBeamCentre(checked, currentData);
			}
		});
		showCalibAndBeamCtrCheckBox.setSelection(true);

		xRayGroup = createXRayGroup(rightComp, SWT.FILL);
		xRayGroup.deactivate(); //deactivate by default
//		// Enable/disable the modifiers
		setXRaysModifiersEnabled(false);

		scrollHolder.layout();
		scrollComposite.setContent(scrollHolder);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.setExpandVertical(true);
		Rectangle r = scrollHolder.getClientArea();
		scrollComposite.setMinSize(scrollHolder.computeSize(r.width, SWT.DEFAULT));
		scrollComposite.layout();
		// end of Diffraction Calibration controls

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

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				initializeSystems();
			}
		});

		CalibrationFactory.addCalibrantSelectionListener(calibrantChangeListener);

		calibrantPositioning.setControlsToUpdate(calibrateImagesButton);
		calibrantPositioning.setTableViewerToUpdate(diffractionTableViewer);
		calibrantPositioning.setNumberOfRingsSpinner(ringNumberSpinner);
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
		toolSystem = (IToolPageSystem) plottingSystem.getAdapter(IToolPageSystem.class);

		// Open the diffraction tool programmatically inside of a viewpart so we can set the hideToolBar boolean
		try {
			DiffractionTool diffTool = (DiffractionTool)toolSystem.getToolPage(DIFFRACTION_ID);
			diffTool.hideToolBar(true);
			DiffractionToolView diffToolView = (DiffractionToolView)getView(DiffractionToolView.ID);
			Composite diffToolComp = diffToolView.getComposite();
			diffToolComp.setLayout(new StackLayout());
			toolSystem.setToolComposite(diffToolComp);
			toolSystem.setToolVisible(DIFFRACTION_ID, ToolPageRole.ROLE_2D, null);
		} catch (Exception e2) {
			logger.error("Could not open diffraction tool:" + e2);
		}
		// set the focus on the plotting system to trigger the tools (powder tool)
		plottingSystem.setFocus();
		calibrantPositioning.setPlottingSystem(plottingSystem);
		calibrantPositioning.setToolSystem(toolSystem);
	}

	private void initializeListeners(){
		// selection change listener for table viewer
		selectionChangeListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection is = event.getSelection();
				if (is instanceof StructuredSelection) {
					StructuredSelection structSelection = (StructuredSelection) is;
					DiffractionTableData selectedData = (DiffractionTableData) structSelection.getFirstElement();
					if (selectedData == null || selectedData == currentData)
						return;
					drawSelectedData(selectedData);
					showCalibrantAndBeamCentre(checked, currentData);
				}
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
					currentData = null; // need to reset this
					plottingSystem.clear();
					setXRaysModifiersEnabled(false);
					calibrateImagesButton.setEnabled(false);
				}
			}
		};

		calibrantChangeListener = new CalibrantSelectedListener() {
			@Override
			public void calibrantSelectionChanged(CalibrantSelectionEvent evt) {
				calibrantCombo.select(calibrantCombo.indexOf(evt.getCalibrant()));
				if (currentData != null)
					showCalibrantAndBeamCentre(checked, currentData);
			}
		};

	}

	/**
	 * Shows/Hides the calibrant and beam centre
	 * @param show
	 * @param currentData
	 */
	private void showCalibrantAndBeamCentre(boolean show, DiffractionTableData currentData) {
		currentData.augmenter.drawCalibrantRings(show, standards.getCalibrant());
		currentData.augmenter.drawBeamCentre(show);
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

	private CheckBoxGroup createXRayGroup(Composite composite, int style) {
		CheckBoxGroup xRayGroup = new CheckBoxGroup(composite, style);
		xRayGroup.setText("X-Rays");
		xRayGroup.setToolTipText("Set the wavelength / energy");
		xRayGroup.setLayout(new GridLayout(3, false));
		xRayGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		Label wavelengthLabel = new Label(xRayGroup, SWT.NONE);
		wavelengthLabel.setText("Wavelength");

		wavelengthFormattedText = new FormattedText(xRayGroup, SWT.SINGLE | SWT.BORDER);
		wavelengthFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		wavelengthFormattedText.getControl().setToolTipText("Set the wavelength in Angstrom");
		wavelengthFormattedText.getControl().setSize(200, 20);
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
				// update wavelength in diffraction tool tree viewer
				NumericNode<Length> node = getDiffractionTreeNode(WAVELENGTH_NODE_PATH);
				if (node.getUnit().equals(NonSI.ANGSTROM)) {
					updateDiffTool(WAVELENGTH_NODE_PATH, distance);
				} else if (node.getUnit().equals(NonSI.ELECTRON_VOLT)) {
					updateDiffTool(WAVELENGTH_NODE_PATH, energy * 1000);
				} else if (node.getUnit().equals(SI.KILO(NonSI.ELECTRON_VOLT))) {
					updateDiffTool(WAVELENGTH_NODE_PATH, energy);
				}
			}
		});
		wavelengthFormattedText.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label unitDistanceLabel = new Label(xRayGroup, SWT.NONE);
		unitDistanceLabel.setText(NonSI.ANGSTROM.toString());

		Label energyLabel = new Label(xRayGroup, SWT.NONE);
		energyLabel.setText("Energy");

		energyFormattedText = new FormattedText(xRayGroup, SWT.SINGLE | SWT.BORDER);
		energyFormattedText.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		energyFormattedText.getControl().setToolTipText("Set the wavelength in keV");
		energyFormattedText.getControl().setSize(200, 20);
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
				// update wavelength in Diffraction tool tree viewer
				NumericNode<Length> node = getDiffractionTreeNode(WAVELENGTH_NODE_PATH);
				if (node.getUnit().equals(NonSI.ANGSTROM)) {
					updateDiffTool(WAVELENGTH_NODE_PATH, distance);
				} else if (node.getUnit().equals(NonSI.ELECTRON_VOLT)) {
					updateDiffTool(WAVELENGTH_NODE_PATH, energy * 1000);
				} else if (node.getUnit().equals(SI.KILO(NonSI.ELECTRON_VOLT))) {
					updateDiffTool(WAVELENGTH_NODE_PATH, energy);
				}
			}
		});
		energyFormattedText.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label unitEnergyLabel = new Label(xRayGroup, SWT.NONE);
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
					// update diffraction tool viewer
					updateDiffTool(BEAM_CENTRE_XPATH, currentData.md.getDetector2DProperties().getBeamCentreCoords()[0]);
					updateDiffTool(BEAM_CENTRE_YPATH, currentData.md.getDetector2DProperties().getBeamCentreCoords()[1]);
					updateDiffTool(DISTANCE_NODE_PATH, currentData.md.getDetector2DProperties().getBeamCentreDistance());

					// update wavelength
					double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
					energyFormattedText.setValue(DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
					wavelengthFormattedText.setValue(wavelength);
					// update wavelength in diffraction tool tree viewer
					NumericNode<Length> node = getDiffractionTreeNode(WAVELENGTH_NODE_PATH);
					if (node.getUnit().equals(NonSI.ANGSTROM)) {
						updateDiffTool(WAVELENGTH_NODE_PATH, wavelength);
					} else if (node.getUnit().equals(NonSI.ELECTRON_VOLT)) {
						updateDiffTool(WAVELENGTH_NODE_PATH, DiffractionCalibrationUtils.getWavelengthEnergy(wavelength) * 1000);
					} else if (node.getUnit().equals(SI.KILO(NonSI.ELECTRON_VOLT))) {
						updateDiffTool(WAVELENGTH_NODE_PATH, DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
					}
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
				Job job = null;
				
				if (model.size() == 1) {
					job = PowderCalibrationUtils.autoFindEllipses(Display.getDefault(), plottingSystem, currentData,ringNumberSpinner.getSelection());
				} else {
					job = PowderCalibrationUtils.autoFindEllipsesMultipleImages(Display.getDefault(), plottingSystem, model, currentData,ringNumberSpinner.getSelection());
				}

				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								updateScrolledComposite();
								updateWavelengthAfterCalibration();
								updateIntegrated(currentData);
							}
						});
					}
				});
				job.setUser(true);
				job.schedule();
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
		Composite composite = new Composite(tabFolder, SWT.FILL);
		composite.setLayout(new GridLayout(1, false));
		calibrantPositioning = new CalibrantPositioningWidget(composite, model);

		calibrateImagesButton = new Button(composite, SWT.PUSH);
		calibrateImagesButton.setText("Run Calibration Process");
		calibrateImagesButton.setToolTipText("Calibrate detector in chosen images");
		calibrateImagesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		calibrateImagesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = null;

				if (model.size() == 1) {
					job = PowderCalibrationUtils.calibrateImagesMajorAxisMethod(Display.getDefault(), plottingSystem, currentData);
				} else {
					job = PowderCalibrationUtils.calibrateMultipleImages(Display.getDefault(), plottingSystem, model, currentData);
				}

				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								updateScrolledComposite();
								updateIntegrated(currentData);
							}
						});
					}
				});
				job.setUser(true);
				job.schedule();
			}
		});
		calibrateImagesButton.setEnabled(false);
		return composite;
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

		return composite;
	}

	private void updateDiffTool(String nodePath, double value) {
		DiffractionTool diffTool = (DiffractionTool) toolSystem.getToolPage(DIFFRACTION_ID);
		DiffractionTreeModel treeModel = diffTool.getModel();

		NumericNode<Length> distanceNode = getDiffractionTreeNode(nodePath);
		distanceNode.setDoubleValue(value);
		treeModel.setNode(distanceNode, nodePath);

		diffTool.refresh();
	}

	@SuppressWarnings("unchecked")
	private NumericNode<Length> getDiffractionTreeNode(String nodePath) {
		NumericNode<Length> node = null;
		if (toolSystem == null)
			return node;
		DiffractionTool diffTool = (DiffractionTool) toolSystem.getToolPage(DIFFRACTION_ID);
		DiffractionTreeModel treeModel = diffTool.getModel();
		if (treeModel == null)
			return node;
		node = (NumericNode<Length>) treeModel.getNode(nodePath);
		return node;
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
		if (currentData != null) {
			DiffractionImageAugmenter aug = currentData.augmenter;
			if (aug != null)
				aug.deactivate(service.getLockedDiffractionMetaData()!=null);
		}

		if (data.image == null)
			return;
		
		if (plottingSystem == null || toolSystem == null) {
			// initialize plotting systems and tools
			initializeSystems();
		}
		plottingSystem.clear();
		plottingSystem.updatePlot2D(data.image, null, null);
		plottingSystem.setTitle(data.name);
		plottingSystem.getAxes().get(0).setTitle("");
		plottingSystem.getAxes().get(1).setTitle("");
		plottingSystem.setKeepAspect(true);
		plottingSystem.setShowIntensity(false);
		
		currentData = data;
		
		calibrantPositioning.setDiffractionData(currentData);
		
		//Data has its own augmenters so disable the tool augmenter
		DiffractionTool diffTool = (DiffractionTool) toolSystem.getToolPage(DIFFRACTION_ID);
		DiffractionImageAugmenter toolAug = diffTool.getAugmenter();
		if (toolAug != null) toolAug.deactivate(service.getLockedDiffractionMetaData()!=null);
		
		DiffractionImageAugmenter aug = data.augmenter;
		if (aug == null) {
			aug = new DiffractionImageAugmenter(plottingSystem);
			data.augmenter = aug;
		}
		if (data.md != null) {
			aug.setDiffractionMetadata(data.md);
			// Add listeners to monitor metadata changes in diffraction tool
			diffractionTableViewer.addDetectorPropertyListener(data);
		}

		aug.activate();
		DiffractionCalibrationUtils.hideFoundRings(plottingSystem);
		DiffractionCalibrationUtils.drawCalibrantRings(aug);
		
		updateIntegrated(data);
	}
	
	private void updateIntegrated(final DiffractionTableData data) {
		PowderCheckTool powderTool = (PowderCheckTool) toolSystem.getToolPage(POWDERCHECK_ID);
		powderTool.update();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		if (key == IPlottingSystem.class) {
			return plottingSystem;
		} else if (key == IToolPageSystem.class) {
			if (plottingSystem != null)
				return plottingSystem.getAdapter(IToolPageSystem.class);
			else
				return PlottingFactory.getPlottingSystem(DiffractionPlotView.DIFFRACTION_PLOT_TITLE);
		}
		return super.getAdapter(key);
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
		// deactivate the diffraction tool
		if (toolSystem != null) {
			DiffractionTool diffTool = (DiffractionTool) toolSystem.getToolPage(DIFFRACTION_ID);
			if (diffTool != null)
				diffTool.deactivate();
		}
		
		// deactivate each augmenter in loaded data
		if (model != null) {
			for (DiffractionTableData d : model) {
				if (d.augmenter != null)
					d.augmenter.deactivate(service.getLockedDiffractionMetaData()!=null);
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

}
