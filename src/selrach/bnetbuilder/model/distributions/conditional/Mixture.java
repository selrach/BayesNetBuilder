package selrach.bnetbuilder.model.distributions.conditional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * This handles a generalized mixture of discrete and continuous components
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class Mixture extends AbstractConditionalDistribution {

	/**
	 * Set of distributions that contribute to this mixture, each one having the
	 * same conditioning continuous vector
	 */
	ConditionalDistribution distributions[];

	/**
	 * This should be a discrete (Table, Sigmoid, MultinomialSigmoid)
	 * distribution whose state space should be equal to that of the set of
	 * distributions making up this mixture (AKA the possible state combinations
	 * of discrete parents), and whose parent conditioning vector is the same
	 * continuous space as the distributions (In the case of a
	 * MultinomialSigmoid).
	 */
	ConditionalDistribution distributionProbability = null;

	public Mixture() {
		super();
	}

	public Mixture(Mixture copy) throws Exception {
		super(copy);
		distributions = new ConditionalDistribution[copy.distributions.length];
		if (copy.distributionProbability != null) {
			distributionProbability = copy.distributionProbability.copy();
		}
		for (int i = 0; i < copy.distributions.length; i++) {
			distributions[i] = copy.distributions[i].copy();
		}
	}

	@Override
	public UnconditionalDistribution getDensity(DoubleMatrix1D parentValues)
			throws Exception {
		int[] continuousIndices = new int[numberContinuousParentDimensions];
		for(int i=0, j=0; i<parentLayout.length; i++)
		{
			if(parentLayout[i] <=0) {
				continuousIndices[j++] = i;
			}
		}
		return distributions[Utility.calculateIndex(parentValues, parentLayout)]
				.getDensity(parentValues.viewSelection(continuousIndices));
	}

	public ConditionalDistribution getDensityProbabilityDistribution()
			throws Exception {
		return distributionProbability.copy();
	}

	public double getDensityProbability(DoubleMatrix1D parentValues)
			throws Exception {
		if (distributionProbability == null) {
			return 1;
		}
		return distributionProbability.getProbability(parentValues.viewPart(
				this.numberDiscreteParentDimensions,
				this.numberContinuousParentDimensions), parentValues.viewPart(
				0, this.numberDiscreteParentDimensions));
	}

	public void setDensityProbability(
			ConditionalDistribution discreteDistribution) throws Exception {
		if (discreteDistribution.getNumberStates() == distributions.length) {
			this.distributionProbability = discreteDistribution.copy();
		} else {
			this.distributionProbability = new Table();
		}
	}

	@Override
	public List<List<DistributionDescriptor>> getDistributionDescriptor()
			throws Exception {
		List<List<DistributionDescriptor>> list = new ArrayList<List<DistributionDescriptor>>();
		List<DistributionDescriptor> mp;
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			list.add(mp = new ArrayList<DistributionDescriptor>());
			mp.addAll(distributions[i].getDistributionDescriptor().get(0));
		}
		return list;
	}

	@Override
	public void setDistributionDescriptor(
			List<List<DistributionDescriptor>> descriptor) throws Exception {
		if (descriptor.size() != numberDiscreteParentStates) {
			throw new Exception(this.getClass().toString()
					+ " invalid number of switching states");
		}
		List<List<DistributionDescriptor>> tmp = new ArrayList<List<DistributionDescriptor>>();
		for (int i = 0; i < numberDiscreteParentStates; i++) {
			List<DistributionDescriptor> mp = descriptor.get(i);
			tmp.add(mp);
			distributions[i].setDistributionDescriptor(tmp);
			tmp.clear();
		}
	}

	@Override
	public String getXMLDescription() throws Exception {
		StringBuilder sb = new StringBuilder("<dpis>\n");
		int[] currentIndex = new int[numberDiscreteParentDimensions];
		int ndpdm1 = numberDiscreteParentDimensions - 1;
		int i = 0;
		int k = 0;
		for (int j = 0; j < numberDiscreteParentStates; j++) {
			sb.append("<dpi index=\"");
			for (i = 0; i < numberDiscreteParentDimensions; i++) {
				sb.append(" ");
				sb.append(currentIndex[i]);
			}

			sb.append("\">");
			sb.append(this.distributions[j].getFlatXMLProbabilityDescription());
			sb.append("</dpi>\n");
			for (i = ndpdm1, k = parentLayout.length - 1; k >= 0; k--) {
				if (parentLayout[k] == -1) {
					continue;
				}
				if (currentIndex[i] >= parentLayout[k] - 1) {
					currentIndex[i] = 0;
				} else {
					currentIndex[i]++;
					break;
				}
				i--;
			}
		}
		sb.append("</dpis>");
		return sb.toString();
	}

	public void setup(List<RandomVariable> currentStructure,
			List<RandomVariable> conditionalVariables,
			List<Integer> conditionalVariableTimes,
			String probabilityDescription,
			Map<String, String> indexedProbabilityDescription) throws Exception {
		if (probabilityDescription != null) {
			throw new Exception(
					"It does not make sense to have a non-indexed probability string in the Mixture");
		}
		if (conditionalVariables.size() == 0) {
			throw new Exception("You need some parents for the Mixture");
		}
		if (conditionalVariableTimes.size() == 0) {
			throw new Exception("You need some parents for the Mixture");
		}
		if (indexedProbabilityDescription.size() == 0) {
			throw new Exception(
					"You need some indexed probabilitydescription strings for the Mixture?  Something is wrong.");
		}
		int[] indexMap = generateIndexMap(currentStructure,
				conditionalVariables, conditionalVariableTimes);

		int[] discreteMap = new int[indexMap.length];
		for (int i = 0, k = 0, d = 0; i < parentLayout.length; i++) {
			if (parentLayout[indexMap[i]] <= 0) {
				discreteMap[indexMap[i]] = d++;
			} else {
				discreteMap[indexMap[i]] = k++;
			}
		}

		List<RandomVariable> fCVars = new ArrayList<RandomVariable>();
		List<Integer> fCTime = new ArrayList<Integer>();
		List<RandomVariable> fCStructure = new ArrayList<RandomVariable>();

		for (RandomVariable rv : currentStructure) {
			if (rv instanceof ContinuousVariable) {
				fCStructure.add(rv);
			}
		}
		for (int i = 0; i < conditionalVariables.size(); i++) {
			RandomVariable rv = conditionalVariables.get(i);
			if (rv instanceof ContinuousVariable) {
				fCVars.add(rv);
				fCTime.add(conditionalVariableTimes.get(i));
			}
		}

		int[] parentValues = new int[numberDiscreteParentDimensions];

		for (Entry<String, String> entry : indexedProbabilityDescription
				.entrySet()) {
			String[] indices = entry.getKey().split(" ");
			if (indices.length != parentValues.length) {
				throw new Exception("Index has wrong number of entries");
			}
			for (int i = 0, k = 0; i < parentLayout.length; i++) {
				if (parentLayout[indexMap[i]] <= 0) {
					continue;
				}
				parentValues[discreteMap[i]] = Integer.parseInt(indices[k]);
				k++;
			}
			ConditionalDistribution dist = distributions[Utility
					.calculateIndex(parentValues, parentLayout)];
			dist.setup(fCStructure, fCVars, fCTime, entry.getValue(), null);
		}
	}

	@Override
	public void randomize() {
		for (ConditionalDistribution cd : distributions) {
			cd.randomize();
		}
		if (distributionProbability != null) {
			distributionProbability.randomize();
		}
	}

	@Override
	public void reset() {
		for (ConditionalDistribution cd : distributions) {
			cd.reset();
		}
		if (distributionProbability != null) {
			distributionProbability.reset();
		}
	}

	@Override
	public ConditionalDistribution setParentEvidence(int[] which,
			DoubleMatrix1D parentValues) throws Exception {
		if(which.length==0) {
			return this;
		}
		// Split up the distribution into its discrete and continuous parents
		// For each discrete parent with evidence set, discard the other parts
		// of the discrete domain that are inconsistent.
		// For each continuous parent, let the slave distribution handle it.
		List<Integer> continuous = new ArrayList<Integer>();
		List<Integer> discrete = new ArrayList<Integer>();
		DoubleMatrix1D allDParents = DoubleFactory1D.dense.make(
				numberDiscreteParentDimensions, 0);
		int[] mapping = new int[parentLayout.length];
		int d = 0;
		int c = 0;
		for (int i = 0; i < parentLayout.length; i++) {
			if (parentLayout[i] <= 0) {
				mapping[i] = c++;
			} else {
				mapping[i] = d++;
			}
		}
		int k = 0;
		List<Integer> ignore = new ArrayList<Integer>();
		for (int i : which) {
			if (parentLayout[i] != -1) {
				ignore.add(mapping[i]);
				discrete.add(k);
				allDParents.setQuick(mapping[i], parentValues.getQuick(k));
			} else {
				continuous.add(k);
			}

			k++;
		}

		int[] comp = Utility.indexComplement(which,
				numberContinuousParentDimensions
						+ numberDiscreteParentDimensions);

		int[] newParentLayout = new int[comp.length];

		int numStates = 1;
		int[] newDiscreteLayout = new int[numberDiscreteParentDimensions - discrete.size()];
		k=0;
		d=0;
		for (int i : comp) {
			if (parentLayout[i] != -1) {
				newDiscreteLayout[d++] = parentLayout[i];
				numStates *= parentLayout[i];
			}
			newParentLayout[k++] = parentLayout[i];
		}

		int[] discConversion = Utility.convert(discrete);
		int[] contConversion = Utility.convert(continuous);
		DoubleMatrix1D dParentValues = parentValues
				.viewSelection(discConversion);
		DoubleMatrix1D cParentValues = parentValues
				.viewSelection(contConversion);
		Mixture cd = (Mixture) this.copy();
		cd.distributions = new ConditionalDistribution[numStates];
		int i = 0;
		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(numStates);
		do {
			int index = Utility.calculateIndex(allDParents, parentLayout);
			cd.distributions[i] = distributions[index].setParentEvidence(
					contConversion, cParentValues);
			if (distributionProbability != null) {
				probabilities.setQuick(i, distributionProbability
						.getProbability(null, allDParents));
			}
			i++;
		} while (Utility.incrementIndice(allDParents, parentLayout, ignore));

		if(newDiscreteLayout.length == 0)
		{
			newDiscreteLayout = new int[]{1};
		}
		cd.parentLayout = newParentLayout;
		cd.numberContinuousParentDimensions = newParentLayout.length - newDiscreteLayout.length;
		cd.numberDiscreteParentDimensions = newDiscreteLayout.length;
		cd.numberDiscreteParentStates = numStates;
		cd.numberStates = numberStates;

		if (distributionProbability != null) {

			cd.distributionProbability = new Table(newDiscreteLayout, probabilities);
		}

		if (cd.distributions.length == 1) {
			return cd.distributions[0];
		}
		return cd;
	}

	public ConditionalDistribution marginalize(int index, boolean onDependencies)
			throws Exception {
		throw new Exception("Marginalize not implemented for "
				+ this.getClass().getCanonicalName());
	}

	public int extend(int i) throws Exception {
		int index = -1;
		if (i <= 0) {
			// Add Continuous
			if (distributions == null) {
				distributions = new ConditionalDistribution[this.distributionProbability
						.getNumberStates()];
			}

			for (int j = 0; j < distributions.length; j++) {
				if (distributions[j] == null) {
					distributions[j] = new LinearGaussian(1, 1);
				} else {
					distributions[j].extend(i);
				}
			}
			index = this.numberContinuousParentDimensions;
			this.numberContinuousParentDimensions++;
		} else {
			// Add discrete
			index = this.distributionProbability.extend(i);
			ConditionalDistribution[] newDists = new ConditionalDistribution[distributions.length
					* i];
			int ind = 0;

			for (int j = 0; j < i; j++) {
				for (int k = 0; k < distributions.length; k++) {
					newDists[ind++] = distributions[k].copy();
				}
			}
			this.distributions = newDists;
		}
		return index;
	}

	@Override
	public ConditionalDistribution marginalize(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {

		// This can be done better in some cases

		ConditionalDistribution lgm = this.copy();
		if (continuous != null) {
			for (Tuple t : continuous) {
				lgm = lgm.marginalize(t.a, false);
			}

		}
		if (discrete != null) {
			for (Tuple t : discrete) {
				lgm = lgm.marginalize(t.a, true);
			}
		}

		return lgm;
	}

	public ConditionalDistribution combine(ConditionalDistribution b,
			Map<String, Tuple> discrete1, Map<String, Tuple> discrete2,
			Map<String, Tuple> head1, Map<String, Tuple> head2,
			Map<String, Tuple> tail1, Map<String, Tuple> tail2)
			throws Exception {

		if (!(b instanceof Mixture)) {
			throw new Exception("Can't combine non mixure with mixture.");
		}

		Mixture bMix = (Mixture) b;

		List<Quadruple> discrete = new ArrayList<Quadruple>(Operation.Quadruple
				.combine(discrete1, discrete2).values());

		ConditionalDistribution newPDist = Operation.combine(
				distributionProbability, bMix.distributionProbability,
				discrete, null, null);

		int[] stateLayout1 = distributionProbability.getStateLayout();
		int[] stateLayout2 = bMix.distributionProbability.getStateLayout();

		DoubleMatrix1D aindices = DoubleFactory1D.dense.make(
				stateLayout1.length, 0);
		DoubleMatrix1D bindices = DoubleFactory1D.dense.make(
				stateLayout2.length, 0);
		DoubleMatrix1D retindices = DoubleFactory1D.dense.make(discrete.size(),
				0);

		int numStates = 1;
		int[] stateLayout = new int[discrete.size()]; // need to add in
		// continuous parts
		for (int i = 0; i < stateLayout.length; i++) {
			Quadruple t = discrete.get(i);
			if (t.a != -1) {
				stateLayout[i] = stateLayout1[t.a];
			} else {
				stateLayout[i] = stateLayout2[t.b];
			}
			numStates *= stateLayout[i];
		}

		ConditionalDistribution[] newdists = new ConditionalDistribution[numStates];

		do {
			Utility.setupIndices(stateLayout, retindices, aindices, bindices,
					discrete);
			// Multiply distributions

			newdists[Utility.calculateIndex(retindices, stateLayout)] = distributions[Utility
					.calculateIndex(aindices, stateLayout1)].combine(
					bMix.distributions[Utility.calculateIndex(bindices,
							stateLayout2)], discrete1, discrete2, head1, head2,
					tail1, tail2);
		} while (Utility.incrementIndice(retindices, stateLayout));

		if (b instanceof LinearGaussianMix) {
			return new LinearGaussianMix(stateLayout, newPDist, newdists);
		} else {
			throw new Exception("mixture results in multinomialsigmoidmix");
			// return new MultinomialSigmoidMix();
		}
	}

	@Override
	public ConditionalDistribution complement(Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		throw new Exception("Not implemented");
	}
	public ConditionalDistribution complement(ConditionalDistribution marginal, Collection<Tuple> discrete,
			Collection<Tuple> continuous) throws Exception {
		throw new Exception("Not implemented");		
	}

	public ConditionalDistribution[] getDistributions() {
		return distributions;
	}
	

	@Override
	public ConditionalDistribution setEvidence(int index, double evidence)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}