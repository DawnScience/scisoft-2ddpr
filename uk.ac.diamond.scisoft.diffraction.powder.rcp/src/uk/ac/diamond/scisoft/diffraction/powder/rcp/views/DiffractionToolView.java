package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * View to hold the diffraction tool
 * @author wqk87977
 *
 */
public class DiffractionToolView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.diffractionToolView";
	private Composite parent;

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public Composite getComposite() {
		return parent;
	}

}
