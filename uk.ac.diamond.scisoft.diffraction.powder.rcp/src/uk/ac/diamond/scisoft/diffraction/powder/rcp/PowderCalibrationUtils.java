package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.MediaSize.Other;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.QSpace;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.CircularROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.diffraction.powder.BruteStandardMatcher;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibratePoints;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.CentreGuess;

public class PowderCalibrationUtils {
	
	private static String REGION_PREFIX = "Pixel peaks";
	private static Logger logger = LoggerFactory.getLogger(PowderCalibrationUtils.class);
	
	/**
	 * Create a job to calibrate images 
	 * @param display
	 * @param plottingSystem
	 * @param model
	 * @param currentData
	 * @param useFixedWavelength if true then fit using a fixed global wavelength
	 * @param postFixedWavelengthFit if true and useFixedWavelength true then fit wavelength afterwards
	 * @return job that needs to be scheduled
	 */
	public static Job calibrateImages(final Display display,
									   final IPlottingSystem plottingSystem,
									   final List<DiffractionTableData> model,
									   final DiffractionTableData currentData,
									   final boolean useFixedWavelength,
									   final boolean postFixedWavelengthFit) {
		Job job = new Job("Calibrate detector") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				IStatus stat = Status.OK_STATUS;
				final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
				monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
				List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
				List<List<? extends IROI>> lROIs = new ArrayList<List<? extends IROI>>();
				List<DetectorProperties> dps = new ArrayList<DetectorProperties>();
				DiffractionCrystalEnvironment env = null;
				for (DiffractionTableData data : model) {
					IDiffractionMetadata md = data.md;
					if (!data.use || data.nrois <= 0 || md == null) {
						continue;
					}
					if (env == null) {
						env = md.getDiffractionCrystalEnvironment();
					}
					data.q = null;

					DetectorProperties dp = md.getDetector2DProperties();
					if (dp == null) {
						continue;
					}
					dps.add(dp);
					lROIs.add(data.rois);
				}
				List<QSpace> qs = null;
				if (useFixedWavelength) {
					monitor.subTask("Fitting all rings");
					try {
						qs = PowderRingsUtils.fitAllEllipsesToAllQSpacesAtFixedWavelength(mon, dps, env, lROIs, spacings, postFixedWavelengthFit);
					} catch (IllegalArgumentException e) {
						logger.debug("Problem in calibrating all image: {}", e);
					}
				} else {
					try {
						qs = PowderRingsUtils.fitAllEllipsesToAllQSpaces(mon, dps, env, lROIs, spacings);
					} catch (IllegalArgumentException e) {
						logger.debug("Problem in calibrating all image: {}", e);
					}
				}

				int i = 0;
				for (DiffractionTableData data : model) {
					IDiffractionMetadata md = data.md;
					if (!data.use || data.nrois <= 0 || md == null) {
						continue;
					}

					DetectorProperties dp = md.getDetector2DProperties();
					if (dp == null) {
						continue;
					}
					data.q = qs.get(i++);
					logger.debug("Q-space = {}", data.q);
				}

				display.syncExec(new Runnable() {
					@Override
					public void run() {
						for (DiffractionTableData data : model) {
							IDiffractionMetadata md = data.md;
							if (data.q == null || !data.use || data.nrois <= 0 || md == null) {
								continue;
							}
							DetectorProperties dp = md.getDetector2DProperties();
							DiffractionCrystalEnvironment ce = md.getDiffractionCrystalEnvironment();
							if (dp == null || ce == null) {
								continue;
							}

							DetectorProperties fp = data.q.getDetectorProperties();
							dp.setGeometry(fp);
							ce.setWavelength(data.q.getWavelength());
						}

						if (currentData == null || currentData.md == null || currentData.q == null)
							return;

						hideFoundRings(plottingSystem);
						drawCalibrantRings(currentData.augmenter);
					}
				});
				return stat;
			}
		};
		job.setPriority(Job.SHORT);
		return job;
	}
	
	/**
	 * 
	 * @param plottingSystem
	 */
	public static void hideFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				r.setVisible(false);
			}
		}
	}
	
	/**
	 * 
	 * @param currentData
	 */
	public static void drawCalibrantRings(DiffractionImageAugmenter aug) {

		if (aug == null)
			return;

		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		aug.drawCalibrantRings(true, standards.getCalibrant());
		aug.drawBeamCentre(true);
	}
	

	public static Job calibrateImagesPointFittingMethod(final Display display,
			   final IPlottingSystem plottingSystem,
			   final DiffractionTableData currentData,
			   final boolean fixedWavelength) {
		
		return new Job("Calibrate detector - Major Axis") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
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
				
				double pixelSize = currentData.md.getDetector2DProperties().getHPxSize();
				double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
				
				
				CalibrationOutput o = CalibratePoints.runKnownWavelength(allEllipses.get(0), allDSpacings.get(0), currentData.md.getDetector2DProperties(),
						currentData.md.getDiffractionCrystalEnvironment().getWavelength());

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
		};
		
	}
}
