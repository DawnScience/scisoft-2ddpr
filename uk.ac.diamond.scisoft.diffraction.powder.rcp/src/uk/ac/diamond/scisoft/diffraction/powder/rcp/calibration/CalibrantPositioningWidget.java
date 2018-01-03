/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration;

import java.lang.reflect.InvocationTargetException;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.workbench.ui.Activator;
import org.dawb.workbench.ui.views.RepeatingMouseAdapter;
import org.dawb.workbench.ui.views.SlowFastRunnable;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.DiffractionCalibrationUtils.ManipulateMode;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

/**
 * 
 * @author wqk87977
 *
 */
public class CalibrantPositioningWidget {

	private Control[] controls;
	private IDiffractionMetadata metadata;

	/**
	 * Creates a widget group with all the calibrant positioning widgets
	 * used in a diffraction calibration view.
	 * @param parent
	 *         parent composite of the widget
	 * @param model
	 *         List of all diffraction data present in the TableViewer (used to update beam centre)
	 */
	public CalibrantPositioningWidget(Composite parent, IDiffractionMetadata diffractionMetadata) {
		this.metadata = diffractionMetadata;
		final Display display = Display.getDefault();

		Composite controllerHolder = new Composite(parent, SWT.FILL);
		controllerHolder.setLayout(new GridLayout(2, false));
		controllerHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridUtils.removeMargins(controllerHolder);

		// Pad composite
		Composite padComp = new Composite(controllerHolder, SWT.NONE);
		padComp.setLayout(new GridLayout(5, false));
		padComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		padComp.setToolTipText("Move calibrant");

		Label l = new Label(padComp, SWT.NONE);
		l = new Label(padComp, SWT.NONE);
		Button upButton = new Button(padComp, SWT.ARROW | SWT.UP);
		upButton.setToolTipText("Move rings up");
		upButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.UP, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		upButton.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false));
		l = new Label(padComp, SWT.NONE);
		l = new Label(padComp, SWT.NONE);

		l = new Label(padComp, SWT.NONE);
		Button leftButton = new Button(padComp, SWT.ARROW | SWT.LEFT);
		leftButton.setToolTipText("Shift rings left");
		leftButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.LEFT, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		leftButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		l = new Label(padComp, SWT.NONE);
		l.setImage(Activator.getImage("icons/centre.png"));
		l.setToolTipText("Move calibrant");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		Button rightButton = new Button(padComp, SWT.ARROW | SWT.RIGHT);
		rightButton.setToolTipText("Shift rings right");
		rightButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.RIGHT, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		rightButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		l = new Label(padComp, SWT.NONE);

		l = new Label(padComp, SWT.NONE);
		l = new Label(padComp, SWT.NONE);
		Button downButton = new Button(padComp, SWT.ARROW | SWT.DOWN);
		downButton.setToolTipText("Move rings down");
		downButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.DOWN, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		downButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		l = new Label(padComp, SWT.NONE);
		l = new Label(padComp, SWT.NONE);

		// Resize group actions
		Composite actionComp = new Composite(controllerHolder, SWT.NONE);
		actionComp.setLayout(new GridLayout(3, false));
		actionComp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

		Composite sizeComp = new Composite(actionComp, SWT.NONE);
		sizeComp.setLayout(new GridLayout(1, false));
		sizeComp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		sizeComp.setToolTipText("Change size");
		GridUtils.removeMargins(sizeComp);

		Button plusButton = new Button(sizeComp, SWT.PUSH);
		plusButton.setImage(Activator.getImage("icons/arrow_out.png"));
		plusButton.setToolTipText("Make rings larger");
		plusButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.ENLARGE, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		plusButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Button minusButton = new Button(sizeComp, SWT.PUSH);
		minusButton.setImage(Activator.getImage("icons/arrow_in.png"));
		minusButton.setToolTipText("Make rings smaller");
		minusButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.SHRINK, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		minusButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Composite shapeComp = new Composite(actionComp, SWT.NONE);
		shapeComp.setLayout(new GridLayout(1, false));
		shapeComp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		shapeComp.setToolTipText("Change shape");
		GridUtils.removeMargins(shapeComp);

		Button elongateButton = new Button(shapeComp, SWT.PUSH);
		elongateButton.setText("Elongate");
		elongateButton.setToolTipText("Make rings more elliptical");
		elongateButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.ELONGATE, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		elongateButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Button squashButton = new Button(shapeComp, SWT.PUSH | SWT.FILL);
		squashButton.setText("Squash");
		squashButton.setToolTipText("Make rings more circular");
		squashButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.SQUASH, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		squashButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Composite rotateComp = new Composite(actionComp, SWT.NONE);
		rotateComp.setLayout(new GridLayout(1, false));
		rotateComp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		rotateComp.setToolTipText("Change rotation");
		GridUtils.removeMargins(rotateComp);

		Button clockButton = new Button(rotateComp, SWT.PUSH);
		clockButton.setImage(Activator.getImage("icons/arrow_rotate_clockwise.png"));
		clockButton.setToolTipText("Rotate rings clockwise");
		clockButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.CLOCKWISE, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		clockButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		Button antiClockButton = new Button(rotateComp, SWT.PUSH);
		antiClockButton.setImage(Activator.getImage("icons/arrow_rotate_anticlockwise.png"));
		antiClockButton.setToolTipText("Rotate rings anti-clockwise");
		antiClockButton.addMouseListener(new RepeatingMouseAdapter(display,
				new SlowFastRunnable() {
					@Override
					public void run() {
						DiffractionCalibrationUtils.changeRings(metadata, ManipulateMode.ANTICLOCKWISE, isFast());
					}

					@Override
					public void stop() {
					}
				}));
		
		antiClockButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	}

	/**
	 * Update the diffraction data
	 * @param data
	 */
	public void setDiffractionMeataData(IDiffractionMetadata metadata) {
		this.metadata = metadata;
	}



	private void setCalibrateOptionsEnabled(boolean b) {
		if (controls == null)
			return;
		for (int i = 0; i < controls.length; i++) {
			if (controls[i] != null)
				controls[i].setEnabled(b);
		}
	}

	/**
	 * set the controls to update (enable/disable)
	 * @param controls
	 */
	public void setControlsToUpdate(Control... controls) {
		this.controls = controls;
	}
}
