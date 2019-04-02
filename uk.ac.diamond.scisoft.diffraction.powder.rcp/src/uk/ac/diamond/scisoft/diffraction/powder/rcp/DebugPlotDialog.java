package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.dawnsci.plotting.actions.ActionBarWrapper;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class DebugPlotDialog extends Dialog {

	private IPlottingSystem<Composite> plottingSystem;
	
	public DebugPlotDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		try {
			ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(container, null);
			plottingSystem = PlottingFactory.createPlottingSystem();
			plottingSystem.createPlotPart(container, "", actionBarWrapper, PlotType.IMAGE, null);
			plottingSystem.setTitle("");
			plottingSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} catch (Exception e1) {
			
		}

		return container;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}
	
	public IPlottingSystem<Composite> getPlottingSystem() {
		return plottingSystem;
	}

}
