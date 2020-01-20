package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.EventObject;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.MaskingOptionsGroup.MaskState;

public class MaskChangedEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	
	private MaskState maskType;

	public MaskChangedEvent(Object source, MaskState newMaskTypeID) {
		super(source);
		maskType = newMaskTypeID;
	}
	
	public MaskState getMaskType() {
		return maskType;
	}
	
	public boolean canMask() {
		return maskType != MaskState.NOMASK; 
	}

}