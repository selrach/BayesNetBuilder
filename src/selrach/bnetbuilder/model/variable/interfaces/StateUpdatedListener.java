package selrach.bnetbuilder.model.variable.interfaces;

import selrach.bnetbuilder.model.variable.DiscreteVariable;

/**
 * Listener called when states have been updated
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface StateUpdatedListener {
	public void statesUpdated(DiscreteVariable variable);
}
