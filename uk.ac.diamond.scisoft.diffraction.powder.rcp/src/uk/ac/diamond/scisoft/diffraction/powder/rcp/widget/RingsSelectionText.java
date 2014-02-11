package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Text widget used to enter unique ring numbers separated by commas
 * @author wqk87977
 *
 */
public class RingsSelectionText {

	private Text ringText;

	public RingsSelectionText(Composite parent, int style) {
		ringText = new Text(parent, style);
		ringText.addListener(SWT.Verify, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String currentText = ringText.getText();
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if (!isUnique(currentText, chars[i])) {
						e.doit = false;
						return;
					}
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
	private boolean isUnique(String currentString, char charEntered) {
		if(!('0' <= charEntered && charEntered <= '9') && charEntered != ',')
			return false;
		currentString = currentString.concat(String.valueOf(charEntered));
		String[] arrayString = currentString.split(",");
		List<String> valueList = Arrays.asList(arrayString);
		Set<String> valueSet = new HashSet<String>(valueList);
		if (valueSet.size() < valueList.size())
			return false;
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

	public int[] getRingNumbers() {
		String[] array = ringText.getText().split(",");
		int[] ringNumbers = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			ringNumbers[i] = Integer.valueOf(array[i]);
		}
		return ringNumbers;
	}
}
