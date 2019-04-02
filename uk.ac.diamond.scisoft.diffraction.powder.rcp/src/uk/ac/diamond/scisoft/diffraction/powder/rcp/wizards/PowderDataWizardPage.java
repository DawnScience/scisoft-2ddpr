package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import org.dawnsci.plotting.actions.ActionBarWrapper;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDelegate;

public class PowderDataWizardPage extends WizardPage {

	private IPlottingSystem<Composite> system;
	private DiffractionDataManager manager;
	
	protected PowderDataWizardPage(DiffractionDataManager manager) {
		super("Powder Calibration Data");
		setTitle("Powder XRD/SAX Calibration - Data Review");
		setDescription("Review the images, and distances ready for calibration, click next to continue.");
		this.manager = manager;
		setPageComplete(true);
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
		
		DiffractionDelegate diffractionTableViewer = new DiffractionDelegate(left, manager);
		diffractionTableViewer.updateTableColumnsAndLayout(0);
		diffractionTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection is = event.getSelection();
				if (is instanceof StructuredSelection) {
					StructuredSelection structSelection = (StructuredSelection) is;
					DiffractionImageData selectedData = (DiffractionImageData) structSelection.getFirstElement();
					manager.setCurrentData(selectedData);
					updatePlot(manager.getCurrentData().getImage());
				}
			}
		});
		
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
	
	private void updatePlot(IDataset image) {
		try {
			system.createPlot2D(DatasetUtils.sliceAndConvertLazyDataset(image), null,null);
		} catch (DatasetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			updatePlot(manager.getCurrentData().getImage());
		} else {
			system.clear();
		}
        super.setVisible(visible);
    }

}
