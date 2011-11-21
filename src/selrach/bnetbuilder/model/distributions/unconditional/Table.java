package selrach.bnetbuilder.model.distributions.unconditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Random;
import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.PlusMult;
import cern.jet.random.Uniform;

/**
 * Creates a basic tabular distribution, can handle many dimensions
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Table extends AbstractUnconditionalDistribution {

	// 
	// Private Variables
	// 

	/**
	 * This is the number of total states in this distribution,
	 * 
	 * 1 is appropriate for singular distribution...essentially the identity
	 * distribution
	 */

	/**
	 * sets up how many states are in each dimension of this distribution
	 */
	// private int[] stateLayout;
	/**
	 * algebra object for matrix manipulation
	 */
	private final Algebra algebra = new Algebra();

	/**
	 * Random uniform value generator
	 */
	private final Uniform random = Random.getUniform();

	/**
	 * Cache for mean
	 */
	private DoubleMatrix1D muCache = null;

	/**
	 * Cache for covariance
	 */
	private DoubleMatrix2D covarianceCache = null;

	/**
	 * probability storage for each state of this distribution, sum should total
	 * 1. For multi-dimensional probabilities, we enumerate out everything based
	 * off the stateLayout settings, for example if we have 3 dimensions with 3
	 * states apiece, we get a total of 27 different states, the index ordering
	 * is as follows 0 - 000, 1-001, 2-002, 3-010, 4-011, 5-012, 6-020, 7-021,
	 * 8-022, 9-100, ..., 25-221, 26-222
	 */
	private DoubleMatrix1D probabilities;

	/**
	 * This is used for sufficient statistics
	 */
	private double[] counts;

	// 
	// Constructors
	// 

	/**
	 * Makes a singular distribution.
	 * 
	 */
	public Table() {
		numberDimensions = 1;
		numberStates = 1;
		stateLayout = new int[] { numberStates };
		createUniform(numberStates);
	}

	public Table(Table copy) {
		super(copy);
		this.stateLayout = copy.stateLayout.clone();
		this.numberStates = copy.numberStates;
		this.probabilities = copy.probabilities.copy();
		counts = new double[numberStates];
	}

	/**
	 * Creates a 1d discrete distribution with the given probabilities
	 * 
	 * @param probabilities
	 * @throws Exception
	 */
	public Table(DoubleMatrix1D probabilities) throws Exception {
		numberStates = probabilities.size();
		stateLayout = new int[] { numberStates };
		this.probabilities = probabilities.copy();
		counts = new double[numberStates];
		// normalize();
	}

	/**
	 * Creates a 1d discrete distribution with the given probabilities
	 * 
	 * @param probabilities
	 * @throws Exception
	 */
	public Table(double[] probabilities) throws Exception {
		numberStates = probabilities.length;
		stateLayout = new int[] { numberStates };
		this.probabilities = DoubleFactory1D.dense.make(probabilities);
		counts = new double[numberStates];
		// normalize();
	}

	/**
	 * Creates a 1d uniform discrete distribution with the right number of
	 * states
	 * 
	 * @param numberStates
	 * @throws Exception
	 */
	public Table(int numberStates) throws Exception {
		this(numberStates, false);
	}

	/**
	 * Creates a discrete distribution
	 * 
	 * @param numberStates
	 * @param random
	 *            true-randomized initialization false-uniform
	 */
	public Table(int numberStates, boolean random) {
		stateLayout = new int[] { numberStates };
		if (random) {
			createRandom(numberStates);
		} else {
			createUniform(numberStates);
		}
	}

	/**
	 * Creates a multi-dimensional uniform discrete distribution with the given
	 * state layout
	 * 
	 * @param stateLayout
	 * @throws Exception
	 */
	public Table(int[] stateLayout) throws Exception {
		this(stateLayout, false);
	}

	/**
	 * Creates a multi-dimensional discrete distribution
	 * 
	 * @param stateLayout
	 * @param random
	 *            true-randomized initialization false-uniform
	 * @throws Exception
	 */
	public Table(int[] stateLayout, boolean random) throws Exception {
		if (stateLayout.length <= 0) {
			throw new Exception(
					"Discrete variables must have at least 1 dimension");
		}
		numberStates = 1;
		for (Integer i : stateLayout) {
			if (i < 2) {
				throw new Exception("All dimensions must at least be binary!");
			}
			numberStates *= i;
		}
		this.stateLayout = stateLayout.clone();
		this.numberDimensions = this.stateLayout.length;
		if (random) {
			createRandom(numberStates);
		} else {
			createUniform(numberStates);
		}
	}

	/**
	 * Creates a multi-dimensional discrete distribution with the given state
	 * layout and probabilities
	 * 
	 * @param stateLayout
	 * @param probabilities
	 * @throws Exception
	 */
	public Table(int[] stateLayout, DoubleMatrix1D probabilities)
			throws Exception {
		if (stateLayout.length <= 0) {
			throw new Exception(
					"Discrete variables must have at least 1 dimension");
		}
		numberStates = 1;
		for (Integer i : stateLayout) {
			// if(i < 2) throw new
			// Exception("All dimensions must at least be binary!");
			numberStates *= i;
		}
		if (numberStates != probabilities.size()) {
			throw new Exception(
					"Dimensions of stateLayout and probabilities must match.");
		}
		this.stateLayout = stateLayout.clone();
		this.numberDimensions = this.stateLayout.length;
		this.probabilities = probabilities.copy();
		counts = new double[numberStates];
		// normalize();
	}

	// /
	// /Private Functions
	// /

	/**
	 * Creates a uniform distribution for the number of states we have
	 * 
	 * @param numStates
	 */
	private void createUniform(int numStates) {
		numberStates = numStates;
		counts = new double[numberStates];
		probabilities = DoubleFactory1D.dense.make(numberStates,
				1.0 / numberStates);
	}

	/**
	 * Creates a randomized initialization vector using a uniform distribution.
	 * 
	 * @param numStates
	 */
	private void createRandom(int numStates) {
		numberStates = numStates;
		counts = new double[numberStates];
		probabilities = DoubleFactory1D.dense.make(numberStates);
		probabilities.assign(random);
		normalize();
	}

	// /
	// /Public Functions
	// /
	/**
	 * Normalizes this discrete variable so that the probabilities add up to 1
	 * 
	 */
	public void normalize() {
		normalize(probabilities);
	}

	private void normalize(DoubleMatrix1D p) {
		double sum = p.zSum();
		if (Math.abs(sum) < eps) {
			for (int i = 0; i < p.size(); i++) {
				p.setQuick(i, 1.0 / numberStates);
			}
		} else if (Math.abs(sum - 1.0) > eps) {
			for (int i = 0; i < p.size(); i++) {
				p.setQuick(i, p.getQuick(i) / sum);
			}
		}
	}

	/**
	 * Sets the probability for a given combination of states, relies on you to
	 * normalize after you are done setting different probabilities
	 * 
	 * @param state
	 * @param probability
	 */
	public void setProbability(int[] currentState, double probability) {
		muCache = null;
		covarianceCache = null;
		int i;
		int ind = 0;
		int factor = 1;
		for (i = numberDimensions - 1; i >= 0; i--)// nD-1
		{
			ind += factor * currentState[i];
			factor *= stateLayout[i];
			// ind = (stateLayout[i]) * (ind + (int) currentState[i]);
		}
		// ind += (int)currentState[i];
		this.probabilities.setQuick(ind, probability);
	}

	@Override
	public double getProbability(DoubleMatrix1D elementValues) throws Exception {

		if (numberDimensions == 0 || numberStates == 1) {
			return probabilities.getQuick(0);
		}
		return probabilities.getQuick(Utility.calculateIndex(elementValues,
				stateLayout));
	}

	public DoubleMatrix1D getProbabilityVector() {
		return probabilities.copy();
	}

	/**
	 * Sets the state probabilities. This is much quicker than piecewise setting
	 * if you are changing all the probabilities
	 * 
	 * @param probabilities
	 */
	public void setProbability(DoubleMatrix1D probabilities) {
		muCache = null;
		covarianceCache = null;
		this.probabilities = probabilities.copy();
	}

	/**
	 * Randomizes the probabilities of each state.
	 * 
	 */
	public void randomize() {
		muCache = null;
		covarianceCache = null;
		probabilities.assign(random);
		normalize();
	}

	// /
	// /Overridden Functions
	// /

	@Override
	public DoubleMatrix1D getExpectedValue() throws Exception {
		if (muCache != null) {
			return muCache;
		}

		DoubleMatrix1D ret = DoubleFactory1D.dense.make(numberDimensions, 0.0);
		for (int i = 0; i < numberStates; i++) {
			double p = probabilities.getQuick(i);
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
	public double getLogProbability(DoubleMatrix1D elementValues)
			throws Exception {
		return Math.log(getProbability(elementValues));
	}

	@Override
	public int getNumberDimensions() {
		return numberDimensions;
	}

	@Override
	public int getNumberStates() {
		return numberStates;
	}

	@Override
	public DoubleMatrix2D getPrecision() throws Exception {
		return algebra.inverse(getCovariance());
	}

	@Override
	public int[] getStateLayout() {
		return stateLayout.clone();
	}

	@Override
	public String getType() {
		return DistributionConstant.TABLE.toString();
	}

	@Override
	public DoubleMatrix2D getCovariance() throws Exception {
		if (covarianceCache != null) {
			return covarianceCache;
		}
		if (numberDimensions == 1) {
			// This should be a bit faster than the quad loop for general
			// covariance
			DoubleMatrix2D ret = DoubleFactory2D.dense.make(numberDimensions,
					numberDimensions);

			double eV = 0, eV2 = 0;
			// we can skip 0
			for (int j = 1; j < numberStates; j++) {
				// This does not work when dimensions > 1
				double tmp = j * probabilities.getQuick(j);
				eV += tmp;
				eV2 += j * tmp;
			}
			ret.setQuick(0, 0, eV2 - eV * eV);
			return covarianceCache = ret;
		} else {
			DoubleMatrix1D ev = DoubleFactory1D.dense.make(numberDimensions,
					0.0);
			DoubleMatrix2D ev2 = DoubleFactory2D.dense.make(numberDimensions,
					numberDimensions, 0.0);
			for (int i = 0; i < numberStates; i++) {
				double p = probabilities.getQuick(i);
				int ind = i;
				for (int j = numberDimensions - 1; j >= 0; j--) {
					int s = stateLayout[j];
					int v = ind % s;
					double tmp = v * p;
					ev.setQuick(j, tmp + ev.getQuick(j));
					for (int m = 0; m < numberStates; m++) {
						double p2 = probabilities.getQuick(m);
						int ind2 = m;
						for (int n = numberDimensions - 1; n >= 0; n--) {
							int v2 = ind2 % stateLayout[n];
							double tmp2 = v2 * tmp * p2;
							ev2.setQuick(j, n, tmp2 + ev2.getQuick(j, n));
						}
					}
					ind = ind / s;
				}
			}
			DoubleMatrix2D mu2 = algebra.multOuter(ev, ev, null);
			return covarianceCache = ev2.assign(mu2, PlusMult.minusMult(1));
		}
	}

	@Override
	public DoubleMatrix1D sample() throws Exception {
		/*
		 * The idea behind this is to take a uniform sampling, then subtract off
		 * the probability of each state along each dimension until we hit 0 or
		 * less than 0, this will transform our probability into the space that
		 * we want for this sample. Each dimension is guarenteed to add up to 1,
		 * so we know we will sample properly.
		 */
		DoubleMatrix1D sample = DoubleFactory1D.dense.make(numberDimensions);
		DoubleMatrix1D v = DoubleFactory1D.dense.make(numberDimensions);
		v.assign(random);
		for (int i = 0; i < numberStates; i++) {
			int ind = i;
			double p = probabilities.getQuick(i);
			for (int j = 0; j < numberDimensions; j++) {
				int s = stateLayout[j];
				int val = ind % s;
				double smp = v.getQuick(j);
				if (smp > 0) {
					smp -= p;
					v.setQuick(j, smp);
					if (smp <= eps) // just in case our math isn't being
					// friendly, the potential distortion this
					// adds is negligible
					{
						sample.setQuick(j, val);
					}
				}
				ind /= s;
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
			mp.add(new DistributionDescriptor("$@" + i + "$$", probabilities
					.get(i), "Probability of [$@" + i + "$$]"));
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
			probabilities.set(i, mp.get(i).getValue());
		}
		normalize();
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
			sb.append(probabilities.getQuick(i));
		}
		return sb.toString();
	}

	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (conditionalVariables != null && conditionalVariables.size() != 0) {
			throw new Exception(
					"Parents don't make sense for table distribution");
		}
		if (conditionalVariableTimes != null
				&& conditionalVariableTimes.size() != 0) {
			throw new Exception(
					"How do you have a parent time on a table distribution?");
		}
		if (indexedProbabilityDescription != null
				&& indexedProbabilityDescription.size() != 0) {
			throw new Exception(
					"You have a condional map with a table distribution?  Something is wrong.");
		}
		String[] probs = probabilityDescription.split(" ");
		if (probs.length != numberStates) {
			throw new Exception(
					"You have a different number of probabilities than possible states in this table");
		}
		for (int i = 0; i < numberStates; i++) {
			probabilities.setQuick(i, Double.parseDouble(probs[i]));
		}
	}

	public UnconditionalDistribution copy() throws Exception {
		return new Table(this);
	}

	public double updateDistributionWithSufficientStatistic() throws Exception {
		// There are no parents to worry about, update directly
		double dif = 0.0;
		DoubleMatrix1D newProb = DoubleFactory1D.dense.make(counts);
		normalize(newProb);
		for (int i = 0; i < counts.length; i++) {
			dif += Math.abs(probabilities.getQuick(i) - newProb.getQuick(i));
		}
		probabilities = newProb;
		return dif;
	}

	public void initializeSufficientStatistics() throws Exception {
		if (counts == null || counts.length != numberStates) {
			counts = new double[numberStates];
		}
		for (int i = 0; i < numberStates; i++) {
			counts[i] = 0.0;
		}
	}

	public void updateSufficientStatisticsWithFactor(
			TransientVariable variable, Factor factor) throws Exception {
		UnconditionalDistribution dist = (UnconditionalDistribution) factor
				.getDistribution();
		if (dist.getNumberStates() != numberStates) {
			throw new Exception("Invalid factor to update table");
		}
		DoubleMatrix1D element = DoubleFactory1D.dense.make(1, 0.0);
		for (int i = 0; i < numberStates; i++) {
			element.setQuick(0, i);
			counts[i] += dist.getProbability(element);
		}
	}

	@Override
	public void reset() {
		createUniform(numberStates);
	}

	public ConditionalDistribution setEvidence(int index, double evidence)
			throws Exception {
		DoubleMatrix1D newProps = DoubleFactory1D.dense.make(numberStates
				/ stateLayout[index]);
		DoubleMatrix1D indices = DoubleFactory1D.dense.make(numberDimensions);
		List<Integer> ignore = new ArrayList<Integer>();
		ignore.add(index);
		indices.setQuick(index, evidence);
		int i = 0, j = 0;
		do {
			newProps.setQuick(i++, probabilities.getQuick(Utility
					.calculateIndex(indices, stateLayout)));
		} while (Utility.incrementIndice(indices, stateLayout, ignore));
		int[] newLayout = new int[stateLayout.length - 1];
		for (i = 0, j = 0; i < stateLayout.length; i++, j++) {
			if (index == i) {
				j--;
			} else {
				newLayout[j] = stateLayout[i];
			}
		}

		if (newLayout.length == 0) {
			newLayout = new int[1];
			newLayout[0] = 1;
		}

		return new Table(newLayout, newProps);
	}

	public UnconditionalDistribution marginalize(int index,
			boolean onDependencies) throws Exception {
		List<Integer> ignore = new ArrayList<Integer>();
		ignore.add(index);

		int numStates = 1;
		int[] stateLayout = new int[this.stateLayout.length - 1];
		if (stateLayout.length == 0) {
			stateLayout = new int[1];
			stateLayout[0] = 1;
		}
		for (int i = 0, j = 0; i < this.stateLayout.length; i++) {
			if (index != i) {
				stateLayout[j] = this.stateLayout[i];
				numStates *= stateLayout[j++];
			}
		}

		DoubleMatrix1D indices = DoubleFactory1D.dense.make(
				this.stateLayout.length, 0);

		double[] probs = new double[numStates];

		for (int i = 0; i < this.stateLayout[index]; i++) {
			indices.setQuick(ignore.get(0), i);
			int j = 0;
			do {
				probs[j++] += getProbability(null, indices);
			} while (Utility.incrementIndice(indices, this.stateLayout, ignore));
		}

		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(probs);
		return new Table(stateLayout, probabilities);
	}

	@Override
	public int extend(int numStates) throws Exception {
		// Okay we need to extend the statelayout and copy the probabilities
		// over
		if (numStates <= 0) {
			throw new Exception(
					"Table is a discrete distribution, cannot extend by <=0 states");
		}
		int[] newLayout;
		if (this.numberStates != 1) {
			newLayout = new int[stateLayout.length + 1];
		} else {
			newLayout = stateLayout;
		}
		this.numberStates = 1;
		for (int i = 0; i < stateLayout.length; i++) {
			this.numberStates *= newLayout[i] = stateLayout[i];
		}
		this.numberStates *= numStates;
		newLayout[newLayout.length - 1] = numStates;

		probabilities = DoubleFactory1D.dense.repeat(probabilities, numStates);

		stateLayout = newLayout;
		numberDimensions = stateLayout.length;
		return stateLayout.length - 1;
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
		List<Quadruple> indices = new ArrayList<Quadruple>();
		for (Tuple t : discrete) {
			indices.add(new Quadruple(t.r, t.r, t.a, t.id));
		}

		return Operation.divide(this, (Table) marginal, indices);
	}

}
