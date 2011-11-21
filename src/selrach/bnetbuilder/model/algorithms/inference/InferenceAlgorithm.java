package selrach.bnetbuilder.model.algorithms.inference;

import java.io.PrintStream;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * Contract for inference algorithms to subscribe to
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface InferenceAlgorithm {

	/**
	 * Does this model use random techniques to figure out its marginals?
	 * 
	 * @return
	 */
	public boolean isRandom();

	/**
	 * What is the name of the algorithm?
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Do the execution of this algorithm
	 * 
	 * @param model
	 *            model to run inference algorithm against
	 * @param allMarginals
	 *            do we want to calculate all marginals or a set query?
	 * @param additionalProperties
	 *            map to additional properties for this algorithm
	 * @return Factor of the query desired or null if we are calculating all
	 *         marginals
	 * @throws Exception
	 */
	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			Map<String, Object> additionalProperties, PrintStream updateTracking)
			throws Exception;

	/**
	 * Gets a factor that represents the marginal of a transient variable and
	 * its parents.
	 * 
	 * @param model
	 * @param variable
	 * @param additionalProperties
	 * @return
	 */
	public Factor getFactorForSufficientStatistics(DynamicBayesNetModel model,
			TransientVariable variable, Map<String, Object> additionalProperties)
			throws Exception;

	/**
	 * Notifies the inference algorithm directly that evidence was set. Useful
	 * when we have evidence notification turned off, but still need to do
	 * different inference tasks when new evidence has come in
	 * 
	 * @param model
	 */
	public void evidenceSet(DynamicBayesNetModel model);

	/**
	 * Notifies the inference algorithm that a parameter learning algorithm has
	 * completed. This should not generally be called directly by the learning
	 * algorithm, but rather the function of the same name on the
	 * InferenceAlgorithmFactory should be called so it notifies all the
	 * inference algorithms that this has been done so they can clear out any
	 * objects that rely on the network distributions
	 * 
	 * @param model
	 */
	public void parameterLearningDone(DynamicBayesNetModel model);
}
