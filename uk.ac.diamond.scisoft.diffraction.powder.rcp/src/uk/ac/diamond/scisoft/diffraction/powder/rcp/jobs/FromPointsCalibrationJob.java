package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;

public class FromPointsCalibrationJob extends AbstractCalibrationJob {

	public FromPointsCalibrationJob(Display display,
			IPlottingSystem plottingSystem, List<DiffractionTableData> model,
			DiffractionTableData currentData, int maxRings) {
		super(display, plottingSystem, model, currentData, maxRings);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IStatus stat = Status.OK_STATUS;
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		int n = currentData.rois.size();
		if (n != spacings.size()) { // always allow a choice to be made
			throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
		}
		
		int totalNonNull = 0;
		
		for (IROI roi : currentData.rois) {
			if (roi != null) totalNonNull++;
		}
		
		double[] ds = new double[totalNonNull];
		List<EllipticalFitROI> erois = new ArrayList<EllipticalFitROI>(totalNonNull);
		
		int count = 0;
		for (int i = 0; i < currentData.rois.size(); i++) {
			IROI roi = currentData.rois.get(i);
			if (roi != null) {
				ds[count]  = spacings.get(i).getDNano()*10;
				
				if (roi instanceof EllipticalFitROI) {
					erois.add((EllipticalFitROI)roi);
				} else {
					throw new IllegalArgumentException("ROI not elliptical fit");
				}
				
				count++;
			}
		}
		
		List<List<EllipticalFitROI>> allEllipses = new ArrayList<List<EllipticalFitROI>> ();
		allEllipses.add(erois);
		List<double[]> allDSpacings = new ArrayList<double[]>();
		allDSpacings.add(ds);
		
		double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
		
		
		CalibrationOutput o = CalibratePoints.runKnownWavelength(allEllipses.get(0), allDSpacings.get(0), currentData.md.getDetector2DProperties(),
				wavelength);

		final CalibrationOutput output = o;
		
		display.syncExec(new Runnable() {
			@Override
			public void run() {
//				
				DetectorProperties dp = currentData.md.getDetector2DProperties();
				
				dp.setBeamCentreDistance(output.getDistance().getDouble(0));
				double[] bc = new double[] {output.getBeamCentreX().getDouble(0),output.getBeamCentreY().getDouble(0) };
				dp.setBeamCentreCoords(bc);
				
				dp.setNormalAnglesInDegrees(output.getTilt().getDouble(0)*-1, 0, output.getTiltAngle().getDouble(0)*-1);
				
				currentData.residual = output.getResidual();

				hideFoundRings(plottingSystem);
				drawCalibrantRings(currentData.augmenter);
			}
		});
		return stat;
	}

}
