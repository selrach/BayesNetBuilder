package selrach.bnetbuilder.model.distributions.unconditional;

import java.util.List;

import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.conditional.AbstractConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Base class of all unconditional distributions, basically this means that the
 * distribution can have no concept of parents
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class AbstractUnconditionalDistribution extends
		AbstractConditionalDistribution implements UnconditionalDistribution {

	protected AbstractUnconditionalDistribution() {

	}

	protected AbstractUnconditionalDistribution(
			AbstractUnconditionalDistribution copy) {
		this.numberDimensions = copy.numberDimensions;
	}

	/**
	 * Computing epsilon since double arithmatic isn't accurate
	 */
	final protected double eps = 1e-10;

	public double getLogProbability(DoubleMatrix1D elementValues)
			throws Exception {
		throw new Exception(getClass().getName()
				+ " getLogProbability not implemented");
	}

	public DoubleMatrix2D getPrecision() throws Exception {
		throw new Exception(getClass().getName()
				+ " getPrecision not implemented");
	}

	public double getProbability(DoubleMatrix1D elementValues) throws Exception {
		throw new Exception(getClass().getName()
				+ " getProbability not implemented");
	}

	public DoubleMatrix1D sample() throws Exception {
		throw new Exception(getClass().getName() + " sample not implemented");
	}

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		return this;
	}

	@Override
	public double getLogProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception {
		return getLogProbability(elementValues);
	}

	@Override
	public int getNumberDimensions() {
		return numberDimensions;
	}

	public int getNumberContinuousParents() {
		return 0;
	}

	public int getNumberDiscreteParents() {
		return 0;
	}

	public int getNumberParents() {
		return 0;
	}

	@Override
	public int getNumberStates() {
		return -1;
	}

	@Override
	public int getNumberParentDimensions() {
		return 0;
	}

	@Override
	public double getProbability(DoubleMatrix1D parentValues,
			DoubleMatrix1D elementValues) throws Exception {
		return getProbability(elementValues);
	}

	@Override
	public String getType() {
		return "AbstractUnconditionalDistribution";
	}

	@Override
	public DoubleMatrix1D sample(DoubleMatrix1D parentValues) throws Exception {
		return sample();
	}

	@Override
	public int[] getStateLayout() {
		return null;
	}

	public int getStateIndex(int[] which) throws Exception {
		int[] stateLayout;
		if ((stateLayout = getStateLayout()) == null) {
			return -1;
		}
		if (which.length != stateLayout.length) {
			throw new Exception("Dimension does not exist");
		}
		int i;
		int ind = 0;
		for (i = 0; i < which.length - 1; i++) {
			ind = (stateLayout[i] - 1) * (ind + which[i]);
		}
		ind += which[i];
		return ind;
	}

	public int[] getStateMatrix(int which) throws Exception {
		int[] stateLayout;
		if ((stateLayout = getStateLayout()) == null) {
			return null;
		}
		if (numberDimensions <= 0) {
			return null;
		}
		int[] ret = new int[numberDimensions];
		int i;
		for (i = numberDimensions - 1; i >= 0; i--) {
			ret[i] = which % (stateLayout[i]);
			which = which / stateLayout[i];
		}
		return ret;
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		throw new Exception("must create sufficient descriptor");
	}

	@Override
	public int getNumberContinuousParentDimensions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberDiscreteParentDimensions() {
		return 0;
	}

	@Override
	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception {
		throw new Exception("must handle sufficient descriptor");
	}

	@Override
	public String getXMLDescription() throws Exception {
		throw new Exception("No XML Setup for distribution " + getType());
	}

	@Override
	public String getFlatXMLProbabilityDescription() throws Exception {
		throw new Exception("No XML Setup for distribution " + getType());
	}

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {
		// This should be trivial
		return null;
	}

	@Override
	public ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception {
		return this;
	}

	@Override
	public ConditionalDistribution getGaussianApproximation() throws Exception {
		return new Gaussian(this.getExpectedValue(), this.getCovariance());
	}
}
