package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.api.roi.IPolylineROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalFitROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.Maths;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.IHKL;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class FromPointsCalibrationRun extends AbstractCalibrationRun {

	private static final String description = "Manual powder diffraction image calibration using point parameters";
	
	public FromPointsCalibrationRun(Display display,
			IPlottingSystem<?> plottingSystem, DiffractionDataManager manager, SimpleCalibrationParameterModel params) {
		super(display, plottingSystem, manager, params);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IProgressMonitor monitor) {
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<IHKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		int n = currentData.getROISize();
		if (n != spacings.size()) { // always allow a choice to be made
			throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
		}
		
		int totalNonNull = currentData.getNonNullROISize();
		
		double[] ds = new double[totalNonNull];
		List<IPolylineROI> erois = new ArrayList<IPolylineROI>(totalNonNull);
		
		int count = 0;
		for (int i = 0; i < currentData.getROISize(); i++) {
			IROI roi = currentData.getRoi(i);
			if (roi != null) {
				ds[count]  = spacings.get(i).getDNano()*10;
				
				if (roi instanceof IPolylineROI) {
					erois.add((IPolylineROI) roi);
				} else if (roi instanceof EllipticalFitROI) {
					erois.add(((EllipticalFitROI) roi).getPoints());
				} else {
					throw new IllegalArgumentException("ROI not elliptical fit");
				}
				count++;
			}
		}
		
		List<List<IPolylineROI>> allEllipses = new ArrayList<List<IPolylineROI>> ();
		allEllipses.add(erois);
		List<double[]> allDSpacings = new ArrayList<double[]>();
		allDSpacings.add(ds);
		
		CalibrationOutput o = CalibratePoints.run(allEllipses.get(0), allDSpacings.get(0), currentData.getMetaData(),params);

		final CalibrationOutput output = o;
		
		double[] fullDSpace = new double[spacings.size()];
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);
		int[] infoIndex = new int[allDSpacings.get(0).length];
		
		for (int j = 0; j < infoIndex.length; j++) {
			infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(0)[j])).argMin();
		}
		Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
		
		PowderCalibrationInfoImpl info = createPowderCalibrationInfo(currentData, false);
		
		info.setPostCalibrationInformation(description, infoSpace, infoSpaceds, output.getResidual());
		info.setResultDescription(output.getCalibrationOutputDescription());
		output.setCalibrationInfo(new IPowderCalibrationInfo[]{info});
		
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
				
				currentData.setCalibrationInfo(output.getCalibrationInfo()[0]);

				PowderCalibrationUtils.clearFoundRings(plottingSystem);
			}
		});
		return;
	}

}
