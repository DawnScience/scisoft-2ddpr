package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Arrays;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.roi.IPolylineROI;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.diffraction.QSpace;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CoordinatesIterator;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheOptimizer;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheOptimizer.Optimizer;

public class CalibratePoints {

	private final static Logger logger = LoggerFactory.getLogger(CalibratePoints.class);
	
	
	public static CalibrationOutput run(List<IPolylineROI> allEllipses, double[] allDSpacings, final IDiffractionMetadata md, final CalibratePointsParameterModel paramModel) {
		
//		if (allEllipses.size() < 2) throw new IllegalArgumentException("Need more than 1 ellipse");
		if (allDSpacings.length ==  0 || allEllipses.size() != allDSpacings.length) throw new IllegalArgumentException("Number of ellipses must equal number of d-spacings");
		
		
		int total = 0;
		
		for (IPolylineROI roi : allEllipses) {
			total += roi.getNumberOfPoints();
		}
		
		final DoubleDataset qd = new DoubleDataset(new int[]{total});
		final DoubleDataset xd = new DoubleDataset(new int[]{total});
		final DoubleDataset yd = new DoubleDataset(new int[]{total});
		int k = 0;
		for (int i = 0; i < allEllipses.size(); i++) {
			IPolylineROI roi = allEllipses.get(i);
			double q = (2*Math.PI)/allDSpacings[i];
			for (int j = 0; j < roi.getNumberOfPoints(); j++) {
				IROI p = roi.getPoint(j);
				xd.set(p.getPointX(), k);
				yd.set(p.getPointY(), k);
				qd.set(q, k);
				k++;
			}
		}
		
		DetectorFunction df = new CalibratePoints(). new DetectorFunction(paramModel.getNumberOfFloatingParameters(),paramModel, md, qd, xd, yd);
		ApacheOptimizer lma = new ApacheOptimizer(Optimizer.LEVENBERG_MARQUARDT);

		try {
			lma.optimize(new IDataset[]{DatasetFactory.createRange(qd.getSize(),Dataset.FLOAT64)}, qd,df);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}

		IDiffractionMetadata outMd = paramModel.getMetadata(df.getParameterValues(), md);

		if (lma.guessParametersErrors() != null) {
			logger.info("Guess errors: " + Arrays.toString(lma.guessParametersErrors()));
		}
		
		return new CalibrationOutput(outMd.getDiffractionCrystalEnvironment().getWavelength(),
									outMd.getDetector2DProperties().getBeamCentreCoords()[0],
									outMd.getDetector2DProperties().getBeamCentreCoords()[1],
									outMd.getDetector2DProperties().getNormalAnglesInDegrees()[0]*-1,
									outMd.getDetector2DProperties().getNormalAnglesInDegrees()[2]*-1,
									outMd.getDetector2DProperties().getBeamCentreDistance(),
									df.residual(true, qd, null, new IDataset[]{yd})/yd.getSize(),lma.guessParametersErrors());
	}
	
	public class DetectorFunction extends AFunction {

		private static final long serialVersionUID = 1L;
		
		private CalibratePointsParameterModel model;
		private IDiffractionMetadata md;
		private Dataset qd;
		private Dataset xd;
		private Dataset yd;

		public DetectorFunction(int nParms, CalibratePointsParameterModel paramModel, IDiffractionMetadata md, Dataset qd, Dataset xd, Dataset yd) {
			super(nParms);
			this.model = paramModel;
			this.md = md;
			this.yd = yd;
			this.xd = xd;
			this.qd = qd;
			setParameterValues(paramModel.getInitialParams(md));
		}

		@Override
		protected void setNames() {
		}

		@Override
		public double val(double... values) {
			IDiffractionMetadata argMd = model.getMetadata(getParameterValues(), md);
			
			QSpace q = new QSpace(argMd.getDetector2DProperties(), argMd.getDiffractionCrystalEnvironment());
			
			return q.qFromPixelPosition(xd.getDouble((int)values[0]), yd.getDouble((int)values[0])).length();
		}

		@Override
		public void fillWithValues(DoubleDataset data, CoordinatesIterator it) {

			IDiffractionMetadata argMd = model.getMetadata(getParameterValues(), md);
			QSpace q = new QSpace(argMd.getDetector2DProperties(), argMd.getDiffractionCrystalEnvironment());
			
			DoubleDataset qOut = new DoubleDataset(qd);
			
			for (int i = 0 ; i < qOut.getSize(); i++) {
				data.set(q.qFromPixelPosition(xd.getDouble(i), yd.getDouble(i)).length(), i);
			}
			it.reset();
		}
		
	}
	
}
