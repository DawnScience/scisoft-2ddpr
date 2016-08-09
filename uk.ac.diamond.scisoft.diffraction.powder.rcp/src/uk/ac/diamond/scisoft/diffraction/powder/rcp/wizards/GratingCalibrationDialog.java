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
import org.eclipse.january.metadata.MaskMetadata;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import uk.ac.diamond.scisoft.diffraction.powder.GratingCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class GratingCalibrationDialog extends Dialog {

	private DiffractionDataManager manager;
	private IPlottingSystem<Composite> plotSystem;
	private IToolPage toolPage;

	private Text rulingText;
	private Label spacingText;
	private Label angleText;
	private Button calibrateButton;
	private Action calibrateAction;
	
	static final double hc = 12.398419738620932; // keV Å 

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
			toolPage.setTitle("Grating Calibration Dialog");
			toolPage.setToolId(String.valueOf(toolPage.hashCode()));
			toolPage.createControl(dataColumn);
			toolPage.activate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Add a composite to hold the grating information
		Composite gratingCompo = new Composite(dataColumn, SWT.FILL | SWT.BORDER);
		gratingCompo.setLayout(new GridLayout(4, true));
		gratingCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label rulingLabel = new Label(gratingCompo, SWT.BORDER);
		rulingLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		rulingLabel.setText("Grating ruling spacing");
		rulingText = new Text(gratingCompo, SWT.BORDER);
		rulingText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		rulingText.setText("100");
		Label rulingUnitLabel = new Label(gratingCompo, SWT.BORDER);
		rulingUnitLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		rulingUnitLabel.setText("nm");
		Label spacingLabel = new Label(gratingCompo, SWT.BORDER);
		spacingLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		spacingLabel.setText("Fringe spacing");
		spacingText = new Label(gratingCompo, SWT.BORDER);
		spacingText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		spacingText.setText("");
		Label spacingUnitLabel = new Label(gratingCompo, SWT.BORDER);
		spacingUnitLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		spacingUnitLabel.setText("px");
		Label angleLabel = new Label(gratingCompo, SWT.BORDER);
		angleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		angleLabel.setText("Grating angle");
		angleText = new Label(gratingCompo, SWT.BORDER);
		angleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		angleText.setText("");
		Label angleUnitLabel = new Label(gratingCompo, SWT.BORDER);
		angleUnitLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		angleUnitLabel.setText("°");
		
		createActions();
		
		return container;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(1000, 700);
	}
	
	@Override
	public boolean close() {
		toolPage.deactivate();
		return super.close();
	}
	
	// TODO: fix the button spacing
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(new GridLayout(3, true));
		calibrateButton = new Button(parent, SWT.NONE);
		calibrateButton.setText("Calibrate");
		calibrateButton.setLayoutData(new GridData(SWT.FILL));
		calibrateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				calibrateAction.run();
			}
		});
		
		super.createButtonsForButtonBar(parent);
	}

	private void createActions() {
		calibrateAction = new Action("Calibrate Grating") {
			@Override
			public void run() {
				GratingCalibration gc = new GratingCalibration();
				gc.setBeamCentre(manager.getCurrentData().getMetaData().getDetector2DProperties().getBeamCentreCoords());
				gc.setEnergy(hc/manager.getCurrentData().getMetaData().getDiffractionCrystalEnvironment().getWavelength());
				gc.setGratingspacing(Double.parseDouble(rulingText.getText()));
				gc.setPixelPitch(manager.getCurrentData().getMetaData().getDetector2DProperties().getHPxSize()); // Assume square pixels for now
				
				Dataset data, mask;
				data = DatasetUtils.convertToDataset(manager.getCurrentData().getImage());
				MaskMetadata maskMD = data.getFirstMetadata(MaskMetadata.class);
				mask = (maskMD != null) ? DatasetUtils.convertToDataset(maskMD.getMask()) : null;
				double distance = gc.getDetectorDistance(data, mask);
				
				// Set the data that is stored in the diffraction metadata
				manager.getCurrentData().getMetaData().getDetector2DProperties().setBeamCentreDistance(distance);
				manager.getCurrentData().getMetaData().getDetector2DProperties().setBeamCentreCoords(gc.getBeamCentre());
				
				// Set the diagnostic data in the dialog
				spacingText.setText(Double.toString(gc.getFringeSpacing()));
				angleText.setText(Double.toString(gc.getPatternAngle()));
				
			}
		};
	}
}
