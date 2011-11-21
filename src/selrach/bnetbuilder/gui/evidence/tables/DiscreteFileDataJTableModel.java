package selrach.bnetbuilder.gui.evidence.tables;

import java.util.HashSet;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import selrach.bnetbuilder.data.ElementMetadata;
import selrach.bnetbuilder.model.variable.DiscreteVariable;

/**
 * Handles state mappings for continuous variables
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class DiscreteFileDataJTableModel implements TableModel {

	private static final long serialVersionUID = -1765938545965266754L;
	final private DiscreteVariable variable;
	final private ElementMetadata metadata;
	
	public DiscreteFileDataJTableModel(DiscreteVariable variable, ElementMetadata metadata)
	{
		this.variable = variable;
		this.metadata = metadata;
	}
	
	final private Set<TableModelListener> listeners = new HashSet<TableModelListener>();

	public void addTableModelListener(TableModelListener l) {
		listeners.add(l);
	}

	public Class<?> getColumnClass(int columnIndex) {
		return getValueAt(0, columnIndex).getClass();
	}

	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int columnIndex) {
		switch(columnIndex)
		{
		case 0:
			return "Data State";
		case 1:
			return "Model State";
		}
		return "Error";
	}

	public int getRowCount() {
		return metadata.getDataStates().size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		String key =  metadata.getDataStates().get(rowIndex);
		switch(columnIndex)
		{
		case 0:
			return key;
		case 1:
			String value = metadata.getPotentialStateMap().get(key);
			return value==null? "" : value;		
		}
		return null;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex==1;
	}

	public void removeTableModelListener(TableModelListener l) {
		if(listeners.contains(l))
		{
			listeners.remove(l);
		}
	}

	private void notifyListeners(TableModelEvent e) {
		if (e == null) {
			return;
		}
		for (TableModelListener l : listeners) {
			l.tableChanged(e);
		}
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try
		{
			String key =  metadata.getDataStates().get(rowIndex);
			metadata.setStateMapping((String)aValue, key);
			notifyListeners(new TableModelEvent(this, rowIndex, rowIndex, columnIndex,
					TableModelEvent.UPDATE));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
}
