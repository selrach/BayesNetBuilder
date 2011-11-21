package selrach.bnetbuilder.data.interfaces;

import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.data.ElementMetadata;
import selrach.bnetbuilder.model.listener.interfaces.VariableUpdatedListener;
import selrach.bnetbuilder.model.variable.interfaces.StateUpdatedListener;

/**
 * This is intended to facilitate multiple types of data sources towards a
 * standard parsing format for the system
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface DataAccessor extends VariableUpdatedListener,
		StateUpdatedListener {

	/**
	 * This gets the data associated with a trial
	 * 
	 * @param which
	 * @return
	 */
	public List<List<String>> getTrial(int which);

	/**
	 * Returns a mapping from a variable id to an element in the metadata
	 * 
	 * @return
	 */
	public Map<String, ElementMetadata> getVariableToElementMetadataMap();

	/**
	 * Returns the number of available trials in the datasource
	 * 
	 * @return
	 */
	public int getNumberTrials();

	/**
	 * Returns the number of timesteps that each trial contains
	 * 
	 * @return
	 */
	public int getNumberTimestepsPerTrial();

	/**
	 * Gets a text description of the datasource, useful for humans
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * Returns the order of the variables in a list.
	 * 
	 * @return
	 */
	public List<String> getVariablesInList();

}
