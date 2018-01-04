package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderCalibrationWizard extends Wizard {

	private DiffractionDataManager manager;
	private IPageChangingListener listener;
	
	public PowderCalibrationWizard(DiffractionDataManager manager) {
		this.manager = manager;
		setUpPages();
	}
	
	public PowderCalibrationWizard(final String filePath, final String datasetName) {
		super();
		this.manager = new DiffractionDataManager();
		manager.loadData(filePath, datasetName,true);
		DiffractionImageData last = manager.getLast();
		manager.setCurrentData(last);
		setUpPages();
	}
	
	private void setUpPages(){

		final PowderSetupWizardPage page = new PowderSetupWizardPage(manager);
		final PowderResultWizardPage result = new PowderResultWizardPage(manager);

		listener = new IPageChangingListener() {

			@Override
			public void handlePageChanging(final PageChangingEvent event) {
				if (event.getCurrentPage() == page && event.getTargetPage() == result  && page.hasModelChanged()) {

					SimpleCalibrationParameterModel model = page.getModel();
					
					final IRunnableWithProgress job  = DiffractionCalibrationUtils.getCalibrationRunnable(model, manager, page.getPlottingSystem());
					page.setLastRunModel(page.getModel());

					try {
						getContainer().run(true, true, job);
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		};
		
		if (manager.getSize() > 1) addPage(new PowderDataWizardPage(manager));
		addPage(page);
		addPage(result);
	}
	
	@Override
	public void createPageControls(Composite pageContainer){
		pageContainer.getShell().setBackgroundMode(SWT.INHERIT_FORCE);
		super.createPageControls(pageContainer);
		IWizardContainer wd = getContainer();
		if (wd instanceof WizardDialog) {
			((WizardDialog)wd).addPageChangingListener(listener);
		}
		getShell().addTraverseListener(new TraverseListener() {
			   @Override
			   public void keyTraversed(TraverseEvent event) {
			     if (event.detail == SWT.TRAVERSE_RETURN) {
			        event.doit = false;
			     }
			   }
			});
	}
	
	@Override
	public boolean performFinish() {
		
		Iterable<DiffractionImageData> iterable = manager.iterable();
		
		for (DiffractionImageData data : iterable) data.clearROIs();
		
		return true;
	}

}
