package selrach.bnetbuilder.model.algorithms.filtering.interfaces;


import selrach.bnetbuilder.model.BayesNetSlice;

/**
 * All filters assume that we have the posterior for previous time and we are
 * only dealing with the current time.  Essentially if we want to do multiple
 * time steps, we need to chain filters along for each time.
 * @author addict
 *
 */
public interface Filter {

	/**
	 * executes the filter for the given model.  In order to set evidence,
	 * before execute is called the values for the variables must be set.
	 * @param slice
	 * @param maxSamples
	 */
	public void execute(BayesNetSlice slice, int maxSamples);
}
