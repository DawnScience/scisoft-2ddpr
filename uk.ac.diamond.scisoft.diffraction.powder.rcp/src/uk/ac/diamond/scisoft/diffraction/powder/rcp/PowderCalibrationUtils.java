package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.dawb.workbench.ui.views.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.views.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionTool;
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
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.CircularROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;
import uk.ac.diamond.scisoft.diffraction.powder.BruteStandardMatcher;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
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
	
	
	public static Job calibrateImagesMajorAxisMethod(final Display display,
			   final IPlottingSystem plottingSystem,
			   final DiffractionTableData currentData) {
		
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
				List<EllipticalROI> erois = new ArrayList<EllipticalROI>(totalNonNull);
				
				int count = 0;
				for (int i = 0; i < currentData.rois.size(); i++) {
					IROI roi = currentData.rois.get(i);
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
				
				List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>> ();
				allEllipses.add(erois);
				List<double[]> allDSpacings = new ArrayList<double[]>();
				allDSpacings.add(ds);
				
				double pixelSize = currentData.md.getDetector2DProperties().getHPxSize();
				double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
				
				final CalibrationOutput output = CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize, wavelength);
				
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						DetectorProperties dp = currentData.md.getDetector2DProperties();
						
						dp.setDetectorDistance(output.getDistance().getDouble(0));
						double[] bc = new double[] {output.getBeamCentreX().getDouble(0),output.getBeamCentreY().getDouble(0) };
						dp.setBeamCentreCoords(bc);
						
						dp.setNormalAnglesInDegrees(output.getTilt().getDouble(0)*-1, 0, output.getTiltAngle().getDouble(0)*-1);

						hideFoundRings(plottingSystem);
						drawCalibrantRings(currentData.augmenter);
					}
				});
				return stat;
			}
		};
		
	}
	
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
	public static Job calibrateMultipleImages(final Display display,
			final IPlottingSystem plottingSystem,
			final List<DiffractionTableData> model,
			final DiffractionTableData currentData,
			final double deltaD) {

		Job job = new Job("Calibrate detector") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				IStatus stat = Status.OK_STATUS;
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

				AbstractDataset delta = AbstractDataset.arange(allEllipses.size(), AbstractDataset.FLOAT64);
				delta.imultiply(deltaD);

				double pixelSize = currentData.md.getDetector2DProperties().getHPxSize();

				final CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings,delta,pixelSize);
				
				display.syncExec(new Runnable() {
					@Override
					public void run() {

						int i = 0;

						for (DiffractionTableData data : model) {
							DetectorProperties dp = data.md.getDetector2DProperties();

							dp.setDetectorDistance(output.getDistance().getDouble(i));
							double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
							dp.setBeamCentreCoords(bc);

							dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
							data.md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
							i++;
						}

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
	
	private static final int MAX_RINGS = 10;
	private static final int CENTRE_MASK_RADIUS = 50;
	
	public static Job autoFindEllipses(final Display display,
			final IPlottingSystem plottingSystem,
			final DiffractionTableData currentData) {

		Job job = new Job("Calibrate detector") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				
				final AbstractDataset image = (AbstractDataset)currentData.image;
				IDiffractionMetadata meta = currentData.md;
				int[] shape = image.getShape();
				monitor.beginTask("Finding approximate centre...", IProgressMonitor.UNKNOWN);
				double[] approxCentre = CentreGuess.guessCentre(image);
				
				double[] farCorner = new double[]{0,0};
				if (approxCentre[0] < shape[0]/2.0) farCorner[0] = shape[0];
				if (approxCentre[1] < shape[1]/2.0) farCorner[1] = shape[1];
				double maxDistance = Math.sqrt(Math.pow(approxCentre[0]-farCorner[0],2)+Math.pow(approxCentre[1]-farCorner[1],2));
				SectorROI sector = new SectorROI(approxCentre[0], approxCentre[1], 0, maxDistance, 0, 2*Math.PI);
				monitor.beginTask("Integrating image...", IProgressMonitor.UNKNOWN);
				AbstractDataset[] profile = ROIProfile.sector(image, null, sector, true, false, false, null, XAxis.PIXEL, false);
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				final AbstractDataset y = profile[0];
				
				for (int i = 0 ; i < CENTRE_MASK_RADIUS ; i++) {
					y.set(0, i);
				}
				
				final AbstractDataset x = AbstractDataset.arange(y.getSize(), AbstractDataset.INT32);
				
				List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
				
				int max = spacings.size() > MAX_RINGS ? MAX_RINGS : spacings.size();
				
				double[] dSpace = new double[max];
				
				for (int i = 0; i < max; i++) dSpace[i] = spacings.get(i).getDNano()*10;
				monitor.beginTask("Matching to standard...", IProgressMonitor.UNKNOWN);
				final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, dSpace, meta.getDetector2DProperties().getHPxSize());
				
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				
				final List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();
				
				double[] inner = new double[dSpace.length];
				double[] outer = new double[dSpace.length];
				
				for (int i = 0; i < dSpace.length; i++) {
					//TODO if dSpace == 1;
					double dVal = dSpaceRadiusMap.get(dSpace[i]);
					ellipses.add(new EllipticalROI(dVal, approxCentre[0],approxCentre[1]));
					inner.toString();
					if (i == 0) {
						double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
						inner[i] = out > 50 ? 50 : out;
						outer[i] = out > 50 ? 50 : out;
						
					} else if (i == dSpace.length -1) {
						double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
						inner[i] = in > 50 ? 50 : in;
						outer[i] = in > 50 ? 50 : in;
					} else {
						double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
						double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
						inner[i] = in > 50 ? 50 : in;;
						outer[i] = out > 50 ? 50 : out;;
					}
					
				}
				monitor.beginTask("Finding ellipses", IProgressMonitor.UNKNOWN);
				final List<EllipticalROI> foundEllipses = new ArrayList<EllipticalROI>();
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				IROI roi = null;
				double corFact = 0;
				double lastAspect = 1;
				double lastAngle = 0;
				List<Double> dList = new ArrayList<Double>();
				int i = 0;
				for (EllipticalROI e : ellipses) {
					
					double startSemi = e.getSemiAxis(0);
					e.setSemiAxis(0, startSemi+corFact);
					e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
					e.setAngle(lastAngle);
					//e.set
					
					IImageTrace t = DiffractionCalibrationUtils.getImageTrace(plottingSystem);
					try {
						roi = DiffractionTool.runEllipsePeakFit(monitor, display, plottingSystem, t, e, inner[i], outer[i]);
					} catch (Exception ex) {
						logger.debug(ex.getMessage());
						roi = null;
					} 
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (roi != null) {
						foundEllipses.add((EllipticalROI)roi);
						dList.add(dSpace[i]);
						corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
						lastAspect = ((EllipticalROI) roi).getAspectRatio();
						lastAngle = ((EllipticalROI) roi).getAngle();
						DiffractionCalibrationUtils.drawFoundRing(monitor, display, plottingSystem, roi, false);
					}
					i++;
				}
				
				List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
				allEllipses.add(foundEllipses);
				
				double pixelSize = currentData.md.getDetector2DProperties().getHPxSize();
				double wavelength = currentData.md.getDiffractionCrystalEnvironment().getWavelength();
				
				List<double[]> allDSpacings = new ArrayList<double[]>();
				double[] dSpaceArray = new double[dList.size()];
				
				for (int j = 0; j < dList.size();j++) {
					dSpaceArray[j] = dList.get(j);
				}
				
				allDSpacings.add(dSpaceArray);
				monitor.beginTask("Calibrating", IProgressMonitor.UNKNOWN);
				final CalibrationOutput output = CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize, wavelength);
				
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						DetectorProperties dp = currentData.md.getDetector2DProperties();

						dp.setBeamCentreDistance(output.getDistance().getDouble(0));
						double[] bc = new double[] {output.getBeamCentreX().getDouble(0),output.getBeamCentreY().getDouble(0) };
						dp.setBeamCentreCoords(bc);

						dp.setNormalAnglesInDegrees(output.getTilt().getDouble(0)*-1, 0, output.getTiltAngle().getDouble(0)*-1);

						hideFoundRings(plottingSystem);
						drawCalibrantRings(currentData.augmenter);
					}
				});

				return Status.OK_STATUS;
			};
		};
		job.setPriority(Job.SHORT);
		return job;
	}
	
	public static Job autoFindEllipsesMultipleImages(final Display display,
			final IPlottingSystem plottingSystem,
			final List<DiffractionTableData> model,
			final DiffractionTableData currentData,
			final double[] deltaDistance) {

		Job job = new Job("Calibrate detector") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				
				AbstractDataset ddist = new DoubleDataset(deltaDistance, new int[]{deltaDistance.length});
				
				List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
				List<double[]> allDSpacings = new ArrayList<double[]>();
				
				for (DiffractionTableData data : model) {
					
					plottingSystem.createPlot2D(data.image, null, monitor);
					
					final AbstractDataset image = (AbstractDataset)data.image;
					IDiffractionMetadata meta = data.md;
					int[] shape = image.getShape();
					monitor.beginTask("Finding approximate centre...", IProgressMonitor.UNKNOWN);
					double[] approxCentre = CentreGuess.guessCentre(image);
					
					double[] farCorner = new double[]{0,0};
					if (approxCentre[0] < shape[0]/2.0) farCorner[0] = shape[0];
					if (approxCentre[1] < shape[1]/2.0) farCorner[1] = shape[1];
					double maxDistance = Math.sqrt(Math.pow(approxCentre[0]-farCorner[0],2)+Math.pow(approxCentre[1]-farCorner[1],2));
					SectorROI sector = new SectorROI(approxCentre[0], approxCentre[1], 0, maxDistance, 0, 2*Math.PI);
					monitor.beginTask("Integrating image...", IProgressMonitor.UNKNOWN);
					AbstractDataset[] profile = ROIProfile.sector(image, null, sector, true, false, false, null, XAxis.PIXEL, false);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					final AbstractDataset y = profile[0];
					
					for (int i = 0 ; i < CENTRE_MASK_RADIUS ; i++) {
						y.set(0, i);
					}
					
					final AbstractDataset x = AbstractDataset.arange(y.getSize(), AbstractDataset.INT32);
					
					List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
					
					int max = spacings.size() > MAX_RINGS ? MAX_RINGS : spacings.size();
					
					double[] dSpace = new double[max];
					
					for (int i = 0; i < max; i++) dSpace[i] = spacings.get(i).getDNano()*10;
					monitor.beginTask("Matching to standard...", IProgressMonitor.UNKNOWN);
					final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, dSpace, meta.getDetector2DProperties().getHPxSize());
					
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					
					final List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();
					
					double[] inner = new double[dSpace.length];
					double[] outer = new double[dSpace.length];
					
					for (int i = 0; i < dSpace.length; i++) {
						//TODO if dSpace == 1;
						double dVal = dSpaceRadiusMap.get(dSpace[i]);
						ellipses.add(new EllipticalROI(dVal, approxCentre[0],approxCentre[1]));
						inner.toString();
						if (i == 0) {
							double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
							inner[i] = out > 50 ? 50 : out;
							outer[i] = out > 50 ? 50 : out;
							
						} else if (i == dSpace.length -1) {
							double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
							inner[i] = in > 50 ? 50 : in;
							outer[i] = in > 50 ? 50 : in;
						} else {
							double in = (dVal - dSpaceRadiusMap.get(dSpace[i-1]))/2;
							double out = (dSpaceRadiusMap.get(dSpace[i+1]) - dVal)/2;
							inner[i] = in > 50 ? 50 : in;;
							outer[i] = out > 50 ? 50 : out;;
						}
						
					}
					monitor.beginTask("Finding ellipses", IProgressMonitor.UNKNOWN);
					final List<EllipticalROI> foundEllipses = new ArrayList<EllipticalROI>();
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					IROI roi = null;
					double corFact = 0;
					double lastAspect = 1;
					double lastAngle = 0;
					List<Double> dList = new ArrayList<Double>();
					int i = 0;
					for (EllipticalROI e : ellipses) {
						
						double startSemi = e.getSemiAxis(0);
						e.setSemiAxis(0, startSemi+corFact);
						e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
						e.setAngle(lastAngle);
						//e.set
						
						IImageTrace t = DiffractionCalibrationUtils.getImageTrace(plottingSystem);
						try {
							roi = DiffractionTool.runEllipsePeakFit(monitor, display, plottingSystem, t, e, inner[i], outer[i]);
						} catch (Exception ex) {
							logger.debug(ex.getMessage());
							roi = null;
						} 
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						if (roi != null) {
							foundEllipses.add((EllipticalROI)roi);
							dList.add(dSpace[i]);
							corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
							lastAspect = ((EllipticalROI) roi).getAspectRatio();
							lastAngle = ((EllipticalROI) roi).getAngle();
							DiffractionCalibrationUtils.drawFoundRing(monitor, display, plottingSystem, roi, false);
						}
						i++;
					}
					double[] dSpaceArray = new double[dList.size()];
					
					for (int j = 0; j < dList.size();j++) {
						dSpaceArray[j] = dList.get(j);
					}
					
					allDSpacings.add(dSpaceArray);
					allEllipses.add(foundEllipses);
					
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							hideFoundRings(plottingSystem);
						}
					});
				}
				
				double pixelSize = model.get(0).md.getDetector2DProperties().getHPxSize();
				
				monitor.beginTask("Calibrating", IProgressMonitor.UNKNOWN);
				final CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings,ddist, pixelSize);
				
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						int i = 0;
						plottingSystem.createPlot2D(currentData.image, null, monitor);
						for (DiffractionTableData data : model) {
							DetectorProperties dp = data.md.getDetector2DProperties();

							dp.setDetectorDistance(output.getDistance().getDouble(i));
							double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
							dp.setBeamCentreCoords(bc);

							dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
							data.md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
							i++;
						}

						hideFoundRings(plottingSystem);
						drawCalibrantRings(currentData.augmenter);
					}
				});

				return Status.OK_STATUS;
			};
		};
		job.setPriority(Job.SHORT);
		return job;
	}
}
