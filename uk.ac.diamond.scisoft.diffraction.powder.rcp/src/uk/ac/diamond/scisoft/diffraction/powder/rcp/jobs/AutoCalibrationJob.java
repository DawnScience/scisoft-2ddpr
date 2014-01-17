package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;

import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;

public class AutoCalibrationJob extends AbstractCalibrationJob {


	public AutoCalibrationJob(Display display, IPlottingSystem plottingSystem,
			List<DiffractionTableData> model, DiffractionTableData currentData,
			int maxRings) {
		super(display, plottingSystem, model, currentData, maxRings);
	}

	@Override
	public void run(final IProgressMonitor monitor) {
		
		double[] deltaDistance = new double[model.size()];
		
		for (int i = 0; i <model.size(); i++) deltaDistance[i] = model.get(i).distance;
		
		AbstractDataset ddist = new DoubleDataset(deltaDistance, new int[]{deltaDistance.length});
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		for (DiffractionTableData data : model) {
			
			if (model.size() > 1) plottingSystem.updatePlot2D(data.image, null, monitor);
			
			final AbstractDataset image = (AbstractDataset)data.image;
			IDiffractionMetadata meta = data.md;
			
			final EllipseFindingStructure efs = getResolutionEllipses(image, meta, maxRings, monitor);
			
			if (monitor.isCanceled()) return;
			
			if (efs == null) return;
			
			List<ResolutionEllipseROI> foundEllipses = getFittedResolutionROIs(plottingSystem, efs, display, monitor);
			
			if (monitor.isCanceled() || foundEllipses == null);
			
			
			double[] dSpaceArray = new double[foundEllipses.size()];
			
			for (int j = 0; j < foundEllipses.size();j++) {
				dSpaceArray[j] = foundEllipses.get(j).getResolution();
			}
			
			allDSpacings.add(dSpaceArray);
			allEllipses.add(new ArrayList<EllipticalROI>(foundEllipses));
			
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					hideFoundRings(plottingSystem);
				}
			});
		}
		
		double pixelSize = model.get(0).md.getDetector2DProperties().getHPxSize();
		
		monitor.beginTask("Calibrating", IProgressMonitor.UNKNOWN);
		
		CalibrationOutput o = null;
		
		if (fixedWavelength) {
			o =  CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize,currentData.md.getDiffractionCrystalEnvironment().getWavelength());
		} else {
			o =  CalibrateEllipses.run(allEllipses, allDSpacings,ddist, pixelSize);
		}
		
		final CalibrationOutput output = o;
		
		updateOnFinish(output);

		return;
	}
	
	public void runForRunnable(final IProgressMonitor monitor) {
		run(monitor);
	}

}
