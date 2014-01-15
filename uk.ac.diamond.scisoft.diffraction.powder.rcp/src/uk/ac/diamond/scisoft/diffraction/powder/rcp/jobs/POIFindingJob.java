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
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.PointROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;

public class POIFindingJob extends Job {

	IPlottingSystem plottingSystem;
	DiffractionTableData currentData;
	int nRings;
	
	private static Logger logger = LoggerFactory.getLogger(POIFindingJob.class);
	
	static String REGION_PREFIX = "Pixel peaks";
	
	public POIFindingJob(final IPlottingSystem plottingSystem,
			final DiffractionTableData currentData,
			final int nRings) {
		super("Finding points of interest");
		this.plottingSystem = plottingSystem;
		this.currentData = currentData;
		this.nRings = nRings;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		IStatus stat = Status.OK_STATUS;
		
		if (currentData == null)
			return Status.CANCEL_STATUS;

		DiffractionImageAugmenter aug = currentData.augmenter;
		if (aug == null)
			return Status.CANCEL_STATUS;

		final List<IROI> resROIs = aug.getResolutionROIs();
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
		
		if (nRings > 0 && nRings < resROIs.size()) numberToFit = nRings;
		
		int n = 0;
		for (int i = 0; i < resROIs.size(); i++) {
			IROI r = resROIs.get(i);
			IROI roi = null;
			try {
				if (!(r instanceof EllipticalROI)) { // cannot cope with other conic sections for now
					continue;
				}
				
				if (i >= numberToFit) continue;
				
				EllipticalROI e = (EllipticalROI) r;
				double major = e.getSemiAxis(0);
				
				double deltalow = major > 50 ? 50 : major;
				double deltahigh = 50;
				
				if (i != 0) {
					deltalow = 0.5*(major - ((EllipticalROI)resROIs.get(i-1)).getSemiAxis(0));
					deltalow = deltalow > 50 ? 50 : deltalow;
				}
				
				if (i < resROIs.size()-1) {
					deltahigh = 0.5*(((EllipticalROI)resROIs.get(i+1)).getSemiAxis(0) - major);
					deltahigh = deltahigh > 50 ? 50 : deltahigh;
				}
				

				try {
					roi = DiffractionUtils.runEllipsePeakFit(monitor, Display.getDefault(), plottingSystem, image, e, deltalow, deltahigh,256);
				} catch (NullPointerException ex) {
					stat = Status.CANCEL_STATUS;
					n = -1; // indicate, to finally clause, problem with getting image or other issues
					return stat;
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
		return stat;
	}
	
	public void setNumberOfRingsToFind(int nRings) {
		this.nRings = nRings;
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
