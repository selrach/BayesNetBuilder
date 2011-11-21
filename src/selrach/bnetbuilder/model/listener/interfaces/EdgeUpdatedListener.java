package selrach.bnetbuilder.model.listener.interfaces;

import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * Listener interface for edge update notifications, useful for front end to
 * hook into back end model changes.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface EdgeUpdatedListener {

	/**
	 * Notifies that an edge has been added to the model.
	 * 
	 * @param fromVariable
	 *            - where the edge is originating
	 * @param toVariable
	 *            - where the edge is going
	 * @param timeSeparation
	 *            - how far in the past are we refering to. 0 means
	 *            intra-timeslice. 1 is one step back..etc
	 */
	public void addEdge(RandomVariable fromVariable, RandomVariable toVariable,
			int timeSeparation);

	/**
	 * Notifies that an edge has been removed from the model
	 * 
	 * @param fromVariable
	 *            - where the edge is originating
	 * @param toVariable
	 *            - where the edge is going
	 * @param timeSeparation
	 *            - how far in the past are we refering to. 0 means
	 *            intra-timeslice. 1 is one step back..etc
	 */
	public void removeEdge(RandomVariable fromVariable,
			RandomVariable toVariable, int timeSeparation);
}
