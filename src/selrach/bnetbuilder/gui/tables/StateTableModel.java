package selrach.bnetbuilder.gui.tables;

import javax.swing.table.DefaultTableModel;

import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.interfaces.StateUpdatedListener;

/**
 * Models the underlying a multi-variable, multi-state display
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class StateTableModel extends DefaultTableModel implements
		StateUpdatedListener {

	private static final long serialVersionUID = 5758455503927958466L;

	DiscreteVariable dv = null;

	public StateTableModel() {
		super();
		DiscreteVariable.subscribe(this);
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		return dv == null ? 0 : dv.getStates().size();
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		if (dv == null) {
			return "";
		}
		return dv.getStates().get(rowIndex).toString();
	}

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		return true;
	}

	public void setDiscreteVariable(final DiscreteVariable variable) {
		dv = variable;
		this.fireTableStructureChanged();
	}

	@Override
	public void setValueAt(final Object value, final int rowIndex,
			final int columnIndex) {
		dv.setState(rowIndex, (String) value);
		fireTableCellUpdated(rowIndex, columnIndex);
	}

	public void statesUpdated(final DiscreteVariable variable) {
		if (variable == dv) {
			this.fireTableStructureChanged();
		}
	}
}
