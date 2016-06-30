package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import org.dawnsci.plotting.tools.preference.diffraction.DiffractionPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;

public class CalibrantSelectionGroup {

	private Combo calibrantCombo;
	private Button showCalibAndBeamCtrCheckBox;
	
	public CalibrantSelectionGroup(Composite composite) {
		
		Group selectCalibComp = new Group(composite, SWT.FILL);
		selectCalibComp.setText("Select calibrant:");
		selectCalibComp.setLayout(new GridLayout(1, false));
		selectCalibComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite comp = new Composite(selectCalibComp, SWT.NONE);
		comp.setLayout(new GridLayout(2, true));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		calibrantCombo = new Combo(comp, SWT.READ_ONLY);
		calibrantCombo.setToolTipText("Select a type of calibrant");
		calibrantCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		calibrantCombo.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if (manager.getCurrentData() == null)
//					return;
//				int index = calibrantCombo.getSelectionIndex();
//				calibrantName = calibrantCombo.getItem(index);
//				// update the calibrant in diffraction tool
//				CalibrationFactory.getCalibrationStandards().setSelectedCalibrant(calibrantName, true);
//				// set the maximum number of rings
//				int ringMaxNumber = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size();
//				ringSelection.setMaximumRingNumber(ringMaxNumber);
//				// Set the calibrant ring number
//				if (calibrantRingsMap.containsKey(calibrantName)) {
//					ringSelection.setRingSpinnerSelection(calibrantRingsMap.get(calibrantName));
//				} else {
//					calibrantRingsMap.put(calibrantName, ringMaxNumber);
//					ringSelection.setRingSpinnerSelection(ringMaxNumber);
//				}
//				// Tell the ring selection field about the maximum number allowed
//				
//			}
//		});
		for (String c : CalibrationFactory.getCalibrationStandards().getCalibrantList()) {
			calibrantCombo.add(c);
		}
		String s = CalibrationFactory.getCalibrationStandards().getSelectedCalibrant();
		if (s != null) {
			calibrantCombo.setText(s);
		}

		Button configCalibrantButton = new Button(comp, SWT.NONE);
		configCalibrantButton.setText("Configure...");
		configCalibrantButton.setToolTipText("Open Calibrant configuration page");
		configCalibrantButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		configCalibrantButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil
						.createPreferenceDialogOn(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								DiffractionPreferencePage.ID, null, null);
				if (pref != null)
					pref.open();
			}
		});

		showCalibAndBeamCtrCheckBox = new Button(selectCalibComp, SWT.CHECK);
		showCalibAndBeamCtrCheckBox.setText("Show Calibrant and Beam Centre");
//		showCalibAndBeamCtrCheckBox.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				checked = showCalibAndBeamCtrCheckBox.getSelection();
//				showCalibrantAndBeamCentre(checked);
//			}
//		});
		showCalibAndBeamCtrCheckBox.setSelection(true);
	}
	
	public void addCalibrantSelectionListener(SelectionListener listener) {
		calibrantCombo.addSelectionListener(listener);
	}
	
	public void removeCalibrantSelectionListener(SelectionListener listener) {
		calibrantCombo.removeSelectionListener(listener);
	}
	
	public void addDisplaySelectionListener(SelectionListener listener) {
		showCalibAndBeamCtrCheckBox.addSelectionListener(listener);
	}
	
	public void removeDisplaySelectionListener(SelectionListener listener) {
		showCalibAndBeamCtrCheckBox.removeSelectionListener(listener);
	}
	
	public String getCalibrant(){
		int index = calibrantCombo.getSelectionIndex();
		return calibrantCombo.getItem(index);
	}
	
	public boolean getShowRings(){
		return showCalibAndBeamCtrCheckBox.getSelection();
	}
	
	
}
