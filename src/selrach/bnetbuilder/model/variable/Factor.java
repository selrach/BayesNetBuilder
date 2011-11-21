package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.conditional.ConditionalTable;
import selrach.bnetbuilder.model.distributions.conditional.Mixture;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Factor object, its used when we are dealing with only transient variables.
 * This represents an intermediate distribution that is a result of different
 * inference algorithms. This is particularly targeted toward transient objects
 * in the system.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Factor {

	private static final Logger logger = Logger.getLogger(Factor.class);

	/**
	 * This maps to the distribution, for this factor. For a completely discrete
	 * network, it probably maps to a Table, continuous network-a
	 * LinearGaussian, for a hybrid network a LinearGaussianMix (hybrid parents,
	 * continuous variable) or even MultinomialSigmoidMix (hybrid parents,
	 * discrete variable)
	 */
	ConditionalDistribution distribution = null;

	/**
	 * This is a list of all the discrete dependencies in this factor
	 */
	private final List<TransientVariable> discreteDependencies = new ArrayList<TransientVariable>();

	/**
	 * These are the head dependencies in this distribution, basically a subset
	 * of the continuous variables that are affecting this distribution
	 */
	private final List<TransientVariable> headContinuous = new ArrayList<TransientVariable>();

	/**
	 * These are the tail dependencies in this distribution, basically a subset
	 * of the continuous variables affecting this distribution.
	 */
	private final List<TransientVariable> tailContinuous = new ArrayList<TransientVariable>();

	/**
	 * Set of variables that have been contained within this factor...used for
	 * debugging
	 */
	private final Set<TransientVariable> components = new TreeSet<TransientVariable>();

	/**
	 * Set of variables that have been summed out of this factor...used for
	 * debugging
	 */
	private final Set<TransientVariable> summedComponents = new TreeSet<TransientVariable>();

	/**
	 * Set of variables that are considered evidence nodes in this factor...used
	 * for debugging
	 */
	private final Set<TransientVariable> evidenceComponents = new TreeSet<TransientVariable>();

	public Factor(int time, Potential potential) throws Exception {
		for (GraphVariable gv : potential.getDependencies()) {
			discreteDependencies.add(gv.getReference().getTransientVariable(
					time - gv.getSlice()));
		}
		for (GraphVariable gv : potential.getTailDependencies()) {
			tailContinuous.add(gv.getReference().getTransientVariable(
					time - gv.getSlice()));
		}
		for (GraphVariable gv : potential.getHeadDependencies()) {
			headContinuous.add(gv.getReference().getTransientVariable(
					time - gv.getSlice()));
		}
		this.distribution = potential.getDistribution().copy();
	}

	public Factor(TransientVariable variable) throws Exception {
		// components.addAll(variable.getParents());
		components.add(variable);

		List<TransientVariable> parents = variable.getParents();
		DoubleMatrix1D parentValues = DoubleFactory1D.dense.make(
				parents.size(), 0);
		List<Integer> pIgnore = new ArrayList<Integer>();
		List<Integer> numStates = new ArrayList<Integer>();
		ArrayList<Double> dbl = new ArrayList<Double>();

		for (TransientVariable p : parents) {
			int i;
			if (p.isEvidence()) {
				if (logger.isDebugEnabled()) {
					evidenceComponents.add(p);
				}
				i = parents.indexOf(p);

				pIgnore.add(i);
				parentValues.setQuick(i, p.getValue());
			} else {
				i = p.getDistribution().getNumberStates();

				if (i == -1) {
					tailContinuous.add(p);
				} else {
					discreteDependencies.add(p);
					numStates.add(i);
				}
			}
		}

		DoubleMatrix1D evidenceList = DoubleFactory1D.dense
				.make(pIgnore.size());
		int j = 0;
		Collections.sort(pIgnore);
		for (Integer ig : pIgnore) {
			evidenceList.setQuick(j++, parentValues.getQuick(ig));
		}

		ConditionalDistribution dist = variable.getDistribution()
				.setParentEvidence(Utility.convert(pIgnore), evidenceList);

		// We can construct the factor by investigating the variable
		if (variable.getReference() instanceof DiscreteVariable) {
			DiscreteVariable dv = (DiscreteVariable) variable.getReference();
			if (tailContinuous.size() > 0) {
				throw new Exception(
						"Sorry cannot handle continuous parents on discrete variables right now: "
								+ dv.getName());
			} else {
				// We have a Table or ConditionalTable

				if (dist instanceof ConditionalTable) {
					dist = ((ConditionalTable) dist).flatten();
				}
				if (variable.isEvidence()) {
					dist = ((Table) dist).setEvidence(discreteDependencies
							.size(), variable.getEvidence());
					if (logger.isDebugEnabled()) {
						evidenceComponents.add(variable);
					}
				} else {
					discreteDependencies.add(variable);
				}
			}
			distribution = dist;

		} else { // We have a continuous variable

			// We cannot incorporate evidence unless there is no tail, so lets
			// check this. A consequence of this is that we need to check when
			// combining factors if there is evidence on one of the head
			// variables and set it if we can. The easiest place to do this is
			// after variable elimination has taken place...there should be no
			// tail dependencies in the final factor
			if (variable.isEvidence() && tailContinuous.size() == 0) {
				// Distribution is a Gaussian or GaussianMix and we can directly
				// incorporate evidence and put it into a probability table,
				// either that or the continuous parents all have evidence so
				// they simplify to one of these distributions

				DoubleMatrix1D evi = DoubleFactory1D.dense.make(1, variable
						.getEvidence());
				DoubleMatrix1D discreteIndex = DoubleFactory1D.dense.make(
						discreteDependencies.size(), 0);
				do {
					dbl.add(dist.getProbability(discreteIndex, evi));
				} while (incrementIndice(discreteIndex, discreteDependencies,
						null));

				final int sz = dbl.size();
				DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(sz);
				for (int i = 0; i < sz; i++) {
					probabilities.setQuick(i, dbl.get(i));
				}
				if (numStates.size() == 0) {
					numStates.add(1);
				}
				if (logger.isDebugEnabled()) {
					evidenceComponents.add(variable);
				}
				distribution = new Table(Utility.convert(numStates),
						probabilities);

			} else {
				// Now we have to mark this factor as having a head and set up
				// the right distribution for this variable. Basically the
				// cases here are:
				// 1) C variable, no parents -> Gaussian
				// 2) C variable, d parents -> GaussianMix
				// 3) C variable, c parents -> LinearGaussian
				// 4) C variable, c,d parents -> LinearGaussianMix
				// If we do have discrete parents, the default LinearGaussianMix
				// has taken them into account as "no action" probabilities
				// right now in the "distributionProbability" model. Since
				// LinearGaussian is just a special case of LinearGaussianMix,
				// we should have no problems incorporating either.
				//
				// All of these are basically LinearGaussianMix, so we should
				// upgrade them all
				headContinuous.add(variable);
				distribution = DistributionFactory.upgradeCPD(dist);
			}

		}
		if (logger.isDebugEnabled()) {
			logger.debug(toString());
		}
	}

	public Factor(Factor copy) throws Exception {
		this.components.addAll(copy.components);
		this.summedComponents.addAll(copy.summedComponents);
		this.evidenceComponents.addAll(copy.evidenceComponents);
		this.discreteDependencies.addAll(copy.discreteDependencies);
		this.headContinuous.addAll(copy.headContinuous);
		this.tailContinuous.addAll(copy.tailContinuous);
		distribution = copy.distribution.copy();
	}

	public Factor(List<TransientVariable> dependencies,
			List<List<Double>> samples) throws Exception {
		this.discreteDependencies.addAll(dependencies);

		List<TransientVariable> continuousDependencies = new ArrayList<TransientVariable>();
		List<TransientVariable> discreteDependencies = new ArrayList<TransientVariable>();
		int numStates = 1;
		List<Integer> states = new ArrayList<Integer>();
		for (TransientVariable tv : dependencies) {
			if (tv.getReference() instanceof DiscreteVariable) {
				discreteDependencies.add(tv);
				int state = ((DiscreteVariable) tv.getReference()).getStates()
						.size();
				numStates *= state;
				states.add(state);
			} else if (tv.getReference() instanceof ContinuousVariable) {
				continuousDependencies.add(tv);
				throw new Exception(
						"Cannot generate factor with continuous dependencies at this time");
			}
		}

		int[] stateLayout = new int[discreteDependencies.size()];
		for (int i = 0; i < states.size(); i++) {
			stateLayout[i] = states.get(i);
		}
		if (stateLayout.length == 0) {
			stateLayout = new int[1];
			stateLayout[0] = 1;
		}

		double[] probabilities = new double[numStates];
		for (List<Double> sample : samples) {
			int index = calculateIndex(sample, stateLayout);
			probabilities[index] += 0.0001;
		}
		UnconditionalDistribution marginal = new Table(probabilities);
		((Table) marginal).normalize();
		this.distribution = marginal;

	}

	public Factor(List<TransientVariable> discreteDependencies,
			List<TransientVariable> headDependencies,
			List<TransientVariable> tailDependencies, List<List<Double>> samples)
			throws Exception {

	}

	protected int calculateIndex(List<Double> sample, int[] layout) {
		if (layout == null || layout.length == 0
				|| sample.size() != layout.length) {
			return -1;
		}
		int i;
		int ind = 0;

		int factor = 1;
		for (i = layout.length - 1; i >= 0; i--)// nD-1
		{
			ind += factor * sample.get(i).intValue();
			factor *= layout[i];
			// ind = (stateLayout[i]) * (ind + (int) currentState[i]);
		}
		return ind;
	}

	public Factor(List<TransientVariable> dependencies,
			UnconditionalDistribution distribution) throws Exception {
		this.discreteDependencies.addAll(dependencies);
		this.distribution = distribution;
	}

	public Factor(List<TransientVariable> discreteDependencies,
			List<TransientVariable> headDependencies,
			List<TransientVariable> tailDependencies,
			ConditionalDistribution distribution) throws Exception {
		this.discreteDependencies.addAll(discreteDependencies);
		this.headContinuous.addAll(headDependencies);
		this.tailContinuous.addAll(tailDependencies);
		this.distribution = distribution;
	}

	private Factor() {
	}

	public Factor(Set<TransientVariable> members) throws Exception {
		for(TransientVariable tv : members)
		{
			if(tv.getReference() instanceof DiscreteVariable)
			{
				discreteDependencies.add(tv);
			}
			else
			{
				//continuous
			}
		}
		int[] stateLayout = new int[discreteDependencies.size()];
		for(int i=0; i<stateLayout.length; i++)
		{
			stateLayout[i] = ((DiscreteVariable)discreteDependencies.get(i).getReference()).getStates().size();
		}
		this.distribution = new Table(stateLayout);
	}

	/**
	 * Increments the values matrix one state forward
	 * 
	 * @param values
	 * @param parents
	 * @param ignore
	 * @return
	 */
	private boolean incrementIndice(DoubleMatrix1D values,
			List<TransientVariable> variables, List<Integer> ignore) {
		for (int i = values.size() - 1; i >= 0; i--) {
			if (ignore != null && ignore.contains(i)) {
				continue;
			}
			values.setQuick(i, values.getQuick(i) + 1.0);
			if (values.getQuick(i) < ((DiscreteVariable) variables.get(i)
					.getReference()).getStates().size()) {
				return true;
			}
			values.setQuick(i, 0);
		}
		return false;
	}

	/**
	 * Calculates the complement of this factor given the marginal of this
	 * factor. We pass in the marginal simply because whenever we use this
	 * function, the marginal has already been calculated. This can be computed
	 * directly with just the set of variables
	 * 
	 * @param marginal
	 * @param set
	 * @return
	 * @throws Exception
	 */
	public Factor complement(Factor marginal, Collection<TransientVariable> set)
			throws Exception {
		List<Tuple> discrete = new ArrayList<Tuple>();
		List<Tuple> continuous = new ArrayList<Tuple>();
		for (int i = 0; i < headContinuous.size(); i++) {
			TransientVariable gv = headContinuous.get(i);
			if (set.contains(gv)) {
				continuous.add(new Tuple(i,
						marginal.headContinuous.indexOf(gv), gv.getId()));
			}
		}
		for (int i = 0; i < discreteDependencies.size(); i++) {
			TransientVariable gv = discreteDependencies.get(i);
			// if (set.contains(gv)) {
			discrete.add(new Tuple(i,
					marginal.discreteDependencies.indexOf(gv), gv.getId()));
			// }

		}

		Factor ret = new Factor();

		ret.distribution = distribution.complement(marginal.getDistribution(),
				discrete, continuous);
		ret.headContinuous.addAll(headContinuous);
		ret.headContinuous.removeAll(set);

		ret.tailContinuous.addAll(tailContinuous);
		ret.tailContinuous.removeAll(set);

		ret.discreteDependencies.addAll(discreteDependencies);
		// ret.discreteDependencies.removeAll(set);

		return ret;
	}

	/**
	 * Computes the combination of two factors
	 * 
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public Factor combine(Factor b) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("----Combine----");
			logger.debug(this);
			logger.debug("----by----");
			logger.debug(b);
		}

		Factor ret = new Factor();

		ret.discreteDependencies.addAll(this.discreteDependencies);
		for (TransientVariable tv : b.discreteDependencies) {
			if (!ret.discreteDependencies.contains(tv)) {
				ret.discreteDependencies.add(tv);
			}
		}
		// Collections.sort(ret.discreteDependencies);

		ret.headContinuous.addAll(this.headContinuous);
		for (TransientVariable tv : b.headContinuous) {
			if (!ret.headContinuous.contains(tv)) {
				ret.headContinuous.add(tv);
			}
		}
		// Collections.sort(ret.headContinuous);

		ret.tailContinuous.addAll(this.tailContinuous);
		for (TransientVariable tv : b.tailContinuous) {
			if (!ret.tailContinuous.contains(tv)) {
				ret.tailContinuous.add(tv);
			}
		}
		// Collections.sort(ret.tailContinuous);

		ret.components.addAll(this.components);
		ret.components.addAll(b.components);

		if (logger.isDebugEnabled()) {

			ret.summedComponents.addAll(this.summedComponents);
			ret.summedComponents.addAll(b.summedComponents);

			ret.evidenceComponents.addAll(this.evidenceComponents);
			ret.evidenceComponents.addAll(b.evidenceComponents);
		}

		List<Quadruple> discreteIndexList = new ArrayList<Quadruple>();
		for (TransientVariable tv : ret.discreteDependencies) {
			int rind = ret.discreteDependencies.indexOf(tv);
			int aind = discreteDependencies.indexOf(tv);
			int bind = b.discreteDependencies.indexOf(tv);
			discreteIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		List<Quadruple> headIndexList = new ArrayList<Quadruple>();
		for (TransientVariable tv : ret.headContinuous) {
			int rind = ret.headContinuous.indexOf(tv);
			int aind = headContinuous.indexOf(tv);
			int bind = b.headContinuous.indexOf(tv);
			headIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		List<Quadruple> tailIndexList = new ArrayList<Quadruple>();
		for (TransientVariable tv : ret.tailContinuous) {
			int rind = ret.tailContinuous.indexOf(tv);
			int aind = tailContinuous.indexOf(tv);
			int bind = b.tailContinuous.indexOf(tv);
			tailIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		ret.distribution = Operation.combine(this.distribution, b.distribution,
				discreteIndexList, headIndexList, tailIndexList);
		ret.tailContinuous.removeAll(ret.headContinuous);
		if (ret.tailContinuous.isEmpty()) {
			List<TransientVariable> headCopy = new ArrayList<TransientVariable>(
					ret.headContinuous);
			for (TransientVariable tv : headCopy) {
				if (tv.isEvidence()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Adding continuous evidence...");
						logger.debug(ret);
					}
					// Incorporate evidence from continuous dependencies in
					// head.
					ret.distribution = ret.distribution.setEvidence(
							ret.headContinuous.indexOf(tv), tv.getEvidence());
					ret.headContinuous.remove(tv);
					if (logger.isDebugEnabled()) {
						ret.evidenceComponents.add(tv);
					}
				}
			}
			List<TransientVariable> discreteCopy = new ArrayList<TransientVariable>(
					ret.discreteDependencies);
			for (TransientVariable tv : discreteCopy) {

				if (tv.isHidden() && components.contains(tv)) {

					if (logger.isDebugEnabled()) {
						logger.debug("Marginalizing out variable...");
						logger.debug(ret);
					}
					// Incorporate evidence from continuous dependencies in
					// head.
					ret.distribution = ret.distribution.marginalize(
							ret.discreteDependencies.indexOf(tv), true);
					ret.discreteDependencies.remove(tv);
					if (logger.isDebugEnabled()) {
						ret.summedComponents.add(tv);
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("--------Combine equals--------");
			logger.debug(ret);
		}
		return ret;
	}

	/**
	 * Computes the combination of two factors
	 * 
	 * @param a
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public static Factor combine(Factor a, Factor b) throws Exception {
		return a.combine(b);
	}

	/**
	 * Sums out all variables not in set.
	 * 
	 * @param set
	 * @return
	 * @throws Exception
	 */
	public Factor marginalize(Collection<TransientVariable> set)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("----marginalize----");
			logger.debug(this);
			logger.debug("----by----");
			logger.debug(set);
			logger.debug("-----------------");
		}
		// if(!dependencies.containsAll(set)) throw new
		// Exception("Cannot project " + this + " onto " + set);
		Factor ret = new Factor(this);
		for (TransientVariable gv : headContinuous) {
			if (!set.contains(gv)) {
				ret.marginalizeOut(gv);
			}
		}
		for (TransientVariable gv : discreteDependencies) {
			if (!set.contains(gv)) {
				ret.marginalizeOut(gv);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("------Marginalization equals--------");
			logger.debug(ret);
		}
		return ret;
	}

	/**
	 * Sums out a dependency in this factor
	 * 
	 * @param variable
	 */
	public void marginalizeOut(TransientVariable variable) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("----marginalizeOut----");
			logger.debug(variable);
		}
		if (!dependsOn(variable)) {
			return;
		}

		if (tailContinuous.size() > 0) {
			// We will have to deal with this later
			return;
		}

		int index;
		if ((index = headContinuous.indexOf(variable)) >= 0) {
			distribution = distribution.marginalize(index, false);
			headContinuous.remove(variable);
		} else {
			distribution = distribution.marginalize(discreteDependencies
					.indexOf(variable), true);
			discreteDependencies.remove(variable);
		}

		if (logger.isDebugEnabled()) {
			summedComponents.add(variable);
			logger.debug(toString());
		}
	}

	public boolean dependsOn(TransientVariable variable) {
		return discreteDependencies.contains(variable)
				|| headContinuous.contains(variable)
				|| tailContinuous.contains(variable);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nDependencies:\t[");
		for (TransientVariable tv : discreteDependencies) {
			sb.append(" ");
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getTime());
			sb.append(", ");
		}
		sb.append("]\nhead:\t\t[");
		for (TransientVariable tv : headContinuous) {
			sb.append(" ");
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getTime());
			sb.append(", ");
		}
		sb.append("]\ntail:\t\t[");
		for (TransientVariable tv : tailContinuous) {
			sb.append(" ");
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getTime());
			sb.append(", ");
		}
		sb.append("]\n");
		if (logger.isDebugEnabled()) {
			sb.append("Components:\t[");
			for (TransientVariable tv : components) {
				sb.append(" ");
				sb.append(tv.getReference().getName());
				sb.append("__");
				sb.append(tv.getTime());
				sb.append(", ");
			}
			sb.append("]\nSummed Out:\t[");
			for (TransientVariable tv : summedComponents) {
				sb.append(" ");
				sb.append(tv.getReference().getName());
				sb.append("__");
				sb.append(tv.getTime());
				sb.append(", ");
			}
			sb.append("]\nEvidenced:\t[");
			for (TransientVariable tv : evidenceComponents) {
				sb.append(" ");
				sb.append(tv.getReference().getName());
				sb.append("__");
				sb.append(tv.getTime());
				sb.append(", ");
			}
			sb.append("]\n");
		}
		try {
			if (distribution instanceof Mixture) {
				sb.append("discrete part:\n");
				sb.append(((Mixture) distribution)
						.getDensityProbabilityDistribution()
						.getXMLDescription());
				sb.append("continuous part:\n");
			}
			sb.append(distribution.getXMLDescription());
		} catch (Exception ex) {
			logger.debug(ex, ex);
		}

		return sb.toString();
	}

	public ConditionalDistribution getDistribution() {
		return this.distribution;
	}

	/**
	 * @return all the discrete dependencies in this factor.
	 */
	public List<TransientVariable> getDependencies() {
		return discreteDependencies;
	}

	/**
	 * @return the head continuous dependencies in this factor, these are
	 *         generally what the distribution represents
	 */
	public List<TransientVariable> getHeadDependencies() {
		return headContinuous;
	}

	/**
	 * 
	 * @return the tail continuous dependencies in this factor, these are
	 *         generally what the distribution is conditioned on.
	 */
	public List<TransientVariable> getTailDependencies() {
		return tailContinuous;
	}

	public void normalize() {
		if (this.distribution instanceof Table) {
			((Table) this.distribution).normalize();
		}
		if(this.distribution instanceof Mixture) {
			try {
				((Table)((Mixture)this.distribution).getDensityProbabilityDistribution()).normalize();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

	public void setEvidence(TransientVariable tv)
			throws Exception {
		if (tv.getReference() instanceof DiscreteVariable) {
			int evidence = tv.getEvidence().intValue();
			Table table = (Table) distribution;
			int[] indices = new int[table.getNumberDimensions()];
			int[] stateLayout = table.getStateLayout();
			int index = discreteDependencies.indexOf(tv);

			do {
				if (indices[index] != evidence) // 0 out everything not
				// consistent with evidence
				{
					table.setProbability(indices, 0.0);
				}
			} while (Utility.incrementIndice(indices, stateLayout));
		}

	}

	public void setDistribution(ConditionalDistribution distribution) {
		this.distribution = distribution;
	}

}
