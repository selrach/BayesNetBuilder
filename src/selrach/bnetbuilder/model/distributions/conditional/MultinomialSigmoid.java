package selrach.bnetbuilder.model.distributions.conditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.distributions.DistributionConstant;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Sigmoid;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * This is a distribution for a discrete variable that has continuous parents
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class MultinomialSigmoid extends AbstractConditionalDistribution {
	/**
	 * So we need an lvalue weight for each parent. Then for that set of parents
	 * we need a list of these weights associated with each possible state that
	 * the system can be in. So each state is a row and each column entry is an
	 * lvalue weight, with the first one being the default starting lvalue. We
	 * can calculate an lvalue by summing over the coefficients particular
	 * lvalue by
	 */
	DoubleMatrix2D coefficients;
	Sigmoid sigmoid;

	public MultinomialSigmoid() throws Exception {
		throw new Exception("Don't use the default constructor");
	}

	/**
	 * Default constructor, number of rows in coefficients must be equal to the
	 * number of total states for the variable PI_{i=0}^m statelayout[i].
	 * 
	 * @param stateLayout
	 * @param coefficients
	 * @throws Exception
	 */
	public MultinomialSigmoid(int[] stateLayout, DoubleMatrix2D coefficients)
			throws Exception {
		setNumberStates(stateLayout);
		this.numberDiscreteParentStates = 0;
		this.numberDiscreteParentDimensions = 0;
		this.numberContinuousParentDimensions = coefficients.columns() - 1;
		this.coefficients = coefficients.copy();
		if (this.numberStates != coefficients.rows()) {
			throw new Exception(
					"Number of coefficient rows does not equal number of possible states in state layout");
		}
		sigmoid = new Sigmoid(stateLayout);
	}

	/**
	 * Constructs a default multinomial sigmoid that maps to numberStates states
	 * and anticipates numParentDimensions coefficients
	 * 
	 * @param numberStates
	 * @param numParentDimensions
	 */
	public MultinomialSigmoid(int[] stateLayout, int numParentDimensions)
			throws Exception {
		setNumberStates(stateLayout);
		this.numberDiscreteParentStates = 0;
		this.numberDiscreteParentDimensions = 0;
		this.numberContinuousParentDimensions = numParentDimensions;
		this.coefficients = DoubleFactory2D.dense.make(numberStates,
				numParentDimensions + 1);
		sigmoid = new Sigmoid(numberStates);
	}

	/**
	 * Constructs a default multinomial sigmoid that maps to numberStates states
	 * and anticipates numParentDimensions coefficients
	 * 
	 * @param numberStates
	 * @param numParentDimensions
	 */
	public MultinomialSigmoid(int numberStates, int numParentDimensions)
			throws Exception {
		int[] stateLayout = new int[1];
		stateLayout[0] = numberStates;
		this.numberStates = numberStates;
		this.numberDiscreteParentStates = 0;
		this.numberDiscreteParentDimensions = 0;
		this.numberContinuousParentDimensions = numParentDimensions;
		this.coefficients = DoubleFactory2D.dense.make(numberStates,
				numParentDimensions + 1);
		sigmoid = new Sigmoid(numberStates);
	}

	public MultinomialSigmoid(MultinomialSigmoid copy) throws Exception {
		super(copy);
		this.coefficients = copy.coefficients.copy();
		this.sigmoid = (Sigmoid) copy.copy();
	}

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		if (parentValues.size() != numberContinuousParentDimensions) {
			throw new Exception(
					"Dimension mismatch between distribution and number of parents");
		}
		DoubleMatrix1D lvalues = DoubleFactory1D.dense.make(numberStates);
		for (int i = 0; i < numberStates; i++) {
			double sum = coefficients.getQuick(i, 0);
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				sum += coefficients.getQuick(i, j + 1)
						* parentValues.getQuick(j);
			}
			lvalues.setQuick(i, sum);
		}
		sigmoid.setDistribution(stateLayout, lvalues);
		return sigmoid;
	}

	@Override
	public ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception {
		if (parentValues.size() == numberContinuousParentDimensions) {
			return getDensity(parentValues);
		}

		// Essentially we roll up the evidence set lvalues into the default
		// l-values
		return null;
	}

	@Override
	public String getType() {
		return DistributionConstant.MULTINOMIAL_SIGMOID.toString();
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		list.add(mp = new ArrayList<DistributionDescriptor>());
		for (int i = 0; i < numberStates; i++) {
			mp.add(new DistributionDescriptor("$@" + i + "$$", coefficients
					.getQuick(i, 0), "Base L-value for state [$@" + i + "$$]"));
			for (int j = 0; j < numberContinuousParentDimensions; j++) {
				mp.add(new DistributionDescriptor("[$$" + j
						+ "$$] time [$#$] on [$@" + i + "$$]", coefficients
						.getQuick(i, j + 1),
						"Conditional L-value weight for parent [$$" + j
								+ "$$], time [$#$] on state [$@" + i + "$$]"));
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
		if (mp.size() != numberStates * (numberContinuousParentDimensions + 1)) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of descriptors");
		}

		int m = 0;
		for (int i = 0; i < numberStates; i++) {
			coefficients.setQuick(i, 0, mp.get(m++).getValue());
			for (int j = 0; j < numberContinuousParentDimensions; i++) {
				coefficients.setQuick(i, j + 1, mp.get(m++).getValue());
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
		for (int i = 0; i < numberStates; i++) {
			for (int j = 0; j < numberContinuousParentDimensions + 1; j++) {
				sb.append(" ");
				sb.append(coefficients.getQuick(i, j));
			}
		}
		return sb.toString();
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

	public UnconditionalDistribution getLikelihoodDistributions(
			DoubleMatrix1D sample, DoubleMatrix1D parentValues,
			int ignoreParentIndex) throws Exception {

		int[] state = new int[numberDimensions];
		for (int i = 0; i < numberDimensions; i++) {
			state[i] = (int) sample.getQuick(i);
		}
		int index = getStateIndex(state);
		double sum = coefficients.getQuick(index, 0);
		for (int j = 0; j < numberContinuousParentDimensions; j++) {
			if (j != ignoreParentIndex) {
				sum += coefficients.getQuick(index, j + 1)
						* parentValues.getQuick(j);
			}
		}

		return null;
	}

	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (indexedProbabilityDescription.size() != 0) {
			throw new Exception(
					"You have a conditional map with a LinearGaussian distribution?  Something is wrong.");
		}
		String[] probs = probabilityDescription.split(" ");
		if (probs.length != numberStates * numberContinuousParentDimensions) {
			throw new Exception(
					"You have a different number of probabilities than possible states in this Sigmoid");
		}

		int[] indexMap = generateIndexMap(currentStructure,
				conditionalVariables, conditionalVariableTimes);

		int ind = 0;
		for (int i = 0; i < numberStates; i++) {
			for (int j = 0; j < numberContinuousParentDimensions + 1; j++) {
				coefficients.setQuick(i, indexMap[j], Double
						.parseDouble(probs[ind++]));
			}
		}

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
		return new MultinomialSigmoid(this);
	}

	@Override
	public ConditionalDistribution marginalize(int index, boolean onDependencies)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int extend(int numStates) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution setEvidence(int index, double evidence)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConditionalDistribution complement(ConditionalDistribution marginal,
			Collection<Tuple> discrete, Collection<Tuple> continuous)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
