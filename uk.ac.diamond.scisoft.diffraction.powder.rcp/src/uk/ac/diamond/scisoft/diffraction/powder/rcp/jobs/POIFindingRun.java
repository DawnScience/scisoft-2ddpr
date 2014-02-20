package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.region.RegionUtils;
import org.dawnsci.plotting.api.region.IRegion.RegionType;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.api.trace.ITrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.PointROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.RingSelectionGroup;

public class POIFindingRun implements IRunnableWithProgress {

	IPlottingSystem plottingSystem;
	DiffractionTableData currentData;
	RingSelectionGroup param;
	
	private static Logger logger = LoggerFactory.getLogger(POIFindingRun.class);
	
	static String REGION_PREFIX = "Pixel peaks";
	
	public POIFindingRun(final IPlottingSystem plottingSystem,
			final DiffractionTableData currentData,
			final RingSelectionGroup param) {
		this.plottingSystem = plottingSystem;
		this.currentData = currentData;
		this.param = param;
	}
	
	@Override
	public void run(IProgressMonitor monitor) {
		
		SimpleCalibrationParameterModel model = extractModelFromWidget(param);
		
		IStatus stat = Status.OK_STATUS;
		
		if (currentData == null && currentData.md == null)
			return;

		final List<IROI> resROIs = PowderCalibrationUtils.getResolutionRings(currentData.md);
		final IImageTrace image = getImageTrace(plottingSystem);
		if (currentData.rois == null) {
			currentData.rois = new ArrayList<IROI>();
		} else {
			currentData.rois.clear();
		}
		currentData.use = false;
		currentData.nrois = 0;
		
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				clearFoundRings(plottingSystem);
			}
		});
		
		int numberToFit = resROIs.size();
		
		if (!model.isUseRingSet()) {
			numberToFit = Math.min(resROIs.size(), model.getNumberOfRings());
		}
		
		int minSpacing = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
		int nPoints = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
		int n = 0;
		for (int i = 0; i < resROIs.size(); i++) {
			IROI r = resROIs.get(i);
			IROI roi = null;
			try {
				if (!(r instanceof EllipticalROI)) { // cannot cope with other conic sections for now
					continue;
				}
				
				if (i >= numberToFit) continue;
				
				if (model.isUseRingSet() && !model.getRingSet().contains(i+1)) continue;
				
				EllipticalROI e = (EllipticalROI) r;
				double major = e.getSemiAxis(0);
				
				double deltalow = major > 50 ? 50 : major;
				double deltahigh = 50;
				
				if (i != 0) {
					
					if (resROIs.get(i-1) instanceof EllipticalROI) {
						deltalow = 0.5*(major - ((EllipticalROI)resROIs.get(i-1)).getSemiAxis(0));
						deltalow = deltalow > 50 ? 50 : deltalow;
					}
				}
				
				if (i < resROIs.size()-1) {
					if (resROIs.get(i+1) instanceof EllipticalROI) {
					deltahigh = 0.5*(((EllipticalROI)resROIs.get(i+1)).getSemiAxis(0) - major);
					deltahigh = deltahigh > 50 ? 50 : deltahigh;
					}
				}
				
				if (deltalow < minSpacing || deltahigh < minSpacing) continue;
				
				try {
					roi = DiffractionUtils.runEllipsePeakFit(monitor, Display.getDefault(), plottingSystem, image, e, deltalow, deltahigh,nPoints);
				} catch (NullPointerException ex) {
					stat = Status.CANCEL_STATUS;
					n = -1; // indicate, to finally clause, problem with getting image or other issues
					return;
				}
				
				if (roi != null) {
					n++;
					stat = drawFoundRing(monitor, Display.getDefault(), plottingSystem, roi);
				}

				if (!stat.isOK())
					break;
			} catch (IllegalArgumentException ex) {
				logger.trace("Could not find ellipse with {}: {}", r, ex);
			} finally {
				if (n >= 0) {
					currentData.rois.add(roi); // can include null placeholder
				} else {
					currentData.rois.clear();
				}
			}
		}
		currentData.nrois = n;
		if (currentData.nrois > 0) {
			currentData.use = true;
		}
		return;
	}
	
	public void setRingParameters(RingSelectionGroup param) {
		this.param = param;
	}
	
	public void setCurrentData(DiffractionTableData currentData) {
		this.currentData = currentData;
	}

	protected IImageTrace getImageTrace(IPlottingSystem system) {
		Collection<ITrace> traces = system.getTraces();
		if (traces != null && traces.size() > 0) {
			ITrace trace = traces.iterator().next();
			if (trace instanceof IImageTrace) {
				return (IImageTrace) trace;
			}
		}
		return null;
	}
	
	protected void clearFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
			}
		}
	}
	
	private SimpleCalibrationParameterModel extractModelFromWidget(final RingSelectionGroup ringSelection) {
		
		final SimpleCalibrationParameterModel model = new SimpleCalibrationParameterModel();
		
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				model.setUseRingSet(!ringSelection.isUsingRingSpinner());
				model.setNumberOfRings(ringSelection.getRingSpinnerSelection());
				model.setRingSet(ringSelection.getRingSelectionText().getUniqueRingNumbers());
			}
		});
		
		return model;
	}
	
	private IStatus drawFoundRing(final IProgressMonitor monitor, Display display, final IPlottingSystem plotter, final IROI froi) {
		final boolean[] status = {true};
		
		IROI roi = null;
		EllipticalFitROI ef = null;
		if (froi instanceof EllipticalFitROI) {
			ef = (EllipticalFitROI)froi.copy();
			PolylineROI points = ef.getPoints();
			for (int i = 0; i< points.getNumberOfPoints(); i++) {
				PointROI proi = points.getPoint(i);
				double[] point = proi.getPoint();
				point[1] += 0.5;
				point[0] += 0.5;
				proi.setPoint(point);
			}
		}
		
		if (ef != null)  roi = ef;
		else roi = froi;
		
		final IROI fr = roi;
		
		display.syncExec(new Runnable() {

			public void run() {
				try {
					IRegion region = plotter.createRegion(RegionUtils.getUniqueName(REGION_PREFIX, plotter), RegionType.ELLIPSEFIT);
					region.setROI(fr);
					region.setRegionColor(ColorConstants.orange);
					monitor.subTask("Add region");
					region.setUserRegion(false);
					plotter.addRegion(region);
					monitor.worked(1);
				} catch (Exception e) {
					status[0] = false;
				}
			}
		});
		return status[0] ? Status.OK_STATUS : Status.CANCEL_STATUS;
	}
}
