package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.dataset.BooleanDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetFactory;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IndexIterator;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.Stats;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.PeakFittingEllipseFinder;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.AbstractPixelIntegration;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.NonPixelSplittingIntegration;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.monitor.IMonitor;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.IPolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.PolylineROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile.XAxis;

public class PowderCalibration {
	
	private static final int MAX_RINGS = 10;
	private static final int CENTRE_MASK_RADIUS = 50;
	private static final int NUMBER_OF_POINTS = 256;
	private static final int MINIMUM_SPACING = 10;
	
	private static final String description = "Automatic powder diffraction image calibration using ellipse parameters";
	private static final String addition = " with final point-based optimisation";
	
	private final static Logger logger = LoggerFactory.getLogger(PowderCalibration.class);
	
	public static CalibrationOutput calibrateKnownWavelength(Dataset image, double wavelength, double pixel, List<HKL> spacings, int nRings) {
		

		int[] options = new int[]{CENTRE_MASK_RADIUS, MINIMUM_SPACING, NUMBER_OF_POINTS};
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setFloatEnergy(false);
		params.setFinalGlobalOptimisation(true);
		
		return calibrateMultipleImages(new IDataset[] {image}, DatasetFactory.zeros(new int[]{1}, Dataset.FLOAT64), pixel,
				spacings,  wavelength, options, params, null, null);
	}
	
	public static CalibrationOutput calibrateSingleImage(Dataset image, double pixel, List<HKL> spacings, int nRings) {
		
		int[] options = new int[]{CENTRE_MASK_RADIUS, MINIMUM_SPACING, NUMBER_OF_POINTS};
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setFinalGlobalOptimisation(true);
		
		return calibrateMultipleImages(new IDataset[] {image}, DatasetFactory.zeros(new int[]{1}, Dataset.FLOAT64), pixel,
				spacings,  0, options, params, null, null);
	}
	
	public static CalibrationOutput calibrateMultipleImages(IDataset[] images, Dataset deltaDistance, double pxSize,
			List<HKL> spacings, double fixed, int[] options, SimpleCalibrationParameterModel params, IMonitor mon, ICalibrationUIProgressUpdate uiUpdate) {
		
		return calibrateMultipleImages(images, deltaDistance, pxSize, spacings, fixed, options, params, mon, uiUpdate, null);
	}
	
	public static CalibrationOutput calibrateMultipleImages(IDataset[] images, Dataset deltaDistance, double pxSize,
			List<HKL> spacings, double fixed, int[] options, SimpleCalibrationParameterModel params, IMonitor mon, ICalibrationUIProgressUpdate uiUpdate, PowderCalibrationInfoImpl[] info) {

		if (info == null) {
			
			info = new PowderCalibrationInfoImpl[images.length];
			
			for (int i = 0; i < images.length; i++) info[i] = new PowderCalibrationInfoImpl();
		}
		
		//options [0]centreMaskRadius, [1]minSpacing, [2]nPoints
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		for (IDataset image: images) {

			if (images.length > 1 && uiUpdate != null) uiUpdate.updatePlotData(image);

			Dataset imds = DatasetUtils.convertToDataset(image).clone();
			
			//Clear any hot pixels that might interfere with centre finding etc
			
			double[] outlierValues = Stats.outlierValues(imds, 0.1, 99.9, -1);
			
			IndexIterator iterator = imds.getIterator();
			
			while (iterator.hasNext()) {
				double val = imds.getElementDoubleAbs(iterator.index);
				if (val < outlierValues[0]) imds.setObjectAbs(iterator.index, outlierValues[0]);
				if (val > outlierValues[1]) imds.setObjectAbs(iterator.index, outlierValues[1]);
			}
			
			final EllipseFindingStructure efs = getResolutionEllipses((Dataset) imds, spacings, pxSize, params,options[0], mon);

			if (mon != null && mon.isCancelled()) return null;

			if (efs == null) throw new IllegalArgumentException("No rings found!");

			List<ResolutionEllipseROI> foundEllipses = getFittedResolutionROIs(uiUpdate, efs, (Dataset) image,options[0],options[1],options[2],mon);

			if (mon != null && mon.isCancelled()) return null;

			if (foundEllipses == null || foundEllipses.size() < 2) throw new IllegalArgumentException("No rings found!");

			double[] dSpaceArray = new double[foundEllipses.size()];

			for (int j = 0; j < foundEllipses.size();j++) {
				dSpaceArray[j] = foundEllipses.get(j).getResolution();
			}

			allDSpacings.add(dSpaceArray);
			allEllipses.add(new ArrayList<EllipticalROI>(foundEllipses));

			if (uiUpdate!= null) uiUpdate.removeRings();
		}

		if (mon != null) mon.subTask("Calibrating");
		//TODO make sure fix wavelength/distance ignored for multiple images
		CalibrationOutput output = CalibrateEllipses.run(allEllipses, allDSpacings,deltaDistance,pxSize, fixed, params);
		
		String desc = description;
		
		if (allEllipses.size() == 1 && params.isFinalGlobalOptimisation()) {
			
			IDiffractionMetadata meta = createMetadataFromOutput(output, 0, images[0].getShape(),pxSize);
			
			CalibratePointsParameterModel paramModel = new CalibratePointsParameterModel(params);
			
			List<IPolylineROI> lineROIList = new ArrayList<IPolylineROI>();
			
			for (EllipticalROI roi : allEllipses.get(0)) {
				if (roi instanceof ResolutionEllipseROI && ((ResolutionEllipseROI)roi).getPoints() != null) {
					lineROIList.add(((ResolutionEllipseROI)roi).getPoints());
				}
			}
			
			desc = desc + addition;
			output = CalibratePoints.run(lineROIList, allDSpacings.get(0), meta, paramModel);
			
		}
		
		double[] fullDSpace = new double[spacings.size()];
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);

		for (int i = 0; i < images.length; i++) {
			int[] infoIndex = new int[allDSpacings.get(i).length];
			
			for (int j = 0; j < infoIndex.length; j++) {
				infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(i)[j])).argMin();
			}
			Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
			
			info[i].setPostCalibrationInformation(desc, infoSpace, infoSpaceds, output.getResidual());
			
		}
		output.setCalibrationInfo(info);
		
		return output;
	}
	
	public static List<ResolutionEllipseROI> findMatchedEllipses(Dataset image, double pixel, List<HKL> spacings) {

		double[] approxCentre = CentreGuess.guessCentre(image);

		logger.info("centre, x: " + approxCentre[0] +" y: " + approxCentre[1]);

		int[] shape = image.getShape();

		double[] farCorner = new double[]{0,0};
		if (approxCentre[0] < shape[0]/2.0) farCorner[0] = shape[0];
		if (approxCentre[1] < shape[1]/2.0) farCorner[1] = shape[1];
		double maxDistance = Math.sqrt(Math.pow(approxCentre[0]-farCorner[0],2)+Math.pow(approxCentre[1]-farCorner[1],2));
		SectorROI sector = new SectorROI(approxCentre[0], approxCentre[1], 0, maxDistance, 0, 2*Math.PI);

		Dataset[] profile = ROIProfile.sector(image, null, sector, true, false, false, null, XAxis.PIXEL, false);

		final Dataset y = profile[0];

		for (int i = 0 ; i < CENTRE_MASK_RADIUS ; i++) {
			y.set(0, i);
		}

		final Dataset x = DatasetFactory.createRange(y.getSize(), Dataset.INT32);

		int max = spacings.size() > MAX_RINGS ? MAX_RINGS : spacings.size();

		double[] dSpace = new double[max];

		for (int i = 0; i < max; i++) dSpace[i] = spacings.get(i).getDNano()*10;

		final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, dSpace, pixel);

		final List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();

		for (int i = 0; i < dSpace.length; i++) {
			ellipses.add(new EllipticalROI(dSpaceRadiusMap.get(dSpace[i]), approxCentre[0],approxCentre[1]));
		}

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

		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		EllipticalROI roi = null;
		int i = 0;
		double corFact = 0;
		double lastAspect = 1;
		double lastAngle = 0;
		for (EllipticalROI e : ellipses) {
			double startSemi = e.getSemiAxis(0);
			e.setSemiAxis(0, startSemi+corFact);
			e.setSemiAxis(1, (startSemi+corFact)/lastAspect);
			e.setAngle(lastAngle);

			try {
				roi = ellipsePeakFit(image, null, e, inner[i], outer[i], 386, null);
			} catch (Exception ex) {
				roi = null;
			} 

			if (roi != null) {
				foundEllipses.add(new ResolutionEllipseROI(roi, dSpace[i]));
				corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
				lastAspect = ((EllipticalROI) roi).getAspectRatio();
				lastAngle = ((EllipticalROI) roi).getAngle();
			}
			i++;
		}

		return foundEllipses;

	}
	
	public static EllipticalROI ellipsePeakFit(Dataset image, BooleanDataset mask,
			EllipticalROI roi, double innerDelta, double outerDelta, int nPoints, IMonitor mon) {
		
		PolylineROI points;
		EllipticalFitROI efroi;
		
		EllipticalROI[] inOut = new EllipticalROI[2];

		inOut[0] = roi.copy();
		inOut[0].setSemiAxis(0, roi.getSemiAxis(0)-innerDelta);
		inOut[0].setSemiAxis(1, roi.getSemiAxis(1)-innerDelta);

		inOut[1] = roi.copy();
		inOut[1].setSemiAxis(0, roi.getSemiAxis(0)+outerDelta);
		inOut[1].setSemiAxis(1, roi.getSemiAxis(1)+outerDelta);
		if (mon != null) mon.subTask("Find POIs near initial ellipse");
		
		points = PeakFittingEllipseFinder.findPointsOnConic(image, mask, roi, inOut,nPoints,null);
		
		if (points.getNumberOfPoints() < 3) {
			throw new IllegalArgumentException("Could not find enough points to trim");
		}
		if (mon != null) mon.subTask("Trim POIs");
		efroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 2, false);

		return efroi;
	}
	
	protected static EllipseFindingStructure getResolutionEllipses(Dataset image,
			List<HKL> spacings, double pxSize, SimpleCalibrationParameterModel params, int centreMaskRadius, IMonitor monitor) {
		int[] shape = image.getShape();
		if (monitor != null) monitor.subTask("Finding approximate centre...");
		double[] approxCentre = CentreGuess.guessCentre(image);
		
		double[] farCorner = new double[]{0,0};
		if (approxCentre[0] < shape[0]/2.0) farCorner[0] = shape[0];
		if (approxCentre[1] < shape[1]/2.0) farCorner[1] = shape[1];
		
		int nBins = AbstractPixelIntegration.calculateNumberOfBins(approxCentre, shape);
		if (monitor != null)  monitor.subTask("Integrating image...");
		
		DiffractionCrystalEnvironment ce = new DiffractionCrystalEnvironment();
		//fill with harmless junk values except for beam centre
		DetectorProperties dp = new DetectorProperties(100, 0, 0, 100, 100, 0.1,0.1);
		dp.setBeamCentreCoords(approxCentre);
		DiffractionMetadata md = new DiffractionMetadata("", dp, ce);
		NonPixelSplittingIntegration npsi = new NonPixelSplittingIntegration(md, nBins);
		npsi.setAxisType(XAxis.PIXEL);
		
		List<Dataset> integration = npsi.integrate(image);
		final Dataset x = integration.get(0);
		final Dataset y = integration.get(1);
		
		if (monitor != null) if (monitor != null && monitor.isCancelled()) return null;
		
		//TODO should look at the x axis
		for (int i = 0 ; i < centreMaskRadius ; i++) {
			y.set(0, i);
		}
		
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
		
		if (monitor != null) monitor.subTask("Matching to standard...");
		final Map<Double,Double> dSpaceRadiusMap = BruteStandardMatcher.bruteForceMatchStandards(x, y, fullDSpace, pxSize);
		
		if (monitor != null && monitor.isCancelled()) return null;
		
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
	
	protected static List<ResolutionEllipseROI> getFittedResolutionROIs(ICalibrationUIProgressUpdate ui, EllipseFindingStructure efs,
			Dataset data, int centreMaskRadius,int minSpacing, int nPoints, IMonitor monitor) {
		if (monitor != null) monitor.subTask("Finding ellipses...");
		final List<ResolutionEllipseROI> foundEllipses = new ArrayList<ResolutionEllipseROI>();
		if (monitor != null && monitor.isCancelled()) return null;
		IROI roi = null;
		
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
		
			try {
				roi = PowderCalibration.ellipsePeakFit(data, null, e,efs.innerSearch[i],efs.outerSearch[i] ,nPoints, monitor);
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
				roi = null;
			} 
			if (monitor != null && monitor.isCancelled()) return null;
			if (roi != null) {
				ResolutionEllipseROI r = new ResolutionEllipseROI((EllipticalROI)roi, e.getResolution());
				r.setPoints(((EllipticalFitROI)roi).getPoints());
				foundEllipses.add(r);
				corFact = ((EllipticalROI)roi).getSemiAxis(0) - startSemi;
				lastAspect = ((EllipticalROI) roi).getAspectRatio();
				lastAngle = ((EllipticalROI) roi).getAngle();
				if (ui != null) ui.drawFoundRing(roi);
			}
		}
		
		if (foundEllipses.size() < 2) return null;
		
		return foundEllipses;
	}
	
	protected static class EllipseFindingStructure {
		List<ResolutionEllipseROI> ellipses;
		double[] innerSearch;
		double[] outerSearch;
	}
	
	public static IDiffractionMetadata createMetadataFromOutput(CalibrationOutput output,int i, int[] shape, double pxSize) {

		DetectorProperties dp = DetectorProperties.getDefaultDetectorProperties(shape);
		dp.setHPxSize(pxSize);
		dp.setVPxSize(pxSize);

		dp.setBeamCentreDistance(output.getDistance().getDouble(i));
		double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
		dp.setBeamCentreCoords(bc);

		dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
		DiffractionCrystalEnvironment de = new DiffractionCrystalEnvironment(output.getWavelength());
		
		return new DiffractionMetadata("FromCalibration", dp, de);
	}
	
	public static void updateMetadataFromOutput(final IDiffractionMetadata md , final CalibrationOutput output, final int i) {

		DetectorProperties dp = md.getDetector2DProperties();

		dp.setBeamCentreDistance(output.getDistance().getDouble(i));
		double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
		dp.setBeamCentreCoords(bc);

		dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
		md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
	}
	

}
