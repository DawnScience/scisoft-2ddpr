package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;

import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;

public class SimpleCalibrationParameterModel {

	public static final int CENTRE_MASK_RADIUS = 50;
	public static final int MINIMUM_SPACING = 10;
	public static final int NUMBER_OF_POINTS = 256;
	public static final int MAX_SIZE = 50;
	public static final boolean FIX_DETECTOR_ROLL = false;

	private EllipseOptions ellipseOptions = new EllipseOptions();
	private PointOptions pointOptions = new PointOptions();

	private boolean isPointCalibration = false;
	private boolean automaticCalibration = false;

	private int nPointsPerRing = NUMBER_OF_POINTS;
	private int minimumSpacing = MINIMUM_SPACING;
	private int nIgnoreCentre = CENTRE_MASK_RADIUS;
	private int maxSearchSize = MAX_SIZE;
	private boolean fixDetectorRoll = FIX_DETECTOR_ROLL;
	

	private int numberOfRings;
	private Set<Integer> ringSet;

	public SimpleCalibrationParameterModel() {
	}

	public SimpleCalibrationParameterModel(SimpleCalibrationParameterModel toCopy) {
		this.pointOptions = new PointOptions(toCopy.pointOptions);
		this.ellipseOptions = new EllipseOptions(toCopy.ellipseOptions);
		this.isPointCalibration = toCopy.isPointCalibration;
		this.automaticCalibration = toCopy.automaticCalibration;
		this.numberOfRings = toCopy.numberOfRings;
		this.ringSet = ringSet == null ? null : new TreeSet<Integer>(ringSet);
	}

	public boolean isAutomaticCalibration() {
		return automaticCalibration;
	}

	public void setAutomaticCalibration(boolean isAutomaticCalibration) {
		this.automaticCalibration = isAutomaticCalibration;
	}

	public boolean isFloatDistance() {
		return (!automaticCalibration && isPointCalibration) ? pointOptions.floatDistance
				: ellipseOptions.floatDistance;
	}

	public boolean isFloatEnergy() {
		return (!automaticCalibration && isPointCalibration) ? pointOptions.floatEnergy : ellipseOptions.floatEnergy;
	}

	public boolean isMasked() {
		return (!automaticCalibration && isPointCalibration) ? pointOptions.isMasked : false; // masking not yet
																								// supported for ellipse
																								// calculations
	}

	public void setFloatEnergy(boolean floatEnergy) {
		if (!automaticCalibration && isPointCalibration) {
			pointOptions.floatEnergy = floatEnergy;
		} else {

			ellipseOptions.floatEnergy = floatEnergy;
			ellipseOptions.floatDistance = !floatEnergy ? !floatEnergy : ellipseOptions.floatDistance;
		}
	}

	public void setFloatDistance(boolean floatDistance) {
		if (!automaticCalibration && isPointCalibration) {
			pointOptions.floatDistance = floatDistance;
		} else {
			ellipseOptions.floatDistance = floatDistance;
			ellipseOptions.floatEnergy = !floatDistance ? !floatDistance : ellipseOptions.floatDistance;
		}
	}

	public void setIsMasked(boolean useMask) {
		if (!automaticCalibration && isPointCalibration) {
			pointOptions.isMasked = useMask;
		}
	}

	public void setNumberOfRings(int numberOfRings) {
		this.numberOfRings = numberOfRings;
	}

	public int getNumberOfRings() {
		return numberOfRings;
	}

	public void setRingSet(Set<Integer> ringSet) {
		this.ringSet = ringSet;
	}

	public Set<Integer> getRingSet() {
		return ringSet;
	}

	public boolean isUseRingSet() {
		return !(ringSet == null || ringSet.isEmpty());
	}

	public boolean isPointCalibration() {
		return isPointCalibration;
	}

	public void setIsPointCalibration(boolean optimise) {
		isPointCalibration = optimise;
	}

	public IDiffractionMetadata getMetadata(double[] params, IDiffractionMetadata md) {

		return (!automaticCalibration && isPointCalibration) ? pointOptions.getMetadata(params, md)
				: ellipseOptions.getMetadata(params, md);
	}

	public double[] getInitialParams(IDiffractionMetadata md) {

		return (!automaticCalibration && isPointCalibration) ? pointOptions.getInitialParams(md)
				: ellipseOptions.getInitialParams(md);
	}

	public CalibrationErrorOutput getErrorOutput(double[] errors) {

		if (!automaticCalibration && isPointCalibration)
			return pointOptions.getErrorOutput(errors);

		return ellipseOptions.getErrorOutput(errors);

	}

	public int getNumberOfFloatingParameters() {

		return (!automaticCalibration && isPointCalibration) ? pointOptions.getNumberOfFloatingParameters()
				: ellipseOptions.getNumberOfFloatingParameters();

	}

	public boolean isFloatBeamCentre() {
		return (!automaticCalibration && isPointCalibration) ? pointOptions.floatBeamCentre : true;
	}

	public boolean isFloatTilt() {
		return (!automaticCalibration && isPointCalibration) ? pointOptions.floatTilt : true;
	}

	public void setFloatBeamCentre(boolean floatBeamCentre) {
		if (!automaticCalibration && isPointCalibration) {
			pointOptions.floatBeamCentre = floatBeamCentre;
		}
	}

	public void setFloatTilt(boolean floatTilt) {
		if (!automaticCalibration && isPointCalibration) {
			pointOptions.floatTilt = floatTilt;
		}
	}

	public boolean isEllipseCalibration() {
		return !isPointCalibration;
	}

	public int getnPointsPerRing() {
		return nPointsPerRing;
	}

	public void setnPointsPerRing(int nPointsPerRing) {
		this.nPointsPerRing = nPointsPerRing;
	}

	public int getMinimumSpacing() {
		return minimumSpacing;
	}

	public void setMinimumSpacing(int minimumSpacing) {
		this.minimumSpacing = minimumSpacing;
	}

	public int getnIgnoreCentre() {
		return nIgnoreCentre;
	}

	public void setnIgnoreCentre(int nIgnoreCentre) {
		this.nIgnoreCentre = nIgnoreCentre;
	}

	public int getMaxSearchSize() {
		return maxSearchSize;
	}

	public void setMaxSearchSize(int maxSearchSize) {
		this.maxSearchSize = maxSearchSize;
	}

	public boolean isFixDetectorRoll() {
		return fixDetectorRoll;
	}

	public void setFixDetectorRoll(boolean fixDetectorRoll) {
		this.fixDetectorRoll = fixDetectorRoll;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ellipseOptions == null) ? 0 : ellipseOptions.hashCode());
		result = prime * result + (automaticCalibration ? 1231 : 1237);
		result = prime * result + (isPointCalibration ? 1231 : 1237);
		result = prime * result + maxSearchSize;
		result = prime * result + minimumSpacing;
		result = prime * result + nIgnoreCentre;
		result = prime * result + nPointsPerRing;
		result = prime * result + numberOfRings;
		result = prime * result + ((pointOptions == null) ? 0 : pointOptions.hashCode());
		result = prime * result + ((ringSet == null) ? 0 : ringSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleCalibrationParameterModel other = (SimpleCalibrationParameterModel) obj;
		if (ellipseOptions == null) {
			if (other.ellipseOptions != null)
				return false;
		} else if (!ellipseOptions.equals(other.ellipseOptions))
			return false;
		if (automaticCalibration != other.automaticCalibration)
			return false;
		if (isPointCalibration != other.isPointCalibration)
			return false;
		if (maxSearchSize != other.maxSearchSize)
			return false;
		if (minimumSpacing != other.minimumSpacing)
			return false;
		if (nIgnoreCentre != other.nIgnoreCentre)
			return false;
		if (nPointsPerRing != other.nPointsPerRing)
			return false;
		if (numberOfRings != other.numberOfRings)
			return false;
		if (pointOptions == null) {
			if (other.pointOptions != null)
				return false;
		} else if (!pointOptions.equals(other.pointOptions))
			return false;
		if (ringSet == null) {
			if (other.ringSet != null)
				return false;
		} else if (!ringSet.equals(other.ringSet))
			return false;
		return true;
	}

	private class EllipseOptions {

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (floatDistance ? 1231 : 1237);
			result = prime * result + (floatEnergy ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EllipseOptions other = (EllipseOptions) obj;
			if (floatDistance != other.floatDistance)
				return false;
			if (floatEnergy != other.floatEnergy)
				return false;
			return true;
		}

		public boolean floatEnergy = true;
		public boolean floatDistance = true;

		public EllipseOptions() {
		}

		public EllipseOptions(EllipseOptions options) {
			this.floatEnergy = options.floatEnergy;
			this.floatDistance = options.floatDistance;
		}

		public CalibrationErrorOutput getErrorOutput(double[] errors) {

			if (errors == null)
				return null;

			int count = 0;

			if (floatEnergy)
				count++;
			if (floatDistance)
				count++;
			count += 4;

			CalibrationErrorOutput ceo = new CalibrationErrorOutput();

			if (errors.length != count)
				return null;

			count = 0;

			if (floatEnergy)
				ceo.setWavelength(errors[count++]);
			if (floatDistance)
				ceo.setDistance(errors[count++]);
			ceo.setBeamCentreX(errors[count++]);
			ceo.setBeamCentreY(errors[count++]);

			ceo.setTilt(errors[count++]);
			ceo.setTiltAngle(errors[count++]);

			return ceo;
		}

		public int getNumberOfFloatingParameters() {

			if (floatDistance && floatEnergy)
				return 6;

			if (floatDistance || floatEnergy)
				return 5;

			// Should not hit
			return 0;

		}

		public double[] getInitialParams(IDiffractionMetadata md) {

			DiffractionCrystalEnvironment de = md.getDiffractionCrystalEnvironment();
			DetectorProperties detProp = md.getDetector2DProperties();

			int count = 0;

			if (floatEnergy)
				count++;
			if (floatDistance)
				count++;
			count += 4;

			double[] initParam = new double[count];

			count = 0;

			if (floatEnergy)
				initParam[count++] = de.getWavelength();
			if (floatDistance)
				initParam[count++] = detProp.getBeamCentreDistance();

			initParam[count++] = detProp.getBeamCentreCoords()[0];
			initParam[count++] = detProp.getBeamCentreCoords()[1];

			// TODO check if normal angles [2] is zero, if not calc better vals
			initParam[count++] = detProp.getNormalAnglesInDegrees()[0];
			initParam[count++] = detProp.getNormalAnglesInDegrees()[2];

			return initParam;

		}

		public IDiffractionMetadata getMetadata(double[] params, IDiffractionMetadata md) {

			DetectorProperties odp = md.getDetector2DProperties();

			final int heightInPixels = odp.getPy();
			final int widthInPixels = odp.getPx();
			final double pixelHeightInMM = odp.getVPxSize();
			final double pixelWidthInMM = odp.getHPxSize();

			DiffractionCrystalEnvironment odce = md.getDiffractionCrystalEnvironment();
			DiffractionCrystalEnvironment ndce = new DiffractionCrystalEnvironment();

			int count = 0;

			if (floatEnergy) {
				ndce.setWavelength(params[count++]);
			} else {
				ndce.setWavelength(odce.getWavelength());
			}

			double distance = odp.getBeamCentreDistance();
			double[] beamCentre = odp.getBeamCentreCoords();
			double[] normAngle = odp.getNormalAnglesInDegrees();

			if (floatDistance)
				distance = params[count++];

			beamCentre[0] = params[count++];
			beamCentre[1] = params[count++];

			normAngle[0] = params[count++];
			normAngle[1] = params[count++];

			DetectorProperties d = new DetectorProperties(distance, beamCentre[0] * pixelHeightInMM,
					beamCentre[1] * pixelWidthInMM, heightInPixels, widthInPixels, pixelHeightInMM, pixelWidthInMM);
			d.setNormalAnglesInDegrees(normAngle[0], 0, normAngle[1]);

			return new DiffractionMetadata(null, d, ndce);
		}

		private SimpleCalibrationParameterModel getOuterType() {
			return SimpleCalibrationParameterModel.this;
		}
	}

	private class PointOptions extends EllipseOptions {

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (floatBeamCentre ? 1231 : 1237);
			result = prime * result + (floatTilt ? 1231 : 1237);
			result = prime * result + (isMasked ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			PointOptions other = (PointOptions) obj;
			if (floatBeamCentre != other.floatBeamCentre)
				return false;
			if (floatTilt != other.floatTilt)
				return false;
			if (isMasked != other.isMasked)
				return false;
			return true;
		}

		public boolean floatBeamCentre = true;
		public boolean floatTilt = true;
		public boolean isMasked = false;

		public PointOptions() {
		};

		public PointOptions(PointOptions options) {
			this.floatEnergy = options.floatEnergy;
			this.floatDistance = options.floatDistance;
			this.floatBeamCentre = options.floatBeamCentre;
			this.floatTilt = options.floatEnergy; // Is this a typo or deliberate?
			this.isMasked = options.isMasked;
		}

		public CalibrationErrorOutput getErrorOutput(double[] errors) {

			if (errors == null)
				return null;

			int count = 0;

			if (floatEnergy)
				count++;
			if (floatDistance)
				count++;
			if (floatBeamCentre)
				count += 2;
			if (floatTilt)
				count += 2;

			CalibrationErrorOutput ceo = new CalibrationErrorOutput();

			if (errors.length != count)
				return null;

			count = 0;

			if (floatEnergy)
				ceo.setWavelength(errors[count++]);
			if (floatDistance)
				ceo.setDistance(errors[count++]);
			if (floatBeamCentre) {
				ceo.setBeamCentreX(errors[count++]);
				ceo.setBeamCentreY(errors[count++]);
			}
			if (floatTilt) {
				// TODO check if normal angles [2] is zero, if not calc better vals
				ceo.setTilt(errors[count++]);
				ceo.setTiltAngle(errors[count++]);
			}

			return ceo;
		}

		public int getNumberOfFloatingParameters() {

			int count = 0;
			if (floatEnergy)
				count++;
			if (floatDistance)
				count++;
			if (floatBeamCentre)
				count += 2;
			if (floatTilt)
				count += 2;

			return count;

		}

		public double[] getInitialParams(IDiffractionMetadata md) {

			DiffractionCrystalEnvironment de = md.getDiffractionCrystalEnvironment();
			DetectorProperties detProp = md.getDetector2DProperties();

			int count = 0;

			if (floatEnergy)
				count++;
			if (floatDistance)
				count++;
			if (floatBeamCentre)
				count += 2;
			if (floatTilt)
				count += 2;

			double[] initParam = new double[count];

			count = 0;

			if (floatEnergy)
				initParam[count++] = de.getWavelength();
			if (floatDistance)
				initParam[count++] = detProp.getBeamCentreDistance();
			if (floatBeamCentre) {
				initParam[count++] = detProp.getBeamCentreCoords()[0];
				initParam[count++] = detProp.getBeamCentreCoords()[1];
			}
			if (floatTilt) {
				// TODO check if normal angles [2] is zero, if not calc better vals
				initParam[count++] = detProp.getNormalAnglesInDegrees()[0];
				initParam[count++] = detProp.getNormalAnglesInDegrees()[2];
			}

			return initParam;

		}

		public IDiffractionMetadata getMetadata(double[] params, IDiffractionMetadata md) {

			DetectorProperties odp = md.getDetector2DProperties();

			final int heightInPixels = odp.getPy();
			final int widthInPixels = odp.getPx();
			final double pixelHeightInMM = odp.getVPxSize();
			final double pixelWidthInMM = odp.getHPxSize();

			DiffractionCrystalEnvironment odce = md.getDiffractionCrystalEnvironment();
			DiffractionCrystalEnvironment ndce = new DiffractionCrystalEnvironment();

			int count = 0;

			if (floatEnergy) {
				ndce.setWavelength(params[count++]);
			} else {
				ndce.setWavelength(odce.getWavelength());
			}

			double distance = odp.getBeamCentreDistance();
			double[] beamCentre = odp.getBeamCentreCoords();
			double[] normAngle = odp.getNormalAnglesInDegrees();

			if (floatDistance)
				distance = params[count++];

			if (floatBeamCentre) {
				beamCentre[0] = params[count++];
				beamCentre[1] = params[count++];
			}

			if (floatTilt) {
				normAngle[0] = params[count++];
				normAngle[2] = params[count++];
			}

			DetectorProperties d = new DetectorProperties(distance, beamCentre[0] * pixelHeightInMM,
					beamCentre[1] * pixelWidthInMM, heightInPixels, widthInPixels, pixelHeightInMM, pixelWidthInMM);
			d.setNormalAnglesInDegrees(normAngle[0], 0, normAngle[2]);

			return new DiffractionMetadata(null, d, ndce);
		}

		private SimpleCalibrationParameterModel getOuterType() {
			return SimpleCalibrationParameterModel.this;
		}
	}
}
