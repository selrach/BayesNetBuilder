package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.Component;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

/**
 * Shows a spinner as the editor for a table.
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class SpinnerEditor implements TableCellEditor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7166564012827827256L;
	private final JSpinner spinner;

	public SpinnerEditor(double value, double min, double max, double step)
	{
		spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
		spinner.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
	}
	
	private final Set<CellEditorListener> listeners = new HashSet<CellEditorListener>();

	public void addCellEditorListener(CellEditorListener l) {
		listeners.add(l);
	}

	public void cancelCellEditing() {
		notifyListeners(new ChangeEvent(this), false);
	}

	public Object getCellEditorValue() {
		return spinner.getValue();
	}

	public boolean isCellEditable(EventObject e) {
		return true;
	}

	public void removeCellEditorListener(CellEditorListener l) {
		if(listeners.contains(l))
		{
			listeners.remove(l);
		}
	}

	public boolean shouldSelectCell(EventObject e) {
		return false;
	}

	public boolean stopCellEditing() {
		try
		{
			spinner.commitEdit();
			notifyListeners(new ChangeEvent(this), true);
		}
		catch(Exception ex)
		{
			
		}
		return true;
	}
	
	private void notifyListeners(ChangeEvent e, boolean stopped)
	{
		for(CellEditorListener l : listeners)
		{
			if(stopped) {
				l.editingStopped(e);
			} else {
				l.editingCanceled(e);
			}
		}
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) 
	{
		if(value instanceof Double)
		{
			spinner.setValue(value);
			return spinner;
		}
		else if(value instanceof JSpinner)
		{
			spinner.setValue(((JSpinner) value).getValue());
			return spinner;
		}
		return null;
	}
}
