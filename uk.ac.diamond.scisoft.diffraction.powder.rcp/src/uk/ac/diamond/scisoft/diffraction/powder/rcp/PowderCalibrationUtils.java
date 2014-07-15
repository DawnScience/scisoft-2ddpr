package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.RegionUtils;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.IPolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;

public class PowderCalibrationUtils {
	

	public static String REGION_PREFIX = "Pixel peaks";
	
	public static void clearFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
			}
		}
	}
	
	public static boolean drawFoundRing(final IPlottingSystem plotter, final IROI froi, final IProgressMonitor monitor) {
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
	
	public static List<IROI> getResolutionRings(IDiffractionMetadata metadata) {
		
		CalibrantSpacing cs = CalibrationFactory.getCalibrationStandards().getCalibrant();
		
		List<IROI> rois = new ArrayList<IROI>(cs.getHKLs().size());
		
		for (HKL hkl : cs.getHKLs()) {
			DetectorProperties detprop = metadata.getDetector2DProperties();
			DiffractionCrystalEnvironment diffenv = metadata.getDiffractionCrystalEnvironment();
			try {
				IROI roi = DSpacing.conicFromDSpacing(detprop, diffenv, Double.valueOf(hkl.getD().doubleValue(NonSI.ANGSTROM)));
				rois.add(roi);
			} catch ( Exception e) {
				rois.add(null);
			}
			
			
		}
		
		return rois;
	}
	
}
