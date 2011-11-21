package selrach.bnetbuilder.model.distributions.conditional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * This is a distribution for a discrete variable with only discrete parents.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ConditionalTable extends Mixture {
	
	/**
	 * Generates a uniform conditional probability table with the right
	 * dimensions
	 * 
	 * @param stateLayout
	 * @param numParentDimensions
	 * @param numParents
	 * @throws Exception
	 */
	public ConditionalTable(int[] stateLayout, int[] parentLayout)
			throws Exception {
		this(stateLayout, parentLayout, false);
	}

	/**
	 * Generates a uniform or random conditional probability table with the
	 * right dimensions
	 * 
	 * @param stateLayout
	 * @param numParentDimensions
	 * @param numParents
	 * @param random
	 * @throws Exception
	 */
	public ConditionalTable(int[] stateLayout, int[] parentLayout,
			boolean random) throws Exception {
		setNumberStates(stateLayout);
		setParentStateLayout(parentLayout);
		distributions = new Table[numberDiscreteParentStates];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new Table(numberStates, random);
		}

	}

	/**
	 * Generates a conditional probability table where each row corresponds to a
	 * specific setting of the parents being conditionalized upon.
	 * 
	 * @param stateLayout
	 * @param probabilities
	 * @param numParents
	 * @throws Exception
	 */
	public ConditionalTable(int[] stateLayout, int[] parentLayout,
			DoubleMatrix2D probabilities) throws Exception {
		setNumberStates(stateLayout);
		setParentStateLayout(parentLayout);
		distributions = new Table[numberDiscreteParentStates];
		setProbability(probabilities);

	}

	/**
	 * Generates a conditional probability table where each row corresponds to a
	 * specific setting of the parents being conditionalized upon.
	 * 
	 * @param stateLayout
	 * @param parentLayout
	 * @param probabilities
	 * @throws Exception
	 */
	public ConditionalTable(int[] stateLayout, int[] parentLayout,
			double[][] probabilities) throws Exception {
		setNumberStates(stateLayout);
		setParentStateLayout(parentLayout);
		distributions = new Table[numberDiscreteParentStates];
		setProbability(DoubleFactory2D.dense.make(probabilities));
	}
	
	/**
	 * Creates a deep copy of the conditional table
	 */
	public ConditionalTable(ConditionalTable copy) throws Exception {
		super(copy);

		distributions = new Table[numberDiscreteParentStates];
		for (int i = 0; i < copy.distributions.length; i++) {
			distributions[i] = copy.distributions[i].copy();
		}
	}

	/**
	 * Normalizes all the tables in this distribution
	 */
	public void normalize() {
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			((Table) distributions[i]).normalize();
		}
	}

	/**
	 * Sets the probability for one set of variables,
	 * 
	 * @param parentStateIndex
	 *            This is the index corresponding to the parent we wish to
	 *            set...Its as if you had flattened out the possible settings
	 *            for this variable. So if you had 3 parents with 3 states
	 *            apiece, this value goes from [0, 26]. Its your job to make
	 *            sure the ordering is consistent whenever you access this. This
	 *            function does not do normalizing, this is your responsibility.
	 *            For a more massive set, use setProbability(parentStateIndex,
	 *            probabilities) as it will be faster and will do normalizing
	 * @param currentState
	 *            the state of this distribution you want to set the probability
	 *            for.
	 * @param probability
	 *            the probability you want to set.
	 */
	public void setProbability(int[] parentState, int[] currentState,
			double probability) throws IndexOutOfBoundsException {
		((Table) distributions[Utility
				.calculateIndex(parentState, parentLayout)]).setProbability(
				currentState, probability);
	}

	/**
	 * Sets the all the probabilities associated with a particular parent
	 * setting
	 * 
	 * @param parentStateIndex
	 *            This is the index corresponding to the parent we wish to
	 *            set...Its as if you had flattened out the possible settings
	 *            for this variable. So if you had 3 parents with 3 states
	 *            apiece, this value goes from [0, 26]. Its your job to make
	 *            sure the ordering is consistent whenever you access this. This
	 *            function does not do normalizing, this is your responsibility.
	 *            For a more massive set, use setProbability(parentStateIndex,
	 *            probabilities) as it will be
	 * @param probabilities
	 *            probabilities you want to assign, this will be normalized
	 *            after being set.
	 */
	public void setProbability(int[] parentState, DoubleMatrix1D probabilities) {
		((Table) distributions[Utility
				.calculateIndex(parentState, parentLayout)])
				.setProbability(probabilities);
	}

	/**
	 * Sets the probabilites of this distribution.
	 * 
	 * @param probabilities
	 * @throws Exception
	 */
	public void setProbability(DoubleMatrix2D probabilities) throws Exception {
		if (this.numberDiscreteParentStates != probabilities.rows()) {
			throw new Exception(
					"probabilites do not have enough columns to match the states layed out in stateLayout");
		}
		if (this.numberStates != probabilities.columns()) {
			throw new Exception(
					"Number of dimensions and probabilities do not match up");
		}

		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new Table(stateLayout, probabilities.viewRow(i));
		}
	}

	
	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		int numStatesInParent = stateLayout[ignoreParentIndex];
		double[] probabilities = new double[numStatesInParent];
		for (int i = 0; i < numStatesInParent; i++) {
			parentValues.set(ignoreParentIndex, i);
			probabilities[i] = getProbability(parentValues, sample);
		}
		return new Table(probabilities);
	}

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		return ((Table) distributions[Utility.calculateIndex(parentValues,
				parentLayout)]);
	}

	@Override
	public int[] getStateLayout() {
		return stateLayout;
	}

	@Override
	public String getType() {
		return DistributionConstant.CONDITIONAL_TABLE.toString();
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			list.add(mp = new ArrayList<DistributionDescriptor>());
			mp.addAll(distributions[i].getDistributionDescriptor().get(0));
		}
		return list;
	}

	@Override
	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception {
		if (descriptor.size() != numberDiscreteParentStates) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of switching states");
		}
		List<List<DistributionDescriptor>> tmp = new ArrayList<List<DistributionDescriptor>>();
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			List<DistributionDescriptor> mp = descriptor.get(i);
			tmp.add(mp);
			distributions[i].setDistributionDescriptor(tmp);
			tmp.clear();
		}
	}

	@Override
	public String getXMLDescription() throws Exception {
		StringBuilder sb = new StringBuilder("<dpis>\n");
		int[] currentIndex = new int[numberDiscreteParentDimensions];
		int ndpdm1 = numberDiscreteParentDimensions - 1;
		int i = 0;
		for (int j = 0; j < numberDiscreteParentStates; j++) {
			sb.append("<dpi index=\"");
			for (i = 0; i < numberDiscreteParentDimensions; i++) {
				sb.append(" ");
				sb.append(currentIndex[i]);
			}

			sb.append("\">");
			sb.append(this.distributions[j].getFlatXMLProbabilityDescription());
			sb.append("</dpi>\n");
			for (i = ndpdm1; i >= 0; i--) {
				if (currentIndex[i] >= parentLayout[i] - 1) {
					currentIndex[i] = 0;
				} else {
					currentIndex[i]++;
					break;
				}
			}
		}
		sb.append("</dpis>");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeselrach.bnetbuilder.model.distributions.conditional.
	 * AbstractConditionalDistribution#getNumberStates()
	 */
	@Override
	public int getNumberStates() {
		return distributions[0].getNumberStates();
	}

	@Override
	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (probabilityDescription != null) {
			throw new Exception(
					"It does not make sense to have a non-indexed probability string in the ConditionalTable");
		}
		if (conditionalVariables.size() == 0) {
			throw new Exception(
					"You need some parents for the ConditionalTable");
		}
		if (conditionalVariableTimes.size() == 0) {
			throw new Exception(
					"You need some parents for the ConditionalTable");
		}
		if (indexedProbabilityDescription.size() == 0) {
			throw new Exception(
					"You need some indexed probabilitydescription strings for the ConditionalTable?  Something is wrong.");
		}
		int[] indexMap = generateIndexMap(currentStructure,
				conditionalVariables, conditionalVariableTimes);
		int[] parentIndex = new int[currentStructure.size()];
		for (Entry<String, String> entry : indexedProbabilityDescription
				.entrySet()) {
			String[] indices = entry.getKey().split(" ");
			if (indices.length != parentIndex.length) {
				throw new Exception("Index has wrong number of entries");
			}
			for (int i = 0; i < parentIndex.length; i++) {
				parentIndex[indexMap[i]] = Integer.parseInt(indices[i]);
			}

			Table tbl = ((Table) distributions[Utility.calculateIndex(
					parentIndex, parentLayout)]);
			tbl.setup(null, null, null, entry.getValue(), null);
		}
	}

	public void initializeSufficientStatistics() throws Exception {

		for (int i = 0; i < numberDiscreteParentDimensions; i++) {
			distributions[i].initializeSufficientStatistics();
		}
	}

	public double updateDistributionWithSufficientStatistic() throws Exception {
		double dif = 0.0;
		for (ConditionalDistribution tbl : distributions) {
			dif += tbl.updateDistributionWithSufficientStatistic();
		}
		return dif;
	}

	public void updateSufficientStatisticsWithFactor(
			TransientVariable variable, Factor factor) throws Exception {
		List<TransientVariable> factorDeps = factor.getDependencies();
		List<TransientVariable> parentOrder = variable.getParents();
		if (parentLayout.length != parentOrder.size()
				&& parentLayout.length + 1 != factorDeps.size()) {
			throw new Exception("Bad dog, no biscuit");
		}
		int myIndex = factorDeps.indexOf(variable);
		int indexMeToFactor[] = new int[parentLayout.length];
		for (int i = 0; i < indexMeToFactor.length; i++) {
			indexMeToFactor[i] = factorDeps.indexOf(parentOrder.get(i));
		}
		// Since we have no continuous parents, we should be returning an
		// UnconditionalDistribution to work with
		UnconditionalDistribution dist = (UnconditionalDistribution) factor
				.getDistribution();
		List<Integer> emptyList = Collections.emptyList();

		DoubleMatrix1D elementValues = DoubleFactory1D.dense.make(factorDeps
				.size());
		DoubleMatrix1D parentState = DoubleFactory1D.dense
				.make(parentLayout.length);
		List<TransientVariable> deps = new ArrayList<TransientVariable>();
		deps.add(factorDeps.get(0));
		Table tFDist = new Table(numberStates);
		Factor tableFactor = new Factor(deps, tFDist);
		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(numberStates);
		do {
			for (int i = 0; i < numberDiscreteParentDimensions; i++) {
				elementValues.setQuick(indexMeToFactor[i], parentState
						.getQuick(i));
			}
			for (int i = 0; i < numberStates; i++) {
				elementValues.setQuick(myIndex, i);
				probabilities.setQuick(i, dist.getProbability(elementValues));
			}
			tFDist.setProbability(probabilities);
			distributions[Utility.calculateIndex(parentState, parentLayout)]
					.updateSufficientStatisticsWithFactor(null, tableFactor);
		} while (Utility.incrementIndice(parentState, parentLayout, emptyList));
	}

	@Override
	public void randomize() {
		for (ConditionalDistribution tbl : distributions) {
			tbl.randomize();
		}
	}

	@Override
	public void reset() {
		for (ConditionalDistribution tbl : distributions) {
			tbl.reset();
		}
	}

	@Override
	public ConditionalDistribution copy() throws Exception {
		return new ConditionalTable(this);
	}

	/**
	 * Flattens this distribution into a table distribution with the statelayout
	 * having the first entries be equivalent to the original entries and the
	 * last entries corresponding to each parent.
	 * 
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution flatten() throws Exception {

		DoubleMatrix1D probs = DoubleFactory1D.dense.make(0);
		for (ConditionalDistribution dist : distributions) {
			Table tbl = (Table) dist;
			probs = DoubleFactory1D.dense.append(probs, tbl
					.getProbabilityVector());
		}

		int[] newStateLayout = new int[parentLayout.length + stateLayout.length];
		int i = 0;
		int j = 0;
		for (i = 0; i < parentLayout.length; i++) {
			newStateLayout[i] = parentLayout[i];
		}
		for (j = 0; j < stateLayout.length; i++, j++) {
			newStateLayout[i] = stateLayout[j];
		}

		return new Table(newStateLayout, probs);
	}

}
