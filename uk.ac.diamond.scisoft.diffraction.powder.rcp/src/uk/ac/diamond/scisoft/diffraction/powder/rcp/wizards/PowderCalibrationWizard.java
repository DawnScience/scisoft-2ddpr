package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.eclipse.jface.wizard.Wizard;

public class PowderCalibrationWizard extends Wizard {

	private DiffractionDataManager manager;
	
	public PowderCalibrationWizard(DiffractionDataManager manager) {
		this.manager = manager;
		setUpPages();
	}
	
	public PowderCalibrationWizard() {
		
		DiffractionDataManager manager = new DiffractionDataManager();
		
		String path = "/dls/science/groups/das/ExampleData/i15/I15_Detector_Calibration/PE_Data/29p2keV/CeO2_29p2keV_d359-00016.tif";
		String dataset = "image-01";
		
		manager.loadData(path, dataset);
		manager.getCurrentData();
		setUpPages();
		
	}
	
	private void setUpPages(){
		addPage(new PowderDataWizardPage(manager));
		addPage(new PowderSetupWizardPage(manager));
		addPage(new PowderResultWizardPage(manager));
	}
	
	@Override
	public boolean performFinish() {
		return true;
	}

}
