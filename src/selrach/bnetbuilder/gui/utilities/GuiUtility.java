package selrach.bnetbuilder.gui.utilities;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.BevelBorder;

/**
 * Several different GUI helper functions
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class GuiUtility {

	private GuiUtility() {
	}

	public static JLabel makeLabel(String name, GridBagLayout gridbag,
			GridBagConstraints c) {
		final JLabel label = new JLabel();
		label.setName(name);
		if (gridbag != null && c != null) {
			gridbag.setConstraints(label, c);
		}
		return label;
	}

	public static JButton makeButton(String name, ActionMap map,
			GridBagLayout gridbag, GridBagConstraints c) {
		final JButton button = new JButton();
		button.setAction(map.get(name));
		if (gridbag != null && c != null) {
			gridbag.setConstraints(button, c);
		}
		button.setBorder(BorderFactory.createEtchedBorder(BevelBorder.RAISED));
		return button;
	}

	public static JRadioButton makeRadioButton(String groupAction, String name,
			ActionMap map, ButtonGroup buttonGroup, GridBagLayout gridbag,
			GridBagConstraints c) {

		final JRadioButton button = new JRadioButton();
		button.setName(name);
		button.setAction(map.get(groupAction));
		if (gridbag != null && c != null) {
			gridbag.setConstraints(button, c);
		}
		if (buttonGroup != null) {
			buttonGroup.add(button);
		}
		return button;
	}
}
