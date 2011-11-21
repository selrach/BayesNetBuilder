package selrach.bnetbuilder.gui.evidence.tables;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import selrach.bnetbuilder.data.ElementMetadata;
import selrach.bnetbuilder.model.variable.DiscreteVariable;

/**
 * This handles the continuous data mapping.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ContinuousFileDataJTableModel implements TableModel {

	private static final long serialVersionUID = 992747624701300700L;
	final private DiscreteVariable variable;
	final private ElementMetadata metadata;
	final private Set<TableModelListener> listeners = new HashSet<TableModelListener>();
	final private List<JButton> buttons = new ArrayList<JButton>();
	final private List<JSpinner> spinners = new ArrayList<JSpinner>();

	ActionListener buttonAction = new ActionListener() {

		private static final long serialVersionUID = 8373752119640223106L;

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null,
					"Need to put code to remove row "
							+ buttons.indexOf(e.getSource()));
		}
	};

	MouseListener buttonMouseListener = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getSource() instanceof JButton) {
				((JButton) e.getSource()).doClick();
			}
			super.mouseClicked(e);
		}
	};

	public ContinuousFileDataJTableModel(DiscreteVariable variable,
			ElementMetadata metadata) {
		this.variable = variable;
		this.metadata = metadata;

		for (int i = 0; i < metadata.getRangeStates().size() - 1; i++) {
			JButton button = new JButton("Remove");
			button.addActionListener(buttonAction);
			button.addMouseListener(buttonMouseListener);
			buttons.add(button);

			JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0,
					Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
			spinners.add(spinner);
		}

		JButton button = new JButton("Remove");
		button.addActionListener(buttonAction);
		button.addMouseListener(buttonMouseListener);
		buttons.add(button);
	}

	public boolean addRow() {
		return false;
	}

	public boolean removeRow(int which) {
		return false;
	}

	public void addTableModelListener(TableModelListener l) {
		listeners.add(l);
	}

	public Class<?> getColumnClass(int columnIndex) {
		return getValueAt(1, columnIndex).getClass();
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return "Boundary";
		case 1:
			return "Model State";
		case 2:
			return "Delete Boundary";
		}
		return "Error";
	}

	public int getRowCount() {
		return metadata.getRangeStates().size() + 1;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		List<String> rangeStates = metadata.getRangeStates();
		switch (columnIndex) {
		case 0:
			if (rowIndex == 0) {
				return Double.NEGATIVE_INFINITY;
			}
			if (rowIndex == rangeStates.size()) {
				return Double.POSITIVE_INFINITY;
			}
			JSpinner spinner = spinners.get(rowIndex - 1);
			spinner.setValue(metadata.getRangeBoundaries().get(rowIndex - 1));
			return spinner;
		case 1:
			if (rowIndex == rangeStates.size()) {
				return null;
			}
			return rangeStates.get(rowIndex);
		case 2:
			if (rowIndex == 0 || rowIndex == rangeStates.size()) {
				return null;
			}
			return buttons.get(rowIndex - 1);
		}
		return null;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		int sz = metadata.getRangeStates().size();
		return (columnIndex == 0 && rowIndex != sz && rowIndex != 0)
				|| (columnIndex == 1 && rowIndex != sz);
	}

	public void removeTableModelListener(TableModelListener l) {
		if (listeners.contains(l)) {
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
		switch (columnIndex) {
		case 0:
			metadata.moveRangeBoundary(rowIndex - 1, (Double) aValue);
			break;
		case 1:
			metadata.setStateMapping(rowIndex, (String) aValue);
			break;
		}
		notifyListeners(new TableModelEvent(this, rowIndex, rowIndex,
				columnIndex, TableModelEvent.UPDATE));
	}
}
