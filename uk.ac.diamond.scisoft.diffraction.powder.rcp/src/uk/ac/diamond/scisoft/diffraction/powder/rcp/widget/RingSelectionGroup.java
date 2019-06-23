package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;

import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public class RingSelectionGroup {

	private Spinner ringNumberSpinner;
	private RingSelectionText ringSelectionText;
	private Button spinnerRadio;
	private Button textRadio;
	private SimpleCalibrationParameterModel model;
	private Group group;

	public RingSelectionGroup(Composite parent, int maximumRingNumber, final SimpleCalibrationParameterModel model) {
		
		this.model = model;

		group = new Group(parent, SWT.FILL);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setText("Select rings to use for calibration:");
		group.setBackground(parent.getBackground());

		spinnerRadio = new Button(group, SWT.RADIO);
		spinnerRadio.setText("Rings to use (from inner):");
		spinnerRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = spinnerRadio.getSelection();
				if (isSelected) {
					textRadio.setSelection(false);
					ringNumberSpinner.setEnabled(true);
					ringSelectionText.setEnabled(false);
					model.setRingSet(null);
				}
			}
		});
		spinnerRadio.setSelection(true);
		
		ringNumberSpinner = new Spinner(group, SWT.BORDER);
		ringNumberSpinner.setMaximum(maximumRingNumber);
		ringNumberSpinner.setMinimum(2);
		ringNumberSpinner.setSelection(100);
		ringNumberSpinner.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setNumberOfRings(ringNumberSpinner.getSelection());
			}
		
		});

		textRadio = new Button(group, SWT.RADIO);
		textRadio.setText("Select specific ring numbers:");
		textRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = textRadio.getSelection();
				if (isSelected) {
					spinnerRadio.setSelection(false);
					ringNumberSpinner.setEnabled(false);
					ringSelectionText.setEnabled(true);
					model.setRingSet(ringSelectionText.getUniqueRingNumbers());
				}
			}
		});
		textRadio.setSelection(false);
		ringSelectionText = new RingSelectionText(group, SWT.BORDER);
		ringSelectionText.setMaximumRingNumber(ringNumberSpinner.getMaximum());
		ringSelectionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ringSelectionText.setToolTipText("Enter unique ring numbers separated by commas");
		ringSelectionText.setEnabled(false);
		ringSelectionText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				model.setRingSet(ringSelectionText.getUniqueRingNumbers());
				
			}
		});
	}
	
	public Control getControl() {
		return group;
	}

	public void addRingNumberSpinnerListener(SelectionListener selectionAdapter) {
		ringNumberSpinner.addSelectionListener(selectionAdapter);
	}

	public int getRingSpinnerSelection() {
		return ringNumberSpinner.getSelection();
	}

	public RingSelectionText getRingSelectionText() {
		return ringSelectionText;
	}

	public void setRingSpinnerSelection(int selection) {
		ringNumberSpinner.setSelection(selection);
	}

	public boolean isUsingRingSpinner() {
		return spinnerRadio.getSelection();
	}

	/**
	 * Sets the maximum ring number: maximum value of spinner and text field
	 * @param maximumRingNumber
	 */
	public void setMaximumRingNumber(int maximumRingNumber) {
		ringNumberSpinner.setMaximum(maximumRingNumber);
		ringSelectionText.setMaximumRingNumber(maximumRingNumber);
		model.setNumberOfRings(maximumRingNumber);
	}
}
