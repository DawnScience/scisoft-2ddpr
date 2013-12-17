package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * View used to hold a plotting system for the Powder diffraction calibration view
 * @author wqk87977
 *
 */
public class DiffractionPlotView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.plottingView";
	private Composite parent;
	public static final String DIFFRACTION_PLOT_TITLE = "Diffraction Plotting";

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
