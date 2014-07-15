package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetFactory;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.roi.CircularROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public class FromRingsCalibrationRun extends AbstractCalibrationRun {

	private static final String description = "Manual powder diffraction image calibration using ellipse parameters";
	
	public FromRingsCalibrationRun(Display display,
			IPlottingSystem plottingSystem,DiffractionDataManager manager,
			DiffractionTableData currentData, SimpleCalibrationParameterModel params) {
		super(display, plottingSystem, manager, currentData, params);
	}

	@Override
	public void run(IProgressMonitor monitor) {
		//final ProgressMonitorWrapper mon = new ProgressMonitorWrapper(monitor);
		monitor.beginTask("Calibrate detector", IProgressMonitor.UNKNOWN);
		List<HKL> spacings = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs();

		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();

		for (DiffractionTableData data : manager.iterable()) {
			
			int n = data.getRois().size();
			if (n != spacings.size()) { // always allow a choice to be made
				throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
			}

			int totalNonNull = 0;

			for (IROI roi : data.getRois()) {
				if (roi != null) totalNonNull++;
			}

			double[] ds = new double[totalNonNull];
			List<EllipticalROI> erois = new ArrayList<EllipticalROI>(totalNonNull);

			int count = 0;
			for (int i = 0; i < data.getRois().size(); i++) {
				IROI roi = data.getRois().get(i);
				if (roi != null) {
					ds[count]  = spacings.get(i).getDNano()*10;

					if (roi instanceof EllipticalROI) {
						erois.add((EllipticalROI)roi);
					} else if(roi instanceof CircularROI) {
						erois.add(new EllipticalROI((CircularROI)roi));
					} else {
						throw new IllegalArgumentException("ROI not elliptical or circular - try point calibration");
					}

					count++;
				}
			}
			allEllipses.add(erois);
			allDSpacings.add(ds);
		}
		

		AbstractDataset ddist = manager.getDistances();
		
		double pixelSize = currentData.getMetaData().getDetector2DProperties().getHPxSize();
		
		CalibrationOutput o = null;
		
		if (!params.isFloatEnergy()) {
			o =  CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize,currentData.getMetaData().getDiffractionCrystalEnvironment().getWavelength());
		} else if (!params.isFloatDistance()){
			o =  CalibrateEllipses.runKnownDistance(allEllipses, allDSpacings, pixelSize,currentData.getMetaData().getDetector2DProperties().getBeamCentreDistance());
		}else {
			o =  CalibrateEllipses.run(allEllipses, allDSpacings,ddist, pixelSize);
		}
		
		final CalibrationOutput output = o;
		
		double[] fullDSpace = new double[spacings.size()];
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);

		PowderCalibrationInfoImpl[] info = new PowderCalibrationInfoImpl[manager.getSize()];
		
		int count = 0;
		for (DiffractionTableData data : manager.iterable()) {

			info[count++] = createPowderCalibrationInfo(data,true);
		}
		
		for (int i = 0; i < allEllipses.size(); i++) {
			int[] infoIndex = new int[allDSpacings.get(i).length];
			
			for (int j = 0; j < infoIndex.length; j++) {
				infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(0)[j])).argMin();
			}
			Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
			
			info[i].setPostCalibrationInformation(description, infoSpace, infoSpaceds, output.getResidual());
			
		}
		output.setCalibrationInfo(info);
		
		updateOnFinish(output);
		
		return;
	}

}
