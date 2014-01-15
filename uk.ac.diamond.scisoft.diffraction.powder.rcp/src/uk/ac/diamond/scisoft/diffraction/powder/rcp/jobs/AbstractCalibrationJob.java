package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;
import uk.ac.diamond.scisoft.diffraction.powder.BruteStandardMatcher;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.CentreGuess;

public abstract class AbstractCalibrationJob extends Job {

	Display display;
	IPlottingSystem plottingSystem;
	List<DiffractionTableData> model;
	DiffractionTableData currentData;
	int maxRings;
	
	boolean fixedWavelength = false;

	private static final int CENTRE_MASK_RADIUS = 50;
	private static String REGION_PREFIX = "Pixel peaks";
	
	private static Logger logger = LoggerFactory.getLogger(AbstractCalibrationJob.class);
	
	public AbstractCalibrationJob(Display display,
			IPlottingSystem plottingSystem,
			List<DiffractionTableData> model,
			DiffractionTableData currentData,
			int maxRings) {
		super("Calibration");
		
		this.display = display;
		this.plottingSystem = plottingSystem;
		this.model = model;
		this.maxRings = maxRings;
		this.currentData = currentData;
	}
	
	/**
	 * 
	 * @param plottingSystem
	 */
	public void hideFoundRings(IPlottingSystem plottingSystem) {
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
	public void drawCalibrantRings(DiffractionImageAugmenter aug) {

		if (aug == null)
			return;

		CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
		aug.drawCalibrantRings(true, standards.getCalibrant());
		aug.drawBeamCentre(true);
	}
	
	protected List<ResolutionEllipseROI> getFittedResolutionROIs(IPlottingSystem plottingSystem, EllipseFindingStructure efs,
			Display display, IProgressMonitor monitor) {
		monitor.beginTask("Finding ellipses", IProgressMonitor.UNKNOWN);
		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		if (monitor.isCanceled()) return null;
		IROI roi = null;
		double corFact = 0;
		double lastAspect = 1;
		double lastAngle = 0;
		int i = 0;
		for (ResolutionEllipseROI e : efs.ellipses) {
			
			double startSemi = e.getSemiAxis(0);
			e.setSemiAxis(0, startSemi+corFact);
			e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
			e.setAngle(lastAngle);
			//e.set
			
			IImageTrace t = DiffractionCalibrationUtils.getImageTrace(plottingSystem);
			try {
				roi = DiffractionUtils.runEllipsePeakFit(monitor, display, plottingSystem, t, e, efs.innerSearch[i], efs.outerSearch[i],256);
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
				roi = null;
			} 
			if (monitor.isCanceled()) return null;
			if (roi != null) {
				foundEllipses.add(new ResolutionEllipseROI((EllipticalROI)roi, e.getResolution()));
				corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
				lastAspect = ((EllipticalROI) roi).getAspectRatio();
				lastAngle = ((EllipticalROI) roi).getAngle();
				DiffractionCalibrationUtils.drawFoundRing(monitor, display, plottingSystem, roi, false);
			}
			i++;
		}
		
		if (foundEllipses.size() < 2) return null;
		
		return foundEllipses;
	}
	
	protected EllipseFindingStructure getResolutionEllipses(AbstractDataset image, IDiffractionMetadata meta, int maxRings, IProgressMonitor monitor) {
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
		if (monitor.isCanceled()) return null;
		final AbstractDataset y = profile[0];
		
		for (int i = 0 ; i < CENTRE_MASK_RADIUS ; i++) {
			y.set(0, i);
		}
		
		final AbstractDataset x = AbstractDataset.arange(y.getSize(), AbstractDataset.INT32);
		
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		int max = spacings.size() > maxRings ? maxRings : spacings.size();
		
		double[] dSpace = new double[max];
		
		for (int i = 0; i < max; i++) dSpace[i] = spacings.get(i).getDNano()*10;
		monitor.beginTask("Matching to standard...", IProgressMonitor.UNKNOWN);
		final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, dSpace, meta.getDetector2DProperties().getHPxSize());
		
		if (monitor.isCanceled()) return null;
		
		final List<ResolutionEllipseROI> ellipses = new ArrayList<ResolutionEllipseROI>();
		
		double[] inner = new double[dSpace.length];
		double[] outer = new double[dSpace.length];
		
		for (int i = 0; i < dSpace.length; i++) {
			//TODO out might be known if max less than spacings.size();
			double dVal = dSpaceRadiusMap.get(dSpace[i]);
			EllipticalROI el = new EllipticalROI(dVal, approxCentre[0],approxCentre[1]);
			ellipses.add(new ResolutionEllipseROI(el, dSpace[i]));
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
		
		EllipseFindingStructure efs = new EllipseFindingStructure();
		efs.ellipses = ellipses;
		efs.innerSearch = inner;
		efs.outerSearch = outer;
		
		return efs;
		
	}
	
	protected class EllipseFindingStructure {
		List<ResolutionEllipseROI> ellipses;
		double[] innerSearch;
		double[] outerSearch;
	}
	
	public boolean isFixedWavelength() {
		return fixedWavelength;
	}

	public void setFixedWavelength(boolean fixedWavelength) {
		this.fixedWavelength = fixedWavelength;
	}
	
	protected void updateOnFinish(final CalibrationOutput output) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				plottingSystem.createPlot2D(currentData.image, null, null);
				for (DiffractionTableData data : model) {
					DetectorProperties dp = data.md.getDetector2DProperties();

					dp.setBeamCentreDistance(output.getDistance().getDouble(i));
					double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
					dp.setBeamCentreCoords(bc);

					dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
					data.residual = output.getResidual();
					data.md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
					i++;
				}

				hideFoundRings(plottingSystem);
				drawCalibrantRings(currentData.augmenter);
			}
		});
	}

}
