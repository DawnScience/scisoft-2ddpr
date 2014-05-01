package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IPolylineROI;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePointsParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public class AutoCalibrationRun extends AbstractCalibrationRun {


	public AutoCalibrationRun(Display display, IPlottingSystem plottingSystem,
			DiffractionDataManager manager, DiffractionTableData currentData,
			SimpleCalibrationParameterModel param) {
		super(display, plottingSystem, manager, currentData, param);
	}

	@Override
	public void run(final IProgressMonitor monitor) {
		
		
		if (manager.isEmpty()) return;
		
		AbstractDataset ddist = manager.getDistances();
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		for (DiffractionTableData data : manager.iterable()) {
			
			if (manager.getSize() > 1) plottingSystem.updatePlot2D(data.getImage(), null, monitor);
			
			final AbstractDataset image = (AbstractDataset)data.getImage();
			IDiffractionMetadata meta = data.getMetaData();
			
			final EllipseFindingStructure efs = getResolutionEllipses(image, meta, params, monitor);
			
			if (monitor.isCanceled()) return;
			
			if (efs == null) throw new IllegalArgumentException("No rings found!");
			
			List<ResolutionEllipseROI> foundEllipses = getFittedResolutionROIs(plottingSystem, efs, display, monitor);
			
			if (monitor.isCanceled()) return;
			
			if (foundEllipses == null || foundEllipses.size() < 2) throw new IllegalArgumentException("No rings found!");
			
			double[] dSpaceArray = new double[foundEllipses.size()];
			
			for (int j = 0; j < foundEllipses.size();j++) {
				dSpaceArray[j] = foundEllipses.get(j).getResolution();
			}
			
			allDSpacings.add(dSpaceArray);
			allEllipses.add(new ArrayList<EllipticalROI>(foundEllipses));
			
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					removeFoundRings(plottingSystem);
				}
			});
		}
		
		monitor.beginTask("Calibrating", IProgressMonitor.UNKNOWN);
		//TODO make sure fix wavelength/distance ignored for multiple images
		CalibrationOutput output =  CalibrateEllipses.run(allEllipses, allDSpacings,ddist,currentData.getMetaData(), params);

		if (allEllipses.size() == 1 && params.isFinalGlobalOptimisation()) {
			
			updateMetaData(currentData.getMetaData(), output, 0);
			
			CalibratePointsParameterModel paramModel = new CalibratePointsParameterModel(params);
			
			List<IPolylineROI> lineROIList = new ArrayList<IPolylineROI>();
			
			for (EllipticalROI roi : allEllipses.get(0)) {
				if (roi instanceof ResolutionEllipseROI && ((ResolutionEllipseROI)roi).getPoints() != null) {
					lineROIList.add(((ResolutionEllipseROI)roi).getPoints());
				}
			}
			
			output = CalibratePoints.run(lineROIList, allDSpacings.get(0), currentData.getMetaData(), paramModel);
		}
		
		updateOnFinish(output);

		return;
	}
	
	public void runForRunnable(final IProgressMonitor monitor) {
		run(monitor);
	}

}
