package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Text widget used to enter unique ring numbers separated by commas
 * @author wqk87977
 *
 */
public class RingSelectionText {

	private StyledText ringText;
	private int maxRingNumber;

	public RingSelectionText(Composite parent, int style) {
		ringText = new StyledText(parent, style);
		ringText.addListener(SWT.Verify, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if(!('0' <= chars[i] && chars[i] <= '9') && chars[i] != ',') {
						e.doit = false;
						return;
					}
				}
			}
		});
		ringText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String currentText = ringText.getText();
				if (currentText.endsWith(","))
					return;
				String[] arrayString = currentText.split(",");
				if (arrayString[arrayString.length - 1].equals(""))
					return;
				int currentNumber = Integer.valueOf(arrayString[arrayString.length - 1]);
				if (!isUnique(currentNumber) || currentNumber > maxRingNumber) {
					// change colour of last entry to red
					StyleRange colourStyle = new StyleRange();
					colourStyle.start = currentText.lastIndexOf(',')+1;
					colourStyle.length = arrayString[arrayString.length - 1].length();
					colourStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED);
					ringText.setStyleRange(colourStyle);
				} else {
					StyleRange resetStyle = new StyleRange();
					resetStyle.start = currentText.lastIndexOf(',')+1;
					resetStyle.length = arrayString[arrayString.length - 1].length();
					ringText.setStyleRange(resetStyle);
				}
			}
		});
	}

	/**
	 * Test if each entry in the string array is unique
	 * and and the entered char is a digit
	 * @param arrayString
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean isUnique(String[] arrayString) {
		List<String> valueList = Arrays.asList(arrayString);
		Set<String> valueSet = new HashSet<String>(valueList);
		if (valueSet.size() < valueList.size())
			return false;
		return true;
	}

	/**
	 * Test if the int entered is unique
	 * @param number
	 * @return
	 */
	private boolean isUnique(int number) {
		String currentText = ringText.getText();
		String[] tmp = currentText.split(",");
		for (int i = 0; i < tmp.length - 1; i++) {
			if (Integer.valueOf(tmp[i]) == number)
				return false;
		}
		return true;
	}

	public void setLayoutData(GridData gridData) {
		ringText.setLayoutData(gridData);
	}

	public void setToolTipText(String string) {
		ringText.setToolTipText(string);
	}

	public void setText(String string) {
		ringText.setText(string);
	}

	public String getText() {
		return ringText.getText();
	}

	/**
	 * The Maximum ring number needs to be set for this widget to properly work
	 * @param maxRingNumber
	 */
	public void setMaximumRingNumber(int maxRingNumber) {
		this.maxRingNumber = maxRingNumber;
	}

	public void setEnabled(boolean isEnabled) {
		ringText.setEnabled(isEnabled);
		if (isEnabled) {
			ringText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		} else {
			ringText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
		}
	}

	public boolean isEnabled() {
		return ringText.isEnabled();
	}

	/**
	 * 
	 * @return A set of unique ring numbers entered in the widget
	 *       null if the text field is empty
	 */
	public Set<Integer> getUniqueRingNumbers() {
		String text = ringText.getText();
		if (text.equals(""))
			return null;
		String[] array = text.split(",");
		Integer[] tmp = new Integer[array.length];
		for (int i = 0; i < array.length; i++) {
			tmp[i] = Integer.valueOf(array[i]);
		}
		List<Integer> list = Arrays.asList(tmp);
		Set<Integer> ringNumbers = new HashSet<Integer>(list);
		return ringNumbers;
	}
}
