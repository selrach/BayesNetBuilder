package selrach.bnetbuilder.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * This represents a single slice in the network that is generated for a
 * particular time
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class BayesNetSlice {

	/**
	 * This should be a direct reflection of the current topologically sorted
	 * variables. There are also prior variables that are not tracked, but are
	 * referenced by variables in this list. They should exist within an
	 * randomvariable's transient variable list.
	 */
	List<TransientVariable> currentVariables;
	Map<String, TransientVariable> currentVariableMap;

	BayesNetSlice(List<TransientVariable> variables,
			Map<String, TransientVariable> map) {
		currentVariables = variables;
		currentVariableMap = map;
	}

	public List<TransientVariable> getVariables() {
		return Collections.unmodifiableList(currentVariables);
	}

	public TransientVariable getVariable(String randomVariableId) {
		return currentVariableMap.get(randomVariableId);
	}
}
