package selrach.bnetbuilder.model.distributions.conditional;

import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix3D;

/**
 * This is a distribution for a discrete variable that has both discrete and
 * continuous parents.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class MultinomialSigmoidMix extends Mixture {

	public MultinomialSigmoidMix(int[] stateLayout,
			int[] parentStateLayout) throws Exception {
		setParentStateLayout(parentStateLayout);
		setNumberStates(stateLayout);
		distributions = new MultinomialSigmoid[this.numberDiscreteParentStates];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new MultinomialSigmoid(this.stateLayout,
					this.numberContinuousParentDimensions);
		}
	}

	public MultinomialSigmoidMix(int[] stateLayout,
			int[] parentStateLayout, DoubleMatrix3D coefficients)
			throws Exception {
		setParentStateLayout(parentStateLayout);
		this.setNumberStates(stateLayout);
		this.numberContinuousParentDimensions = coefficients.columns() - 1;
		this.setMultinomialSigmoidMix(coefficients);
		distributions = new MultinomialSigmoid[this.numberDiscreteParentDimensions];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new MultinomialSigmoid(this.stateLayout,
					this.numberContinuousParentDimensions);
		}
	}

	public MultinomialSigmoidMix(MultinomialSigmoidMix copy) throws Exception {
		super(copy);
	}

	/**
	 * Sets up all the distribution information.
	 * 
	 * @param basemu
	 * @param coefficients
	 * @param covariance
	 * @throws Exception
	 */
	public void setMultinomialSigmoidMix(DoubleMatrix3D coefficients)
			throws Exception {
		if (this.numberStates != coefficients.rows()
				|| this.numberDiscreteParentStates != coefficients.slices()
				|| this.numberContinuousParentDimensions + 1 != coefficients
						.columns()) {
			throw new Exception(
					"Number of discrete states does not match up with the given distributions");
		}
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new MultinomialSigmoid(stateLayout, coefficients
					.viewSlice(i));
		}
	}

	@Override
	public String getType() {
		return DistributionConstant.MULTINOMIAL_SIGMOID_MIX.toString();
	}

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
	public ConditionalDistribution copy() throws Exception {
		return new MultinomialSigmoidMix(this);
	}


}
