package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.Collection;
import java.util.List;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.roi.IParametricROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.HyperbolicROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.MultiplePeakFittingEllipseFinder;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.PowderCalibrationUtils;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionTableData;

public class POIFindingRun implements IRunnableWithProgress {

	IPlottingSystem<?> plottingSystem;
	DiffractionTableData currentData;
	SimpleCalibrationParameterModel model;
	ICalibrationUIProgressUpdate uiUpdater;
	
	private static final int MAX_SIZE = 50;
	
	private static Logger logger = LoggerFactory.getLogger(POIFindingRun.class);
	
	static String REGION_PREFIX = "Pixel peaks";
	
	public POIFindingRun(ICalibrationUIProgressUpdate uiUpdater,
			final DiffractionTableData currentData,
			final SimpleCalibrationParameterModel model) {
		this.uiUpdater =  uiUpdater;
		this.currentData = currentData;
		this.model = model;
	}
	
	@Override
	public void run(IProgressMonitor monitor) {
		
		monitor.beginTask("Finding Points of Interest...", IProgressMonitor.UNKNOWN);
		
		if (currentData == null && currentData.getMetaData() == null)
			return;

		final List<IROI> resROIs = PowderCalibrationUtils.getResolutionRings(currentData.getMetaData());
		
		currentData.clearROIs();
		currentData.setUse(false);
		currentData.setNrois(0);
		
		if (uiUpdater != null) uiUpdater.removeRings();
		
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
				if (i >= numberToFit) continue;
				if (monitor.isCanceled()) continue;
				
				
				if (model.isUseRingSet() && !model.getRingSet().contains(i+1)) continue;
				
				if (r instanceof IParametricROI) {
					try {
					roi = fitParametricROI(resROIs,(IParametricROI)r, currentData.getImage(), i, minSpacing, nPoints, monitor);
					} catch (NullPointerException ex) {
						n = -1; // indicate, to finally clause, problem with getting image or other issues
						return;
					}
				}
				
				if (roi != null && uiUpdater != null) {
					n++;
					uiUpdater.drawFoundRing(roi);
				}


			} catch (IllegalArgumentException ex) {
				logger.trace("Could not find ellipse with {}: {}", r, ex);
			} finally {
				if (n >= 0) {
					currentData.addROI(roi); // can include null placeholder
				} else {
					currentData.clearROIs();
				}
			}
		}
		currentData.setNrois(n);
		if (currentData.getNrois() > 0) {
			currentData.setUse(true);
		}
		
		if (uiUpdater != null) uiUpdater.completed();
		return;
	}

	protected IImageTrace getImageTrace(IPlottingSystem<?> system) {
		Collection<ITrace> traces = system.getTraces();
		if (traces != null && traces.size() > 0) {
			ITrace trace = traces.iterator().next();
			if (trace instanceof IImageTrace) {
				return (IImageTrace) trace;
			}
		}
		return null;
	}
	
	private IROI fitParametricROI(List<IROI> resROIs, IParametricROI r, IDataset image, int i, int minSpacing, int nPoints, IProgressMonitor monitor) {
		
		IParametricROI[] inOut = getInnerAndOuterRangeROIs(resROIs, r,i,minSpacing);
		
		if (inOut == null) return null;
		
		return MultiplePeakFittingEllipseFinder.runConicPeakFit(new ProgressMonitorWrapper(monitor),DatasetUtils.convertToDataset(image), r,inOut,nPoints);
	}
	
	private IParametricROI[] getInnerAndOuterRangeROIs(List<IROI> resROIs, IParametricROI r, int i, int minSpacing) {
		IParametricROI[] inOut = new IParametricROI[2];
		//TODO min spacing for non-elliptical
		//TODO include parabolic case
		if (r instanceof HyperbolicROI) {
			HyperbolicROI h = (HyperbolicROI)r;
			double slr = h.getSemilatusRectum();
			
			if (i != 0) {
				
				if (resROIs.get(i-1) instanceof HyperbolicROI) {
					HyperbolicROI inner =  (HyperbolicROI)resROIs.get(i-1);
					double sd = (slr-inner.getSemilatusRectum())/4;
					sd = sd > MAX_SIZE ? MAX_SIZE : sd;
					double semi = slr - sd;
					double px = h.getPointX() - (h.getPointX() - inner.getPointX())/4;
					double py = h.getPointY() - (h.getPointY() - inner.getPointY())/4;
					inOut[0] = new HyperbolicROI(semi, h.getEccentricity(), h.getAngle(), px, py);
				}
			}
			
			if (i < resROIs.size()-1) {
				if (resROIs.get(i+1) instanceof HyperbolicROI) {
					HyperbolicROI outer =  (HyperbolicROI)resROIs.get(i+1);
					double sd = (outer.getSemilatusRectum()-slr)/4;
					sd = sd > MAX_SIZE ? MAX_SIZE : sd;
					double pxd = (outer.getPointX() - h.getPointX())/4;
					double pyd = (outer.getPointY() - h.getPointY())/4;
					inOut[1] = new HyperbolicROI(h.getSemilatusRectum()+sd,
							h.getEccentricity(), h.getAngle(), h.getPointX()+pxd, h.getPointY()+pyd);
					
					
					if (inOut[0] == null) {
						inOut[0] = new HyperbolicROI(h.getSemilatusRectum()-sd,
								outer.getEccentricity(), outer.getAngle(), h.getPointX()-pxd, h.getPointY()-pyd);
					}
				}
			}
			
			
		} else if (r instanceof EllipticalROI) {
			EllipticalROI e = (EllipticalROI) r;
			double major = e.getSemiAxis(0);
			
			double deltalow = major > MAX_SIZE ? MAX_SIZE : major;
			double deltahigh = MAX_SIZE;
			
			if (i != 0) {
				
				if (resROIs.get(i-1) instanceof EllipticalROI) {
					deltalow = 0.5*(major - ((EllipticalROI)resROIs.get(i-1)).getSemiAxis(0));
					deltalow = deltalow > MAX_SIZE ? MAX_SIZE : deltalow;
				}
			}
			
			if (i < resROIs.size()-1) {
				if (resROIs.get(i+1) instanceof EllipticalROI) {
				deltahigh = 0.5*(((EllipticalROI)resROIs.get(i+1)).getSemiAxis(0) - major);
				deltahigh = deltahigh > MAX_SIZE ? MAX_SIZE : deltahigh;
				}
			}
			
			if (deltalow < minSpacing || deltahigh < minSpacing) return null;
			
			EllipticalROI in = e.copy();
			in.setSemiAxis(0, e.getSemiAxis(0)-deltalow);
			in.setSemiAxis(1, e.getSemiAxis(1)-deltalow);
			inOut[0] = in;
			
			EllipticalROI out = e.copy();
			out.setSemiAxis(0, e.getSemiAxis(0)+deltahigh);
			out.setSemiAxis(1, e.getSemiAxis(1)+deltahigh);
			inOut[1] = out;
		}
		
		if (inOut[0] == null || inOut[1] == null) return null;
		
		return inOut;
	}
	
	public void updateData(DiffractionTableData data) {
		this.currentData = data;
	}
}
