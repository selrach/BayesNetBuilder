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
import cern.clhep.PhysicalConstants;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.CholeskyDecomposition;
import cern.jet.math.PlusMult;
import cern.jet.random.Normal;

/**
 * This handles a Gaussian distribution, both single and multivariate.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">crobertson</a>
 * 
 */
public class Gaussian extends AbstractUnconditionalDistribution {

	// /
	// /Private Variables
	// /

	/**
	 * Expected value for this gaussian distribution
	 */
	private DoubleMatrix1D mu;

	/**
	 * Covarience matrix
	 */
	private DoubleMatrix2D covariance;

	/**
	 * Cache of choleskydecomposition of covariance matrix
	 */
	private CholeskyDecomposition covariance_cd;

	/**
	 * Cache of determinant of covariance matrix
	 */
	private double covariance_det;

	/**
	 * Cached precision matrix which is the inverse of the covariance matrix
	 */
	private DoubleMatrix2D precision;

	/**
	 * object to handle linear algebra in this object
	 */
	private final Algebra algebra = new Algebra();

	/**
	 * Random number generator for this distribution
	 */
	private final Normal random = Random.getNormal();

	// /
	// /Constructors
	// /

	/**
	 * Creates a 1-d Gaussian w/ mu = 0 & variance = 1
	 * 
	 */
	public Gaussian() throws Exception {
		mu = DoubleFactory1D.dense.make(1, 0);
		covariance = DoubleFactory2D.dense.make(1, 1, 1);
		createCached();
	}

	/**
	 * Creates a multivariate Gaussian centered on 0 with a "variance" of 1
	 * 
	 * @param dimensions
	 */
	public Gaussian(int dimensions) throws Exception {
		// TODO check generic construction...may just be along diagonal
		numberDimensions = dimensions;
		mu = DoubleFactory1D.dense.make(dimensions, 0);
		covariance = DoubleFactory2D.dense.make(dimensions, dimensions, 1);
		createCached();
	}

	/**
	 * Creates a multi-variate Gaussian with supplied mu and variance
	 * 
	 * @param mu
	 * @param covariance
	 * @throws Exception
	 */
	public Gaussian(DoubleMatrix1D mu, DoubleMatrix2D covariance)
			throws Exception {
		setGaussian(mu, covariance);
	}

	/**
	 * Creates a 1d Gaussian with given mu and variance
	 * 
	 * @param mu
	 * @param variance
	 */
	public Gaussian(double mu, double variance) throws Exception {
		setGaussian(mu, variance);
	}

	/**
	 * creates a multivariate gaussian, centered on "0" with the given
	 * covariance, useful when we don't care where the mean is right now.
	 * 
	 * @param covariance
	 * @throws Exception
	 */
	public Gaussian(DoubleMatrix2D covariance) throws Exception {
		if (covariance.rows() != covariance.columns()) {
			throw new Exception("Covariance must be square.");
		}
		setGaussian(DoubleFactory1D.dense.make(covariance.rows()), covariance);
	}

	// /
	// /Private Functions
	// /

	/**
	 * Creates all the things that should be cached for quicker usage.
	 * 
	 */
	private void createCached() throws Exception {
		precision = algebra.inverse(covariance);
		covariance_cd = new CholeskyDecomposition(this.covariance);
		if (!covariance_cd.isSymmetricPositiveDefinite()) {
			throw new Exception(
					"Bad covariance matrix, not symmetricpositivedefinite");
		}
		covariance_det = algebra.det(covariance);
	}

	// /
	// /Public Functions
	// /

	/**
	 * Sets the mean, must have the right number of dimensions
	 * 
	 * @param mu
	 * @throws Exception
	 */
	public void setMean(DoubleMatrix1D mu) throws Exception {
		if (mu.size() != numberDimensions || mu.size() != numberDimensions) {
			throw new Exception("Invalid dimensions for mean");
		}
		this.mu = mu.copy();
	}

	/**
	 * Sets the mean, this Gaussian must be 1d.
	 */
	public void setMean(double mu) throws Exception {
		if (numberDimensions != 1) {
			throw new Exception("Invalid dimensions for mean");
		}
		this.mu = DoubleFactory1D.dense.make(1, mu);
	}

	/**
	 * Sets the covariance matrix, it must have same dimensionality as this
	 * Gaussian
	 * 
	 * @param covariance
	 * @throws Exception
	 */
	public void setCovariance(DoubleMatrix2D covariance) throws Exception {
		if (numberDimensions != covariance.rows()
				|| numberDimensions != covariance.columns()) {
			throw new Exception("invalid dimensions for covariance");
		}
		this.covariance = covariance.copy();
		createCached();
	}

	/**
	 * Sets the variance, this Gaussian must be 1d
	 * 
	 * @param variance
	 * @throws Exception
	 */
	public void setVariance(double variance) throws Exception {
		if (numberDimensions != 1) {
			throw new Exception("Invalid dimensions for variance");
		}
		this.covariance = DoubleFactory2D.dense.make(1, 1, variance);
		createCached();
	}

	/**
	 * Resets the parameters of this Gaussian.
	 * 
	 * @param mu
	 * @param covariance
	 * @throws Exception
	 */
	public void setGaussian(DoubleMatrix1D mu, DoubleMatrix2D covariance)
			throws Exception {
		numberDimensions = mu.size();
		if (numberDimensions != covariance.rows()
				|| numberDimensions != covariance.columns()) {
			throw new Exception("mu and sigma must be of same dimension");
		}

		this.mu = mu.copy();
		this.covariance = covariance.copy();
		createCached();
	}

	/**
	 * Resets the parameters of this Gaussian
	 * 
	 * @param mu
	 * @param variance
	 */
	public void setGaussian(double mu, double variance) throws Exception {
		numberDimensions = 1;
		this.mu = DoubleFactory1D.dense.make(1, mu);
		this.covariance = DoubleFactory2D.dense.make(1, 1, variance);
		createCached();
	}

	// /
	// /Overridden Functions
	// /

	@Override
	public DoubleMatrix1D getExpectedValue() throws Exception {
		return mu.copy();
	}

	@Override
	public DoubleMatrix2D getPrecision() throws Exception {
		return precision;
	}

	@Override
	public double getProbability(DoubleMatrix1D elementValues) throws Exception {
		DoubleMatrix1D cpy = elementValues.copy();
		cpy.assign(mu, PlusMult.plusMult(-1));
		return Math.pow(PhysicalConstants.twopi,
				-(double) numberDimensions / 2.0)
				* Math
						.exp(-algebra.mult(cpy, algebra.mult(precision, cpy)) / 2.0)
				/ Math.sqrt(covariance_det);
	}

	@Override
	public double getLogProbability(DoubleMatrix1D elementValues)
			throws Exception {
		DoubleMatrix1D cpy = elementValues.copy();
		cpy.assign(mu, PlusMult.plusMult(-1));
		return (-algebra.mult(cpy, algebra.mult(precision, cpy)) / 2.0)
				- (numberDimensions / 2.0) * Math.log(PhysicalConstants.twopi)
				- (Math.log(covariance_det) / 2.0);
	}

	@Override
	public String getType() {
		return DistributionConstant.GAUSSIAN.toString();
	}

	@Override
	public DoubleMatrix2D getCovariance() throws Exception {
		return covariance;
	}

	@Override
	public DoubleMatrix1D sample() throws Exception {
		DoubleMatrix1D ret = DoubleFactory1D.dense.make(numberDimensions);
		ret.assign(random);
		ret = algebra.mult(covariance_cd.getL(), ret);
		ret.assign(mu, PlusMult.plusMult(1.0));
		return ret;
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		list.add(mp = new ArrayList<DistributionDescriptor>());
		if (numberDimensions == 1) {
			mp.add(new DistributionDescriptor("mu", mu.getQuick(0),
					"Mean of Gaussian"));
			mp.add(new DistributionDescriptor("variance", covariance.getQuick(
					0, 0), "Variance of Gaussian"));
		} else {
			for (int i = 0; i < numberDimensions; i++) {
				mp.add(new DistributionDescriptor("mu_" + i, mu.getQuick(i),
						"Mean in dimension " + i));
			}
			for (int i = 0; i < numberDimensions; i++) {
				for (int j = 0; j < numberDimensions; j++) {
					mp.add(new DistributionDescriptor("cov_" + i + "_" + j,
							covariance.getQuick(i, j),
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
		if (mp.size() != numberDimensions + numberDimensions * numberDimensions) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of descriptors");
		}
		int k = 0;
		for (int i = 0; i < numberDimensions; i++) {
			mu.setQuick(i, mp.get(k++).getValue());
		}
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				covariance.setQuick(i, j, mp.get(k++).getValue());
			}
		}
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
			sb.append(mu.getQuick(i));
		}
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				sb.append(" ");
				sb.append(covariance.getQuick(i, j));
			}
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
					"Parents don't make sense for Gaussian distribution");
		}
		if (conditionalVariableTimes.size() != 0) {
			throw new Exception(
					"How do you have a parent time on a Gaussian distribution?");
		}
		if (indexedProbabilityDescription.size() != 0) {
			throw new Exception(
					"You have a condional map with a Gaussian distribution?  Something is wrong.");
		}
		String[] probs = probabilityDescription.split(" ");
		if (probs.length != numberDimensions + numberDimensions
				* numberDimensions) {
			throw new Exception(
					"You have a different number of probabilities than possible states in this Sigmoid");
		}
		for (int i = 0; i < numberDimensions; i++) {
			mu.setQuick(i, Double.parseDouble(probs[i]));
		}
		int ind = numberDimensions;
		for (int i = 0; i < numberDimensions; i++) {
			for (int j = 0; j < numberDimensions; j++) {
				covariance.setQuick(i, j, Double.parseDouble(probs[ind++]));
			}
		}
	}

	public UnconditionalDistribution copy() throws Exception {		
		return new Gaussian(mu, covariance);
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
	public UnconditionalDistribution setEvidence(int index, double evidence)
			throws Exception {
		// Only singluar Gaussians can be set, index is ignored since it should
		// always be 0
		if (index != 0 || numberDimensions > 1) {
			throw new Exception(
					"Evidence can only be set on single dimension Gaussians");
		}
		return new Table(new double[] { getProbability(DoubleFactory1D.dense
				.make(1, evidence)) });
	}

	@Override
	public ConditionalDistribution marginalize(int index, boolean onDependencies) throws Exception {
		// Summing over all a dimension in a Gaussian is simply removing that
		// dimension from the distribution, this assumption follows if there is
		// no correlation between variables (only diagonal of covariance is
		// interesting). Otherwise we need to do the calculation:
		// U = U_{11} - U_{12}U_{22}^{-1}U_{21}
		// to find the covariance matrix
		int[] indices = new int[mu.size() - 1];
		for (int i = 0, j = 0; i < mu.size(); i++) {
			if (i != index) {
				indices[j++] = i;
			}
		}
		DoubleMatrix1D newMu = mu.viewSelection(indices).copy();
		DoubleMatrix2D covar = covariance.viewSelection(indices, indices);

		return new Gaussian(newMu, covar);
	}

	@Override
	public int extend(int numStates) throws Exception {

		if (numStates > 0) {
			throw new Exception(
					"Gaussian distribution does not have states to extend");
		}
		this.mu = DoubleFactory1D.dense.append(this.mu, DoubleFactory1D.dense
				.make(1));
		DoubleMatrix2D[][] parts = { { this.covariance, null },
				{ null, DoubleFactory2D.dense.identity(1) } };
		this.covariance = DoubleFactory2D.dense.compose(parts);
		createCached();		
		return this.mu.size()-1;
	}

	@Override
	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception {
		//This should probably never be called
		throw new Exception("Combine not implemented for Gaussian");
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		//This should probably never be called
		throw new Exception("Complement not implemented for Gaussian");
	}

	@Override
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		//This should probably never be called
		throw new Exception("Marginalize not implemented for Gaussian");
	}

	@Override
	public ConditionalDistribution getGaussianApproximation() throws Exception {
		return copy();
	}

	@Override
	public ConditionalDistribution complement(ConditionalDistribution marginal,
			Collection<Tuple> discrete, Collection<Tuple> continuous)
			throws Exception {
		//This should probably never be called
		throw new Exception("Complement not implemented for Gaussian");
	}

}
