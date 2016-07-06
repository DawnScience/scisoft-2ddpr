package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderCalibrationWizard extends Wizard {

	private DiffractionDataManager manager;
	
	public PowderCalibrationWizard(DiffractionDataManager manager) {
		this.manager = manager;
		setUpPages();
	}
	
	private void setUpPages(){
		addPage(new PowderDataWizardPage(manager));
		addPage(new PowderSetupWizardPage(manager));
		addPage(new PowderResultWizardPage(manager));
	}
	
	@Override
	public void createPageControls(Composite pageContainer){
		super.createPageControls(pageContainer);
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
		
		return true;
	}

}
