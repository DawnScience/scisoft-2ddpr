package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.lang.reflect.InvocationTargetException;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
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
import org.eclipse.swt.widgets.Label;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderResultWizardPage extends WizardPage {

	
	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	private DiffractionImageAugmenter augmenter;
	IToolPage toolPage;

	protected PowderResultWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Set-Up");
		this.manager = manager;
//		manager = new DiffractionDataManager();
//		
//		
//		
//		String path = "/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d359-00016.tif";
//		String dataset = "image-01";
//		
//		manager.loadData(path, dataset);
//		DiffractionTableData currentData = manager.getCurrentData();
//		manager.toString();
		
//		try {
//			ILazyDataset image = LoaderFactory.getData(path).getLazyDataset(dataset);
//			
//			manager.setImage(image);
//			
//			IDiffractionMetadata md = DiffractionDefaultMetadata.getDiffractionMetadata(image.getShape());
//			image.setMetadata(md);
//			data.setMetaData(md);
//			data.setName("CeO2_29p2keV_d359-00016.tif");
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
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
			system.createPlot2D(DatasetUtils.sliceAndConvertLazyDataset(manager.getCurrentData().getImage()), null,null);
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
		
		new Label(left, SWT.None).setText("Add cool stuff here:");
	}

	
	@Override
	public void setVisible(boolean visible) {
		if (visible) toolPage.activate();
		else toolPage.deactivate();
		
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
