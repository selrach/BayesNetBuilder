package selrach.bnetbuilder.model.distributions.unconditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Random;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import cern.jet.random.Uniform;

/**
 * Handles a multi-state discrete representation through the use of a sigmoid
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Sigmoid extends AbstractUnconditionalDistribution {

	// /
	// /Private Variables
	// /

	/**
	 * Appropriate for binary variable, in general number of states must be >= 2
	 */
	private int numberStates = 2;

	/**
	 * Corresponds to calculated lvalues, its jagged to account for the
	 * different number of states in each dimension
	 */
	private DoubleMatrix1D lvalues;

	/**
	 * Corresponds to the sum of each lvalue
	 */
	private double lvalueSum;

	/**
	 * Number of states in each dimension, mirrors lvalues[x].size()
	 */
	private int[] stateLayout;

	/**
	 * Cache for mean
	 */
	private DoubleMatrix1D muCache = null;

	/**
	 * Uniform random number generator
	 */
	private final Uniform random = Random.getUniform();

	/**
	 * algebra object for matrix manipulation
	 */
	private final Algebra algebra = new Algebra();

	// /
	// /Public Constructors
	// /

	public Sigmoid() throws Exception {
		// Basic sigmoid

	}

	public Sigmoid(int numberStates) throws Exception {
		int[] stateLayout = new int[1];
		stateLayout[0] = numberStates;
		setDistribution(stateLayout, false);
	}

	/**
	 * Makes a uniform multinomial distribution
	 * 
	 * @param stateLayout
	 * @throws Exception
	 */
	public Sigmoid(int[] stateLayout) throws Exception {
		setDistribution(stateLayout, false);
	}

	/**
	 * Makes a multinomial distribution
	 * 
	 * @param stateLayout
	 * @param random
	 *            if set, it picks lvalues from 1 to 10 uniformly at random,
	 *            otherwise all lvalues are set to 1
	 */
	public Sigmoid(int[] stateLayout, boolean random) {
		setDistribution(stateLayout, random);
	}

	/**
	 * Makes the multinomial distri bution with a set of lvalues, each vector
	 * entry can be a different size, this size corresponds to the number of
	 * states in that dimension and the stateLayout will be set accordingly.
	 * 
	 * @param lvalues
	 * @throws Exception
	 */
	public Sigmoid(DoubleMatrix1D lvalues) throws Exception {
		setDistribution(lvalues);
	}

	// /
	// /Public Functions
	// /

	/**
	 * Sets the parameters for the multinomial distribution
	 * 
	 * @param stateLayout
	 * @param random
	 *            if set it will pick lvalues from 1 to 10 uniformly at random,
	 *            otherwise all lvalues are set to 1
	 */
	public void setDistribution(int[] stateLayout, boolean random) {
		muCache = null;
		lvalueSum = 0.0;
		this.stateLayout = stateLayout.clone();
		numberStates = 1;
		for (int i = 0; i < stateLayout.length; i++) {
			numberStates *= stateLayout[i];
		}
		this.lvalues = DoubleFactory1D.dense.make(numberStates);
		this.lvalues.assign(this.random);
		this.lvalueSum = lvalues.aggregate(Functions.plus, Functions.exp);
	}

	public void setDistribution(int[] stateLayout, DoubleMatrix1D lvalues)
			throws Exception {
		muCache = null;
		this.lvalueSum = 0.0;
		this.stateLayout = stateLayout.clone();
		this.numberStates = 1;
		this.numberDimensions = this.stateLayout.length;
		for (int i = 0; i < stateLayout.length; i++) {
			numberStates *= stateLayout[i];
		}
		if (numberStates != lvalues.size()) {
			throw new Exception(
					"The total number of states does not equal how many lvalues given");
		}
		this.lvalues = lvalues.copy();
		this.lvalueSum = lvalues.aggregate(Functions.plus, Functions.exp);
	}

	/**
	 * Makes the multinomial distribution with a set of lvalues, each vector
	 * entry can be a different size, this size corresponds to the number of
	 * states in that dimension and the stateLayout will be set
	 * 
	 * @param lvalues
	 * @throws Exception
	 */
	public void setDistribution(DoubleMatrix1D lvalues) throws Exception {
		muCache = null;
		this.stateLayout = new int[1];
		this.stateLayout[0] = lvalues.size();
		this.lvalues = lvalues.copy();
		this.numberStates = lvalues.size();
		this.numberDimensions = 1;
		this.lvalueSum = lvalues.aggregate(Functions.plus, Functions.exp);
	}

	public void setLValue(int[] state, double value) throws Exception {
		muCache = null;
		int index = getStateIndex(state);
		double oldValue = this.lvalues.getQuick(index);
		this.lvalues.setQuick(index, value);
		oldValue = Math.expm1(oldValue) + 1;
		value = Math.expm1(value) + 1;
		this.lvalueSum = this.lvalueSum - oldValue + value;
	}

	// /
	// /Overridden Functions
	// /

	@Override
	public int getNumberStates() {
		return numberStates;
	}

	@Override
	public DoubleMatrix2D getPrecision() throws Exception {
		return algebra.inverse(getCovariance());
	}

	@Override
	public double getProbability(DoubleMatrix1D elementValues) throws Exception {

		int[] state = new int[numberDimensions];
		for (int i = 0; i < numberDimensions; i++) {
			state[i] = (int) elementValues.getQuick(i);
		}
		return (Math.expm1(lvalues.getQuick(getStateIndex(state))) + 1.)
				/ lvalueSum;
	}

	@Override
	public double getLogProbability(DoubleMatrix1D elementValues)
			throws Exception {
		return Math.log(getProbability(elementValues));
	}

	@Override
	public int[] getStateLayout() {
		return stateLayout.clone();
	}

	@Override
	public String getType() {
		return DistributionConstant.SIGMOID.toString();
	}

	@Override
	public DoubleMatrix1D getExpectedValue() throws Exception {

		if (muCache != null) {
			return muCache;
		}

		DoubleMatrix1D ret = DoubleFactory1D.dense.make(numberDimensions, 0.0);
		for (int i = 0; i < numberStates; i++) {
			double p = (Math.expm1(lvalues.getQuick(i)) + 1.) / lvalueSum;
			int ind = i;
			for (int j = numberDimensions - 1; j >= 0; j--) {
				int states = stateLayout[j];
				ret.setQuick(j, p * (ind % states) + ret.getQuick(j));
				ind = ind / states;
			}
		}

		return muCache = ret;

	}

	@Override
	public DoubleMatrix2D getCovariance() throws Exception {
		// TODO multidimensional discrete variance;
		throw new Exception("Variance not handled yet ");
	}

	@Override
	public DoubleMatrix1D sample() throws Exception {
		/*
		 * The idea behind this is to take a uniform sampling, then subtract off
		 * the probability of each state along each dimension until we hit 0 or
		 * less than 0, this will transform our probability into the space that
		 * we want for this sample. Each dimension is guaranteed to add up to 1,
		 * so we know we will sample properly.
		 */
		DoubleMatrix1D sample = DoubleFactory1D.dense.make(numberDimensions);
		double v = random.nextDouble();
		for (int i = 0; i < numberStates; i++) {
			v -= (Math.expm1(lvalues.getQuick(i)) + 1.) / lvalueSum;
			if (v <= 0) {
				int[] state = getStateMatrix(i);
				for (int j = 0; j < state.length; j++) {
					sample.setQuick(j, state[j]);
				}
				return sample;
			}
		}
		return sample;
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		list.add(mp = new ArrayList<DistributionDescriptor>());
		for (int i = 0; i < numberStates; i++) {
			mp.add(new DistributionDescriptor(String.valueOf(i), lvalues
					.getQuick(i), "L-value for [$@" + i + "$$]"));
		}
		return list;
	}

	@Override
	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception {
		if (descriptor.size() != 1) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of switching states");
		}
		List<DistributionDescriptor> mp = descriptor.get(0);
		if (mp.size() != numberStates) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of descriptors");
		}
		for (int i = 0; i < numberStates; i++) {
			lvalues.set(i, mp.get(i).getValue());
		}
		this.lvalueSum = lvalues.aggregate(Functions.plus, Functions.exp);
	}

	@Override
	public String getXMLDescription() throws Exception {
		StringBuilder sb = new StringBuilder("<dpis>\n");
		sb.append("<dpi>");
		sb.append(getFlatXMLProbabilityDescription());
		sb.append("</dpi>\n");
		sb.append("</dpis>\n");
		return sb.toString();
	}

	@Override
	public String getFlatXMLProbabilityDescription() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numberStates; i++) {
			sb.append(" ");
			sb.append(lvalues.getQuick(i));
		}
		return sb.toString();
	}

	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (conditionalVariables.size() != 0) {
			throw new Exception(
					"Parents don't make sense for Sigmoid distribution");
		}
		if (conditionalVariableTimes.size() != 0) {
			throw new Exception(
					"How do you have a parent time on a Sigmoid distribution?");
		}
		if (indexedProbabilityDescription.size() != 0) {
			throw new Exception(
					"You have a condional map with a Sigmoid distribution?  Something is wrong.");
		}
		String[] probs = probabilityDescription.split(" ");
		if (probs.length != numberStates) {
			throw new Exception(
					"You have a different number of probabilities than possible states in this Sigmoid");
		}
		for (int i = 0; i < numberStates; i++) {
			lvalues.setQuick(i, Double.parseDouble(probs[i]));
		}
	}

	public UnconditionalDistribution copy() throws Exception {
		throw new Exception("need to create copy for sigmoid");
	}

	public void initializeSufficientStatistics() throws Exception {
		// TODO Auto-generated method stub

	}

	public double updateDistributionWithSufficientStatistic() throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	public void updateSufficientStatisticsWithFactor(
			TransientVariable variable, Factor factor) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void randomize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public ConditionalDistribution setEvidence(int index, double evidence) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution marginalize(int index, boolean onDependencies)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int extend(int numStates) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution complement(ConditionalDistribution marginal,
			Collection<Tuple> discrete, Collection<Tuple> continuous)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
