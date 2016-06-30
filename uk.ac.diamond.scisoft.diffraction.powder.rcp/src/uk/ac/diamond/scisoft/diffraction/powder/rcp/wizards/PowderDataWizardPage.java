package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.lang.reflect.InvocationTargetException;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.AutoCalibrationRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderDataWizardPage extends WizardPage {

	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	
	protected PowderDataWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Data");
		this.manager = manager;
	}

	@Override
	public void createControl(Composite parent) {
		
		SashForm sashForm= new SashForm(parent, SWT.HORIZONTAL);
		setControl(sashForm);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,2,1));
		
		final Composite left = new Composite(sashForm, SWT.NONE);
		left.setLayout(new GridLayout(2, false));
		Composite right = new Composite(sashForm, SWT.NONE);
		right.setLayout(new GridLayout());
		
		Composite plotComp = new Composite(right, SWT.NONE);
		plotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		plotComp.setLayout(new GridLayout());
		ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(plotComp, null);
		Composite displayPlotComp  = new Composite(plotComp, SWT.BORDER);
		displayPlotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		displayPlotComp.setLayout(new FillLayout());
		
		
		try {
			system = PlottingFactory.createPlottingSystem();
			system.createPlotPart(displayPlotComp, "PlotDataWizard", actionBarWrapper, PlotType.IMAGE, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			system.createPlot2D(DatasetUtils.sliceAndConvertLazyDataset(manager.getCurrentData().getImage()), null,null);
		} else {
			system.clear();
		}
        super.setVisible(visible);
    }

}
