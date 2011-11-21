package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.Operation;
import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import selrach.bnetbuilder.model.distributions.Operation.Tuple;
import selrach.bnetbuilder.model.distributions.conditional.ConditionalTable;
import selrach.bnetbuilder.model.distributions.conditional.Mixture;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Manages a distribution over a Clique or CliqueSeparator. This is used as a
 * starting point for the factors generated when actually constructing a
 * JunctionTree from the Cliques
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Potential {

	// This is almost a mirror of the factor class except that it uses graph
	// variables

	private final static Logger logger = Logger.getLogger(Potential.class);

	private final List<GraphVariable> allMembers = new ArrayList<GraphVariable>();

	private final List<GraphVariable> discreteDependencies = new ArrayList<GraphVariable>();
	// private final List<GraphVariable> continuousDependencies = new
	// ArrayList<GraphVariable>();

	/**
	 * These are the head dependencies in this distribution, basically a subset
	 * of the continuous variables that are affecting this distribution
	 */
	private final List<GraphVariable> headContinuous = new ArrayList<GraphVariable>();

	/**
	 * These are the tail dependencies in this distribution, basically a subset
	 * of the continuous variables affecting this distribution.
	 */
	private final List<GraphVariable> tailContinuous = new ArrayList<GraphVariable>();

	/**
	 * 
	 */
	private final List<GraphVariable> allContinuousDependencies = new ArrayList<GraphVariable>();

	ConditionalDistribution distribution = null;

	public Potential(Collection<GraphVariable> members) throws Exception {

		int totalNumStates = 1;
		List<Integer> numStates = new ArrayList<Integer>();
		int i;
		for (GraphVariable gv : members) {
			RandomVariable rv = gv.getReference();
			if (rv instanceof DiscreteVariable) {
				discreteDependencies.add(gv);
				i = ((DiscreteVariable) gv.getReference()).getStates().size();
				totalNumStates *= i;
				numStates.add(i);
			} else {
				allContinuousDependencies.add(gv);
			}
		}
		int numDimensions = numStates.size();

		int[] stateLayout = new int[numDimensions];
		for (i = 0; i < numStates.size(); i++) {
			stateLayout[i] = numStates.get(i);
		}
		if (stateLayout.length == 0) {
			stateLayout = new int[1];
			stateLayout[0] = 1;
		}
		if (allContinuousDependencies.size() == 0) {
			distribution = new Table(stateLayout, DoubleFactory1D.dense.make(
					totalNumStates, 1.0));
		} else {
			// probably just a table will be okay for here too, when we actually
			// multiply the continuous into this distribution we can figure out
			// if it is a head or tail.
			distribution = new Table(stateLayout, DoubleFactory1D.dense.make(
					totalNumStates, 1.0));
		}
	}

	public Potential(GraphVariable variable) throws Exception {
		// We can construct the factor by investigating the variable

		List<GraphVariable> pCopy = variable.getParents();
		List<RandomVariable> rvParents = variable.getReference().getParents(
				variable.getCpdSlice());

		// Sets up some proper bookkeeping, making sure that the order in this
		// clique matches that of the parent distribution
		for (RandomVariable rv : rvParents) {
			for (int i = 0; i < pCopy.size(); i++) {
				if (pCopy.get(i).getReference() == rv) {
					GraphVariable parent = pCopy.get(i);
					if (parent.getReference() instanceof ContinuousVariable) {
						tailContinuous.add(parent);
					} else {
						discreteDependencies.add(parent);
					}
					pCopy.remove(i);
					break;
				}
			}
		}

		if (variable.getReference() instanceof DiscreteVariable) {
			DiscreteVariable dv = (DiscreteVariable) variable.getReference();
			if (tailContinuous.size() > 0) {
				throw new Exception(
						"We cannot handle continuous parents on discrete variables right now.");
			}
			if (variable.getSlice() == 0)
			// We include it in our initial potential
			{
				this.distribution = dv.getCpd(variable.getCpdSlice()).copy();
				if (distribution instanceof ConditionalTable) {
					distribution = ((ConditionalTable) distribution).flatten();
				}

				discreteDependencies.add(variable);

			} else
			// It has already been accounted for in a previous slice,
			// just allocate an initial distribution.
			{
				int[] stateLayout = new int[1];
				stateLayout = new int[1];
				stateLayout[0] = dv.getStates().size();
				distribution = new Table(stateLayout, DoubleFactory1D.dense
						.make(stateLayout[0], 1.0));
				discreteDependencies.clear();
				discreteDependencies.add(variable);
			}
		} else // Continuous Variable
		{
			if (variable.getSlice() == 0) {
				headContinuous.add(variable);
				distribution = DistributionFactory.upgradeCPD(variable
						.getReference().getCpd(variable.getCpdSlice()));
			} else {
				tailContinuous.clear();
				distribution = new Table();
			}
		}
	}

	public Potential(CliqueSeparator cliqueSeparator) throws Exception {
		int i = 0;
		List<Integer> numStates = new ArrayList<Integer>();
		for (GraphVariable gv : cliqueSeparator.getMembers()) {
			if (gv.getReference() instanceof DiscreteVariable) {
				numStates.add(((DiscreteVariable) gv.getReference())
						.getStates().size());
				this.discreteDependencies.add(gv);
			} else {
				this.tailContinuous.add(gv);
			}
		}
		int[] stateLayout = new int[numStates.size()];
		int totalStates = 1;
		for (i = 0; i < stateLayout.length; i++) {
			totalStates *= stateLayout[i] = numStates.get(i);
		}
		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(totalStates,
				1.0);
		distribution = new Table(stateLayout, probabilities);
	}

	public Potential(Potential copy) throws Exception {
		this.discreteDependencies.addAll(copy.discreteDependencies);
		this.headContinuous.addAll(copy.headContinuous);
		this.tailContinuous.addAll(copy.tailContinuous);
		distribution = copy.getDistribution().copy();
	}

	public Potential() throws Exception {
		this.distribution = new Table();
	}
	

	public Potential combine(Potential p) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("\n----Combine----");
			logger.debug(this.toDetailedString("\t"));
			logger.debug("----by----");
			logger.debug(p.toDetailedString("\t"));
		}

		Potential ret = new Potential();

		ret.discreteDependencies.addAll(this.discreteDependencies);
		for (GraphVariable tv : p.discreteDependencies) {
			if (!ret.discreteDependencies.contains(tv)) {
				ret.discreteDependencies.add(tv);
			}
		}
		// Collections.sort(ret.discreteDependencies);

		ret.headContinuous.addAll(this.headContinuous);
		for (GraphVariable tv : p.headContinuous) {
			if (!ret.headContinuous.contains(tv)) {
				ret.headContinuous.add(tv);
			}
		}
		// Collections.sort(ret.headContinuous);

		ret.tailContinuous.addAll(this.tailContinuous);
		for (GraphVariable tv : p.tailContinuous) {
			if (!ret.tailContinuous.contains(tv)) {
				ret.tailContinuous.add(tv);
			}
		}
		// Collections.sort(ret.tailContinuous);

		List<Quadruple> discreteIndexList = new ArrayList<Quadruple>();
		for (GraphVariable tv : ret.discreteDependencies) {
			int rind = ret.discreteDependencies.indexOf(tv);
			int aind = discreteDependencies.indexOf(tv);
			int bind = p.discreteDependencies.indexOf(tv);
			discreteIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		List<Quadruple> headIndexList = new ArrayList<Quadruple>();
		for (GraphVariable tv : ret.headContinuous) {
			int rind = ret.headContinuous.indexOf(tv);
			int aind = headContinuous.indexOf(tv);
			int bind = p.headContinuous.indexOf(tv);
			headIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		List<Quadruple> tailIndexList = new ArrayList<Quadruple>();
		for (GraphVariable tv : ret.tailContinuous) {
			int rind = ret.tailContinuous.indexOf(tv);
			int aind = tailContinuous.indexOf(tv);
			int bind = p.tailContinuous.indexOf(tv);
			tailIndexList.add(new Quadruple(rind, aind, bind, tv.getId()));
		}

		ret.distribution = Operation.combine(this.distribution, p.distribution,
				discreteIndexList, headIndexList, tailIndexList);

		ret.tailContinuous.removeAll(ret.headContinuous);

		if (logger.isDebugEnabled()) {
			logger.debug("----equals----");
			logger.debug(ret);
		}
		return ret;
	}

	public static Potential combine(Potential a, Potential b) throws Exception {
		return a.combine(b);
	}

	public Potential complement(Potential marginal,
			Collection<GraphVariable> set) throws Exception {
		List<Tuple> discrete = new ArrayList<Tuple>();
		List<Tuple> continuous = new ArrayList<Tuple>();
		for (int i = 0; i < headContinuous.size(); i++) {
			GraphVariable gv = headContinuous.get(i);
			if (set.contains(gv)) {
				continuous.add(new Tuple(i,
						marginal.headContinuous.indexOf(gv), gv.getId()));
			}
		}
		for (int i = 0; i < discreteDependencies.size(); i++) {
			GraphVariable gv = discreteDependencies.get(i);
			// if (set.contains(gv)) {
			discrete.add(new Tuple(i,
					marginal.discreteDependencies.indexOf(gv), gv.getId()));
			// }

		}

		Potential ret = new Potential();

		ret.distribution = distribution.complement(marginal.getDistribution(),
				discrete, continuous);
		ret.headContinuous.addAll(headContinuous);
		ret.headContinuous.removeAll(set);

		ret.tailContinuous.addAll(tailContinuous);
		ret.tailContinuous.removeAll(set);

		ret.discreteDependencies.addAll(discreteDependencies);
		// ret.discreteDependencies.removeAll(set);

		ret.allMembers.addAll(allMembers);
		ret.allMembers.removeAll(set);

		ret.allContinuousDependencies.addAll(allContinuousDependencies);
		ret.allContinuousDependencies.removeAll(set);

		return ret;
	}

	public Potential marginalize(Collection<GraphVariable> set)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("----marinalize----");
			logger.debug(this.toDetailedString("\t"));
			logger.debug("----by----");
			logger.debug(set);
			logger.debug("-----------------");
		}
		// if(!dependencies.containsAll(set)) throw new
		// Exception("Cannot project " + this + " onto " + set);
		Potential ret = new Potential(this);
		for (GraphVariable gv : headContinuous) {
			if (!set.contains(gv)) {
				ret.marginalizeOut(gv);
			}
		}
		for (GraphVariable gv : discreteDependencies) {
			if (!set.contains(gv)) {
				ret.marginalizeOut(gv);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("------equals--------");
			logger.debug(ret);
		}
		return ret;
	}

	/**
	 * Sums out a dependency in this factor
	 * 
	 * @param variable
	 */
	public void marginalizeOut(GraphVariable variable) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("----marginalizeOut----");
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
			logger.debug(toString());
		}
	}

	public ConditionalDistribution getDistribution() {
		return distribution;
	}

	public void setEvidence(GraphVariable gv, TransientVariable tv)
			throws Exception {
		if (gv.getReference() != tv.getReference()) {
			throw new Exception("References are not equal");
		}
		if (gv.getReference() instanceof DiscreteVariable) {
			int evidence = tv.getEvidence().intValue();
			Table table = (Table) distribution;
			int[] indices = new int[table.getNumberDimensions()];
			int[] stateLayout = table.getStateLayout();
			int index = discreteDependencies.indexOf(gv);

			do {
				if (indices[index] != evidence) // 0 out everything not
				// consistent with evidence
				{
					table.setProbability(indices, 0.0);
				}
			} while (Utility.incrementIndice(indices, stateLayout));
		}

	}

	public void setDistribution(ConditionalDistribution conditionalDistribution) {
		this.distribution = conditionalDistribution;
	}

	@Override
	public String toString() {
		return toDetailedString("");
	}

	public String toDetailedString(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append("Dependencies:\t[");
		for (GraphVariable tv : discreteDependencies) {
			sb.append(' ');
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getSlice());
			sb.append(", ");
		}
		sb.append("]\nhead:\t\t[");
		for (GraphVariable tv : headContinuous) {
			sb.append(" ");
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getSlice());
			sb.append(", ");
		}
		sb.append("]\ntail:\t\t[");
		for (GraphVariable tv : tailContinuous) {
			sb.append(" ");
			sb.append(tv.getReference().getName());
			sb.append("__");
			sb.append(tv.getSlice());
			sb.append(", ");
		}
		sb.append("]\n");
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
		}

		return sb.toString();
	}

	/**
	 * @return the dependencies
	 */
	public List<GraphVariable> getDependencies() {
		return discreteDependencies;
	}

	public List<GraphVariable> getHeadDependencies() {
		return headContinuous;
	}

	public List<GraphVariable> getTailDependencies() {
		return tailContinuous;
	}

}
