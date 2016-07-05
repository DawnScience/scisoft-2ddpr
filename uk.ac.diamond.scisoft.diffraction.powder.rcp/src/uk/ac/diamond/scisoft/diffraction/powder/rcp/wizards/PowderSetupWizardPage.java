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

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.CalibrantSelectionGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.CalibrationOptionsGroup;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.RingSelectionGroup;

public class PowderSetupWizardPage extends WizardPage {

	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	private DiffractionImageAugmenter augmenter;
	private SimpleCalibrationParameterModel model = new SimpleCalibrationParameterModel();
	private SimpleCalibrationParameterModel lastRunModel = null;
	private IToolPage toolPage;
	
	protected PowderSetupWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Set-Up");
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
		
		final CalibrantSelectionGroup group = new CalibrantSelectionGroup(left);
		group.addCalibrantSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (manager.getCurrentData() == null)
					return;
				String calibrantName = group.getCalibrant();
				// update the calibrant in diffraction tool
				CalibrationFactory.getCalibrationStandards().setSelectedCalibrant(calibrantName, true);
				// set the maximum number of rings
				int ringMaxNumber = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size();
				
			}
		});
		
		group.addDisplaySelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showCalibrantAndBeamCentre(group.getShowRings());
			}
		});
		
		final RingSelectionGroup ringSelection = new RingSelectionGroup(left, CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size());
		ringSelection.addRingNumberSpinnerListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ringNumber = ringSelection.getRingSpinnerSelection();
				model.setNumberOfRings(ringNumber);
//				calibrantRingsMap.put(calibrantName, ringNumber);
			}
		});
		CalibrationOptionsGroup options = new CalibrationOptionsGroup(left, model, true,false);
		
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
		if (!visible) {
			if (!model.equals(lastRunModel)) {
				model.setFinalGlobalOptimisation(true);
				IRunnableWithProgress job = new AutoCalibrationRun(Display.getDefault(), system,manager , manager.getCurrentData(), model);
				lastRunModel = new SimpleCalibrationParameterModel(model);

				try {
					getContainer().run(true, true, job);
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (visible) toolPage.activate();
		else toolPage.deactivate();
		
		super.setVisible(visible);
	}

}
