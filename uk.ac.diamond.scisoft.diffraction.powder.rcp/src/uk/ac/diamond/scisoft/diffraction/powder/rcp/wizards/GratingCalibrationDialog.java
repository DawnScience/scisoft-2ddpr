/*-
 * Copyright 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package uk.ac.diamond.scisoft.diffraction.powder.rcp.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class GratingCalibrationDialog extends Dialog {

	DiffractionDataManager manager;
	
	public GratingCalibrationDialog(Shell shell, DiffractionDataManager manager) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.manager = manager;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout twoColumns = new GridLayout(2, true);
		container.setLayout(twoColumns);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite imageColumn = new Composite(container, SWT.FILL | SWT.BORDER);
		Composite dataColumn = new Composite(container, SWT.FILL | SWT.BORDER);
		
		return container;
	}
}
