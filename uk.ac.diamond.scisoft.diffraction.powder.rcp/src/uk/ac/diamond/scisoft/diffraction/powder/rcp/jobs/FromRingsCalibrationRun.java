package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.List;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class FromRingsCalibrationRun extends AbstractCalibrationRun {

	private static final String description = "Manual powder diffraction image calibration using ellipse parameters";
	
	public FromRingsCalibrationRun(Display display,
			IPlottingSystem<?> plottingSystem,DiffractionDataManager manager, SimpleCalibrationParameterModel params, ICalibrationUIProgressUpdate uiUpdater) {
		super(manager, params, uiUpdater);
	}

	@Override
	public void run(IProgressMonitor monitor) {
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();

		
		
//		
//		
//		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
//		List<double[]> allDSpacings = new ArrayList<double[]>();

//		for (DiffractionImageData data : manager.iterable()) {
//			
//			int n = data.getROISize();
//			if (n != spacings.size()) { // always allow a choice to be made
//				throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
//			}
//
//			int totalNonNull = data.getNonNullROISize();
//
//			double[] ds = new double[totalNonNull];
//			List<EllipticalROI> erois = new ArrayList<EllipticalROI>(totalNonNull);
//
//			int count = 0;
//			for (int i = 0; i < data.getROISize(); i++) {
//				IROI roi = data.getRoi(i);
//				if (roi != null) {
//					ds[count]  = spacings.get(i).getDNano()*10;
//
//					if (roi instanceof EllipticalROI) {
//						erois.add((EllipticalROI)roi);
//					} else if(roi instanceof CircularROI) {
//						erois.add(new EllipticalROI((CircularROI)roi));
//					} else {
//						throw new IllegalArgumentException("ROI not elliptical or circular - try point calibration");
//					}
//
//					count++;
//				}
//			}
//			allEllipses.add(erois);
//			allDSpacings.add(ds);
//		}
		

		Dataset ddist = manager.getDistances();
		
		double pixelSize = currentData.getMetaData().getDetector2DProperties().getHPxSize();
		
//		CalibrationOutput o = null;
//		
//		if (!params.isFloatEnergy()) {
//			o =  CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize,currentData.getMetaData().getDiffractionCrystalEnvironment().getWavelength());
//		} else if (!params.isFloatDistance()){
//			o =  CalibrateEllipses.runKnownDistance(allEllipses, allDSpacings, pixelSize,currentData.getMetaData().getDetector2DProperties().getBeamCentreDistance());
//		}else {
//			o =  CalibrateEllipses.run(allEllipses, allDSpacings,ddist, pixelSize);
//		}
//		
//		final CalibrationOutput output = o;
//		
//		double[] fullDSpace = new double[spacings.size()];
//		
//		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
//		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);
//
//		PowderCalibrationInfoImpl[] info = new PowderCalibrationInfoImpl[manager.getSize()];
//		
//		int count = 0;
//		for (DiffractionImageData data : manager.iterable()) {
//
//			info[count++] = createPowderCalibrationInfo(data,true);
//		}
//		
//		for (int i = 0; i < allEllipses.size(); i++) {
//			int[] infoIndex = new int[allDSpacings.get(i).length];
//			
//			for (int j = 0; j < infoIndex.length; j++) {
//				infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(0)[j])).argMin();
//			}
//			Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
//			
//			info[i].setPostCalibrationInformation(description, infoSpace, infoSpaceds, output.getResidual());
//			info[i].setResultDescription(output.getCalibrationOutputDescription());
//			
//		}
//		output.setCalibrationInfo(info);
//		
//		updateOnFinish(output);
		
		
		PowderCalibrationInfoImpl[] info = new PowderCalibrationInfoImpl[manager.getSize()];
		
		CalibrationOutput output = PowderCalibration.manualCalibrateMultipleImagesEllipse(manager.getDataList(), ddist, pixelSize, spacings, params);
		
		updateOnFinish(output);
		
		return;
	}

}
