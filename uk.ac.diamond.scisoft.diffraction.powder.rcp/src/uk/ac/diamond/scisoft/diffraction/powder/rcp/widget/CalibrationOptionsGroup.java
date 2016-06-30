package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.ArrayList;
import java.util.List;

import org.dawnsci.common.widgets.radio.RadioGroupWidget;
import org.dawnsci.common.widgets.utils.GridUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationPreferencePage;

public class CalibrationOptionsGroup {

	public CalibrationOptionsGroup(Composite content, final SimpleCalibrationParameterModel parameters, boolean showEllipseOptions, boolean showPointOptions) {

		Composite composite = new Composite(content, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group ellipseParamGroup = new Group(composite, SWT.FILL);
		ellipseParamGroup.setText("Ellipse Calibration Options");
		ellipseParamGroup.setToolTipText("Set the Ellipse Parameters");
		ellipseParamGroup.setLayout(new GridLayout());
		ellipseParamGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		RadioGroupWidget calibEllipseParamRadios = new RadioGroupWidget(ellipseParamGroup);
		calibEllipseParamRadios.setActions(getEllipseParamActions(parameters), true);

		final Group pointCalibrateGroup = new Group(composite, SWT.FILL);
		pointCalibrateGroup.setText("Manual Point Calibration Options");
		pointCalibrateGroup.setToolTipText("Set the Point Parameters");
		pointCalibrateGroup.setLayout(new GridLayout());
		pointCalibrateGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Button fixEnergyButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixEnergyButton.setText("Fix Energy");
		fixEnergyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parameters.setFloatEnergy(!((Button)e.getSource()).getSelection());
			}
		});
		fixEnergyButton.setSelection(!parameters.isFloatEnergy());

		Button fixDistanceButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixDistanceButton.setText("Fix Distance");
		fixDistanceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parameters.setFloatDistance(!((Button)e.getSource()).getSelection());
			}
		});
		fixDistanceButton.setSelection(!parameters.isFloatDistance());

		Button fixBeamCentreButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixBeamCentreButton.setText("Fix Beam Centre");
		fixBeamCentreButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parameters.setFloatBeamCentre(!((Button)e.getSource()).getSelection());
			}
		});
		fixBeamCentreButton.setSelection(!parameters.isFloatBeamCentre());

		Button fixTiltButton = new Button(pointCalibrateGroup, SWT.CHECK);
		fixTiltButton.setText("Fix Tilt");
		fixTiltButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parameters.setFloatTilt(!((Button)e.getSource()).getSelection());
			}
		});
		fixTiltButton.setSelection(!parameters.isFloatTilt());

		Button advanced = new Button(composite, SWT.NONE);
		advanced.setText("Advanced...");
		advanced.setToolTipText("Open preference page for advanced settings");
		advanced.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
		advanced.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil
						.createPreferenceDialogOn(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								DiffractionCalibrationPreferencePage.ID, null, null);
				if (pref != null)
					pref.open();
			}
		});

		enableControl(ellipseParamGroup, showEllipseOptions);
		GridUtils.setVisible(ellipseParamGroup, showEllipseOptions);
		enableControl(pointCalibrateGroup, showPointOptions);
		GridUtils.setVisible(pointCalibrateGroup, showPointOptions);

	}


	private List<Action> getEllipseParamActions(final SimpleCalibrationParameterModel parameters) {
		List<Action> radioActions = new ArrayList<Action>();
		Action fixNoneAction = new Action() {
			@Override
			public void run() {
				parameters.setFloatDistance(true);
				parameters.setFloatEnergy(true);
			}
		};
		fixNoneAction.setText("Fix None");
		fixNoneAction.setToolTipText("No parameter is fixed");
		fixNoneAction.setChecked(parameters.isFloatDistance()&&parameters.isFloatEnergy());

		Action fixEnergyAction = new Action() {
			@Override
			public void run() {
				parameters.setFloatDistance(true);
				parameters.setFloatEnergy(false);
			}
		};
		fixEnergyAction.setText("Fix Energy");
		fixEnergyAction.setToolTipText("Energy parameter is fixed");
		fixEnergyAction.setChecked(parameters.isFloatDistance()&&!parameters.isFloatEnergy());

		Action fixDistanceAction = new Action() {
			@Override
			public void run() {
				parameters.setFloatDistance(false);
				parameters.setFloatEnergy(true);
			}
		};
		fixDistanceAction.setText("Fix Distance");
		fixDistanceAction.setToolTipText("Distance parameter is fixed");
		fixDistanceAction.setChecked(!parameters.isFloatDistance()&&parameters.isFloatEnergy());

		radioActions.add(fixNoneAction);
		radioActions.add(fixEnergyAction);
		radioActions.add(fixDistanceAction);

		boolean somethingSelected = false;
		for (Action action : radioActions) {
			if (action.isChecked()) {
				somethingSelected = true;
			}
		}
		if (!somethingSelected) fixNoneAction.setChecked(true);

		return radioActions;
	}



	private void enableControl(Group group, boolean enabled) {
		for (Control child : group.getChildren())
			child.setEnabled(enabled);

		group.setEnabled(enabled);
	}


}
	
