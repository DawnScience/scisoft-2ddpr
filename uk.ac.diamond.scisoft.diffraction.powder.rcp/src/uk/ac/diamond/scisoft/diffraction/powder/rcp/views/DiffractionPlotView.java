package uk.ac.diamond.scisoft.diffraction.powder.rcp.views;

import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View used to hold a plotting system for the Powder diffraction calibration view
 * @author wqk87977
 *
 */
public class DiffractionPlotView extends ViewPart {

	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.plottingView";
	private IPlottingSystem<Composite> plottingSystem;
	private final Logger logger = LoggerFactory.getLogger(DiffractionPlotView.class);
	public static final String DIFFRACTION_PLOT_TITLE = "Diffraction Plotting";

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		try {
			plottingSystem = PlottingFactory.getPlottingSystem(DiffractionPlotView.DIFFRACTION_PLOT_TITLE);
			if (plottingSystem == null) {
				plottingSystem = PlottingFactory.createPlottingSystem();
				plottingSystem.createPlotPart(parent, DiffractionPlotView.DIFFRACTION_PLOT_TITLE, 
						this.getViewSite().getActionBars(), PlotType.IMAGE, this);
			}
		} catch (Exception e1) {
			logger .error("Could not create plotting system:" + e1);
		}
	}

	@Override
	public void setFocus() {
		plottingSystem.setFocus();
	}

	/**
	 * Required if you want to make tools work with Abstract Plotting System.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(final Class clazz) {
		if (clazz == IToolPageSystem.class) {
			return plottingSystem != null ? plottingSystem.getAdapter(clazz) : null;
		}else if (clazz == IPlottingSystem.class) {
			return plottingSystem;
		}
		return super.getAdapter(clazz);
	}

	@Override
	public void dispose() {
		if (plottingSystem != null)
			plottingSystem.dispose();
		plottingSystem = null;
	}
}
