package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.List;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.roi.IParametricROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrantSpacing;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;
import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;

public class POIFindingRun implements IRunnableWithProgress {

	DiffractionImageData currentData;
	SimpleCalibrationParameterModel model;
	ICalibrationUIProgressUpdate uiUpdater;
	
	private int maxSize = 20;
	
	private static Logger logger = LoggerFactory.getLogger(POIFindingRun.class);
	
	static String REGION_PREFIX = "Pixel peaks";
	
	public POIFindingRun(ICalibrationUIProgressUpdate uiUpdater,
			final DiffractionImageData currentData,
			final SimpleCalibrationParameterModel model) {
		this.uiUpdater =  uiUpdater;
		this.currentData = currentData;
		this.model = model;
	}
	
	@Override
	public void run(IProgressMonitor monitor) {
		
		maxSize = model.getMaxSearchSize();
		
		monitor.beginTask("Finding Points of Interest...", IProgressMonitor.UNKNOWN);
		
		if (currentData == null && currentData.getMetaData() == null)
			return;

		CalibrantSpacing cs = CalibrationFactory.getCalibrationStandards().getCalibrant();
		
		final List<IROI> resROIs = DSpacing.getResolutionRings(currentData.getMetaData(),cs);
		
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
					roi = DSpacing.fitParametricROI(resROIs,(IParametricROI)r, currentData.getImage(), i, minSpacing, nPoints, maxSize, new ProgressMonitorWrapper(monitor));
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
	
	
	public void updateData(DiffractionImageData data) {
		this.currentData = data;
	}
}
