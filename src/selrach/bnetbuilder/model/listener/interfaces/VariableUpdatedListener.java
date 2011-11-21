package selrach.bnetbuilder.model.listener.interfaces;

import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * Notification listener of different variable events.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface VariableUpdatedListener {

	void updateVariable(RandomVariable variable);

	void addVariable(RandomVariable variable);

	void removeVariable(RandomVariable variable);
}
