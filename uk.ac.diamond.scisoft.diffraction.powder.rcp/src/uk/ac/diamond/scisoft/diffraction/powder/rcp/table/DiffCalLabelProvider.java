/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;

public class DiffCalLabelProvider implements ITableLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element == null)
			return null;

		DiffractionImageData data = (DiffractionImageData) element;
		if (columnIndex == 0) {
			return data.getName();
		} else if (columnIndex == 1) { // # of rings
			if (data.getNonNullROISize() == 0)
				return null;
			return String.valueOf(data.getNrois());
		} else if (columnIndex == 2) { // distance
			return String.format("%.2f", data.getDistance()) + "*";
		}
		return null;
	}
}