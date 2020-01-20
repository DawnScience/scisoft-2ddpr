package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.EventListener;

public interface ICalibrationMaskStateListener extends EventListener {

	public void maskStateChanged(MaskChangedEvent event);
	
}