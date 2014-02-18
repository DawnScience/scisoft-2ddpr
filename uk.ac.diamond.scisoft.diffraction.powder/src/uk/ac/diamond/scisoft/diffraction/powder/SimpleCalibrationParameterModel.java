package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Set;

public class SimpleCalibrationParameterModel {
	
	boolean floatEnergy = true;
	boolean floatDistance = true;
	boolean useRingSet = false;
	boolean finalGlobalOptimisation = false;
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
}
