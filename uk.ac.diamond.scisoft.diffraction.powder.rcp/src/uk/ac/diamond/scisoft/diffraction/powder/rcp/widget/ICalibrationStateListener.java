package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.EventListener;

public interface ICalibrationStateListener extends EventListener {

	public void calibrationStateChanged(StateChangedEvent event);
	
}
