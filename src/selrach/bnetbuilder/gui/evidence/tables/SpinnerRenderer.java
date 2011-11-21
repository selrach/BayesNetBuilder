package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.Component;

import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Renders a spinner as the editor on the table
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class SpinnerRenderer extends JSpinner implements TableCellRenderer {

	final private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer(); 
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 966002573040091735L;

	public SpinnerRenderer()
	{
		super();
	}
	
	public SpinnerRenderer(SpinnerModel model)
	{
		super(model);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if(value instanceof JSpinner) {
			return (Component) value;
		}
		if((Double)value==Double.POSITIVE_INFINITY || (Double)value==Double.NEGATIVE_INFINITY) {
			return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		

    if (isSelected) 
    {
      setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } 
    else 
    {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
  	}
    
		setValue(value);
		return this;
	}

}
