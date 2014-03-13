package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dawb.workbench.ui.diffraction.DiffractionCalibrationUtils;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;
import uk.ac.diamond.scisoft.diffraction.powder.BruteStandardMatcher;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.CentreGuess;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;

public abstract class AbstractCalibrationRun implements IRunnableWithProgress {

	Display display;
	IPlottingSystem plottingSystem;
	List<DiffractionTableData> model;
	DiffractionTableData currentData;
	SimpleCalibrationParameterModel params;

	private static String REGION_PREFIX = "Pixel peaks";
	
	private static Logger logger = LoggerFactory.getLogger(AbstractCalibrationRun.class);
	
	public AbstractCalibrationRun(Display display,
			IPlottingSystem plottingSystem,
			List<DiffractionTableData> model,
			DiffractionTableData currentData,
			SimpleCalibrationParameterModel params) {
		
		this.display = display;
		this.plottingSystem = plottingSystem;
		this.model = model;
		this.params = params;
		this.currentData = currentData;
	}
	
	/**
	 * 
	 * @param plottingSystem
	 */
	public void removeFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
				}
		}
	}
	
	protected List<ResolutionEllipseROI> getFittedResolutionROIs(IPlottingSystem plottingSystem, EllipseFindingStructure efs,
			Display display, IProgressMonitor monitor) {
		monitor.beginTask("Finding ellipses", IProgressMonitor.UNKNOWN);
		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		if (monitor.isCanceled()) return null;
		IROI roi = null;
		
		int minSpacing = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
		int nPoints = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
		
		double corFact = 0;
		double lastAspect = 1;
		double lastAngle = 0;
		int i = -1;
		for (ResolutionEllipseROI e : efs.ellipses) {
			i++;
			if (efs.innerSearch[i] < minSpacing || efs.outerSearch[i] < minSpacing) continue;
			
			double startSemi = e.getSemiAxis(0);
			e.setSemiAxis(0, startSemi+corFact);
			e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
			e.setAngle(lastAngle);
			//e.set
			
			IImageTrace t = DiffractionCalibrationUtils.getImageTrace(plottingSystem);
			try {
				EllipticalROI[] inOut = new EllipticalROI[2];
				inOut[0] = e.copy();
				inOut[0].setSemiAxis(0, e.getSemiAxis(0)-efs.innerSearch[i]);
				inOut[0].setSemiAxis(1, e.getSemiAxis(1)-efs.innerSearch[i]);
				
				inOut[1] = e.copy();
				inOut[1].setSemiAxis(0, e.getSemiAxis(0)+efs.outerSearch[i]);
				inOut[1].setSemiAxis(1, e.getSemiAxis(1)+efs.outerSearch[i]);
				
				roi = DiffractionUtils.runConicPeakFit(monitor, display, plottingSystem, t, e, inOut,nPoints);
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
				roi = null;
			} 
			if (monitor.isCanceled()) return null;
			if (roi != null) {
				ResolutionEllipseROI r = new ResolutionEllipseROI((EllipticalROI)roi, e.getResolution());
				r.setPoints(((EllipticalFitROI)roi).getPoints());
				foundEllipses.add(r);
				corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
				lastAspect = ((EllipticalROI) roi).getAspectRatio();
				lastAngle = ((EllipticalROI) roi).getAngle();
				DiffractionCalibrationUtils.drawFoundRing(monitor, display, plottingSystem, roi, false);
			}
		}
		
		if (foundEllipses.size() < 2) return null;
		
		return foundEllipses;
	}
	
	protected EllipseFindingStructure getResolutionEllipses(AbstractDataset image, IDiffractionMetadata meta, SimpleCalibrationParameterModel params, IProgressMonitor monitor) {
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
		
		int centreMaskRadius = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS);
		
		for (int i = 0 ; i < centreMaskRadius ; i++) {
			y.set(0, i);
		}
		
		final AbstractDataset x = AbstractDataset.arange(y.getSize(), AbstractDataset.INT32);
		
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();
		
		List<Integer> ringList = null;
		
		if (params.isUseRingSet()) {
			ringList = new ArrayList<Integer>(new TreeSet<Integer>(params.getRingSet()));
			Iterator<Integer> it = ringList.iterator();
			while(it.hasNext()) {
				if (it.next() > spacings.size()) it.remove();
			}
		} else {
			int max = Math.min(params.getNumberOfRings(),spacings.size());
			
			ringList = new ArrayList<Integer>(max);
			for (int i = 1; i<= max;i++) ringList.add(i);
		}
		
		//int max = spacings.size() > maxRings ? maxRings : spacings.size();
		
		double[] dSpace = new double[ringList.size()];
		double[] fullDSpace = new double[spacings.size()];
		for (int i = 0; i < ringList.size(); i++){
			dSpace[i] = spacings.get(ringList.get(i)-1).getDNano()*10;
		}
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		
		monitor.beginTask("Matching to standard...", IProgressMonitor.UNKNOWN);
		final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, fullDSpace, meta.getDetector2DProperties().getHPxSize());
		
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
	
	protected void updateOnFinish(final CalibrationOutput output) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				for (DiffractionTableData data : model) {
					updateMetaData(data.md, output, i);
					data.residual = output.getResidual();
					i++;
				}

				removeFoundRings(plottingSystem);
			}
		});
	}
	
	protected void updateMetaData(final IDiffractionMetadata md , final CalibrationOutput output, final int i) {
		
		if (display.getThread()!=Thread.currentThread()) {
			display.syncExec(new Runnable() {

				@Override
				public void run() {
					updateMetaData(md, output, i);

				}
			});
		} else {

			DetectorProperties dp = md.getDetector2DProperties();

			dp.setBeamCentreDistance(output.getDistance().getDouble(i));
			double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
			dp.setBeamCentreCoords(bc);

			dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
			md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
		}
		
	}

}
