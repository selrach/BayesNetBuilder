package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

/**
 * Forwards button events within a table
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class JTableButtonTableMouseListener implements MouseListener {
  final private JTable table;

  private void forwardEventToComponent(MouseEvent e) {
    TableColumnModel columnModel = table.getColumnModel();
    int column = columnModel.getColumnIndexAtX(e.getX());
    int row    = e.getY() / table.getRowHeight();
    Object value;
    JButton component;
    MouseEvent event;

    if(row >= table.getRowCount() || row < 0 ||
       column >= table.getColumnCount() || column < 0) {
		return;
	}

    value = table.getValueAt(row, column);

    if(!(value instanceof JButton)) {
		return;
	}
    

    component = (JButton)value;

    event =
      SwingUtilities.convertMouseEvent(table, e, component);
    System.err.println(event);
    component.dispatchEvent(event);
    
    table.updateUI();
  }

  public JTableButtonTableMouseListener(JTable table) {
    this.table = table;
  }

  public void mouseClicked(MouseEvent e) {
    forwardEventToComponent(e);
  }

  public void mouseEntered(MouseEvent e) {
    forwardEventToComponent(e);
  }

  public void mouseExited(MouseEvent e) {
    forwardEventToComponent(e);
  }

  public void mousePressed(MouseEvent e) {
    forwardEventToComponent(e);
  }

  public void mouseReleased(MouseEvent e) {
    forwardEventToComponent(e);
  }


}
