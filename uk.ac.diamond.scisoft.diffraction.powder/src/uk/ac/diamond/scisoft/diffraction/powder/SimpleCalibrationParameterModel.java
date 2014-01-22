package uk.ac.diamond.scisoft.diffraction.powder;

public class SimpleCalibrationParameterModel {
	
	boolean floatEnergy = true;
	boolean floatDistance = true;
	int nRings;
	
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

}
