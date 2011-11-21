package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.variable.interfaces.StateUpdatedListener;

/**
 * A DiscreteVariable is a variable with a limited amount of states that it can
 * be in.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DiscreteVariable extends RandomVariable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3487327191227511958L;

	static private class StateUpdatedEvent {
		static private StateUpdatedEvent instance = null;

		static public StateUpdatedEvent getInstance() {
			if (instance == null) {
				instance = new StateUpdatedEvent();
			}
			return instance;
		}

		private StateUpdatedEvent() {
		}

		private final ArrayList<StateUpdatedListener> subscribers = new ArrayList<StateUpdatedListener>();

		protected void statesUpdated(DiscreteVariable variable) {
			for (StateUpdatedListener l : subscribers) {
				l.statesUpdated(variable);
			}
		}

	}

	static private StateUpdatedEvent stateUpdatedEvent = StateUpdatedEvent
			.getInstance();

	static public void subscribe(StateUpdatedListener listener) {
		if (!stateUpdatedEvent.subscribers.contains(listener)) {
			stateUpdatedEvent.subscribers.add(listener);
		}
	}

	static public void unsubscribe(StateUpdatedListener listener) {
		if (stateUpdatedEvent.subscribers.contains(listener)) {
			stateUpdatedEvent.subscribers.remove(listener);
		}
	}

	public DiscreteVariable(DynamicBayesNetModel model) {
		super(model);
		this.name = "New Discrete Variable";
		states.add("True");
		states.add("False");
		states.size();
		subscribe(model);
	}

	@Override
	public String getType() {
		return "discrete";
	}

	ArrayList<String> states = new ArrayList<String>();

	private int state = -1;
	
	/**
	 * Sets up the state list
	 * 
	 * @param in_states
	 * @throws Exception
	 */
	public void setStates(List<String> in_states) throws Exception {
		if (in_states.size() < 2) {
			throw new Exception("Discrete variable must at least be binary!");
		}
		states.clear();
		for (String t : in_states) {
			states.add(new String(t));
		}
		stateUpdatedEvent.statesUpdated(this);
	}

	public List<String> getStates() {
		return Collections.unmodifiableList(states);
	}

	public void addState(String name) {
		states.add(new String(name));
		cpds.clear();
		stateUpdatedEvent.statesUpdated(this);
	}

	public void setState(int index, String name) {
		if (index >= states.size() || index < 0) {
			throw new IndexOutOfBoundsException();
		}
		states.set(index, name);
		stateUpdatedEvent.statesUpdated(this);
	}

	public void removeState(int index) {
		if (index < 0 || index >= states.size() || states.size() <= 2) {
			return;
		}
		cpds.clear();
		states.remove(index);
		stateUpdatedEvent.statesUpdated(this);
	}


	/**
	 * Returns the current state of the variable, -1 if not known
	 * 
	 * @return
	 */
	@Override
	public double getValue() throws Exception {
		if (state == -1) {
			throw new Exception("Bad discrete value");
		}
		return state;
	}

	public int getDiscreteValue() throws Exception {
		if (state == -1) {
			throw new Exception("Bad discrete value!");
		}
		return state;
	}

	@Override
	public void setValue(double state) throws Exception {
		int s = (int) Math.round(state);
		if (s < 0 || s >= states.size()) {
			throw new Exception("Trying to set invalid state.");
		}
		this.state = s;
	}

	public void setValue(int state) throws Exception {
		if (state < 0 || state >= states.size()) {
			throw new Exception("Trying to set invalid state.");
		}
		this.state = state;
	}

	@Override
	public void clearValue() {
		state = -1;
	}

	public String getCurrentState() throws Exception {
		if (state == -1) {
			throw new Exception("Bad State");
		}
		return states.get(state);
	}

	@Override
	public boolean hasValue() {
		return state != -1;
	}

	@Override
	public RandomVariable copy() {
		return new DiscreteVariable(this.model);
	}

	/*
	 * private int constructHeader(List<StatDescriptor> sd, int parentIndex,
	 * List<RandomVariable> parents, int headerIndex, String str) {
	 * RandomVariable var = null; while(var==null && parentIndex <
	 * parents.size()) { var = parents.get(parentIndex++); if(!(var instanceof
	 * DiscreteVariable)) var=null; } if(var==null) {
	 * sd.get(headerIndex).setKey(str); return headerIndex+1; } DiscreteVariable
	 * dv = (DiscreteVariable) var; List<String> states = dv.getStates();
	 * for(String s : states) { headerIndex = constructHeader(sd, parentIndex,
	 * parents, headerIndex, str + ' ' + s); } return headerIndex; }
	 */

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor(int time) {

		List<List<DistributionDescriptor>> stat = null;
		try {
			stat = getCpd(time).getDistributionDescriptor();
			/*
			 * List<StatDescriptor> states = stat.get(0); for(int i=0;
			 * i<states.size(); i++) { states.get(i).setKey(this.states.get(i));
			 * }
			 */
			// List<RandomVariable> parents = new
			// ArrayList<RandomVariable>(getParents(time));
			// constructHeader(states, 0, parents, 0, new String());
		} catch (Exception ex) {

		}

		return stat;
	}

	@Override
	protected String getVariableSpecificXML() {
		StringBuilder sb = new StringBuilder("<stateset>\n");
		for (int i = 0; i < states.size(); i++) {
			sb.append("<statename>");
			sb.append(states.get(i));
			sb.append("</statename>");
		}
		sb.append("</stateset>\n");
		return sb.toString();
	}

}
