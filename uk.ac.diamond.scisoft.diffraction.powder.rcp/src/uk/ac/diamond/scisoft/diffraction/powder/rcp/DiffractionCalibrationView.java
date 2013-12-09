package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import uk.ac.diamond.scisoft.analysis.io.ILoaderService;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawb.workbench.ui.views.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.views.CalibrantPositioningWidget;
import org.dawb.workbench.ui.views.DiffractionTableData;
import org.dawb.workbench.ui.views.RepeatingMouseAdapter;
import org.dawb.workbench.ui.views.SlowFastRunnable;
import org.dawnsci.common.widgets.tree.NumericNode;
import org.dawnsci.common.widgets.utils.RadioUtils;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.api.axis.IAxis;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.region.RegionUtils;
import org.dawnsci.plotting.api.region.IRegion.RegionType;
import org.dawnsci.plotting.api.tool.IToolPageSystem;
import org.dawnsci.plotting.api.tool.IToolPage.ToolPageRole;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionTool;
import org.dawnsci.plotting.tools.diffraction.DiffractionTreeModel;
import org.dawnsci.plotting.tools.powdercheck.PowderCheckTool;
import org.dawnsci.plotting.util.PlottingUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.widgets.formattedtext.FormattedText;
import org.eclipse.nebula.widgets.formattedtext.NumberFormatter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectedListener;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSelectionEvent;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorPropertyEvent;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.IDetectorPropertyListener;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.QSpace;
import uk.ac.diamond.scisoft.analysis.hdf5.HDF5NodeLink;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.XAxisLineBoxROI;

public class DiffractionCalibrationView extends ViewPart {

	private static Logger logger = LoggerFactory.getLogger(DiffractionCalibrationView.class);

	private DiffractionTableData currentData;

	private static final String POWDERCHECK_ID = "org.dawnsci.plotting.tools.powdercheck";
	private static final String DIFFRACTION_ID = "org.dawb.workbench.plotting.tools.diffraction.Diffraction";
	private static final String WAVELENGTH_NODE_PATH = "/Experimental Information/Wavelength";
	private static final String BEAM_CENTRE_XPATH = "/Detector/Beam Centre/X";
	private static final String BEAM_CENTRE_YPATH = "/Detector/Beam Centre/Y";
	private static final String DISTANCE_NODE_PATH = "/Experimental Information/Distance";

	public static final String FORMAT_MASK = "##,##0.##########";

	private List<DiffractionTableData> model = new ArrayList<DiffractionTableData>();
	private ILoaderService service;

	private Composite parent;
	private ScrolledComposite scrollComposite;
	private Composite scrollHolder;
	private TableViewer tableViewer;
	private Button calibrateImagesButton;
	private Combo calibrantCombo;
	private Group wavelengthComp;
	private Group calibOptionGroup;
	private Action deleteAction;
	private Spinner deltaDistSpinner;

	private IPlottingSystem plottingSystem;

	private ISelectionChangedListener selectionChangeListener;
	private DropTargetAdapter dropListener;
//	private IDiffractionCrystalEnvironmentListener diffractionCrystEnvListener;
	private IDetectorPropertyListener detectorPropertyListener;
	private CalibrantSelectedListener calibrantChangeListener;

	private List<String> pathsList = new ArrayList<String>();

	private FormattedText wavelengthDistanceField;
	private FormattedText wavelengthEnergyField;

	private IToolPageSystem toolSystem;

	private boolean useI12 = true; // these two flags should match the default calibration action
	private boolean useMultiple = false;
	
	private CalibrantPositioningWidget calibrantPositioning;

	public DiffractionCalibrationView() {
		service = (ILoaderService) PlatformUI.getWorkbench().getService(ILoaderService.class);
	}

	private static final String DATA_PATH = "DataPath";
	private static final String CALIBRANT = "Calibrant";

	private String calibrantName;

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
			for (TableItem t : tableViewer.getTable().getItems()) {
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
				}
			}
		};

//		diffractionCrystEnvListener = new IDiffractionCrystalEnvironmentListener() {
//			@Override
//			public void diffractionCrystalEnvironmentChanged(
//					final DiffractionCrystalEnvironmentEvent evt) {
//				display.asyncExec(new Runnable() {
//					@Override
//					public void run() {
//						// if the change is triggered by the text field update and not the diffraction tool
//						// update spinner value with data from diff tool
//						NumericNode<Length> node = getDiffractionTreeNode(WAVELENGTH_NODE_PATH);
//						if (node.getUnit().equals(NonSI.ANGSTROM)) {
//							wavelengthDistanceField.setValue(node.getDoubleValue());
//							wavelengthEnergyField.setValue(getWavelengthEnergy(node.getDoubleValue()));
//						} else if (node.getUnit().equals(NonSI.ELECTRON_VOLT)) {
//							wavelengthDistanceField.setValue(getWavelengthEnergy(node.getDoubleValue() / 1000));
//							wavelengthEnergyField.setValue(node.getDoubleValue() / 1000);
//						} else if (node.getUnit().equals(SI.KILO(NonSI.ELECTRON_VOLT))) {
//							wavelengthDistanceField.setValue(getWavelengthEnergy(node.getDoubleValue()));
//							wavelengthEnergyField.setValue(node.getDoubleValue());
//						}
//						tableViewer.refresh();
//					}
//				});
//			}
//		};

		detectorPropertyListener = new IDetectorPropertyListener() {
			@Override
			public void detectorPropertiesChanged(DetectorPropertyEvent evt) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						tableViewer.refresh();
					}
				});
			}
		};

		calibrantChangeListener = new CalibrantSelectedListener() {
			@Override
			public void calibrantSelectionChanged(CalibrantSelectionEvent evt) {
				calibrantCombo.select(calibrantCombo.indexOf(evt.getCalibrant()));
			}
		};

		dropListener = new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				Object dropData = event.data;
				DiffractionTableData good = null;
				if (dropData instanceof IResource[]) {
					IResource[] res = (IResource[]) dropData;
					for (int i = 0; i < res.length; i++) {
						DiffractionTableData d = createData(res[i].getRawLocation().toOSString(), null);
						if (d != null) {
							good = d;
							setWavelength(d);
						}
					}
				} else if (dropData instanceof TreeSelection) {
					TreeSelection selectedNode = (TreeSelection) dropData;
					Object obj[] = selectedNode.toArray();
					for (int i = 0; i < obj.length; i++) {
						DiffractionTableData d = null;
						if (obj[i] instanceof HDF5NodeLink) {
							HDF5NodeLink node = (HDF5NodeLink) obj[i];
							if (node == null)
								return;
							d = createData(node.getFile().getFilename(), node.getFullName());
						} else if (obj[i] instanceof IFile) {
							IFile file = (IFile) obj[i];
							d = createData(file.getLocation().toOSString(), null);
						}
						if (d != null) {
							good = d;
							setWavelength(d);
						}
					}
				} else if (dropData instanceof String[]) {
					String[] selectedData = (String[]) dropData;
					for (int i = 0; i < selectedData.length; i++) {
						DiffractionTableData d = createData(selectedData[i], null);
						if (d != null) {
							good = d;
							setWavelength(d);
						}
					}
				}

				tableViewer.refresh();
				if (currentData == null && good != null) {
					tableViewer.getTable().deselectAll();
					tableViewer.setSelection(new StructuredSelection(good));
				}
				if (model.size() > 0)
					setXRaysModifiersEnabled(true);
			}
		};

		deleteAction = new Action("Delete item", Activator.getImageDescriptor("icons/delete_obj.png")) {
			@Override
			public void run() {
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				DiffractionTableData selectedData = (DiffractionTableData) selection.getFirstElement();
				if (model.size() > 0) {
					if (model.remove(selectedData)) {
						selectedData.augmenter.deactivate(service.getLockedDiffractionMetaData()!=null);
						selectedData.md.getDetector2DProperties().removeDetectorPropertyListener(detectorPropertyListener);
//						selectedData.md.getDiffractionCrystalEnvironment().removeDiffractionCrystalEnvironmentListener(diffractionCrystEnvListener);
						tableViewer.refresh();
					}
				}
				if (!model.isEmpty()) {
					drawSelectedData((DiffractionTableData) tableViewer.getElementAt(0));
				} else {
					currentData = null; // need to reset this
					plottingSystem.clear();
					setXRaysModifiersEnabled(false);
					setCalibrateOptionsEnabled(false);
				}
			}
		};

		// main sash form which contains the left sash and the plotting system
		SashForm mainSash = new SashForm(parent, SWT.HORIZONTAL);
		mainSash.setBackground(new Color(display, 192, 192, 192));
		mainSash.setLayout(new FillLayout());

		// left sash form which contains the diffraction calibration controls
		// and the diffraction tool
		SashForm leftSash = new SashForm(mainSash, SWT.VERTICAL);
		leftSash.setBackground(new Color(display, 192, 192, 192));
		leftSash.setLayout(new GridLayout(1, false));
		leftSash.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite controlComp = new Composite(leftSash, SWT.NONE);
		controlComp.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(controlComp);
		createToolbarActions(controlComp);

		Label instructionLabel = new Label(controlComp, SWT.WRAP);
		instructionLabel.setText("Drag/drop a file/data to the table below, " +
				"choose a type of calibrant, " +
				"modify the rings using the positioning controls, " +
				"modify the wavelength/energy with the wanted values, " +
				"match rings to the image, " +
				"and select the calibration type before running the calibration process.");
		instructionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		Point pt = instructionLabel.getSize(); pt.x +=4; pt.y += 4; instructionLabel.setSize(pt);

		// make a scrolled composite
		scrollComposite = new ScrolledComposite(controlComp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrollComposite.setLayout(new GridLayout(1, false));
		scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrollHolder = new Composite(scrollComposite, SWT.NONE);

		GridLayout gl = new GridLayout(1, false);
		scrollHolder.setLayout(gl);
		scrollHolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

		// table of images and found rings
		tableViewer = new TableViewer(scrollHolder, SWT.FULL_SELECTION
				| SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		createColumns(tableViewer);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setToolTipText("Drag/drop file(s)/data to this table");
		tableViewer.setContentProvider(new MyContentProvider());
		tableViewer.setLabelProvider(new MyLabelProvider());
		tableViewer.setInput(model);
		tableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer.addSelectionChangedListener(selectionChangeListener);
		tableViewer.refresh();
		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				if (!selection.isEmpty()) {
					deleteAction.setText("Delete "+ ((DiffractionTableData) selection.getFirstElement()).name);
					mgr.add(deleteAction);
				}
			}
		});
		tableViewer.getControl().setMenu(mgr.createContextMenu(tableViewer.getControl()));
		// add drop support
		DropTarget dt = new DropTarget(tableViewer.getControl(), DND.DROP_MOVE | DND.DROP_DEFAULT | DND.DROP_COPY);
		dt.setTransfer(new Transfer[] { TextTransfer.getInstance(),
				FileTransfer.getInstance(), ResourceTransfer.getInstance(),
				LocalSelectionTransfer.getTransfer() });
		dt.addDropListener(dropListener);

		Composite calibrantHolder = new Composite(scrollHolder, SWT.NONE);
		calibrantHolder.setLayout(new GridLayout(1, false));
		calibrantHolder.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		Composite mainControlComp = new Composite(calibrantHolder, SWT.NONE);
		mainControlComp.setLayout(new GridLayout(2, false));
		mainControlComp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		Composite leftCalibComp = new Composite(mainControlComp, SWT.NONE);
		leftCalibComp.setLayout(new GridLayout(1, false));
		leftCalibComp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		
		// create calibrant combo
		Composite selectCalibComp = new Composite(leftCalibComp, SWT.FILL);
		selectCalibComp.setLayout(new GridLayout(2, false));
		selectCalibComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label l = new Label(selectCalibComp, SWT.NONE);
		l.setText("Calibrant:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		calibrantCombo = new Combo(selectCalibComp, SWT.READ_ONLY);
		final CalibrationStandards standards = CalibrationFactory
				.getCalibrationStandards();
		calibrantCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (currentData == null)
					return;
				String calibrantName = calibrantCombo.getItem(calibrantCombo
						.getSelectionIndex());
				// update the calibrant in diffraction tool
				standards.setSelectedCalibrant(calibrantName, true);
				DiffractionCalibrationUtils
				.drawCalibrantRings(currentData.augmenter);
			}
		});
		for (String c : standards.getCalibrantList()) {
			calibrantCombo.add(c);
		}
		String s = standards.getSelectedCalibrant();
		if (s != null) {
			calibrantCombo.setText(s);
		}
		calibrantCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button goBabyGoButton = new Button(leftCalibComp, SWT.PUSH);
		goBabyGoButton.setText("Quick Calibration");
		goBabyGoButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		goBabyGoButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = PowderCalibrationUtils.autoFindEllipses(display, plottingSystem, currentData);
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								updateIntegrated(currentData);
							}
						});
					}
				});
				job.setUser(true);
				job.schedule();
				
//				IRunnableWithProgress runnable = PowderCalibrationUtils.autoCalibrate(display, plottingSystem, currentData);
//				
//				try {
//					new ProgressMonitorDialog(display.getActiveShell()).run(true, true, runnable);
//				} catch (InvocationTargetException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
			}
		});

		calibrantPositioning = new CalibrantPositioningWidget(leftCalibComp, model);

		Composite rightCalibComp = new Composite(mainControlComp, SWT.NONE);
		rightCalibComp.setLayout(new GridLayout(1, false));
		rightCalibComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Radio group
		calibOptionGroup = new Group(rightCalibComp, SWT.BORDER);
		calibOptionGroup.setLayout(new GridLayout(1, false));
		calibOptionGroup.setText("Calibration options");
		try {
			RadioUtils.createRadioControls(calibOptionGroup, createWavelengthRadioActions());
		} catch (Exception e) {
			logger.error("Could not create controls:" + e);
		}
		
		deltaDistSpinner = new Spinner(calibOptionGroup, SWT.BORDER);
		deltaDistSpinner.setMaximum(1000);
		deltaDistSpinner.setMinimum(0);
		deltaDistSpinner.setSelection(100);
		deltaDistSpinner.setLayout(new GridLayout(1, false));
		calibrateImagesButton = new Button(calibOptionGroup, SWT.PUSH);
		calibrateImagesButton.setText("Run Calibration Process");
		calibrateImagesButton.setToolTipText("Calibrate detector in chosen images");
		calibrateImagesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		calibrateImagesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model.size() <= 0)
					return;
				//TODO make new job
				Job calibrateJob;
				if (useI12) calibrateJob = PowderCalibrationUtils.calibrateImagesMajorAxisMethod(display, plottingSystem, currentData);
				else if (useMultiple) calibrateJob = PowderCalibrationUtils.calibrateMultipleImages(display, plottingSystem, model, currentData, (double)deltaDistSpinner.getSelection());
				else calibrateJob = PowderCalibrationUtils.calibrateImages(display, plottingSystem, model, currentData,
						true, false);
				if (calibrateJob == null)
					return;
				calibrateJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						display.asyncExec(new Runnable() {
							public void run() {
								refreshTable();
								double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
								int previousPrecision = BigDecimal.valueOf((Double)wavelengthDistanceField.getValue()).precision();
								wavelength = DiffractionCalibrationUtils.setPrecision(wavelength, previousPrecision);
								wavelengthDistanceField.setValue(wavelength);
								wavelengthEnergyField.setValue(DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
								setCalibrateButtons();
								updateIntegrated(currentData);
							}
						});
					}
				});
				calibrateJob.schedule();
			}
		});
		setCalibrateOptionsEnabled(false);

		createXRayGroup(rightCalibComp);
		// Enable/disable the modifiers
		setXRaysModifiersEnabled(false);

		scrollHolder.layout();
		scrollComposite.setContent(scrollHolder);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.setExpandVertical(true);
		scrollComposite.setMinSize(scrollHolder.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrollComposite.layout();
		// end of Diffraction Calibration controls

		// start plotting system
//		Composite plotComp = new Composite(mainSash, SWT.NONE);
//		plotComp.setLayout(new GridLayout(1, false));
//		plotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		try {
//			ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(plotComp, null);
//			plottingSystem = PlottingFactory.createPlottingSystem();
//			plottingSystem.createPlotPart(plotComp, "", actionBarWrapper, PlotType.IMAGE, this);
//			plottingSystem.setTitle("");
//			plottingSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		} catch (Exception e1) {
//			logger.error("Could not create plotting system:" + e1);
//		}
		
		SashForm right = new SashForm(mainSash, SWT.VERTICAL);
		right.setBackground(new Color(display, 192, 192, 192));
		right.setLayout(new GridLayout(1, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// start plotting system
		Composite plotComp = new Composite(right, SWT.NONE);
		plotComp.setLayout(new GridLayout(1, false));
		plotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridUtils.removeMargins(plotComp);
		
		
		try {
			ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(plotComp, null);
			plottingSystem = PlottingFactory.createPlottingSystem();
			plottingSystem.createPlotPart(plotComp, "", actionBarWrapper, PlotType.IMAGE, this);
			plottingSystem.setTitle("");
			plottingSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} catch (Exception e1) {
			logger.error("Could not create plotting system:" + e1);
		}
		
		
//		try {
//			ActionBarWrapper actionBarWrapper2 = ActionBarWrapper.createActionBars(resultComp, null);
//			resultSystem = PlottingFactory.createPlottingSystem();
//			resultSystem.createPlotPart(resultComp, "", actionBarWrapper2, PlotType.XY, this);
//			resultSystem.setTitle("");
//			resultSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		} catch (Exception e1) {
//			logger.error("Could not create plotting system:" + e1);
//		}

		
		// try to load the previous data saved in the memento
		DiffractionTableData good = null;
		for (String p : pathsList) {
			if (!p.endsWith(".nxs")) {
				DiffractionTableData d = createData(p, null);
				if (good == null && d != null) {
					good = d;
					setWavelength(d);
					setCalibrant();
				}
			}
		}
		tableViewer.refresh();
		if (good != null) {
			final DiffractionTableData g = good;
			display.asyncExec(new Runnable() { // this is necessary to give the plotting system time to lay out itself
				@Override
				public void run() {
					tableViewer.setSelection(new StructuredSelection(g));
				}
			});
		}
		if (model.size() > 0)
			setXRaysModifiersEnabled(true);

		// start diffraction tool
		Composite diffractionToolComp = new Composite(leftSash, SWT.BORDER);
		//diffractionToolComp.setLayout(new FillLayout());
		try {
			toolSystem = (IToolPageSystem) plottingSystem.getAdapter(IToolPageSystem.class);
			// Show tools here, not on a page.
			toolSystem.setToolComposite(diffractionToolComp);
			toolSystem.setToolVisible(DIFFRACTION_ID, ToolPageRole.ROLE_2D, null);
		} catch (Exception e2) {
			logger.error("Could not open diffraction tool:" + e2);
		}

		CalibrationFactory.addCalibrantSelectionListener(calibrantChangeListener);
		// mainSash.setWeights(new int[] { 1, 2});
		
		// start plotting system
		Composite resultComp = new Composite(right, SWT.NONE);
		//resultComp.setLayout(new GridLayout(1, false));
		resultComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridUtils.removeMargins(resultComp);
		
		
		resultComp.setLayout(new FillLayout());
		try {
			toolSystem.setToolComposite(resultComp);
			toolSystem.setToolVisible(POWDERCHECK_ID, ToolPageRole.ROLE_2D, null);
		} catch (Exception e2) {
			logger.error("Could not open powder check tool:" + e2);
		}
		
		calibrantPositioning.setPlottingSystem(plottingSystem);
		calibrantPositioning.setToolSystem(toolSystem);
		calibrantPositioning.setControlsToUpdate(calibOptionGroup, calibrateImagesButton);
		calibrantPositioning.setTableViewerToUpdate(tableViewer);
		
		
	}

	private void setCalibrateOptionsEnabled(boolean b) {
		calibOptionGroup.setEnabled(b);
		calibrateImagesButton.setEnabled(b);
	}
	
	private void createXRayGroup(Composite composite) {
		wavelengthComp = new Group(composite, SWT.NONE);
		wavelengthComp.setText("X-Rays");
		wavelengthComp.setToolTipText("Set the wavelength / energy");
		wavelengthComp.setLayout(new GridLayout(3, false));
		wavelengthComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label wavelengthLabel = new Label(wavelengthComp, SWT.NONE);
		wavelengthLabel.setText("Wavelength");

		wavelengthDistanceField = new FormattedText(wavelengthComp, SWT.SINGLE | SWT.BORDER);
		wavelengthDistanceField.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		wavelengthDistanceField.getControl().setToolTipText("Set the wavelength in Angstrom");
		wavelengthDistanceField.getControl().addListener(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// update wavelength of each image
				double distance = 0;
				Object obj = wavelengthDistanceField.getValue();
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
					wavelengthEnergyField.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
				}
				wavelengthEnergyField.setValue(energy);
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
		wavelengthDistanceField.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label unitDistanceLabel = new Label(wavelengthComp, SWT.NONE);
		unitDistanceLabel.setText(NonSI.ANGSTROM.toString());

		Label energyLabel = new Label(wavelengthComp, SWT.NONE);
		energyLabel.setText("Energy");

		wavelengthEnergyField = new FormattedText(wavelengthComp, SWT.SINGLE | SWT.BORDER);
		wavelengthEnergyField.setFormatter(new NumberFormatter(FORMAT_MASK, FORMAT_MASK, Locale.UK));
		wavelengthEnergyField.getControl().setToolTipText("Set the wavelength in keV");
		wavelengthEnergyField.getControl().addListener(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// update wavelength of each image
				double energy = 0;
				Object obj = wavelengthEnergyField.getValue();
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
					wavelengthDistanceField.setFormatter(new NumberFormatter(FORMAT_MASK, newFormat, Locale.UK));
				}
				wavelengthDistanceField.setValue(distance);
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
		wavelengthEnergyField.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label unitEnergyLabel = new Label(wavelengthComp, SWT.NONE);
		unitEnergyLabel.setText(SI.KILO(NonSI.ELECTRON_VOLT).toString());
	}

	private void setXRaysModifiersEnabled(boolean b) {
		wavelengthComp.setEnabled(b);
		wavelengthDistanceField.getControl().setEnabled(b);
		wavelengthEnergyField.getControl().setEnabled(b);
	}

	private void createToolbarActions(Composite parent) {
		ToolBar tb = new ToolBar(parent, SWT.NONE);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

		Image exportImage = new Image(Display.getDefault(), Activator.getImageDescriptor("icons/page_white_excel.png").getImageData());
		Image resetRingsImage = new Image(Display.getDefault(), Activator.getImageDescriptor("icons/reset_rings.png").getImageData());
		Image resetImage = new Image(Display.getDefault(), Activator.getImageDescriptor("icons/table_delete.png").getImageData());
		ToolItem exportItem = new ToolItem(tb, SWT.PUSH);
		ToolItem resetRingsItem = new ToolItem(tb, SWT.PUSH);
		ToolItem resetItem = new ToolItem(tb, SWT.PUSH);

		Button exportButton = new Button(tb, SWT.PUSH);
		exportItem.setToolTipText("Export metadata to XLS");
		exportItem.setControl(exportButton);
		exportItem.setImage(exportImage);

		Button resetRingsButton = new Button(tb, SWT.PUSH);
		resetRingsItem.setToolTipText("Remove found rings");
		resetRingsItem.setControl(resetRingsButton);
		resetRingsItem.setImage(resetRingsImage);

		Button resetButton = new Button(tb, SWT.PUSH);
		resetItem.setToolTipText("Reset metadata");
		resetItem.setControl(resetButton);
		resetItem.setImage(resetImage);

		exportItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
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
		});

		resetRingsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				DiffractionCalibrationUtils.hideFoundRings(plottingSystem);
			}
		});

		resetItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// select last item in table
				if (model != null && model.size() > 0) {
					tableViewer.setSelection(new StructuredSelection(model.get(model.size() - 1)));
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
					wavelengthEnergyField.setValue(DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
					wavelengthDistanceField.setValue(wavelength);
					// update wavelength in diffraction tool tree viewer
					NumericNode<Length> node = getDiffractionTreeNode(WAVELENGTH_NODE_PATH);
					if (node.getUnit().equals(NonSI.ANGSTROM)) {
						updateDiffTool(WAVELENGTH_NODE_PATH, wavelength);
					} else if (node.getUnit().equals(NonSI.ELECTRON_VOLT)) {
						updateDiffTool(WAVELENGTH_NODE_PATH, DiffractionCalibrationUtils.getWavelengthEnergy(wavelength) * 1000);
					} else if (node.getUnit().equals(SI.KILO(NonSI.ELECTRON_VOLT))) {
						updateDiffTool(WAVELENGTH_NODE_PATH, DiffractionCalibrationUtils.getWavelengthEnergy(wavelength));
					}
					tableViewer.refresh();
				}
			}
		});
	}

	private List<Action> createWavelengthRadioActions() {
		List<Action> radioActions = new ArrayList<Action>();

		Action usedFixedWavelengthAction = new Action() {
			@Override
			public void run() {
				useI12 = true;
				useMultiple = false;
			}
		};
		usedFixedWavelengthAction.setText("Per Image Fit with fixed wavelength (I12)");
		usedFixedWavelengthAction.setToolTipText("Individual fit with fixed wavelength"); // TODO write a more detailed tool tip

		Action simultaneousFitAction = new Action() {
			@Override
			public void run() {
				useI12 = false;
				useMultiple = false;
			}
		};
		simultaneousFitAction.setText("Per Image Fit with fixed wavelength (Pete)");
		simultaneousFitAction.setToolTipText("Fits all the parameters at once"); // TODO write a more detailed tool tip

		Action postWavelengthAction = new Action() {
			@Override
			public void run() {
				useI12 = false;
				useMultiple = true;
			}
		};
		postWavelengthAction.setText("Multiple image fit");
		postWavelengthAction.setToolTipText("Fit for wavelength and distances"); // TODO write a more detailed tool tip

//		simultaneousFitAction.setEnabled(false);
//		postWavelengthAction.setEnabled(false);
		radioActions.add(usedFixedWavelengthAction);
		radioActions.add(simultaneousFitAction);
		radioActions.add(postWavelengthAction);

		return radioActions;
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
			wavelengthDistanceField.setValue(wavelength);
			double energy = DiffractionCalibrationUtils.getWavelengthEnergy(wavelength);
			if (energy != Double.POSITIVE_INFINITY) {
				wavelengthEnergyField.setFormatter(new NumberFormatter(FORMAT_MASK, 
						DiffractionCalibrationUtils.getFormatMask(wavelength, energy), Locale.UK));
			}
			wavelengthEnergyField.setValue(energy);
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

	private DiffractionTableData createData(String filePath, String dataFullName) {
		// Test if the selection has already been loaded and is in the model
		DiffractionTableData data = null;
		if (filePath == null)
			return data;

		for (DiffractionTableData d : model) {
			if (filePath.equals(d.path)) {
				data = d;
				break;
			}
		}

		if (data == null) {
			IDataset image = PlottingUtils.loadData(filePath, dataFullName);
			if (image == null)
				return data;
			int j = filePath.lastIndexOf(File.separator);
			String fileName = j > 0 ? filePath.substring(j + 1) : null;
			image.setName(fileName + ":" + image.getName());

			data = new DiffractionTableData();
			data.path = filePath;
			data.name = fileName;
			data.image = image;
			String[] statusString = new String[1];
			data.md = DiffractionTool.getDiffractionMetadata(image, filePath, service, statusString);
			model.add(data);
		}

		return data;
	}

	private void drawSelectedData(DiffractionTableData data) {
		if (currentData != null) {
			DiffractionImageAugmenter aug = currentData.augmenter;
			if (aug != null)
				aug.deactivate(service.getLockedDiffractionMetaData()!=null);
		}

		if (data.image == null)
			return;

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
			data.md.getDetector2DProperties().addDetectorPropertyListener(detectorPropertyListener);
//			data.md.getDiffractionCrystalEnvironment().addDiffractionCrystalEnvironmentListener(diffractionCrystEnvListener);
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
			return plottingSystem.getAdapter(IToolPageSystem.class);
		}
		return super.getAdapter(key);
	}

	class MyContentProvider implements IStructuredContentProvider {
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null) {
				return null;
			}
			return ((List<?>) inputElement).toArray();
		}
	}

	private static final Image TICKED = Activator.getImageDescriptor("icons/ticked.png").createImage();
	private static final Image UNTICKED = Activator.getImageDescriptor("icons/unticked.gif").createImage();

	class MyLabelProvider implements ITableLabelProvider {
		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return true;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (element == null)
				return null;

			DiffractionTableData data = (DiffractionTableData) element;
			if (data.use)
				return TICKED;
			return UNTICKED;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0)
				return null;
			if (element == null)
				return null;

			DiffractionTableData data = (DiffractionTableData) element;
			if (columnIndex == 1) {
				return data.name;
			} else if (columnIndex == 2) {
				if (data.rois == null)
					return null;
				return String.valueOf(data.nrois);
			}

			IDiffractionMetadata md = data.md;
			if (md == null)
				return null;

			if (columnIndex == 3) {
				DetectorProperties dp = md.getDetector2DProperties();
				if (dp == null)
					return null;
				return String.format("%.2f", dp.getDetectorDistance());
			} else if (columnIndex == 4) {
				DetectorProperties dp = md.getDetector2DProperties();
				if (dp == null)
					return null;
				return String.format("%.2f", dp.getBeamCentreCoords()[0]);
			} else if (columnIndex == 5) {
				DetectorProperties dp = md.getDetector2DProperties();
				if (dp == null)
					return null;
				return String.format("%.2f", dp.getBeamCentreCoords()[1]);
			} else if (columnIndex == 6) {
				if (data.use && data.q != null) {
					return String.format("%.2f", Math.sqrt(data.q.getResidual()));
				}
			}
			return null;
		}
	}

	class MyEditingSupport extends EditingSupport {
		private TableViewer tv;
		private int column;

		public MyEditingSupport(TableViewer viewer, int col) {
			super(viewer);
			tv = viewer;
			this.column = col;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new CheckboxCellEditor(null, SWT.CHECK);
		}

		@Override
		protected boolean canEdit(Object element) {
			if (column == 0)
				return true;
			else
				return false;
		}

		@Override
		protected Object getValue(Object element) {
			DiffractionTableData data = (DiffractionTableData) element;
			return data.use;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (column == 0) {
				DiffractionTableData data = (DiffractionTableData) element;
				data.use = (Boolean) value;
				tv.refresh();

				setCalibrateButtons();
			}
		}
	}

	private void createColumns(TableViewer tv) {
		TableViewerColumn tvc = new TableViewerColumn(tv, SWT.NONE);
		tvc.setEditingSupport(new MyEditingSupport(tv, 0));
		TableColumn tc = tvc.getColumn();
		tc.setText("Use");
		tc.setWidth(40);

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("Image");
		tc.setWidth(200);
		tvc.setEditingSupport(new MyEditingSupport(tv, 1));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("# of rings");
		tc.setWidth(75);
		tvc.setEditingSupport(new MyEditingSupport(tv, 2));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("Distance");
		tc.setToolTipText("in mm");
		tc.setWidth(70);
		tvc.setEditingSupport(new MyEditingSupport(tv, 3));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("X Position");
		tc.setToolTipText("in Pixel");
		tc.setWidth(80);
		tvc.setEditingSupport(new MyEditingSupport(tv, 4));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("Y Position");
		tc.setToolTipText("in Pixel");
		tc.setWidth(80);
		tvc.setEditingSupport(new MyEditingSupport(tv, 5));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("Residuals");
		tc.setToolTipText("Root mean of squared residuals from fit");
		tc.setWidth(80);
		tvc.setEditingSupport(new MyEditingSupport(tv, 5));
	}

	private void refreshTable() {
		if (tableViewer == null)
			return;
		tableViewer.refresh();
		// reset the scroll composite
		Rectangle r = scrollHolder.getClientArea();
		scrollComposite.setMinSize(scrollHolder.computeSize(r.width, SWT.DEFAULT));
		scrollHolder.layout();
	}

	private void setCalibrateButtons() {
		// enable/disable calibrate button according to use column
		int used = 0;
		for (DiffractionTableData d : model) {
			if (d.use && d.nrois > 0) {
				used++;
			}
		}
		setCalibrateOptionsEnabled(used > 0);
	}

	private void removeListeners() {
		tableViewer.removeSelectionChangedListener(selectionChangeListener);
		CalibrationFactory.removeCalibrantSelectionListener(calibrantChangeListener);
		// deactivate the diffraction tool
		DiffractionTool diffTool = (DiffractionTool) toolSystem.getToolPage(DIFFRACTION_ID);
		if (diffTool != null)
			diffTool.deactivate();
		// deactivate each augmenter in loaded data
		for (DiffractionTableData d : model) {
			if (d.augmenter != null)
				d.augmenter.deactivate(service.getLockedDiffractionMetaData()!=null);
			d.md.getDetector2DProperties().removeDetectorPropertyListener(detectorPropertyListener);
//			d.md.getDiffractionCrystalEnvironment().removeDiffractionCrystalEnvironmentListener(diffractionCrystEnvListener);
		}
		model.clear();
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