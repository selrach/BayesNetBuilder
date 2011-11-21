package selrach.bnetbuilder.model.distributions.conditional;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Gaussian;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Distribution for a continuous variable with only discrete parents
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class GaussianMix extends Mixture {


	/**
	 * 
	 * @param numDimensions The number of dimensions for the continuous part
	 * @param parentLayout The discrete parent layout
	 * @throws Exception
	 */
	public GaussianMix(int numDimensions, int[] parentLayout) throws Exception {
		setParentStateLayout(parentLayout);
		this.numberDimensions = numDimensions;
		distributions = new Gaussian[numberDiscreteParentStates];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new Gaussian(numDimensions);
		}
	}

	public GaussianMix(GaussianMix copy) throws Exception {
		super(copy);
	}	

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		return (UnconditionalDistribution) distributions[Utility
				.calculateIndex(parentValues, parentLayout)];
	}

	@Override
	public String getType() {
		return DistributionConstant.GAUSSIAN_MIX.toString();
	}

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		// We are dealing with a discrete parent
		// For each parent, we need to get the probability of that state in
		// accordance to the Gaussian associated with it and the continuous
		// values set.

		int numStatesInParent = stateLayout[ignoreParentIndex];
		double[] probabilities = new double[numStatesInParent];
		for (int i = 0; i < numStatesInParent; i++) {
			parentValues.set(ignoreParentIndex, i);
			probabilities[i] = ((Gaussian) distributions[Utility
					.calculateIndex(parentValues.viewPart(0,
							this.numberDiscreteParentDimensions), parentLayout)])
					.getProbability(sample);
		}
		return new Table(probabilities);
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
	public ConditionalDistribution copy() throws Exception {
		return new GaussianMix(this);
	}

}
