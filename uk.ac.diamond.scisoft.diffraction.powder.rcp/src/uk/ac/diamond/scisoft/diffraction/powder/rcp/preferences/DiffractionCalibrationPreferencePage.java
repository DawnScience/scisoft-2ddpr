package uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences;


import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;

public class DiffractionCalibrationPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {
	
	private Spinner radius;
	private Spinner spacing;
	private Spinner nPoints;
	private Spinner maxSize;
	
	private Button fixRoll;
	
	public static final String ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp.calibrationPreferencePage";

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		GridData gdc = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gdc);

		Group group = new Group(comp, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		group.setLayout(new GridLayout(2, false));
		group.setText("Ring Finding Options");
		
		Label radiusLab = new Label(group, SWT.NONE);
		radiusLab.setText("Radius from beam centre ignored");
		radiusLab.setToolTipText("Used to prevent strong signal from the direct beam affecting the pattern matching");

		radius = new Spinner(group, SWT.BORDER);
		radius.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		radius.setDigits(0);
		radius.setMinimum(0);
		radius.setMaximum(10000);
		radius.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storePreferences();
			}			
		});
		
		
		Label spacingLab = new Label(group, SWT.NONE);
		spacingLab.setText("Set minimum spacing for ring detection");
		spacingLab.setToolTipText("Used to minimise chance of error with closely spaced rings");

		spacing = new Spinner(group, SWT.BORDER);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		spacing.setDigits(0);
		spacing.setMinimum(0);
		spacing.setMaximum(10000);
		spacing.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storePreferences();
			}			
		});
		
		Label pointsLab = new Label(group, SWT.NONE);
		pointsLab.setText("Number of points to find on each ring");
		pointsLab.setToolTipText("More points give more accurate parameters but take longer");

		nPoints = new Spinner(group, SWT.BORDER);
		nPoints.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		nPoints.setDigits(0);
		nPoints.setMinimum(10);
		nPoints.setMaximum(5000);
		nPoints.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storePreferences();
			}			
		});
		
		Label searchLab = new Label(group, SWT.NONE);
		searchLab.setText("Max search distance (point finding)");
		searchLab.setToolTipText("Smaller search distance means less likely to accidentally find neighbouring ring");

		maxSize = new Spinner(group, SWT.BORDER);
		maxSize.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		maxSize.setDigits(0);
		maxSize.setMinimum(2);
		maxSize.setMaximum(5000);
		maxSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storePreferences();
			}			
		});
		
		Label fr = new Label(group, SWT.NONE);
		fr.setText("Fix the Detector Roll");
		fr.setToolTipText("Perform the calibration but change Yaw and Pitch instead of Yaw and Roll");

		fixRoll = new Button(group, SWT.CHECK);
		fixRoll.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		fixRoll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storePreferences();
			}			
		});
		
		initializePage();
		
		return comp;
	}
	
	@Override
	public boolean performOk() {
		return storePreferences();
	}

	@Override
	protected void performDefaults() {
		radius.setSelection(getDefaultRadius());
		spacing.setSelection(getDefaultSpacing());
		nPoints.setSelection(getDefaultNumberOfPoints());
		maxSize.setSelection(getDefaultMaxSize());
		fixRoll.setSelection(getDefaultFixRoll());
	}
	
	private void initializePage() {
		radius.setSelection(getRadius());
		spacing.setSelection(getSpacing());
		nPoints.setSelection(getNumberOfPoints());
		maxSize.setSelection(getMaxSize());
		fixRoll.setSelection(getFixRoll());
	}

	private boolean storePreferences() {
		setRadius(radius.getSelection());
		setSpacing(spacing.getSelection());
		setNumberOfPoints(nPoints.getSelection());
		setMaxSize(maxSize.getSelection());
		setFixRoll(fixRoll.getSelection());
		return true;
	}
	
	public void setRadius(int radius) {
		getPreferenceStore().setValue(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS, radius);
	}
	
	public void setSpacing(int spacing) {
		getPreferenceStore().setValue(DiffractionCalibrationConstants.MINIMUM_SPACING, spacing);
	}
	
	public void setNumberOfPoints(int nPoints) {
		getPreferenceStore().setValue(DiffractionCalibrationConstants.NUMBER_OF_POINTS, nPoints);
	}
	
	public void setMaxSize(int maxSize) {
		getPreferenceStore().setValue(DiffractionCalibrationConstants.MAX_SEARCH_SIZE, maxSize);
	}
	
	public void setFixRoll(boolean fixRoll) {
		getPreferenceStore().setValue(DiffractionCalibrationConstants.FIX_DETECTOR_ROLL, fixRoll);
	}
	
	
	public int getRadius() {
		return getPreferenceStore().getInt(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS);
	}
	
	public int getSpacing() {
		return getPreferenceStore().getInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
	}
	
	public int getNumberOfPoints() {
		return getPreferenceStore().getInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
	}
	
	public int getDefaultRadius() {
		return getPreferenceStore().getDefaultInt(DiffractionCalibrationConstants.CENTRE_MASK_RADIUS);
	}
	
	public int getDefaultSpacing() {
		return getPreferenceStore().getDefaultInt(DiffractionCalibrationConstants.MINIMUM_SPACING);
	}
	
	public int getDefaultNumberOfPoints() {
		return getPreferenceStore().getDefaultInt(DiffractionCalibrationConstants.NUMBER_OF_POINTS);
	}
	
	public int getDefaultMaxSize() {
		return getPreferenceStore().getDefaultInt(DiffractionCalibrationConstants.MAX_SEARCH_SIZE);
	}
	
	public int getMaxSize() {
		return getPreferenceStore().getInt(DiffractionCalibrationConstants.MAX_SEARCH_SIZE);
	}
	
	public boolean getDefaultFixRoll() {
		return getPreferenceStore().getDefaultBoolean(DiffractionCalibrationConstants.FIX_DETECTOR_ROLL);
	}
	
	public boolean getFixRoll() {
		return getPreferenceStore().getBoolean(DiffractionCalibrationConstants.FIX_DETECTOR_ROLL);
	}

}
