package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.roi.IParametricROI;
import org.eclipse.dawnsci.analysis.api.roi.IPolylineROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalFitROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolylineROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.BooleanDataset;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IndexIterator;
import org.eclipse.january.dataset.IntegerDataset;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.dataset.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.HKL;
import uk.ac.diamond.scisoft.analysis.diffraction.DSpacing;
import uk.ac.diamond.scisoft.analysis.diffraction.PeakFittingEllipseFinder;
import uk.ac.diamond.scisoft.analysis.diffraction.PowderRingsUtils;
import uk.ac.diamond.scisoft.analysis.diffraction.ResolutionEllipseROI;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.AbstractPixelIntegration;
import uk.ac.diamond.scisoft.analysis.diffraction.powder.NonPixelSplittingIntegration;
import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.XAxis;

public class PowderCalibration {
	
	private static final int MAX_RINGS = 10;
	private static final int CENTRE_MASK_RADIUS = 50;
	private static final int NUMBER_OF_POINTS = 256;
	private static final int MINIMUM_SPACING = 10;
	
	private static final String description = "Automatic powder diffraction image calibration using ellipse parameters";
	private static final String descriptionManualEllipse = "Manual powder diffraction image calibration using ellipse parameters";
	private static final String descriptionManualPoints = "Manual powder diffraction image calibration using point parameters";
	private static final String addition = " with final point-based optimisation";
	
	private static final String descEllipse = "Reference for ellipse parameter calibration routine";
	
	private static final String doiEllipse ="10.1107/S0021889813022437";
	
	private static final String bibtexEllipse = "@article{hart2013complete, "+
		 "title={Complete elliptical ring geometry provides energy and instrument calibration for synchrotron-based two-dimensional X-ray diffraction},"+
		  "author={Hart, Michael L and Drakopoulos, Michael and Reinhard, Christina and Connolley, Thomas},"+
		 "journal={Journal of applied crystallography},"+
		  "volume={46},"+
		  "number={5},"+
		  "pages={1249--1260},"+
		  "year={2013},"+
		  "publisher={International Union of Crystallography}"+
		"}";
	
	private static final String endnoteEllipse = "%0 Journal Article" +
			"%T Complete elliptical ring geometry provides energy and instrument calibration for synchrotron-based two-dimensional X-ray diffraction"+
			"%A Hart, Michael L"+
			"%A Drakopoulos, Michael"+
			"%A Reinhard, Christina"+
			"%A Connolley, Thomas"+
			"%J Journal of applied crystallography"+
			"%V 46"+
			"%N 5"+
			"%P 1249-1260"+
			"%@ 0021-8898"+
			"%D 2013"+
			"%I International Union of Crystallography";
	
	private final static Logger logger = LoggerFactory.getLogger(PowderCalibration.class);
	
	public static CalibrationOutput calibrateKnownWavelength(Dataset image, double wavelength, double pixel, List<HKL> spacings, int nRings) {
		
		return calibrateKnownWavelength(image, wavelength, pixel, spacings, nRings, null);
	}
	
	public static CalibrationOutput calibrateKnownWavelength(Dataset image, double wavelength, double pixel, List<HKL> spacings, int nRings,  PowderCalibrationInfoImpl info) {

		int[] options = new int[]{CENTRE_MASK_RADIUS, MINIMUM_SPACING, NUMBER_OF_POINTS};
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setFloatEnergy(false);
		params.setIsPointCalibration(true);
		params.setAutomaticCalibration(true);
		
		return calibrateMultipleImages(new IDataset[] {image}, DatasetFactory.zeros(DoubleDataset.class, new int[]{1}), pixel,
				spacings,  wavelength, options, params, null, null, info == null ? null : new PowderCalibrationInfoImpl[]{info});
	}
	
	public static CalibrationOutput calibrateSingleImage(Dataset image, double pixel, List<HKL> spacings, int nRings) {
		
		return  calibrateSingleImage(image,pixel,spacings,nRings,null);
	}
	
	public static CalibrationOutput calibrateSingleImage(Dataset image, double pixel, List<HKL> spacings, int nRings, PowderCalibrationInfoImpl info) {
		
		int[] options = new int[]{CENTRE_MASK_RADIUS, MINIMUM_SPACING, NUMBER_OF_POINTS};
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setIsPointCalibration(true);
		
		return calibrateMultipleImages(new IDataset[] {image}, DatasetFactory.zeros(DoubleDataset.class, new int[]{1}), pixel,
				spacings,  0, options, params, null, null, info == null ? null : new PowderCalibrationInfoImpl[]{info});
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
			
			final EllipseFindingStructure efs = getResolutionEllipses(imds, spacings, pxSize, params,options[0], mon);

			if (mon != null && mon.isCancelled()) return null;

			if (efs == null) throw new IllegalArgumentException("No rings found!");

			List<ResolutionEllipseROI> foundEllipses = getFittedResolutionROIs(uiUpdate, efs, DatasetUtils.convertToDataset(image),options[0],options[1],options[2],mon);

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
		
		if (allEllipses.size() == 1 && params.isPointCalibration()) {
			
			IDiffractionMetadata meta = createMetadataFromOutput(output, 0, images[0].getShape(),pxSize);
			
			SimpleCalibrationParameterModel paramModel = params;
			
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
			info[i].setResultDescription(output.getCalibrationOutputDescription());
			
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

		final Dataset x = DatasetFactory.createRange(IntegerDataset.class,y.getSize());

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
		
		if (points == null || points.getNumberOfPoints() < 3) {
			throw new IllegalArgumentException("Could not find enough points to trim");
		}
		if (mon != null) mon.subTask("Trim POIs");
		efroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 2, false);
		
		EllipticalFitROI cfroi = PowderRingsUtils.fitAndTrimOutliers(null, points, 100, true);
		
		
		double dma = efroi.getSemiAxis(0)-cfroi.getSemiAxis(0);
		double dmi = efroi.getSemiAxis(1)-cfroi.getSemiAxis(0);
		
		double crms = Math.sqrt((dma*dma + dmi*dmi)/2);
		double rms = efroi.getRMS();
		
		if (crms < rms) {
			efroi = cfroi;
			logger.warn("SWITCHING TO CIRCLE - RMS SEMIAX-RADIUS {} < FIT RMS {}",crms,rms);
		}

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
			if (efs.innerSearch[i] < minSpacing || efs.outerSearch[i] < minSpacing) {
				if (monitor != null) monitor.subTask("Separation lower than minimum required! (Minimum can be changed in calibration settings)");
				logger.warn("Actual spacing smaller than minimum, inner: " +efs.innerSearch[i] + ", outer: "+  efs.outerSearch[i] + ", min: " + minSpacing );
				continue;
			}
			
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
	
	public static void updateMetadataFromOutput(final IDiffractionMetadata md , final CalibrationOutput output, final int i, Double roll) {

		//TODO clone dp and copy over (use setGeometry on dp).
		DetectorProperties dp = md.getDetector2DProperties();

		dp.setBeamCentreDistance(output.getDistance().getDouble(i));
		double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
		dp.setBeamCentreCoords(bc);

		dp.setNormalAnglesInDegrees(output.getTilt().getDouble(i)*-1, 0, output.getTiltAngle().getDouble(i)*-1);
		md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
		if (roll != null) setDetectorFastAxisAngle(md.getDetector2DProperties(),roll);
		
//		Specifying fast axis orientation
//		double fastAngleDegrees = 30;
//
//		DetectorProperties dp = md.getDetector2DProperties();
//		dp.setNormalAnglesInDegrees(0,0,0);
//		dp.setDetectorDistance(100);
//		dp.setBeamCentreCoords(new double[]{0,0});
//
//		double[] bc = new double[] {output.getBeamCentreX().getDouble(i),output.getBeamCentreY().getDouble(i) };
//		dp.setOrientationEulerZYZ(Math.toRadians(-fastAngleDegrees), Math.toRadians(output.getTilt().getDouble(i)),Math.toRadians(output.getTiltAngle().getDouble(i)));
//		dp.setBeamCentreCoords(bc);
//		double offset = fastAngleDegrees - dp.getNormalAnglesInDegrees()[2];
//		dp.setOrientationEulerZYZ(Math.toRadians(-fastAngleDegrees - offset), Math.toRadians(output.getTilt().getDouble(i)),Math.toRadians(output.getTiltAngle().getDouble(i)));
//		dp.setBeamCentreCoords(bc);
//		dp.setBeamCentreDistance(output.getDistance().getDouble(i));
//		
//		md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
	}
	
	public static void setDetectorFastAxisAngle(DetectorProperties dp, double fastAngleDegrees){
//		Specifying fast axis orientation
//		double fastAngleDegrees = 30;
		
		double[] beamCentre = dp.getBeamCentreCoords();
		double[] normalAngles = dp.getNormalAnglesInDegrees();
		double beamCentreDistance = dp.getBeamCentreDistance();
		
		dp.setNormalAnglesInDegrees(0,0,0);
		dp.setDetectorDistance(100);
		dp.setBeamCentreCoords(new double[]{0,0});

		double[] bc = new double[] {beamCentre[0], beamCentre[1]};
		dp.setOrientationEulerZYZ(Math.toRadians(-fastAngleDegrees), Math.toRadians(normalAngles[0]),Math.toRadians(normalAngles[2]));
		dp.setBeamCentreCoords(bc);
		double offset = fastAngleDegrees - dp.getNormalAnglesInDegrees()[2];
		dp.setOrientationEulerZYZ(Math.toRadians(-fastAngleDegrees - offset), Math.toRadians(normalAngles[0]),Math.toRadians(normalAngles[2]));
		dp.setBeamCentreCoords(bc);
		dp.setBeamCentreDistance(beamCentreDistance);
		
//		md.getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());
	}
	
	
	public static CalibrationOutput manualCalibrateKnownWavelength(Dataset image, double wavelength, double pixel, List<HKL> spacings, int nRings,  PowderCalibrationInfoImpl info) {
		
		int[] options = new int[]{CENTRE_MASK_RADIUS, MINIMUM_SPACING, NUMBER_OF_POINTS};
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setFloatEnergy(false);
		params.setIsPointCalibration(true);
		params.setAutomaticCalibration(true);
		
		DiffractionImageData imdata = new DiffractionImageData();
		imdata.setImage(image);
//		imdata.se
		
//		findPointsOfInterest()
		
		return null;
	}
	
	public static void findPointsOfInterest(DiffractionImageData currentData, SimpleCalibrationParameterModel model, ICalibrationUIProgressUpdate uiUpdater, List<HKL> hkls, IMonitor monitor, int minSpacing, int nPoints) {
		int maxSize = model.getMaxSearchSize();
		
		final List<IROI> resROIs = DSpacing.getResolutionRings(currentData.getMetaData(),hkls);
		
		currentData.clearROIs();
		currentData.setUse(false);
		currentData.setNrois(0);
		
		if (uiUpdater != null) uiUpdater.removeRings();
		
		int numberToFit = resROIs.size();
		
		if (!model.isUseRingSet()) {
			numberToFit = Math.min(resROIs.size(), model.getNumberOfRings());
		}
		
//		int minSpacing = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
//		int nPoints = Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
		int n = 0;
		for (int i = 0; i < resROIs.size(); i++) {
			IROI r = resROIs.get(i);
			IROI roi = null;
			try {
				if (i >= numberToFit) continue;
				if (monitor != null && monitor.isCancelled()) continue;
				
				
				if (model.isUseRingSet() && !model.getRingSet().contains(i+1)) continue;
				
				if (r instanceof IParametricROI) {
					try {
					roi = DSpacing.fitParametricROI(resROIs,(IParametricROI)r, currentData.getImage(), i, minSpacing, nPoints, maxSize, monitor);
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
	}
	
	public static CalibrationOutput manualCalibrateMultipleImagesEllipse(List<DiffractionImageData> images, Dataset ddist, double pixelSize,
			List<HKL> spacings, SimpleCalibrationParameterModel params) {
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<>();
		List<double[]> allDSpacings = new ArrayList<>();

		for (DiffractionImageData data : images) {
			
			int n = data.getROISize();
			if (n != spacings.size()) { // always allow a choice to be made
				throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
			}

			int totalNonNull = data.getNonNullROISize();

			double[] ds = new double[totalNonNull];
			List<EllipticalROI> erois = new ArrayList<EllipticalROI>(totalNonNull);

			int count = 0;
			for (int i = 0; i < data.getROISize(); i++) {
				IROI roi = data.getRoi(i);
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
		
		CalibrationOutput o = null;
		
		if (!params.isFloatEnergy()) {
			o =  CalibrateEllipses.runKnownWavelength(allEllipses, allDSpacings, pixelSize,images.get(0).getMetaData().getDiffractionCrystalEnvironment().getWavelength());
		} else if (!params.isFloatDistance()){
			o =  CalibrateEllipses.runKnownDistance(allEllipses, allDSpacings, pixelSize,images.get(0).getMetaData().getDetector2DProperties().getBeamCentreDistance());
		}else {
			o =  CalibrateEllipses.run(allEllipses, allDSpacings,ddist, pixelSize);
		}
		
		final CalibrationOutput output = o;
		
		double[] fullDSpace = new double[spacings.size()];
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);
		
		PowderCalibrationInfoImpl[] info = new PowderCalibrationInfoImpl[images.size()];
		
		int count = 0;
		for (DiffractionImageData data : images) {

			info[count++] = createPowderCalibrationInfo(data,true);
		}
		
		for (int i = 0; i < allEllipses.size(); i++) {
			int[] infoIndex = new int[allDSpacings.get(i).length];
			
			for (int j = 0; j < infoIndex.length; j++) {
				infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(0)[j])).argMin();
			}
			Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
			
			info[i].setPostCalibrationInformation(descriptionManualEllipse, infoSpace, infoSpaceds, output.getResidual());
			info[i].setResultDescription(output.getCalibrationOutputDescription());
			
		}
		output.setCalibrationInfo(info);
		
		return output;
	}
	
	public static CalibrationOutput manualCalibrateMultipleImagesPoints(DiffractionImageData image, List<HKL> spacings,
			SimpleCalibrationParameterModel params, IMonitor mon, ICalibrationUIProgressUpdate uiUpdate) {
		
		
		int n = image.getROISize();
		if (n != spacings.size()) { // always allow a choice to be made
			throw new IllegalArgumentException("Number of ellipses should be equal to spacings");
		}
		
		int totalNonNull = image.getNonNullROISize();
		
		double[] ds = new double[totalNonNull];
		List<IPolylineROI> erois = new ArrayList<IPolylineROI>(totalNonNull);
		
		int count = 0;
		for (int i = 0; i < image.getROISize(); i++) {
			IROI roi = image.getRoi(i);
			if (roi != null) {
				ds[count]  = spacings.get(i).getDNano()*10;
				
				if (roi instanceof IPolylineROI) {
					erois.add((IPolylineROI) roi);
				} else if (roi instanceof EllipticalFitROI) {
					erois.add(((EllipticalFitROI) roi).getPoints());
				} else {
					throw new IllegalArgumentException("ROI not elliptical fit");
				}
				count++;
			}
		}
		
		List<List<IPolylineROI>> allEllipses = new ArrayList<List<IPolylineROI>> ();
		allEllipses.add(erois);
		List<double[]> allDSpacings = new ArrayList<double[]>();
		allDSpacings.add(ds);
		
		CalibrationOutput o = CalibratePoints.run(allEllipses.get(0), allDSpacings.get(0), image.getMetaData(),params);

		final CalibrationOutput output = o;
		
		double[] fullDSpace = new double[spacings.size()];
		
		for (int i = 0; i< spacings.size(); i++) fullDSpace[i] = spacings.get(i).getDNano()*10;
		Dataset infoSpace = DatasetFactory.createFromObject(fullDSpace);
		int[] infoIndex = new int[allDSpacings.get(0).length];
		
		for (int j = 0; j < infoIndex.length; j++) {
			infoIndex[j] = Maths.abs(Maths.subtract(infoSpace, allDSpacings.get(0)[j])).argMin();
		}
		Dataset infoSpaceds = DatasetFactory.createFromObject(infoIndex);
		
		PowderCalibrationInfoImpl info = createPowderCalibrationInfo(image, false);
		
		info.setPostCalibrationInformation(description, infoSpace, infoSpaceds, output.getResidual());
		info.setResultDescription(output.getCalibrationOutputDescription());
		output.setCalibrationInfo(new IPowderCalibrationInfo[]{info});
		
		DetectorProperties dp = image.getMetaData().getDetector2DProperties();

		double roll = dp.getNormalAnglesInDegrees()[2];

		image.getMetaData().getDiffractionCrystalEnvironment().setWavelength(output.getWavelength());

		dp.setBeamCentreDistance(output.getDistance().getDouble(0));
		double[] bc = new double[] {output.getBeamCentreX().getDouble(0),output.getBeamCentreY().getDouble(0) };
		dp.setBeamCentreCoords(bc);

		dp.setNormalAnglesInDegrees(output.getTilt().getDouble(0)*-1, 0, output.getTiltAngle().getDouble(0)*-1);

		if (params.isFixDetectorRoll()) {
			PowderCalibration.setDetectorFastAxisAngle(dp, roll);
		}
		
		return output;
	}
	
	public static CalibrationOutput calibrateSingleImageManualPoint(Dataset image, List<HKL> spacings, int nRings,IDiffractionMetadata metadata, boolean fixEnergy) {
		return calibrateSingleImageManualPoint(image, spacings, nRings, metadata, fixEnergy, MINIMUM_SPACING, NUMBER_OF_POINTS);
	}
	
	public static CalibrationOutput calibrateSingleImageManualPoint(Dataset image, List<HKL> spacings, int nRings,IDiffractionMetadata metadata, boolean fixEnergy, int minSpacing, int nPoints) {
		
		SimpleCalibrationParameterModel params = new SimpleCalibrationParameterModel();
		params.setNumberOfRings(nRings);
		params.setIsPointCalibration(true);
		params.setFloatEnergy(!fixEnergy);
		
		return calibrateSingleImageManualPoint(image, spacings, metadata, params, minSpacing, nPoints);
	}
	
public static CalibrationOutput calibrateSingleImageManualPoint(Dataset image, List<HKL> spacings,IDiffractionMetadata metadata, SimpleCalibrationParameterModel model, int minSpacing, int nPoints) {
		
		DiffractionImageData imdata = new DiffractionImageData();
		imdata.setImage(image);
		imdata.setMetaData(metadata);
		
		
		findPointsOfInterest(imdata, model, null, spacings, null, minSpacing, nPoints);
		
		return manualCalibrateMultipleImagesPoints(imdata, spacings, model, (IMonitor)null, (ICalibrationUIProgressUpdate)null);
	}
	
	private static PowderCalibrationInfoImpl createPowderCalibrationInfo(DiffractionImageData data, boolean ellipse) {
		PowderCalibrationInfoImpl info = new PowderCalibrationInfoImpl(CalibrationFactory.getCalibrationStandards().getSelectedCalibrant(),
				data.getPath() + data.getName(), "detector");
		
		if (!ellipse) return info;
		
		info.setCitationInformation(new String[]{descEllipse,doiEllipse,endnoteEllipse,bibtexEllipse});
		return info;
	}
}
