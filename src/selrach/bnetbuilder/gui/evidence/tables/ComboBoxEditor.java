package selrach.bnetbuilder.gui.evidence.tables;

import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
 * Editor that exposes a combobox by default
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class ComboBoxEditor extends DefaultCellEditor {
	private static final long serialVersionUID = -9202779725442425716L;

	public ComboBoxEditor(List<String> items) {
    super(new JComboBox(items.toArray()));
	}
}
