package selrach.bnetbuilder.model.distributions.conditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Gaussian;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix3D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Mult;
import cern.jet.math.PlusMult;

/**
 * Distribution for a continuous variable that has both discrete and continuous
 * parents. Essentially we store a LinearGaussian for each discrete state
 * combination.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class LinearGaussianMix extends Mixture {

	public LinearGaussianMix(int numberDimensions, int[] parentStateLayout)
			throws Exception {
		setParentStateLayout(parentStateLayout);
		this.numberDimensions = numberDimensions;
		int[] stateLayout = new int[numberDiscreteParentDimensions];
		for (int i = 0, k = 0; i < parentLayout.length; i++) {
			if (parentLayout[i] == -1) {
				continue;
			}
			stateLayout[k++] = parentLayout[i];
		}
		this.distributionProbability = new Table(stateLayout,
				DoubleFactory1D.dense.make(numberDiscreteParentStates, 1.0));
		distributions = new LinearGaussian[this.numberDiscreteParentStates];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new LinearGaussian(numberDimensions,
					numberContinuousParentDimensions);
		}
	}

	public LinearGaussianMix(int[] parentStateLayout, DoubleMatrix2D basemu,
			DoubleMatrix3D coefficients, DoubleMatrix3D covariance)
			throws Exception {
		setParentStateLayout(parentStateLayout);
		this.numberContinuousParentDimensions = coefficients.rows();
		this.numberDimensions = basemu.columns();
		setLinearGaussianMix(basemu, coefficients, covariance);
	}

	public LinearGaussianMix(LinearGaussianMix copy) throws Exception {
		super(copy);
	}

	protected LinearGaussianMix(int[] parentStateLayout,
			ConditionalDistribution newPDist, ConditionalDistribution[] newDists)
			throws Exception {
		setParentStateLayout(parentStateLayout);
		if (newDists[0] != null) {
			this.numberContinuousParentDimensions = newDists[0]
					.getNumberContinuousParentDimensions();
			this.numberDimensions = newDists[0].getNumberDimensions()
					+ ((newPDist.getNumberStates() == 1) ? 0 : newPDist
							.getNumberDimensions());
			distributions = newDists;
		} else {
			this.numberContinuousParentDimensions = 0;
			this.numberDimensions = newPDist.getNumberDimensions();
			distributions = null;
		}
		distributionProbability = newPDist;
	}

	public LinearGaussianMix(ConditionalDistribution distribution)
			throws Exception {
		numberContinuousParentDimensions = distribution
				.getNumberContinuousParentDimensions();
		numberDiscreteParentDimensions = distribution
				.getNumberDiscreteParentDimensions();
		numberDimensions = distribution.getNumberDimensions();
		numberDiscreteParentStates = distribution
				.getNumberDiscreteParentStates();
		if (distribution instanceof LinearGaussian) {
			distributionProbability = new Table();
			distributions = new LinearGaussian[1];
			distributions[0] = distribution.copy();
			numberDiscreteParentStates = 1;
			parentLayout = new int[((LinearGaussian) distribution).numberContinuousParentDimensions];
			for (int i = 0; i < parentLayout.length; i++) {
				parentLayout[i] = -1;
			}
		} else if (distribution instanceof GaussianMix) {
			GaussianMix gm = (GaussianMix) distribution;
			if (gm.distributionProbability != null) {
				distributionProbability = gm.distributionProbability.copy();
			} else {
				distributionProbability = new Table(gm.parentLayout,
						DoubleFactory1D.dense
								.make(gm.distributions.length, 1.0));
			}
			distributions = new LinearGaussian[gm.distributions.length];
			for (int i = 0; i < distributions.length; i++) {
				distributions[i] = new LinearGaussian(
						(Gaussian) gm.distributions[i]);
			}
			numberStates = gm.numberStates;
			parentLayout = ((GaussianMix) distribution).parentLayout.clone();
		} else if (distribution instanceof Gaussian) {
			distributionProbability = new Table();
			distributions = new LinearGaussian[1];
			distributions[0] = new LinearGaussian((Gaussian) distribution);
			numberDiscreteParentStates = 1;
			parentLayout = new int[0];
		} else if (distribution instanceof Table) {
			distributionProbability = distribution.copy();
			numberStates = ((Table) distribution).numberStates;
			distributions = new LinearGaussian[numberStates];
			for (int i = 0; i < numberStates; i++) {
				distributions[i] = new LinearGaussian();
			}
			parentLayout = new int[0];
		}
	}

	/**
	 * Sets up all the distribution information.
	 * 
	 * @param basemu
	 * @param coefficients
	 * @param covariance
	 * @throws Exception
	 */
	public void setLinearGaussianMix(DoubleMatrix2D basemu,
			DoubleMatrix3D coefficients, DoubleMatrix3D covariance)
			throws Exception {
		if (this.numberDiscreteParentStates != basemu.rows()
				|| this.numberDiscreteParentStates != coefficients.slices()
				|| this.numberDiscreteParentStates != covariance.slices()) {
			throw new Exception(
					"Number of discrete states does not match up with the given distributions");
		}
		int[] stateLayout = new int[numberDiscreteParentDimensions];
		for (int i = 0, k = 0; i < parentLayout.length; i++) {
			if (parentLayout[i] == -1) {
				continue;
			}
			stateLayout[k++] = parentLayout[i];
		}
		this.distributionProbability = new Table(stateLayout,
				DoubleFactory1D.dense.make(numberDiscreteParentStates, 1.0));
		distributions = new LinearGaussian[this.numberDiscreteParentStates];
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			distributions[i] = new LinearGaussian(basemu.viewRow(i),
					coefficients.viewSlice(i), covariance.viewSlice(i));
		}
	}

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		if (numberDimensions > 1) {
			throw new Exception("Can't handle multivariate right now");
		}
		if (ignoreParentIndex >= this.numberDiscreteParentDimensions) {
			// We are dealing with a continuous parent. We just grab the
			// LinearGaussian associated distributionProbability.getStateLayout
			// with it and get the likelihood distribution from it.

			return distributions[Utility.calculateIndex(parentValues.viewPart(
					0, this.numberDiscreteParentDimensions), parentLayout)]
					.getLikelihoodDistributions(sample.viewPart(
							numberDiscreteParentDimensions,
							numberContinuousParentDimensions), parentValues
							.viewPart(this.numberDiscreteParentDimensions - 1,
									this.numberContinuousParentDimensions),
							ignoreParentIndex - numberDiscreteParentDimensions);
		} else {
			// We are dealing with a discrete parent
			// For each parent, we need to get the probability of that state in
			// accordance to the Gaussian associated with it and the continuous
			// values set.

			int numStatesInParent = stateLayout[ignoreParentIndex];
			double[] probabilities = new double[numStatesInParent];
			DoubleMatrix1D continuousParents = parentValues.viewPart(
					this.numberDiscreteParentDimensions - 1,
					this.numberContinuousParentDimensions);
			DoubleMatrix1D continuousSample = sample.viewPart(
					numberDiscreteParentDimensions,
					numberContinuousParentDimensions);
			for (int i = 0; i < numStatesInParent; i++) {
				parentValues.set(ignoreParentIndex, i);
				probabilities[i] = distributions[Utility.calculateIndex(
						parentValues.viewPart(0,
								this.numberDiscreteParentDimensions),
						parentLayout)].getProbability(continuousParents,
						continuousSample);
			}
			return new Table(probabilities);
		}
	}

	@Override
	public String getType() {
		return DistributionConstant.LINEAR_GAUSSIAN_MIX.toString();
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
		return new LinearGaussianMix(this);
	}

	Algebra algebra = new Algebra();

	@Override
	public ConditionalDistribution marginalize(int index, boolean onDependencies)
			throws Exception {
		ConditionalDistribution newPDist;
		ConditionalDistribution[] newDists;
		if (onDependencies) // We should only be marginalizing on discrete
		// entities, but lets check
		{
			if (numberContinuousParentDimensions > 0) {
				throw new Exception(
						"Marginalization on discrete entries with tail variables is undefined for LinearGaussianMix.");
			}

			// Marginalize out the discrete conditional statement
			if(distributionProbability instanceof Table)
			{
				((Table) distributionProbability).normalize();
			}
			newPDist = distributionProbability.marginalize(index, false);
			newDists = new ConditionalDistribution[newPDist.getNumberStates()];
			int[] discreteLayout = distributionProbability.getStateLayout();
			List<Integer> ignore = new ArrayList<Integer>();
			ignore.add(index);

			int[] indices = new int[discreteLayout.length - 1];
			int[] parentLayout = new int[discreteLayout.length - 1];
			for (int i = 0, j = 0; i < discreteLayout.length; i++) {
				if (i != index) {
					parentLayout[j] = discreteLayout[i];
					indices[j++] = i;
				}
			}

			// Calculate new mu
			DoubleMatrix1D values = DoubleFactory1D.dense
					.make(discreteLayout.length);
			int sz = ((LinearGaussian) distributions[0]).basemu.size();
			List<DoubleMatrix1D> newMus = new ArrayList<DoubleMatrix1D>();
			do {
				DoubleMatrix1D newMu = DoubleFactory1D.dense.make(sz);
				for (int i = 0; i < discreteLayout[index]; i++) {
					values.setQuick(index, i);
					DoubleMatrix1D mu = ((LinearGaussian) distributions[Utility
							.calculateIndex(values, discreteLayout)]).basemu;
					double p = distributionProbability.getProbability(null,
							values);
					newMu.assign(mu, PlusMult.plusMult(p));
				}

				newMu.assign(Mult.div(newPDist.getProbability(null, values
						.viewSelection(indices))));
				newMus.add(newMu);

			} while (Utility.incrementIndice(values, discreteLayout, ignore));

			DoubleMatrix2D tmp = ((LinearGaussian) distributions[0]).gaussian
					.getCovariance();
			// Calculate new covariance
			int j = 0;
			do {
				DoubleMatrix2D newCov = DoubleFactory2D.dense.make(tmp.rows(),
						tmp.columns());
				DoubleMatrix1D newMu = newMus.get(j++);
				for (int i = 0; i < discreteLayout[index]; i++) {
					values.setQuick(index, i);
					LinearGaussian lg = ((LinearGaussian) distributions[Utility
							.calculateIndex(values, discreteLayout)]);
					DoubleMatrix2D cov = lg.gaussian.getCovariance().copy();
					DoubleMatrix1D mu = lg.basemu.copy();
					mu.assign(newMu, PlusMult.plusMult(-1));

					cov.assign(algebra.multOuter(mu, mu, null), PlusMult
							.plusMult(1.0));

					double p = distributionProbability.getProbability(null,
							values);
					newCov.assign(cov, PlusMult.plusMult(p));
				}

				newCov.assign(Mult.div(newPDist.getProbability(null, values
						.viewSelection(indices))));
				newDists[Utility.calculateIndex(values.viewSelection(indices),
						newPDist.getStateLayout())] = new LinearGaussian(newMu,
						DoubleFactory2D.dense.make(newMu.size(), 0), newCov);
			} while (Utility.incrementIndice(values, discreteLayout, ignore));

			return new LinearGaussianMix(parentLayout, newPDist, newDists);
		} else {
			newDists = new ConditionalDistribution[distributions.length];
			// Marginalize out the continuous
			for (int i = 0; i < distributions.length; i++) {
				newDists[i] = distributions[i].marginalize(index, false);
			}
			if (newDists[0].getNumberDimensions() == 0) {
				// We now have marginalized out all continuous information
				// return the discrete emergence
				return distributionProbability.copy();
			}

			return new LinearGaussianMix(parentLayout, distributionProbability
					.copy(), newDists);
		}
	}

	@Override
	public ConditionalDistribution complement(ConditionalDistribution marginal,
			Collection<Tuple> discrete, Collection<Tuple> continuous)
			throws Exception {
		if (marginal instanceof Table) {
			List<Quadruple> indices = new ArrayList<Quadruple>();
			for (Tuple t : discrete) {
				indices.add(new Quadruple(t.r, t.a, t.r, t.id));
			}
			// using discrete

			Table tbl = (Table) marginal;
			ConditionalDistribution newPDist = Operation.divide(tbl,
					(Table) distributionProbability, indices);
			ConditionalDistribution[] newDists = new ConditionalDistribution[distributionProbability
					.getNumberStates()];

			// Complement each LinearGaussian out the continuous
			for (int i = 0; i < distributions.length; i++) {
				newDists[i] = distributions[i].copy();
			}

			return new LinearGaussianMix(parentLayout, newPDist, newDists);

		} else if (marginal instanceof LinearGaussianMix) {
			LinearGaussianMix lgm = (LinearGaussianMix) marginal;
			ConditionalDistribution[] newDists = new ConditionalDistribution[distributionProbability
					.getNumberStates()];
			ConditionalDistribution newPDist = new Table(DoubleFactory1D.dense
					.make(distributionProbability.getNumberStates(), 1.0));
			// Complement each LinearGaussian out the continuous
			for (int i = 0; i < distributions.length; i++) {
				newDists[i] = distributions[i].complement(lgm.distributions[i],
						null, continuous);
			}
			return new LinearGaussianMix(parentLayout, newPDist, newDists);
		}
		return null;
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {

		int[] stateLayout = new int[continuous.size() + discrete.size()];
		for (Tuple t : discrete) {
			stateLayout[t.r] = this.stateLayout[t.a];
		}
		for (Tuple t : continuous) {
			stateLayout[t.r] = this.stateLayout[t.a]; // should all be -1
		}

		// Division of the strong marginal with only continuous variables being
		// marginalized on results in simple 1.0 for each entry in the
		// probability table
		ConditionalDistribution newPDist = new Table(DoubleFactory1D.dense
				.make(distributionProbability.getNumberStates(), 1.0));

		ConditionalDistribution[] newDists;

		newDists = new ConditionalDistribution[distributionProbability
				.getNumberStates()];
		// Complement each LinearGaussian out the continuous
		for (int i = 0; i < distributions.length; i++) {
			newDists[i] = distributions[i].complement(discrete, continuous);
		}
		return new LinearGaussianMix(stateLayout, newPDist, newDists);

	}

	@Override
	public ConditionalDistribution setEvidence(int index, double evidence)
			throws Exception {

		if (numberContinuousParentDimensions > 0) {
			throw new Exception(
					"Cannot set evidence on a LinearGaussian with tail variables");
		}
		LinearGaussianMix newDist = new LinearGaussianMix(this);

		DoubleMatrix1D elementValues = DoubleFactory1D.dense
				.make(this.numberDiscreteParentDimensions);
		DoubleMatrix1D newProbabilities = DoubleFactory1D.dense
				.make(this.numberDiscreteParentStates);
		int[] discreteLayout = distributionProbability.getStateLayout();
		do {
			int ind = Utility.calculateIndex(elementValues, discreteLayout);
			newDist.distributions[ind]
					.setWeighting(newDist.distributionProbability
							.getProbability(null, elementValues));
			newDist.distributions[ind].setEvidence(index, evidence);
			newProbabilities.setQuick(ind, newDist.distributions[ind]
					.getWeighting());

		} while (Utility.incrementIndice(elementValues, discreteLayout));
		newDist.distributionProbability = new Table(discreteLayout,
				newProbabilities);

		return newDist;
	}
}
