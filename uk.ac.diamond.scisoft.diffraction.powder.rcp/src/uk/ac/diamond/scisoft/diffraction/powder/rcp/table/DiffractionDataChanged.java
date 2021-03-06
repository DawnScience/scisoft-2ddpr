/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import java.util.EventObject;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;

public class DiffractionDataChanged extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DiffractionDataChanged(DiffractionImageData source) {
		super(source);
	}

}
