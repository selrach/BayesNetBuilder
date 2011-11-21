package selrach.bnetbuilder.model.distributions.interfaces;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * This is a distribution that has no parents. This is just a special
 * implementation of a conditional distribution with no parents, as such it can
 * have a couple more functions.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface UnconditionalDistribution extends ConditionalDistribution {
	/**
	 * Gets the probability of the current distribution
	 * 
	 * @param elementValues
	 * @return
	 */
	public double getProbability(DoubleMatrix1D elementValues) throws Exception;

	/**
	 * Gets the log probability of the current distribution
	 * 
	 * @param elementValues
	 * @return
	 */
	public double getLogProbability(DoubleMatrix1D elementValues)
			throws Exception;

	/**
	 * Returns a random sample from the current distribution
	 * 
	 * @return
	 */
	public DoubleMatrix1D sample() throws Exception;

	/**
	 * Returns the expected value from this distribution
	 * 
	 * @return
	 */
	public DoubleMatrix1D getExpectedValue() throws Exception;

	/**
	 * Returns the variance from this distribution, this is not defined for all
	 * unconditional distributions
	 * 
	 * @return
	 */
	public DoubleMatrix2D getCovariance() throws Exception;

	/**
	 * Returns the precision of the current distribution, this is the inverse of
	 * variance.
	 * 
	 * @return
	 */
	public DoubleMatrix2D getPrecision() throws Exception;

	public String getFlatXMLProbabilityDescription() throws Exception;

	/**
	 * Returns a mixed Gaussian distribution that is an approximation of the
	 * current distribution
	 * 
	 * @return
	 * @throws Exception
	 */
	public ConditionalDistribution getGaussianApproximation() throws Exception;

}
