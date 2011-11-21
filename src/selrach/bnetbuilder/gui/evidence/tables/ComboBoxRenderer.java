package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.Component;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer for combo box
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class ComboBoxRenderer extends JComboBox implements TableCellRenderer {

	private static final long serialVersionUID = 8585173803453565565L;
	
	final private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer(); 

	public ComboBoxRenderer(List<String> items)
	{
		super(items.toArray());
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } 
    else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
  	}

    if(value!=null)
    {
		  // Select the current value
		  setSelectedItem(value);
		  return this;
    }
    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

}
