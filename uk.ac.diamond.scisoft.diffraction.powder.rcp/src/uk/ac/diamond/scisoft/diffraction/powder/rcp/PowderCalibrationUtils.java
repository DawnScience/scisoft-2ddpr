package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.roi.IPolylineROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalFitROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolylineROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.RegionUtils;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class PowderCalibrationUtils {
	

	public static String REGION_PREFIX = "Pixel peaks";
	
	public static void clearFoundRings(IPlottingSystem<?> plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
			}
		}
	}
	
	public static boolean drawFoundRing(final IPlottingSystem<?> plotter, final IROI froi, final IProgressMonitor monitor) {
		final boolean[] status = {true};
		
		Color col = ColorConstants.orange;
		RegionType type = RegionType.ELLIPSEFIT;
		int lineWidth = 2;
		
		IROI roi = null;
		IROI shiftROI = null;
		if (froi instanceof EllipticalFitROI) {
			EllipticalFitROI ef = (EllipticalFitROI)froi.copy();
			for (IROI p : ef.getPoints()) {
				p.addPoint(0.5, 0.5);
			}
			shiftROI = ef;
		}
		
		if (froi instanceof IPolylineROI) {
			PolylineROI points = ((PolylineROI)froi).copy();
			for (IROI p : points) {
				p.addPoint(0.5, 0.5);
			}
			shiftROI = points;
			col = ColorConstants.yellow;
			type = RegionType.POLYLINE;
			lineWidth = 0;
		}
		
		if (shiftROI != null)  roi = shiftROI;
		else roi = froi;
		
		final IROI fr = roi;
		final Color fcol = col;
		final RegionType ftype = type;
		final int flw = lineWidth;
		
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				try {
					IRegion region = plotter.createRegion(RegionUtils.getUniqueName(REGION_PREFIX, plotter), ftype);
					region.setROI(fr);
					region.setRegionColor(fcol);
					region.setLineWidth(flw);
					if (monitor != null) monitor.subTask("Add region");
					region.setUserRegion(false);
					plotter.addRegion(region);
					if (monitor != null) monitor.worked(1);
				} catch (Exception e) {
					status[0] = false;
				}
			}
		});
		return status[0];
	}
	
	
}
