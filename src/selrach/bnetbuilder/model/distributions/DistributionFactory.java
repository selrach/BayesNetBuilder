package selrach.bnetbuilder.model.distributions;

import java.util.ArrayList;
import java.util.List;

import selrach.bnetbuilder.model.distributions.conditional.ConditionalTable;
import selrach.bnetbuilder.model.distributions.conditional.GaussianMix;
import selrach.bnetbuilder.model.distributions.conditional.LinearGaussian;
import selrach.bnetbuilder.model.distributions.conditional.LinearGaussianMix;
import selrach.bnetbuilder.model.distributions.conditional.Mixture;
import selrach.bnetbuilder.model.distributions.conditional.MultinomialSigmoid;
import selrach.bnetbuilder.model.distributions.conditional.MultinomialSigmoidMix;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Gaussian;
import selrach.bnetbuilder.model.distributions.unconditional.Sigmoid;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;
import cern.colt.matrix.DoubleFactory1D;

/**
 * Multiple functions to handle the automatic creation of different
 * distributions.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DistributionFactory {

	private DistributionFactory() {
	}

	/**
	 * Inspects the random variable and tries to figure out which cpd is best
	 * suited for the current setup it has.
	 * 
	 * Tabular for a discrete variables with only discrete parents (or no
	 * parents) MultinomialLogit for a discrete variables with continuous
	 * parents ConditionalMultinomialLogit for a discrete variables with mixed
	 * parents
	 * 
	 * Gaussian for a continuous variable with no parents LinearGaussian for a
	 * continuous variable with only continuous parents (or no parents)
	 * ConditionalLinearGaussian for a continuous variable with discrete parents
	 * or mixed parents
	 * 
	 * @param randomVariable
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution getDefaultCPD(
			RandomVariable randomVariable, int time) throws Exception {
		List<RandomVariable> parents = randomVariable.getParents(time);
		boolean discreteParents = false, continuousParents = false;
		for (RandomVariable rv : parents) {
			String t = rv.getType();
			if (t.equals("discrete")) {
				discreteParents = true;
			} else if (t.equals("continuous")) {
				continuousParents = true;
			}
			if (discreteParents && continuousParents) {
				break;
			}
		}
		if (randomVariable.getType().equals("discrete")) {
			if (!continuousParents) {
				if (!discreteParents) {
					return getTableCPD(randomVariable, time);
				}
				return getConditionalTabularCPD(randomVariable, time);
			}
			if (discreteParents) {
				return getConditionalMultinomialLogitCPD(randomVariable, time);
			}
			return getMultinomialLogitCPD(randomVariable, time);
		} else {
			if (discreteParents) {
				if (continuousParents) {
					return getLinearGaussianMixCPD(randomVariable, time);
				} else {
					return getGausianMixCPD(randomVariable, time);
				}
			}
			if (continuousParents) {
				return getLinearGaussianCPD(randomVariable, time);
			}
			return getGaussianCPD(randomVariable, time);
		}
	}

	public static ConditionalDistribution getCPD(RandomVariable randomVariable,
			int time, String typeName) throws Exception {
		DistributionConstant dc = DistributionConstant.getEnum(typeName);
		if (dc == null) {
			return null;
		}
		switch (dc) {
		case CONDITIONAL_TABLE:
			return getConditionalTabularCPD(randomVariable, time);
		case GAUSSIAN:
			return getGaussianCPD(randomVariable, time);
		case GAUSSIAN_MIX:
			return getGausianMixCPD(randomVariable, time);
		case LINEAR_GAUSSIAN:
			return getLinearGaussianCPD(randomVariable, time);
		case LINEAR_GAUSSIAN_MIX:
			return getLinearGaussianMixCPD(randomVariable, time);
		case MULTINOMIAL_SIGMOID:
			return getMultinomialLogitCPD(randomVariable, time);
		case MULTINOMIAL_SIGMOID_MIX:
			return getConditionalMultinomialLogitCPD(randomVariable, time);
		case SIGMOID:
			return getSigmoidCPD(randomVariable, time);
		case TABLE:
			return getTableCPD(randomVariable, time);
		default:
		}
		throw new Exception("DistributionConstant enum not handled!");
	}

	/**
	 * Grabs the list of all potential CPDs for a particular variable
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static List<String> getPotentialCPD(RandomVariable randomVariable,
			int time) throws Exception {
		List<String> potential = new ArrayList<String>();

		List<RandomVariable> parents = randomVariable.getParents(time);
		boolean discreteParents = false, continuousParents = false;
		for (RandomVariable rv : parents) {
			String t = rv.getType();
			if (t.equals("discrete")) {
				discreteParents = true;
			} else if (t.equals("continuous")) {
				continuousParents = true;
			}
			if (discreteParents && continuousParents) {
				break;
			}
		}
		if (randomVariable.getType().equals("discrete")) {
			if (!continuousParents) {
				if (!discreteParents) {
					potential.add(DistributionConstant.TABLE.getDisplayName());
					potential
							.add(DistributionConstant.SIGMOID.getDisplayName());
				} else {
					potential.add(DistributionConstant.CONDITIONAL_TABLE
							.getDisplayName());
				}
			} else {
				if (discreteParents) {
					potential.add(DistributionConstant.MULTINOMIAL_SIGMOID_MIX
							.getDisplayName());
				} else {
					potential.add(DistributionConstant.MULTINOMIAL_SIGMOID
							.getDisplayName());
				}
			}
		} else {
			if (!continuousParents) {
				if (!discreteParents) {
					potential.add(DistributionConstant.GAUSSIAN
							.getDisplayName());
				} else {
					potential.add(DistributionConstant.GAUSSIAN_MIX
							.getDisplayName());
				}
			} else {
				if (discreteParents) {
					potential.add(DistributionConstant.LINEAR_GAUSSIAN_MIX
							.getDisplayName());
				} else {
					potential.add(DistributionConstant.LINEAR_GAUSSIAN
							.getDisplayName());
				}
			}
		}
		return potential;
	}

	/**
	 * Generates a CPD that basically can contain the largest diversity of
	 * variable mixes given the current distribution, this means that for
	 * continuous variables we are going to end up with a LinearGaussianMix for
	 * now, discrete variables probably should not use this right away, as right
	 * now they just return the distribution that they were already a part of.
	 * 
	 * @param distribution
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution upgradeCPD(
			ConditionalDistribution distribution) throws Exception {
		if (distribution instanceof Gaussian
				|| distribution instanceof GaussianMix
				|| distribution instanceof LinearGaussian) {
			return new LinearGaussianMix(distribution);
		}
		return distribution;
	}

	/**
	 * Ideally this will inspect a cpd and see if we can simplify its
	 * representation, for example if there are no continuous parents in a
	 * LinearGaussianMix, we should go to a GaussianMix. In any case it will
	 * return the unconditional distribution that fits closest
	 * 
	 * @param distribution
	 * @return
	 * @throws Exception
	 */
	public static UnconditionalDistribution downgradeCPD(
			ConditionalDistribution distribution) throws Exception {

		if (distribution instanceof Mixture) {
			Mixture m = (Mixture) distribution;
			if (m.getDensityProbabilityDistribution().getNumberStates() == 1) {
				distribution = m.getDistributions()[0];
			}
			if (m.getDistributions().length == 0) {
				distribution = m.getDensityProbabilityDistribution();
			}
		}
		if (distribution instanceof LinearGaussian) {
			LinearGaussian lg = (LinearGaussian) distribution;
			if (lg.getNumberContinuousParentDimensions() == 0) {
				distribution = lg.getDensity(DoubleFactory1D.dense.make(0));
			}
		}

		if (distribution instanceof UnconditionalDistribution) {
			return (UnconditionalDistribution) distribution;
		}

		// Now we should calculate an approximate distribution to show.
		return null;
	}

	/**
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static UnconditionalDistribution getGaussianCPD(
			RandomVariable randomVariable, int time) throws Exception {
		return new Gaussian();
	}

	/**
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static UnconditionalDistribution getTableCPD(
			RandomVariable randomVariable, int time) throws Exception {
		return new Table(((DiscreteVariable) randomVariable).getStates().size());
	}

	/**
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static UnconditionalDistribution getSigmoidCPD(
			RandomVariable randomVariable, int time) throws Exception {
		return new Sigmoid(((DiscreteVariable) randomVariable).getStates()
				.size());
	}

	private static ConditionalDistribution getGausianMixCPD(
			RandomVariable randomVariable, int time) throws Exception {
		List<RandomVariable> parents = randomVariable.getParents(time);
		List<Integer> parentStates = new ArrayList<Integer>();
		for (RandomVariable parent : parents) {
			if (parent instanceof DiscreteVariable) {
				parentStates
						.add(((DiscreteVariable) parent).getStates().size());
			}
		}
		int[] parentlayout = new int[parentStates.size()];
		for (int i = 0; i < parentlayout.length; i++) {
			parentlayout[i] = parentStates.get(i);
		}
		return new GaussianMix(1, parentlayout);
	}

	/**
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution getConditionalTabularCPD(
			RandomVariable randomVariable, int time) throws Exception {
		DiscreteVariable dv = (DiscreteVariable) randomVariable;
		List<RandomVariable> parents = dv.getParents(time);
		List<Integer> parentStates = new ArrayList<Integer>();
		for (RandomVariable parent : parents) {
			if (parent instanceof DiscreteVariable) {
				parentStates
						.add(((DiscreteVariable) parent).getStates().size());
			}
		}
		int[] statelayout = new int[1];
		int[] parentlayout = new int[parentStates.size()];
		for (int i = 0; i < parentlayout.length; i++) {
			parentlayout[i] = parentStates.get(i);
		}
		statelayout[0] = dv.getStates().size();
		return new ConditionalTable(statelayout, parentlayout);
	}

	/**
	 * Gets a linear Gaussian CPD, assumes that all parents, if there are any,
	 * are continuous.
	 * 
	 * @param randomVariable
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution getLinearGaussianCPD(
			RandomVariable randomVariable, int time) throws Exception {
		return new LinearGaussian(1, randomVariable.getParents(time).size());
	}

	/**
	 * Gets a conditional linear Gaussian CPD, utilizes the normal linear
	 * Gaussian CPD and uses discrete parents to switch between which one we are
	 * utilizing.
	 * 
	 * @param randomVariable
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution getLinearGaussianMixCPD(
			RandomVariable randomVariable, int time) throws Exception {
		List<RandomVariable> parents = randomVariable.getParents(time);
		int[] arr = new int[parents.size()];
		int i = 0;
		for (RandomVariable rv : parents) {
			if (rv instanceof DiscreteVariable) {
				arr[i++] = ((DiscreteVariable) rv).getStates().size();
			} else {
				arr[i++] = -1;
			}
		}

		return new LinearGaussianMix(1, arr);
	}

	/**
	 * 
	 * @param randomVariable
	 * @return
	 */
	public static ConditionalDistribution getMultinomialLogitCPD(
			RandomVariable randomVariable, int time) throws Exception {

		return new MultinomialSigmoid(((DiscreteVariable) randomVariable)
				.getStates().size(), randomVariable.getParents(time).size());
	}

	/**
	 * 
	 * @param randomVariable
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public static ConditionalDistribution getConditionalMultinomialLogitCPD(
			RandomVariable randomVariable, int time) throws Exception {
		if (!(randomVariable instanceof DiscreteVariable)) {
			throw new Exception(
					"Multinomial logit needs to refer to a discrete variable");
		}
		DiscreteVariable dv = (DiscreteVariable) randomVariable;
		int[] stateLayout = new int[] { dv.getStates().size() };
		List<RandomVariable> parents = randomVariable.getParents(time);
		int[] parentLayout = new int[parents.size()];
		int i = 0;
		for (RandomVariable rv : parents) {
			if (rv instanceof DiscreteVariable) {
				parentLayout[i++] = ((DiscreteVariable) rv).getStates().size();
			} else {
				parentLayout[i++] = -1;
			}
		}

		return new MultinomialSigmoidMix(stateLayout, parentLayout);
	}
}
