package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePointsParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;

public class FromPointsCalibrationRun extends AbstractCalibrationRun {

	public FromPointsCalibrationRun(Display display,
			IPlottingSystem plottingSystem, DiffractionDataManager manager,
			DiffractionTableData currentData, CalibratePointsParameterModel params) {
		super(display, plottingSystem, manager, currentData, params);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IProgressMonitor monitor) {
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		int n = currentData.getRois().size();
		if (n != spacings.size()) { // always allow a choice to be made
			throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
		}
		
		int totalNonNull = 0;
		
		for (IROI roi : currentData.getRois()) {
			if (roi != null) totalNonNull++;
		}
		
		double[] ds = new double[totalNonNull];
		List<PolylineROI> erois = new ArrayList<PolylineROI>(totalNonNull);
		
		int count = 0;
		for (int i = 0; i < currentData.getRois().size(); i++) {
			IROI roi = currentData.getRois().get(i);
			if (roi != null) {
				ds[count]  = spacings.get(i).getDNano()*10;
				
				if (roi instanceof PolylineROI) {
					erois.add((PolylineROI)roi);
				} else if (roi instanceof EllipticalFitROI) {
					erois.add(((EllipticalFitROI)roi).getPoints());
				} else {
					throw new IllegalArgumentException("ROI not elliptical fit");
				}
				count++;
			}
		}
		
		List<List<PolylineROI>> allEllipses = new ArrayList<List<PolylineROI>> ();
		allEllipses.add(erois);
		List<double[]> allDSpacings = new ArrayList<double[]>();
		allDSpacings.add(ds);
		
		CalibrationOutput o = CalibratePoints.run(allEllipses.get(0), allDSpacings.get(0), currentData.getMetaData(),(CalibratePointsParameterModel)params);

		final CalibrationOutput output = o;
		
		display.syncExec(new Runnable() {
			@Override
			public void run() {
//				
				DetectorProperties dp = currentData.getMetaData().getDetector2DProperties();
				
				currentData.getMetaData().getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
				
				dp.setBeamCentreDistance(output.getDistance().getDouble(0));
				double[] bc = new double[] {output.getBeamCentreX().getDouble(0),output.getBeamCentreY().getDouble(0) };
				dp.setBeamCentreCoords(bc);
				
				dp.setNormalAnglesInDegrees(output.getTilt().getDouble(0)*-1, 0, output.getTiltAngle().getDouble(0)*-1);
				
				currentData.setResidual(output.getResidual());

				removeFoundRings(plottingSystem);
			}
		});
		return;
	}

}
