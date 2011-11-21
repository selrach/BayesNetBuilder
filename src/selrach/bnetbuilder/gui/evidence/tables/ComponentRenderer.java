package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Forwards a component as the object to handle the rendering.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ComponentRenderer implements TableCellRenderer {

	final private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

	private static final long serialVersionUID = 6804423947493080913L;

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof Component) {
			return (Component) value;
		}
		return defaultRenderer.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);
	}

}
