package uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;

public class DiffractionCalibrationPreferenceInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS,  SimpleCalibrationParameterModel.CENTRE_MASK_RADIUS);
		store.setDefault(DiffractionCalibrationConstants.MINIMUM_SPACING, SimpleCalibrationParameterModel.MINIMUM_SPACING);
		store.setDefault(DiffractionCalibrationConstants.NUMBER_OF_POINTS, SimpleCalibrationParameterModel.NUMBER_OF_POINTS);
		store.setDefault(DiffractionCalibrationConstants.MAX_SEARCH_SIZE, SimpleCalibrationParameterModel.MAX_SIZE);
		store.setDefault(DiffractionCalibrationConstants.FIX_DETECTOR_ROLL, SimpleCalibrationParameterModel.FIX_DETECTOR_ROLL);
	}

}
