package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * View to hold the powder calibration check plot
 *
 */
public class DiffractionPowderCalibCheckView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionPowderCalibCheckView";
	private Composite parent;

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
	}

	@Override
	public void setFocus() {
	}

	public Composite getComposite() {
		return parent;
	}

}
