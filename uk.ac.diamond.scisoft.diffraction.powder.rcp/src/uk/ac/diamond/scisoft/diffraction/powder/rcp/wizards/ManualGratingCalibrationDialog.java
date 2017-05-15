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
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.api.roi.IRectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.tool.ToolPageFactory;
import org.eclipse.dawnsci.plotting.api.trace.MetadataPlotUtils;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.metadata.MaskMetadata;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.diffraction.powder.ManualGratingCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class ManualGratingCalibrationDialog extends Dialog {

	// Let's set up a logger first
	private static final Logger logger = LoggerFactory.getLogger(ManualGratingCalibrationDialog.class);

	
	private DiffractionDataManager manager;
	private IPlottingSystem<Composite> imagePlotSystem;
	private IPlottingSystem<Composite> graphPlotSystem;
	private IToolPage toolPage;

	private Text rulingText;
	private Label spacingText;
	private Label angleText;
	private Button calibrateButton;
	private Button refineBeamCentreCheckbox;
	private Action calibrateAction;
	
	private IRegion selectionRegion;
	private SectorROI regionOfInterest;
	private Dataset diffractionImage;
	private Dataset imageMask;
	
	static final double hc = 12.398419738620932; // keV Å 

	public ManualGratingCalibrationDialog(Shell shell, DiffractionDataManager manager) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.manager = manager;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		// Set up an overall container for the dialog box complete with layout information
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Complete with subhomes for the graphs
		SashForm graphSash = new SashForm(container, SWT.VERTICAL);
		graphSash.setLayout(new GridLayout(4, false));
		graphSash.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				
		// And the calibration data
		SashForm dataSash = new SashForm(container, SWT.VERTICAL);
		dataSash.setLayout(new GridLayout(2, false));
		dataSash.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Now let's obtain some information from the manager before the try/catch
		this.diffractionImage = (Dataset) manager.getCurrentData().getImage();
		this.diffractionImage.setMetadata(manager.getCurrentData().getMetaData());
		createMask();
				
		double[] beamCenter = manager.getCurrentData().getMetaData().getDetector2DProperties().getBeamCentreCoords();

		
		// Then we'll try to create the plots
		try {
			// Starting with the diffraction image
			imagePlotSystem = PlottingFactory.createPlottingSystem();
			ActionBarWrapper imageActionBar = ActionBarWrapper.createActionBars(graphSash, null);
			imagePlotSystem.createPlotPart(graphSash, "Diffraction image", imageActionBar, PlotType.IMAGE, null);
			imagePlotSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			// And then the lineplot from this
			graphPlotSystem = PlottingFactory.createPlottingSystem();
			ActionBarWrapper graphActionBar = ActionBarWrapper.createActionBars(graphSash, null);
			graphPlotSystem.createPlotPart(graphSash, "Lineplot from diffraction image", graphActionBar, PlotType.XY, null);
			graphPlotSystem.getPlotComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			// Now we'll set the image plot to the correct image
			imagePlotSystem.createPlot2D(this.diffractionImage, null, null);
			imagePlotSystem.setTitle("Diffraction image");

			// Now create a linear selection region
			selectionRegion = imagePlotSystem.createRegion("Sector", RegionType.SECTOR);
			// Now we'll create a new listener for this plot region
			selectionRegion.addROIListener(new IROIListener.Stub() {
				@Override
				public void roiChanged(ROIEvent userEvent) {
					// Get the event and typecast it
					SectorROI sectorROI = (SectorROI) userEvent.getROI().copy();
					// Set up the external ROI
					regionOfInterest = sectorROI;
					// Integrate the ROI
					Dataset[] integratedRegion = ROIProfile.sector(diffractionImage, imageMask, sectorROI);
					// Push the integrated region out of the stub
					Dataset plottableRegion = integratedRegion[0];
					
					// Clear the plot implicitly first (inside the stub)
					graphPlotSystem.clear();
					// Then plot the data for the user to see
					MetadataPlotUtils.plotDataWithMetadata(plottableRegion, graphPlotSystem);
					graphPlotSystem.setTitle("Lineplot from selected sector");
				}
			});
			
			// And set up a line on it, from which the graph plot shall be populated
			IROI roi = new SectorROI(beamCenter[0], beamCenter[1], 0, 500, 1.55, 1.59);
			// And add this to the plot
			selectionRegion.setROI(roi);
			imagePlotSystem.addRegion(selectionRegion);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Then we'll set up some information for the page itself
		IToolPageSystem tps = (IToolPageSystem) imagePlotSystem.getAdapter(IToolPageSystem.class);
		try {
			toolPage = ToolPageFactory.getToolPage("uk.ac.diamond.scisoft.diffraction.powder.rcp.powderDiffractionTool");
			toolPage.setPlottingSystem(imagePlotSystem);
			toolPage.setToolSystem(tps);
			toolPage.setTitle("Manual Grating Calibration Dialog");
			toolPage.setToolId(String.valueOf(toolPage.hashCode()));
			toolPage.createControl(dataSash);
			toolPage.activate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Add a composite to hold the grating information
		Composite gratingCompo = new Composite(dataSash, SWT.FILL | SWT.BORDER);
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
		
		// Set the default GUI weightings
		container.setWeights(new int[]{69, 31});
		graphSash.setWeights(new int[]{5, 65, 5, 25});
		dataSash.setWeights(new int[]{80, 20});
		
		return container;
	}
	
	
	@Override
	protected Point getInitialSize() {
		return new Point(1500, 1000);
	}
	
	
	@Override
	public boolean close() {
		toolPage.deactivate();
		return super.close();
	}
	
	
	// TODO: fix the button spacing
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(new GridLayout(5, false));
		refineBeamCentreCheckbox = new Button(parent, SWT.CHECK);
		refineBeamCentreCheckbox.setSelection(true);
		refineBeamCentreCheckbox.setText("Refine Beam Centre?");
		refineBeamCentreCheckbox.setLayoutData(new GridData(SWT.FILL));	
		calibrateButton = new Button(parent, SWT.NONE);
		calibrateButton.setText("Calibrate ");
		calibrateButton.setLayoutData(new GridData(SWT.FILL));
		calibrateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				calibrateAction.run();
			}
		});
				
		super.createButtonsForButtonBar(parent);
	}

	private void createMask() {
		// This method is to create a mask for the internally held diffraction image dataset
		
		// First we shall see if there is a mask already held by the image
		MaskMetadata maskMetadata = this.diffractionImage.getFirstMetadata(MaskMetadata.class);
		
		// However, as this is a calibration we shall assume that there might well not be one and think ahead...
		if (maskMetadata != null) {
			this.imageMask = (Dataset) maskMetadata.getMask();
		}
		else {
			// Creating our own image mask where necessary
			this.imageMask = DatasetFactory.zeros(this.diffractionImage, Dataset.INT8);
			
			// On the basis that if not even one photon of radiation doesn't hit the detector we should ignore that pixel 
			for (int loopIterX = 0; loopIterX < this.diffractionImage.getShape()[0]; loopIterX ++) {
				for (int loopIterY = 0; loopIterY < this.diffractionImage.getShape()[1]; loopIterY ++) {
					if (this.diffractionImage.getDouble(loopIterX, loopIterY) < 1) {
						this.imageMask.set(0, loopIterX, loopIterY);
					}
					else {
						this.imageMask.set(1, loopIterX, loopIterY);
					}
				}
			}
		}
	}
	
	
	private void createActions() {
		calibrateAction = new Action("Calibrate Grating") {
			@Override
			public void run() {
				ManualGratingCalibration gc = new ManualGratingCalibration();
				gc.setBeamCentre(manager.getCurrentData().getMetaData().getDetector2DProperties().getBeamCentreCoords());
				gc.setEnergy(hc/manager.getCurrentData().getMetaData().getDiffractionCrystalEnvironment().getWavelength());
				gc.setGratingspacing(Double.parseDouble(rulingText.getText()));
				gc.setPixelPitch(manager.getCurrentData().getMetaData().getDetector2DProperties().getHPxSize()); // Assume square pixels for now
				
				boolean beamCentreRefinement = refineBeamCentreCheckbox.getSelection();
				
				// Now get the detector distance using this mask
				double distance = gc.getDetectorDistance(diffractionImage, imageMask, regionOfInterest, beamCentreRefinement);

				// And set up a line on it, from which the graph plot shall be populated
				IROI roi = gc.getSectorROI();
				// And add this to the plot
				selectionRegion.setROI(roi);

				
				// Set the data that is stored in the diffraction metadata
				manager.getCurrentData().getMetaData().getDetector2DProperties().setBeamCentreDistance(distance);
				manager.getCurrentData().getMetaData().getDetector2DProperties().setBeamCentreCoords(gc.getBeamCentre());
				
				// Set the diagnostic data in the dialog
				spacingText.setText(Double.toString(gc.getFringeSpacing()));
				angleText.setText(Double.toString(gc.getPatternAngle()));
				toolPage.activate();
				
				
			}
		};
	}
}
