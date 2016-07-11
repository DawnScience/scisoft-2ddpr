package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.EventObject;

public class StateChangedEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	
	private boolean canRun;

	public StateChangedEvent(Object source, boolean canRun) {
		super(source);
		
		this.canRun = canRun;
	}
	
	public boolean isCanRun() {
		return canRun;
	}

}
