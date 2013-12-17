package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * View to hold the powder diffraction tool
 * @author wqk87977
 *
 */
public class DiffractionPowderToolView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionPowderToolView";
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
