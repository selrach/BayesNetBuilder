package selrach.bnetbuilder.model.distributions.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * A conditional distribution potentially has parents to condition on. Examples
 * of this would be a linear Gaussian distribution in which a Gaussian has one
 * or more continuous parents.
 * 
 * One thing to note, as a convention, all discrete parent variables should come
 * before continuous variables in any distribution construction. Users of this
 * class will need to keep track of their individual ordering.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface ConditionalDistribution {

	/**
	 * This should be -1 if the distribution is continuous
	 * 
	 * @return
	 */
	public int getNumberStates();

	/**
	 * Returns the number of states for each dimension, this should be null for
	 * any continuous dimension. If this is null, this is a completely
	 * continuous item.
	 * 
	 * @return
	 */
	public int[] getStateLayout();

	/**
	 * Returns the number of dimensions for the current distributions
	 * 
	 * @return
	 */
	public int getNumberDimensions();

	/**
	 * Returns the total number of dimensions that parents have, any input will
	 * input vector for conditionalizing will have to have this many values.
	 * 
	 * @return
	 */
	public int getNumberParentDimensions();

	/**
	 * Returns the total number of states that the conditional discrete parents
	 * can be in
	 * 
	 * @return
	 */
	public int getNumberDiscreteParentStates();

	/**
	 * Returns the number of discrete parents this distribution has
	 * 
	 * @return
	 */
	public int getNumberDiscreteParentDimensions();

	/**
	 * Returns the number of continuous parents this distribution has
	 * 
	 * @return
	 */
	public int getNumberContinuousParentDimensions();

	/**
	 * This grabs a set of likelihood distributions that try to estimate the
	 * actual distribution of a parent given all other parents have a set value
	 * as well as a set value for this distribution's variable.
	 * 
	 * @param sample
	 * @param parentValues
	 * @param ignoreParentIndex
	 * @return
	 * @throws Exception
	 */
	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception;

	/**
	 * This returns the probability distribution post conditionalization on
	 * parents. You will need to handle the ordering of what the parents based
	 * off how this distribution was initialized.
	 * 
	 * @return
	 */
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception;

	/**
	 * This returns the probability of the distribution conditioned on the
	 * current parent variable values
	 * 
	 * @return
	 */
	public double getProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception;

	/**
	 * This returns the log probability of the distribution conditioned on the
	 * current parent variable values
	 * 
	 * @return
	 */
	public double getLogProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception;

	/**
	 * Returns a random sample from this distribution given the current values
	 * in the parents.
	 * 
	 * @param parentValues
	 * @return
	 */
	public DoubleMatrix1D sample(DoubleMatrix1D parentValues) throws Exception;

	/**
	 * Gets a string representing the distribution type
	 * 
	 * @return
	 */
	public String getType();

	/**
	 * This grabs a matrix that specifies the sufficient variables that need to
	 * be set in order to specify the distribution completely. We should be able
	 * to use this to describe the distribution in an xml file, or as a part of
	 * the front end.
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception;

	/**
	 * This sets up a distribution with the current sufficient description. It
	 * is useful to handle persistence as well as front end
	 * displays/manipulations
	 * 
	 * @param descriptor
	 * @throws Exception
	 */
	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception;

	public ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception;

	/**
	 * Sets evidence on one dimension of this distribution
	 * @param index
	 * @param value
	 * @return
	 */
	public ConditionalDistribution setEvidence(int index, double evidence) throws Exception;
	
	/**
	 * Generates the XML description of this distribution
	 * 
	 * @return
	 */
	public String getXMLDescription() throws Exception;

	/**
	 * Generates a flat representation of an instance of a switched probability
	 * state.
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getFlatXMLProbabilityDescription() throws Exception;

	/**
	 * Sets up a distribution given the given layout
	 * 
	 * @param currentStructure
	 * @param conditionalVariables
	 * @param conditionalVariableTimes
	 * @param probabilityDescription
	 * @param indexedProbabilityDescription
	 * @throws Exception
	 */
	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception;

	/**
	 * Initializes sufficient statistics associated with this distribution to a
	 * starting state
	 * 
	 */
	public void initializeSufficientStatistics() throws Exception;

	/**
	 * Gets the SufficientStatistic structure associated with this distribution.
	 * 
	 * @return
	 */
	public void updateSufficientStatisticsWithFactor(
			TransientVariable variable, Factor factor) throws Exception;

	/**
	 * Calculates the distribution and sets it in accordance to the collected
	 * sufficient statistics
	 * 
	 * @return
	 */
	public double updateDistributionWithSufficientStatistic() throws Exception;

	/**
	 * Randomize the distribution
	 */
	public void randomize();

	/**
	 * Reset the distribution back to a default set of values
	 */
	public void reset();

	/**
	 * Makes a deep copy of the distribution
	 * 
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution copy() throws Exception;

	/**
	 * Marginalizes out one dimension. If index is < numDiscreteDimensions, it
	 * marginalizes out that discrete dimension at index, otherwise it
	 * marginalizes out numContinuousDimensions - index dimension in the
	 * continuous domain.
	 * 
	 * @param index
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution marginalize(int index, boolean onParents) throws Exception;

	/**
	 * Marginalizes out a set of variables
	 * 
	 * @param discrete
	 * @param continuous
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception;

	/**
	 * This returns the complement of the distribution returned through
	 * marginalizing the given values
	 * 
	 * @param discrete
	 * @param continuous
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception;

	
	public ConditionalDistribution complement(ConditionalDistribution marginal, Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception;
	/**
	 * This extends the distribution by 1 dimension. If numStates <= 0, then it
	 * extends it in a continuous direction, otherwise it extends it in the
	 * discrete domain by adding a dimension with numStates states
	 * 
	 * @param numStates
	 * @return
	 * @throws Exception
	 *             if there is a problem extending the distribution
	 */
	public int extend(int numStates) throws Exception;

	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception;


	public double getWeighting();

	public void setWeighting(double weighting);
	
}