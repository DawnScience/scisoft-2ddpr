package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;

import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;

public class SimpleCalibrationParameterModel {
	
	public static final int CENTRE_MASK_RADIUS = 50;
	public static final int MINIMUM_SPACING =  10;
	public static final int NUMBER_OF_POINTS =  256;
	
	private boolean floatEnergy = true;
	private boolean floatDistance = true;
	private boolean floatBeamCentre = true;
	private boolean floatTilt = true;
	private boolean isPointCalibration = false;
	private boolean isAutomaticCalibration = false;
	
	private int nPointsPerRing = NUMBER_OF_POINTS;
	private int minimumSpacing = MINIMUM_SPACING;
	private int nIgnoreCentre = CENTRE_MASK_RADIUS;
	
	private int nRings;
	private Set<Integer> ringSet;
	
	public SimpleCalibrationParameterModel() {}

	public SimpleCalibrationParameterModel(SimpleCalibrationParameterModel toCopy) {
		this.floatEnergy = toCopy.floatEnergy;
		this.floatDistance = toCopy.floatDistance;
		this.floatBeamCentre = toCopy.floatBeamCentre;
		this.floatTilt = toCopy.floatTilt;
		this.isPointCalibration = toCopy.isPointCalibration;
		this.isAutomaticCalibration = toCopy.isAutomaticCalibration;
		this.nRings = toCopy.nRings;
		this.ringSet = ringSet == null ? null : new TreeSet<Integer>(ringSet);
	}
	
	public boolean isAutomaticCalibration() {
		return isAutomaticCalibration;
	}

	public void setAutomaticCalibration(boolean isAutomaticCalibration) {
		this.isAutomaticCalibration = isAutomaticCalibration;
	}

	public boolean isFloatDistance() {
		return floatDistance;
	}

	public boolean isFloatEnergy() {
		return floatEnergy;
	}
	
	public void setFloatEnergy(boolean floatEnergy) {
		this.floatEnergy = floatEnergy;
	}

	public void setFloatDistance(boolean floatDistance) {
		this.floatDistance = floatDistance;
	}
	
	public void setNumberOfRings(int nRings) {
		this.nRings = nRings;
	}
	
	public int getNumberOfRings() {
		return nRings;
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
		
		if (floatDistance) distance = params[count++];
		
		if (floatBeamCentre) {
			beamCentre[0] = params[count++];
			beamCentre[1] = params[count++];
		}
		
		if (floatTilt) {
			normAngle[0] = params[count++];
			normAngle[1] = params[count++];
		}
		
		DetectorProperties d = new DetectorProperties(distance,
				beamCentre[0]*pixelHeightInMM, beamCentre[1]*pixelWidthInMM, heightInPixels, widthInPixels, pixelHeightInMM, pixelWidthInMM);
		d.setNormalAnglesInDegrees(normAngle[0], 0, normAngle[1]);
		
		return new DiffractionMetadata(null, d, ndce);
	}
	
	public double[] getInitialParams(IDiffractionMetadata md) {
		
		DiffractionCrystalEnvironment de = md.getDiffractionCrystalEnvironment();
		DetectorProperties detProp = md.getDetector2DProperties();
		
		int count = 0;
		
		if (floatEnergy) count++;
		if (floatDistance) count++;
		if (floatBeamCentre) count+=2;
		if (floatTilt) count+=2;
		
		double[] initParam = new double[count];
		
		count = 0;
		
		if (floatEnergy) initParam[count++] = de.getWavelength();
		if (floatDistance) initParam[count++] = detProp.getBeamCentreDistance();
		if (floatBeamCentre) {
			initParam[count++] = detProp.getBeamCentreCoords()[0];
			initParam[count++] = detProp.getBeamCentreCoords()[1];
		}
		if (floatTilt) {
			//TODO check if normal angles [2] is zero, if not calc better vals
			initParam[count++] = detProp.getNormalAnglesInDegrees()[0];
			initParam[count++] = detProp.getNormalAnglesInDegrees()[2];
		}
		
		return initParam;
	}
	
	public CalibrationErrorOutput getErrorOutput(double[] errors) {
		
		if (errors == null) return null;
		
		int count = 0;
		
		if (floatEnergy) count++;
		if (floatDistance) count++;
		if (floatBeamCentre) count+=2;
		if (floatTilt) count+=2;
		
		CalibrationErrorOutput ceo = new CalibrationErrorOutput();
		
		if (errors.length != count) return null;
		
		count = 0;
		
		if (floatEnergy) ceo.setWavelength(errors[count++]);
		if (floatDistance) ceo.setDistance(errors[count++]);
		if (floatBeamCentre) {
			ceo.setBeamCentreX(errors[count++]);
			ceo.setBeamCentreY(errors[count++]);
		}
		if (floatTilt) {
			//TODO check if normal angles [2] is zero, if not calc better vals
			ceo.setTilt(errors[count++]);
			ceo.setTiltAngle(errors[count++]);
		}
		
		
		
		return ceo;
	}
	
	public int getNumberOfFloatingParameters() {
		
		int count = 0;
		if (floatEnergy) count++;
		if (floatDistance) count++;
		if (floatBeamCentre) count+=2;
		if (floatTilt) count+=2;
		
		return count;
	}

	public boolean isFloatBeamCentre() {
		return floatBeamCentre;
	}

	public boolean isFloatTilt() {
		return floatTilt;
	}

	public void setFloatBeamCentre(boolean floatBeamCentre) {
		this.floatBeamCentre = floatBeamCentre;
	}

	public void setFloatTilt(boolean floatTilt) {
		this.floatTilt = floatTilt;
	}
	
	public boolean isEllipseCalibration() {
		return !isPointCalibration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isPointCalibration ? 1231 : 1237);
		result = prime * result + (floatBeamCentre ? 1231 : 1237);
		result = prime * result + (floatDistance ? 1231 : 1237);
		result = prime * result + (floatEnergy ? 1231 : 1237);
		result = prime * result + (floatTilt ? 1231 : 1237);
		result = prime * result + (isAutomaticCalibration ? 1231 : 1237);
		result = prime * result + minimumSpacing;
		result = prime * result + nIgnoreCentre;
		result = prime * result + nPointsPerRing;
		result = prime * result + nRings;
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
		if (isPointCalibration != other.isPointCalibration)
			return false;
		if (floatBeamCentre != other.floatBeamCentre)
			return false;
		if (floatDistance != other.floatDistance)
			return false;
		if (floatEnergy != other.floatEnergy)
			return false;
		if (floatTilt != other.floatTilt)
			return false;
		if (isAutomaticCalibration != other.isAutomaticCalibration)
			return false;
		if (minimumSpacing != other.minimumSpacing)
			return false;
		if (nIgnoreCentre != other.nIgnoreCentre)
			return false;
		if (nPointsPerRing != other.nPointsPerRing)
			return false;
		if (nRings != other.nRings)
			return false;
		if (ringSet == null) {
			if (other.ringSet != null)
				return false;
		} else if (!ringSet.equals(other.ringSet))
			return false;
		return true;
	}

	
}
