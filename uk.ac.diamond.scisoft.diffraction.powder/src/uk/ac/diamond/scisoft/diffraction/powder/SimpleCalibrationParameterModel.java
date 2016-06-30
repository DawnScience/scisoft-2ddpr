package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Set;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;

import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;

public class SimpleCalibrationParameterModel {
	
	boolean floatEnergy = true;
	boolean floatDistance = true;
	boolean floatBeamCentre = true;
	boolean floatTilt = true;
	boolean useRingSet = false;
	boolean finalGlobalOptimisation = false;
	boolean isEllipseCalibration = false;

	int nRings;
	Set<Integer> ringSet;
	
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
		return useRingSet;
	}
	
	public void setUseRingSet(boolean useRingSet) {
		this.useRingSet = useRingSet;
	}
	
	public boolean isFinalGlobalOptimisation() {
		return finalGlobalOptimisation;
	}

	public void setFinalGlobalOptimisation(boolean optimise) {
		finalGlobalOptimisation = optimise;
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
		return isEllipseCalibration;
	}

	public void setEllipseCalibration(boolean isEllipseCalibration) {
		this.isEllipseCalibration = isEllipseCalibration;
	}
}
