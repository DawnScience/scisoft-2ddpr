package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

public class PowderCalibrationUtils {
	
//	/**
//	 * Create a job to calibrate images 
//	 * @param display
//	 * @param plottingSystem
//	 * @param model
//	 * @param currentData
//	 * @param useFixedWavelength if true then fit using a fixed global wavelength
//	 * @param postFixedWavelengthFit if true and useFixedWavelength true then fit wavelength afterwards
//	 * @return job that needs to be scheduled
//	 */
//	public static Job calibrateImages(final Display display,
//									   final IPlottingSystem plottingSystem,
//									   final List<DiffractionTableData> model,
//									   final DiffractionTableData currentData,
//									   final boolean useFixedWavelength,
//									   final boolean postFixedWavelengthFit) {
//		Job job = new Job("Calibrate detector") {
//			@Override
//			protected IStatus run(final IProgressMonitor monitor) {
//				IStatus stat = Status.OK_STATUS;
//				final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
//				monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
//				List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
//				List<List<? extends IROI>> lROIs = new ArrayList<List<? extends IROI>>();
//				List<DetectorProperties> dps = new ArrayList<DetectorProperties>();
//				DiffractionCrystalEnvironment env = null;
//				for (DiffractionTableData data : model) {
//					IDiffractionMetadata md = data.md;
//					if (!data.use || data.nrois <= 0 || md == null) {
//						continue;
//					}
//					if (env == null) {
//						env = md.getDiffractionCrystalEnvironment();
//					}
//					data.q = null;
//
//					DetectorProperties dp = md.getDetector2DProperties();
//					if (dp == null) {
//						continue;
//					}
//					dps.add(dp);
//					lROIs.add(data.rois);
//				}
//				List<QSpace> qs = null;
//				if (useFixedWavelength) {
//					monitor.subTask("Fitting all rings");
//					try {
//						qs = PowderRingsUtils.fitAllEllipsesToAllQSpacesAtFixedWavelength(mon, dps, env, lROIs, spacings, postFixedWavelengthFit);
//					} catch (IllegalArgumentException e) {
//						logger.debug("Problem in calibrating all image: {}", e);
//					}
//				} else {
//					try {
//						qs = PowderRingsUtils.fitAllEllipsesToAllQSpaces(mon, dps, env, lROIs, spacings);
//					} catch (IllegalArgumentException e) {
//						logger.debug("Problem in calibrating all image: {}", e);
//					}
//				}
//
//				int i = 0;
//				for (DiffractionTableData data : model) {
//					IDiffractionMetadata md = data.md;
//					if (!data.use || data.nrois <= 0 || md == null) {
//						continue;
//					}
//
//					DetectorProperties dp = md.getDetector2DProperties();
//					if (dp == null) {
//						continue;
//					}
//					data.q = qs.get(i++);
//					logger.debug("Q-space = {}", data.q);
//				}
//
//				display.syncExec(new Runnable() {
//					@Override
//					public void run() {
//						for (DiffractionTableData data : model) {
//							IDiffractionMetadata md = data.md;
//							if (data.q == null || !data.use || data.nrois <= 0 || md == null) {
//								continue;
//							}
//							DetectorProperties dp = md.getDetector2DProperties();
//							DiffractionCrystalEnvironment ce = md.getDiffractionCrystalEnvironment();
//							if (dp == null || ce == null) {
//								continue;
//							}
//
//							DetectorProperties fp = data.q.getDetectorProperties();
//							dp.setGeometry(fp);
//							ce.setWavelength(data.q.getWavelength());
//						}
//
//						if (currentData == null || currentData.md == null || currentData.q == null)
//							return;
//
//						hideFoundRings(plottingSystem);
//						//drawCalibrantRings(currentData.augmenter);
//					}
//				});
//				return stat;
//			}
//		};
//		job.setPriority(Job.SHORT);
//		return job;
//	}
	
	public static List<IROI> getResolutionRings(IDiffractionMetadata metadata) {
		
		CalibrantSpacing cs = CalibrationFactory.getCalibrationStandards().getCalibrant();
		
		List<IROI> rois = new ArrayList<IROI>(cs.getHKLs().size());
		
		for (HKL hkl : cs.getHKLs()) {
			
			DetectorProperties detprop = metadata.getDetector2DProperties();
			DiffractionCrystalEnvironment diffenv = metadata.getDiffractionCrystalEnvironment();
			rois.add(DSpacing.conicFromDSpacing(detprop, diffenv, Double.valueOf(hkl.getD().doubleValue(NonSI.ANGSTROM))));
		}
		
		return rois;
	}
	
}
