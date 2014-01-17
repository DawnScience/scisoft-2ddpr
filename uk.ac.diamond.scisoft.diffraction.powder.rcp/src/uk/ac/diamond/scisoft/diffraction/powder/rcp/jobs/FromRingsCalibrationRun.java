package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.roi.CircularROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;

public class FromRingsCalibrationRun extends AbstractCalibrationRun {

	public FromRingsCalibrationRun(Display display,
			IPlottingSystem plottingSystem, List<DiffractionTableData> model,
			DiffractionTableData currentData, int maxRings) {
		super(display, plottingSystem, model, currentData, maxRings);
	}

	@Override
	public void run(IProgressMonitor monitor) {
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();

		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();

		for (DiffractionTableData data : model) {
			
			int n = data.rois.size();
			if (n != spacings.size()) { // always allow a choice to be made
				throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
			}

			int totalNonNull = 0;

			for (IROI roi : data.rois) {
				if (roi != null) totalNonNull++;
			}

			double[] ds = new double[totalNonNull];
			List<EllipticalROI> erois = new ArrayList<EllipticalROI>(totalNonNull);

			int count = 0;
			for (int i = 0; i < data.rois.size(); i++) {
				IROI roi = data.rois.get(i);
				if (roi != null) {
					ds[count]  = spacings.get(i).getDNano()*10;

					if (roi instanceof EllipticalROI) {
						erois.add((EllipticalROI)roi);
					} else if(roi instanceof CircularROI) {
						erois.add(new EllipticalROI((CircularROI)roi));
					} else {
						throw new IllegalArgumentException("ROI not elliptical or circular");
					}

					count++;
				}
			}
			allEllipses.add(erois);
			allDSpacings.add(ds);
		}
		
		double[] deltaDistance = new double[model.size()];
		for (int i = 0; i <model.size(); i++) deltaDistance[i] = model.get(i).distance;

		AbstractDataset ddist = new DoubleDataset(deltaDistance, new int[]{deltaDistance.length});
		
		double pixelSize = currentData.md.getDetector2DProperties().getHPxSize();
		
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

}
