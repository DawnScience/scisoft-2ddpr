/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import org.dawnsci.common.widgets.celleditor.FloatSpinnerCellEditor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;

public class DiffCalEditingSupport extends EditingSupport {
		private TableViewer tv;
		private int column;

		public DiffCalEditingSupport(TableViewer viewer, int col) {
			super(viewer);
			tv = viewer;
			this.column = col;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			if (column == 2) { // distance column
				final FloatSpinnerCellEditor fse = new FloatSpinnerCellEditor((Composite)getViewer().getControl(), SWT.RIGHT);
				fse.setFormat(100, 2);
				fse.setMaximum(Double.MAX_VALUE);
				fse.setMinimum(-Double.MAX_VALUE);
				return fse;
			}
			return null;
		}

		@Override
		protected boolean canEdit(Object element) {
			if (column == 2) // distance
				return true;
			else
				return false;
		}

		@Override
		protected Object getValue(Object element) {
			DiffractionImageData data = (DiffractionImageData) element;
			if (column == 2) {
				return data.getDistance();
			}
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			DiffractionImageData data = (DiffractionImageData) element;

			if (column == 2) {
				data.setDistance((Double) value);
				tv.refresh();
			}
		}
	}