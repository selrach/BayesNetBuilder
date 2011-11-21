package selrach.bnetbuilder.gui.tables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * This makes a column based header model, useful to show multi-variable states
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ColumnHeaderModel extends AbstractTableModel {

	private static final long serialVersionUID = -2940820976980929700L;
	final private int numberColumns;
	final private int numberRows;
	final private List<List<String>> states = new ArrayList<List<String>>();
	final private List<Integer> columnsPerSwitch = new ArrayList<Integer>();

	public ColumnHeaderModel(List<TransientVariable> dependencies) {
		List<TransientVariable> deps = new ArrayList<TransientVariable>(
				dependencies);
		int totalColumns = 1;
		Collections.reverse(deps);
		for (TransientVariable tv : dependencies) {
			RandomVariable rv = tv.getReference();
			if (rv instanceof DiscreteVariable) {
				List<String> state = ((DiscreteVariable) rv).getStates();
				states.add(new ArrayList<String>(state));
				columnsPerSwitch.add(new Integer(totalColumns));
				totalColumns *= state.size();
			} else {
				// TODO: Fix this for actual continuous variables
				List<String> descriptors = new ArrayList<String>();
				descriptors.add("mu");
				descriptors.add("variance");
				states.add(descriptors);
				totalColumns *= 2;
				columnsPerSwitch.add(new Integer(2));
			}
		}
		Collections.reverse(states);
		Collections.reverse(columnsPerSwitch);
		numberColumns = totalColumns;
		numberRows = deps.size();

	}

	public int getColumnCount() {
		return numberColumns;
	}

	public int getRowCount() {
		return numberRows;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		int calculatedIndex = 0;
		calculatedIndex = (int) Math.floor((double) columnIndex
				/ (double) columnsPerSwitch.get(rowIndex));
		calculatedIndex = calculatedIndex % states.get(rowIndex).size();
		return states.get(rowIndex).get(calculatedIndex);
	}

}
