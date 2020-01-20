package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dawnsci.common.widgets.radio.RadioGroupWidget;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Widget;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public class MaskingOptionsGroup {

	private Set<ICalibrationMaskStateListener> listeners = new HashSet<>();
	private Group maskGroup;
	private RadioGroupWidget applyMaskRadio;
	private Button applyMaskButton;
	private Composite composite;
	private Group maskSummaryGroup;
	private StyledText summaryText;

	public enum MaskState {
		NOMASK, INTERNAL, FROMTOOL;
	}

	private MaskState currentMaskType = MaskState.NOMASK;

	public MaskingOptionsGroup(Composite content, SimpleCalibrationParameterModel initialModel) {

		composite = new Composite(content, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		this.maskGroup = new Group(composite, SWT.FILL);
		maskGroup.setText("Point Finding Mask Options");
		maskGroup.setLayout(new GridLayout());
		maskGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		applyMaskRadio = new RadioGroupWidget(maskGroup);
		applyMaskRadio.setActions(getMaskActions());

		maskSummaryGroup = new Group(composite, SWT.NONE);
		maskSummaryGroup.setLayout(new GridLayout());
		maskSummaryGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		maskSummaryGroup.setToolTipText("Mask summary information");
		maskSummaryGroup.setText("Mask Summary Info");

		summaryText = new StyledText(maskSummaryGroup, SWT.LEFT );
		summaryText.setAlwaysShowScrollBars(false);
		summaryText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		if (initialModel.isAutomaticCalibration())
			summaryText.setEnabled(false);

		applyMaskButton = new Button(composite, SWT.PUSH);
		applyMaskButton.setText("Apply selected mask");
		applyMaskButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		applyMaskButton.setEnabled(
				(currentMaskType != MaskState.NOMASK) && !initialModel.isAutomaticCalibration() && initialModel.isPointCalibration());
	}

	public void disableMaskingOptions(boolean disable) {
		maskGroup.setToolTipText("Set optional masking parameters");
		if (disable)
			composite.setToolTipText("Masking only available for manual calibration");
		for (Widget s : applyMaskRadio.getRadiosList())
			((Button) s).setEnabled(!disable);		
		maskGroup.setEnabled(!disable);
		applyMaskButton.setEnabled(!disable && currentMaskType != MaskState.NOMASK);
		maskSummaryGroup.setEnabled(!disable);
		summaryText.setVisible(!disable);
	}

	public MaskState getMaskType() {
		return currentMaskType;
	}

	public void updateMaskSummaryInfo(String info) {
		summaryText.setText(info);
	}

	private void updateMask(MaskState newMaskType) {
		if (currentMaskType != newMaskType) {
			currentMaskType = newMaskType;
			// Call a MaskChangedEvent
			fireMaskStateListenerEvent();
		}
	}

	public void resetMask() {
		updateMask(MaskState.NOMASK); // reset the mask
	}

	private List<Action> getMaskActions(){

		List<Action> radioActions = new ArrayList<>();
		Action noMaskAction = new Action() {
			@Override
			public void run() {
				updateMask(MaskState.NOMASK);
			}
		};

		noMaskAction.setText("No Mask");
		noMaskAction.setToolTipText("Ignore all masks on the data");
		noMaskAction.setChecked(true);

		Action fromSelectionAction = new Action() {
			@Override
			public void run() {
				updateMask(MaskState.FROMTOOL);
			}
		};
		fromSelectionAction.setText("Select In Plot");
		fromSelectionAction.setToolTipText("Specify the mask in the plotting window using the FastMask tool");
		
		Action fromInternalAction = new Action() {
			@Override
			public void run() {
				updateMask(MaskState.INTERNAL);
				}
		};
		fromInternalAction.setText("Load From Internal Dataset");
		fromInternalAction.setToolTipText("Load a mask from a compatible dataset contained in the file.\n");

		radioActions.add(noMaskAction);
		radioActions.add(fromSelectionAction);
		radioActions.add(fromInternalAction);

		return radioActions;
	}

	private void fireMaskStateListenerEvent() {
		MaskChangedEvent mask = new MaskChangedEvent(this, currentMaskType);
		for (ICalibrationMaskStateListener l : listeners)
			l.maskStateChanged(mask);
	}

	public void addCalibrationMaskChangedListener(ICalibrationMaskStateListener listener) {
		listeners.add(listener);
	}

	public void removeCalibrationMaskChangedListener(ICalibrationMaskStateListener listener) {
		listeners.remove(listener);
	}

	public Control getControl() {
		return composite;
	}

	public Button getMaskApplicationButton() {
		return applyMaskButton;
	}
	
	

}
