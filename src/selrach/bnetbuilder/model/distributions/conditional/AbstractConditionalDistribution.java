package selrach.bnetbuilder.model.distributions.conditional;

import java.util.List;

import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.variable.RandomVariable;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * The abstract implementation of a conditional distribution, all distributions
 * should eventually stem from this
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class AbstractConditionalDistribution implements
		ConditionalDistribution {

	/**
	 * number of dimensions this distribution lives in
	 */
	protected int numberDimensions = 1;

	/**
	 * Number of continuous dimensions that parents have
	 */
	protected int numberContinuousParentDimensions = 0;

	/**
	 * Number of discrete dimensions that parents have.
	 */
	protected int numberDiscreteParentDimensions = 0;

	/**
	 * number of states that the discrete parents can take
	 */
	protected int numberDiscreteParentStates = 0;

	/**
	 * Number of states, -1 for continuous distributions
	 */
	protected int numberStates = -1;

	/**
	 * This is the state layout of this particular distribution
	 */
	protected int[] stateLayout;

	/**
	 * This is the layout for the parents, continuous are marked by -1, discrete
	 * by their number of states
	 */
	protected int[] parentLayout;

	/**
	 * This is an additional weighting that we can enforce on this distribution
	 * that affects the final probabilities
	 */
	protected double weighting = 1.0;

	protected AbstractConditionalDistribution() {
	}

	protected AbstractConditionalDistribution(
			AbstractConditionalDistribution copy) {
		this.numberDimensions = copy.numberDimensions;
		this.numberContinuousParentDimensions = copy.numberContinuousParentDimensions;
		this.numberDiscreteParentDimensions = copy.numberDiscreteParentDimensions;
		this.numberDiscreteParentStates = copy.numberDiscreteParentStates;
		this.numberStates = copy.numberStates;
		if (copy.stateLayout != null) {
			this.stateLayout = copy.stateLayout.clone();
		}
		if (copy.parentLayout != null) {
			this.parentLayout = copy.parentLayout.clone();
		}
	}

	protected int[] generateIndexMap(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes) {
		final int[] indexMap = new int[currentStructure.size()];

		for (int i = 0; i < indexMap.length; i++) {
			int whichTime = conditionalVariableTimes.get(i);
			final RandomVariable whichVariable = conditionalVariables.get(i);
			int index = -1;
			for (int j = 0; j < indexMap.length; j++) {
				if (currentStructure.get(j) == whichVariable) {
					whichTime--;
					index = j;
					if (whichTime < 0) {
						break;
					}
				}
			}
			indexMap[i] = index;
		}

		return indexMap;
	}

	// /
	// /Protected Functions
	// /

	public String generateXMLPost() {
		return "</dist>\n";
	}

	public String generateXMLPre() {
		final StringBuilder sb = new StringBuilder("<dist type=\"");
		sb.append(getType());
		sb.append("\">\n");
		return sb.toString();
	}

	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		throw new Exception(getClass().getName()
				+ " getDensity not implemented");
	}

	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		throw new Exception("Must create SufficientDescriptor");
	}

	public String getFlatXMLProbabilityDescription() throws Exception {
		throw new Exception("No Flat XML probablility description from "
				+ getType());
	}

	public double getLogProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception {
		return getDensity(parentValues).getLogProbability(elementValues);
	}

	public int getNumberContinuousParentDimensions() {
		return numberContinuousParentDimensions;
	}

	public int getNumberDimensions() {
		return numberDimensions;
	}

	public int getNumberDiscreteParentDimensions() {
		return numberDiscreteParentDimensions;
	}

	public int getNumberParentDimensions() {
		return numberContinuousParentDimensions
				+ numberDiscreteParentDimensions;
	}

	public int getNumberDiscreteParentStates() {
		return numberDiscreteParentStates;
	}

	public int getNumberStates() {
		return -1;
	}

	public int[] getParentLayout() {
		return parentLayout;
	}

	public double getProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception {
		return getDensity(parentValues).getProbability(elementValues);
	}

	public int[] getStateLayout() {
		return stateLayout;
	}

	public String getType() {
		return "AbstractConditionalDistribution";
	}

	public String getXMLDescription() throws Exception {
		throw new Exception("No XML Setup for distribution " + getType());
	}

	public DoubleMatrix1D sample(DoubleMatrix1D parentValues) throws Exception {
		return getDensity(parentValues).sample();
	}

	/**
	 * Calculates the number of states for all the discrete parents and the
	 * dimension of discrete and continuous parents.
	 * 
	 * @param parentLayout
	 * @return the number of dimensions
	 * @throws Exception
	 */
	protected void setParentStateLayout(int[] parentLayout) throws Exception {
		int numStates = 1;
		int continuousCount = 0;
		for (final int i : parentLayout) {
			if (i == -1) {
				continuousCount++;
			} else {
				numStates *= i;
			}
		}
		this.parentLayout = parentLayout.clone();
		this.numberDiscreteParentStates = numStates;
		this.numberDiscreteParentDimensions = parentLayout.length
				- continuousCount;
		this.numberContinuousParentDimensions = continuousCount;
	}

	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception {
		throw new Exception("Must utilize SufficientDescriptor");

	}

	/**
	 * Counts the number of states this distribution will need to represent,
	 * this makes sense only for discrete conditional probability distributions
	 * 
	 * @param stateLayout
	 * @return
	 * @throws Exception
	 */
	protected void setNumberStates(int[] stateLayout) throws Exception {
		if (stateLayout.length <= 0) {
			throw new Exception(
					"Discrete variables must have at least one dimension");
		}
		int numStates = 1;
		for (final int i : stateLayout) {
			if (i < 2) {
				throw new Exception("All dimensions must at least be binary!");
			}
			numStates *= i;
		}
		this.stateLayout = stateLayout.clone();
		this.numberStates = numStates;
		this.numberDimensions = stateLayout.length;
	}

	public abstract ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception;

	@Override
	public String toString() {
		try {
			return getFlatXMLProbabilityDescription();
		} catch (Exception ex) {
		}
		return this.getClass().toString();
	}

	public double getWeighting() {
		return weighting;
	}

	public void setWeighting(double weighting) {
		this.weighting = weighting;
	}
}
