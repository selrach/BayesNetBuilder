package selrach.bnetbuilder.model.distributions.conditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Gaussian;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import cern.jet.math.PlusMult;

/**
 * Distribution for continuous variable with only continuous parents, this is
 * linear in the sense that the mean is a linear combination of each parent
 * value. Variance is not dependent upon the values of parents. i.e. P(X) =
 * N(B_0 + B^TX^T, sigma^2). In the case of multivariate parents, each dimension
 * needs another coefficient. Also this distribution itself can be multivariate
 * and as such will need another set of coefficients for each dimension.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class LinearGaussian extends AbstractConditionalDistribution {

	/**
	 * This is the unconditional offset for this distribution, essentially B_0
	 * this should have size equal to the dimensions of this distribution
	 * 
	 * this should be r x 1
	 */
	DoubleMatrix1D basemu;

	/**
	 * These are the conditional offsets for this distribution, essentially B^T
	 * this should have size parent dimensions by this distribution dimensions
	 * 
	 * this should be r x s
	 */
	DoubleMatrix2D coefficients;

	/**
	 * Since we will only change the mean for things like sampling, we would
	 * like a cached gaussian that already has done things like compute the
	 * cholesky decomposition
	 * 
	 * the covariance matrix should be r x r
	 */
	Gaussian gaussian;

	private final Algebra algebra = new Algebra();

	/**
	 * Creates a default linear gaussian with no parents, corresponding to a mu
	 * of 0 and variance of 1;
	 */
	public LinearGaussian() throws Exception {
		basemu = DoubleFactory1D.dense.make(0);
		coefficients = DoubleFactory2D.dense.make(0, 0);
		this.numberContinuousParentDimensions = 0;
		this.stateLayout = new int[] { -1 };
		gaussian = new Gaussian(0);
		this.numberDimensions = 1;
		this.numberDiscreteParentDimensions = 0;
		this.numberDiscreteParentStates = 0;
		this.numberStates = 1;
		this.numberContinuousParentDimensions = 0;
	}

	/**
	 * Creates a default Linear Gaussian distribution with the set number of
	 * dimensions and the number of continuous parent dimensions
	 * 
	 * @param numDimensions
	 * @param numParentDimensions
	 * @throws Exception
	 */
	public LinearGaussian(int numDimensions, int numParentDimensions)
			throws Exception {
		this.numberContinuousParentDimensions = numParentDimensions;
		gaussian = new Gaussian(numDimensions);
		this.stateLayout = new int[numDimensions];
		for (int i = 0; i < stateLayout.length; i++) {
			stateLayout[i] = -1;
		}
		basemu = DoubleFactory1D.dense.make(numDimensions);
		coefficients = DoubleFactory2D.dense.make(numDimensions,
				numParentDimensions);
		this.numberContinuousParentDimensions = coefficients.columns();
		this.numberDimensions = numDimensions;
		this.numberDiscreteParentDimensions = 0;
		this.numberDiscreteParentStates = 0;
		this.numberStates = numDimensions;
	}

	/**
	 * Creates a Linear Gaussian distribution with the specified mean,
	 * coefficients, and covariance.
	 * 
	 * @param basemu
	 * @param coefficients
	 * @param covariance
	 * @throws Exception
	 */
	public LinearGaussian(DoubleMatrix1D basemu, DoubleMatrix2D coefficients,
			DoubleMatrix2D covariance) throws Exception {
		if (coefficients != null && coefficients.rows() > 0
				&& coefficients.rows() != basemu.size()) {
			throw new Exception(
					"Number of coefficient columns must match basemu rows");
		}
		this.stateLayout = new int[1];
		for (int i = 0; i < stateLayout.length; i++) {
			stateLayout[i] = -1;
		}
		gaussian = new Gaussian(covariance);
		this.basemu = basemu.copy();
		if (coefficients != null) {
			this.coefficients = coefficients.copy();
		}
		this.numberContinuousParentDimensions = coefficients.columns();
		this.numberDimensions = basemu.size();
		this.numberDiscreteParentDimensions = 0;
		this.numberDiscreteParentStates = 0;
		this.numberStates = basemu.size();
	}

	/**
	 * Creates a copy of this linear Gaussian
	 * 
	 * @param copy
	 * @throws Exception
	 */
	public LinearGaussian(LinearGaussian copy) throws Exception {
		super(copy);
		this.basemu = copy.basemu.copy();
		this.coefficients = copy.coefficients.copy();
		this.gaussian = (Gaussian) copy.gaussian.copy();
		this.stateLayout = copy.stateLayout.clone();
	}

	/**
	 * Upgrades a Gaussian distribution to a Linear Gaussian distribution with
	 * no parents
	 * 
	 * @param dist
	 * @throws Exception
	 */
	public LinearGaussian(Gaussian dist) throws Exception {
		this.basemu = dist.getExpectedValue();
		this.coefficients = DoubleFactory2D.dense.make(1, 0);
		this.gaussian = (Gaussian) dist.copy();
		this.stateLayout = new int[1];
		for (int i = 0; i < stateLayout.length; i++) {
			stateLayout[i] = -1;
		}
		this.numberDimensions = dist.numberDimensions;
		this.numberDiscreteParentDimensions = dist.numberDiscreteParentDimensions;
		this.numberDiscreteParentStates = dist.numberDiscreteParentStates;
		this.numberStates = dist.numberStates;
		this.numberContinuousParentDimensions = dist.numberContinuousParentDimensions;
	}

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		if (numberDimensions > 1) {
			throw new Exception("Can't handle multivariate right now");
		}
		// Basically we want to calculate a distribution to multiply the
		// parent's prior by to find the parent's posterior
		double mu = basemu.get(0), // are going to do simple algebra to get the
		// expected mean
		variance = this.gaussian.getCovariance().get(0, 0); // Assume that the
		// variance is going
		// to be our
		// variance...not
		// sure if this is
		// correct, but
		// because we are
		// assuming set
		// values for the
		// parents..they
		// have no variance
		// to take into
		// account.

		for (int i = 0; i < coefficients.columns(); i++) {
			if (i != ignoreParentIndex) {
				mu += parentValues.get(i) * coefficients.get(0, i);
			}
		}

		// Adjust the computed mean by our estimated value
		mu = sample.getQuick(0) - mu;

		// This is the most likely value of the mu of the parent from
		// this child's viewpoint at the moment...as it is the most likely
		// value of the parent that generated the sample.
		mu /= coefficients.get(0, ignoreParentIndex);

		return new Gaussian(mu, variance);
	}

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		DoubleMatrix1D mu = basemu.copy();
		mu.assign(algebra.mult(coefficients, parentValues), Functions.plus);
		gaussian.setMean(mu);
		return gaussian;
	}

	/**
	 * Reduces this distribution in respect to evidence parents.
	 * 
	 * @param which
	 *            The indices of the parents we are setting evidence on, this
	 *            needs to be ascending and unique
	 * @param parentValues
	 *            The values of each corresponding index
	 * @return the distribution with the evidence parents conditioned on, if all
	 *         parents have been conditioned on this returns a Gaussian,
	 *         otherwise a LinearGaussian
	 * @throws Exception
	 */
	@Override
	public ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception {
		if (which.length != parentValues.size()) {
			throw new Exception(
					"Indices and values must have same length when applying evidence");
		}
		DoubleMatrix1D mu = basemu.copy();

		mu.assign(algebra.mult(coefficients.viewSelection(null, which),
				parentValues), Functions.plus);
		DoubleMatrix2D coef = coefficients.viewSelection(
				null,
				Utility.indexComplement(which, numberContinuousParentDimensions
						+ numberDiscreteParentDimensions)).copy();
		return new LinearGaussian(mu, coef, gaussian.getCovariance());
	}

	@Override
	public String getType() {
		return DistributionConstant.LINEAR_GAUSSIAN.toString();
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		list.add(mp = new ArrayList<DistributionDescriptor>());
		if (numberDimensions == 1) {
			mp.add(new DistributionDescriptor("mu", basemu.getQuick(0),
					"Base mean"));
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				mp.add(new DistributionDescriptor("B_" + j, coefficients
						.getQuick(0, j), "Linear weighting coefficient of [$$"
						+ j + "$$] time [$#$]"));
			}
			mp.add(new DistributionDescriptor("variance", gaussian
					.getCovariance().get(0, 0), "Variance of Gaussian"));
		} else {
			for (int i = 0; i < numberDimensions; i++) {
				mp.add(new DistributionDescriptor("mu_" + i,
						basemu.getQuick(i), "Base mean in dimension " + i));
				for (int j = 0; j < numberContinuousParentDimensions; j++) {
					mp.add(new DistributionDescriptor("B_" + i + "_" + j,
							coefficients.getQuick(j, i),
							"Linear weighting coefficient of [$$" + j
									+ "$$] time [$#$] in dimension " + i));
				}
			}
			for (int i = 0; i < numberDimensions; i++) {
				for (int j = 0; j < numberDimensions; j++) {
					mp.add(new DistributionDescriptor("cov_" + i + "_" + j,
							gaussian.getCovariance().get(i, j),
							"Covariance matrix entry (" + i + "," + j + ")"));
				}
			}
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
		if (mp.size() != numberDimensions + numberDimensions
				* numberContinuousParentDimensions + numberDimensions
				* numberDimensions) {
			throw new Exception("");
		}

		int k = 0;
		for (int i = 0; i < numberDimensions; i++) {
			basemu.setQuick(i, mp.get(k++).getValue());
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				coefficients.setQuick(i, j, mp.get(k++).getValue());
			}
		}
		DoubleMatrix2D cov = gaussian.getCovariance();
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				cov.setQuick(i, j, mp.get(k++).getValue());
			}
		}
		gaussian.setCovariance(cov);
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
		for (int i = 0; i < numberDimensions; i++) {
			sb.append(" ");
			sb.append(basemu.getQuick(i));
		}
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				sb.append(" ");
				sb.append(coefficients.getQuick(i, j));
			}
		}
		DoubleMatrix2D cov = this.gaussian.getCovariance();
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				sb.append(" ");
				sb.append(cov.getQuick(i, j));
			}
		}
		return sb.toString();
	}

	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (indexedProbabilityDescription != null
				&& indexedProbabilityDescription.size() != 0) {
			throw new Exception(
					"You have a conditional map with a LinearGaussian distribution?  Something is wrong.");
		}

		int[] indexMap = generateIndexMap(currentStructure,
				conditionalVariables, conditionalVariableTimes);

		String[] probs = probabilityDescription.split(" ");

		if (probs.length != numberDimensions + numberDimensions
				* numberContinuousParentDimensions + numberDimensions
				* numberDimensions) {
			throw new Exception(
					"You have a different number of probabilities than possible states in this Linear Gaussin");
		}

		int ind = 0;
		for (int i = 0; i < numberDimensions; i++) {
			basemu.setQuick(i, Double.parseDouble(probs[ind++]));
		}

		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				coefficients.setQuick(indexMap[i], indexMap[j], Double
						.parseDouble(probs[ind++]));
			}
		}

		DoubleMatrix2D cov = this.gaussian.getCovariance();
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				cov.setQuick(indexMap[i], indexMap[j], Double
						.parseDouble(probs[ind++]));
			}
		}

		this.gaussian.setCovariance(cov);

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
		return new LinearGaussian(this);
	}

	@Override
	public ConditionalDistribution marginalize(int index, boolean onDependencies)
			throws Exception {
		// Marginalization over a continuous distribution is only defined over
		// the head variables...which corresponds to the mu here
		//
		// this assumption follows if there is
		// no correlation between variables (only diagonal of covariance is
		// interesting). Otherwise we need to do the calculation:
		// U = U_{11} - U_{12}U_{22}^{-1}U_{21}
		// to find the covariance matrix

		if (onDependencies) {
			throw new Exception(
					"Cannot marginalize over parents for LinearGaussian distribution");
		}
		int[] i_2 = new int[] { index };
		int[] i_1 = Utility.indexComplement(i_2, basemu.size());
		DoubleMatrix1D newMu = basemu.viewSelection(i_1).copy();
		DoubleMatrix2D newCof = coefficients.viewSelection(i_1, null);

		DoubleMatrix2D U_11 = gaussian.getCovariance().viewSelection(i_1, i_1);
		DoubleMatrix2D U_12 = gaussian.getCovariance().viewSelection(i_1, i_2)
				.copy();
		DoubleMatrix2D U_21 = gaussian.getCovariance().viewSelection(i_2, i_1)
				.copy();
		DoubleMatrix2D U_22 = gaussian.getCovariance().viewSelection(i_2, i_2)
				.copy();

		/*
		 * U_11.assign( algebra.mult(U_12, algebra.mult(algebra.inverse(U_22),
		 * U_21)), PlusMult.plusMult(-1.0)).copy();
		 */
		return new LinearGaussian(newMu, newCof, U_11);
	}

	@Override
	public int extend(int numStates) throws Exception {
		if (numStates > 0) {
			throw new Exception(
					"LinearGaussian distribution does not have states to extend");
		}
		coefficients = DoubleFactory2D.dense.appendColumns(coefficients,
				DoubleFactory2D.dense.make(coefficients.rows(), 1));
		return coefficients.columns() - 1;
	}

	@Override
	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception {

		if (b instanceof LinearGaussian) {

			LinearGaussian lgB = (LinearGaussian) b;

			Map<String, Tuple> F_1 = new HashMap<String, Tuple>(tail2);
			F_1.keySet().removeAll(tail1.keySet());

			Map<String, Tuple> F_2 = new HashMap<String, Tuple>(tail2);
			F_2.keySet().removeAll(head1.keySet());

			int[] f_1Map = Operation.makeIndexMap(F_1);
			int[] f_2Map = Operation.makeIndexMap(F_2);
			int[] h_1Map = Operation.makeIndexMap(head1);
			int[] h_2Map = Operation.makeIndexMap(head2);
			int[] t_1Map = Operation.makeIndexMap(tail1);
			int[] t_2Map = Operation.makeIndexMap(tail2);

			DoubleMatrix1D A = basemu.viewSelection(h_1Map);
			DoubleMatrix2D B = coefficients.viewSelection(h_1Map, t_1Map);
			DoubleMatrix2D C = gaussian.getCovariance().viewSelection(h_1Map,
					h_1Map);

			DoubleMatrix1D E = lgB.basemu.viewSelection(h_2Map).copy();
			DoubleMatrix2D F1 = lgB.coefficients.viewSelection(h_2Map, f_1Map);
			DoubleMatrix2D F2 = lgB.coefficients.viewSelection(h_2Map, f_2Map);
			DoubleMatrix2D G = lgB.gaussian.getCovariance().viewSelection(
					h_2Map, h_2Map).copy();

			E.assign(algebra.mult(F1, A), PlusMult.plusMult(1.0));

			DoubleMatrix1D newMu = DoubleFactory1D.dense.append(A, E);

			DoubleMatrix2D otherCof = F2.copy();
			otherCof.assign(algebra.mult(F1, B), PlusMult.plusMult(1.0));

			DoubleMatrix2D[][] newCofParts = { { B }, { otherCof } };
			DoubleMatrix2D newCof = DoubleFactory2D.dense.compose(newCofParts);

			DoubleMatrix2D F1xC = algebra.mult(F1, C);

			DoubleMatrix2D[][] newCovParts = {
					{ C, algebra.mult(C, F1.viewDice()) },
					{
							F1xC,
							G.assign(algebra.mult(F1xC, F1.viewDice().copy()),
									PlusMult.plusMult(1.0)) } };

			DoubleMatrix2D newCov = DoubleFactory2D.dense.compose(newCovParts);
			
			int[] map = new int[newMu.size()];
			int i=0;
			for(Tuple t : head1.values())
			{
				map[i++] = t.r;
			}
			for(Tuple t : head2.values())
			{
				map[i++] = t.r;
			}
			
			i=0;
			int[] tailMap = new int[newCof.columns()];
			for(Tuple t : tail1.values())
			{
				tailMap[i++] = t.r;
			}

			return new LinearGaussian(newMu.viewSelection(map), newCof.viewSelection(map, tailMap), newCov.viewSelection(map, map));
		}
		throw new Exception(
				"LinearGaussians can only combine with other LinearGaussians");
	}

	public ConditionalDistribution complement(ConditionalDistribution marginal,
			Collection<Tuple> discrete, Collection<Tuple> continuous)
			throws Exception {
		if (marginal instanceof LinearGaussian) {
			LinearGaussian lg = (LinearGaussian) marginal;

			int[] mIndices = new int[basemu.size() - continuous.size()];
			int[] cIndices = new int[basemu.size() - mIndices.length];
			for (int i = 0, m = 0, c = 0; i < basemu.size(); i++) {
				boolean found = false;
				for (Tuple t : continuous) {
					if (t.a == i) {
						cIndices[c++] = i;
						found = true;
						continue;
					}
				}
				if (!found) {
					mIndices[m++] = i;
				}
			}

			DoubleMatrix2D C_11i = algebra.inverse(lg.gaussian.getCovariance());
			DoubleMatrix2D C_21 = gaussian.getCovariance().viewSelection(
					cIndices, mIndices).copy();
			DoubleMatrix2D C_12 = gaussian.getCovariance().viewSelection(
					mIndices, cIndices).copy();
			DoubleMatrix2D C_22 = gaussian.getCovariance().viewSelection(
					cIndices, cIndices).copy();

			DoubleMatrix2D C_21xC_11i = algebra.mult(C_21, C_11i);

			DoubleMatrix1D newMu = basemu.viewSelection(cIndices).copy();
			newMu.assign(algebra.mult(C_21xC_11i, lg.basemu));

			DoubleMatrix2D newCof = coefficients.viewSelection(cIndices, null);

			newCof = DoubleFactory2D.dense.appendColumns(C_21xC_11i, newCof
					.assign(algebra.mult(C_21xC_11i, lg.coefficients), PlusMult
							.plusMult(-1.0)));

			C_22
					.assign(algebra.mult(C_21xC_11i, C_12), PlusMult
							.plusMult(-1.0));

			return new LinearGaussian(newMu, newCof, C_22);
		}
		return null;
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {

		int[] mIndices = new int[basemu.size() - continuous.size()];
		int[] cIndices = new int[basemu.size() - mIndices.length];
		for (int i = 0, m = 0, c = 0; i < basemu.size(); i++) {
			boolean found = false;
			for (Tuple t : continuous) {
				if (t.a == i) {
					cIndices[c++] = i;
					found = true;
					continue;
				}
			}
			if (!found) {
				mIndices[m++] = i;
			}
		}

		DoubleMatrix2D C_11i = algebra.inverse(gaussian.getCovariance()
				.viewSelection(mIndices, mIndices));
		DoubleMatrix2D C_21 = gaussian.getCovariance().viewSelection(cIndices,
				mIndices);
		DoubleMatrix2D C_12 = gaussian.getCovariance().viewSelection(mIndices,
				cIndices);
		DoubleMatrix2D C_22 = gaussian.getCovariance().viewSelection(cIndices,
				cIndices).copy();

		DoubleMatrix2D C_21xC_11i = algebra.mult(C_21, C_11i);

		DoubleMatrix1D newMu = basemu.viewSelection(cIndices).copy();
		newMu.assign(algebra.mult(C_21xC_11i, basemu.viewSelection(mIndices)));

		DoubleMatrix2D newCof = coefficients.viewSelection(cIndices, null);

		newCof = DoubleFactory2D.dense.appendColumns(C_21xC_11i, newCof.assign(
				algebra.mult(C_21xC_11i, coefficients.viewSelection(mIndices,
						null)), PlusMult.plusMult(-1.0)));

		C_22.assign(algebra.mult(C_21xC_11i, C_12), PlusMult.plusMult(-1.0));

		return new LinearGaussian(newMu, newCof, C_22);
	}

	@Override
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {

		ConditionalDistribution m = this.copy();
		for (Tuple t : continuous) {
			m = m.marginalize(t.a, false);
		}
		return m;
	}

	@Override
	public ConditionalDistribution setEvidence(int index, double evidence)
			throws Exception {
		if (numberContinuousParentDimensions > 0) {
			throw new Exception(
					"Cannot set evidence on a LinearGaussian with tail variables");
		}
		int[] indices = new int[] { index };
		int[] compIndices = Utility.indexComplement(indices, numberDimensions);
		DoubleMatrix1D A_1 = basemu.viewSelection(compIndices);
		double A_2 = basemu.getQuick(index);
		DoubleMatrix2D C_11 = gaussian.getCovariance().viewSelection(
				compIndices, compIndices);
		DoubleMatrix2D C_12 = gaussian.getCovariance().viewSelection(
				compIndices, indices);
		DoubleMatrix2D C_21 = gaussian.getCovariance().viewSelection(indices,
				compIndices);
		double C_22 = gaussian.getCovariance().getQuick(index, index);
		this.coefficients = coefficients.viewSelection(compIndices, null)
				.copy();
		numberDimensions--;

		if (C_22 > Utility.eps) {
			double muDif = (evidence - A_2);
			double divC = muDif / C_22;

			double x = -0.5 * muDif * divC;
			if (x < 0.00001) {
				x = Math.expm1(x) + 1.0;
			} else {
				x = Math.exp(x);
			}
			x /= Math.sqrt(Math.PI * 2 * C_22);
			weighting *= x;
			basemu = A_1.copy().assign(
					algebra.mult(C_12, DoubleFactory1D.dense.make(1, divC)),
					PlusMult.plusMult(1.0));
			C_11 = C_11.copy().assign(algebra.mult(C_12, C_21),
					PlusMult.minusDiv(C_22));
			gaussian = new Gaussian(basemu, C_11);
		} else {
			weighting = 0;
			basemu = A_1.copy();
			gaussian = new Gaussian(basemu, C_11.copy());
		}

		// now we know that the distribution is simply multidimensional

		// TODO: return a copy of the distribution
		return this;
	}

}
