package uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;

public class DiffractionCalibrationPreferenceInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS,  100);
		store.setDefault(DiffractionCalibrationConstants.MINIMUM_SPACING, 10);
	}

}
