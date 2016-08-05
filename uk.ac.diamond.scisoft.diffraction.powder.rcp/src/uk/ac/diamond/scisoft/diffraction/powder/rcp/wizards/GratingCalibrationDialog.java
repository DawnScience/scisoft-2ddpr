/*-
 * Copyright 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.tool.ToolPageFactory;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class GratingCalibrationDialog extends Dialog {

	private DiffractionDataManager manager;
	private IPlottingSystem<Composite> plotSystem;
	private IToolPage toolPage;
	
	public GratingCalibrationDialog(Shell shell, DiffractionDataManager manager) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.manager = manager;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout twoColumns = new GridLayout(2, true);
		container.setLayout(twoColumns);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite imageColumn = new Composite(container, SWT.FILL | SWT.BORDER);
		imageColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		imageColumn.setLayout(new GridLayout());
		
		// Action bar for the plot
		ActionBarWrapper aBW = ActionBarWrapper.createActionBars(imageColumn, null);
		// Composite for the plot itself
		Composite plotComp = new Composite(imageColumn, SWT.BORDER);
		plotComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		plotComp.setLayout(new FillLayout());
		// try to plot the image
		try {
			// The plot system
			plotSystem = PlottingFactory.createPlottingSystem();
			plotSystem.createPlotPart(plotComp, "Plot Dialog", aBW, PlotType.IMAGE, null);
			Dataset data = DatasetUtils.sliceAndConvertLazyDataset(manager.getCurrentData().getImage());
			data.setMetadata(manager.getCurrentData().getMetaData());
			plotSystem.createPlot2D(data, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Data table		
		Composite dataColumn = new Composite(container, SWT.FILL | SWT.BORDER);
		dataColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataColumn.setLayout(new GridLayout(1, true));

		IToolPageSystem tps = (IToolPageSystem) plotSystem.getAdapter(IToolPageSystem.class);
		try {
			toolPage = ToolPageFactory.getToolPage("uk.ac.diamond.scisoft.diffraction.powder.rcp.powderDiffractionTool");
			toolPage.setPlottingSystem(plotSystem);
			toolPage.setToolSystem(tps);
			toolPage.setTitle("Powder wizard tool");
			toolPage.setToolId(String.valueOf(toolPage.hashCode()));
			toolPage.createControl(dataColumn);
			toolPage.activate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Add a composite to hold the grating information
		Composite gratingCompo = new Composite(dataColumn, SWT.FILL | SWT.BORDER);
		
		return container;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}
}
