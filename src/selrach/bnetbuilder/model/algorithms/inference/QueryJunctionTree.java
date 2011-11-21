package selrach.bnetbuilder.model.algorithms.inference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.exceptions.QueryVariableNotSetException;
import selrach.bnetbuilder.model.algorithms.graph.GenerateEliminationCliques;
import selrach.bnetbuilder.model.algorithms.graph.GenerateJunctionTreeFromCliques;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.JunctionTree;
import selrach.bnetbuilder.model.variable.TransientClique;
import selrach.bnetbuilder.model.variable.TransientCliqueSeparator;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * This assumes that we have already calibrated the junction tree based off of
 * the evidence set in the model
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class QueryJunctionTree implements InferenceAlgorithm {

	private static final Logger logger = Logger
			.getLogger(QueryJunctionTree.class);

	private QueryJunctionTree() {
	}

	private static final QueryJunctionTree instance = new QueryJunctionTree();

	public static QueryJunctionTree getInstance() {
		return instance;
	}

	/**
	 * Run just the query set in the model
	 * 
	 * @param model
	 */
	public Factor execute(DynamicBayesNetModel model, PrintStream updateTracking)
			throws Exception {
		return execute(model, false, updateTracking);
	}

	/**
	 * Run either the query set in the model or each variable independently
	 * 
	 * @param model
	 * @param allMarginals
	 *            Flag to say if we want to calculate each non-evidence variable
	 *            independently or the query set in the model
	 * @return The potential generated by the query, or null if we were
	 *         calculating all independent marginals
	 */
	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			PrintStream updateTracking) throws Exception {
		for (int i = 0; i < model.getMaxNumberSlices(); i++) 
			// Run this for each timeslice
		{
			model.getSlice(i);
		}
		
		if (!model.getJunctionTreeTemplate().isCalibrated()) {
			CalibrateJunctionTreeSerial.getInstance().execute(model,
					updateTracking);
		}
		if (allMarginals) {
			setIndependentMarginals(model);
			return null;
		} else {
			return processQueryFromModel(model);
		}
	}

	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			Map<String, Object> additionalProperties, PrintStream updateTracking)
			throws Exception {
		return execute(model, allMarginals, updateTracking);
	}

	private void doMarginals(DynamicBayesNetModel model,
			TransientClique clique, int time) throws Exception {

		int i = 0;
		i++;
		for (TransientVariable tv : clique.getMembers().values()) {
			if (!tv.isEvidence() && tv.getMarginal() == null) {
				Factor complementFactor = clique.getFactor();
				if (clique.getParentSeparator() != null) {
					Factor weakMarginal = clique.getParentSeparator()
							.getFactor();
					complementFactor = weakMarginal.combine(complementFactor);
				}
				UnconditionalDistribution marginal = DistributionFactory
						.downgradeCPD(complementFactor.marginalize(
								Collections.singleton(tv)).getDistribution());
				if (marginal instanceof Table) {
					((Table) marginal).normalize();
				}
				tv.setMarginal(marginal);
			}
		}

		for (TransientCliqueSeparator tcs : clique.getSeparators()) {
			TransientClique child;
			if (tcs.getCliqueA() == clique) {
				child = tcs.getCliqueB();
			} else {
				child = tcs.getCliqueA();
			}
			if (child.getParent() == null) {
				child.setParent(clique);

				doMarginals(model, child, time);
			}
		}
	}

	private void setIndependentMarginals(DynamicBayesNetModel model)
			throws Exception {
		model.clearTransientMarginals();
		model.storeState();
		for (int i = 0; i < model.getMaxNumberSlices(); i++) {
			model.getSlice(i); // make sure that the slice is generated
			// correctly
			JunctionTree jt = model.getJunctionTreeTemplate()
					.getJunctionTreeSlices().get(i);

			jt.clearParents();

			TransientClique root = jt.getRoot();
			root.setParent(root);
			doMarginals(model, root, i);
		}
		model.restoreState();
	}

	private Factor processQueryFromModel(DynamicBayesNetModel model)
			throws Exception {
		JunctionTree jt = model.getJunctionTreeTemplate()
				.getJunctionTreeSlices().get(0);
		jt.clearParents();
		TransientClique base = jt.getRoot();
		Set<TransientVariable> overallQueryVariables = new HashSet<TransientVariable>();

		Map<String, TransientVariable> queryVariables = new HashMap<String, TransientVariable>();
		Set<TransientVariable> childQueryVariables = new HashSet<TransientVariable>();

		for (TransientVariable tv : base.getMembers().values()) {
			if (tv.isQuery()) {
				queryVariables.put(tv.getId(), tv);
			}
		}

		base.setParent(base); // Mark us as strong root

		TransientClique subtreeRoot = markMinimalSubtree(model, 1, base,
				queryVariables, childQueryVariables, overallQueryVariables);
		//jt.clearParents();
		if (subtreeRoot == null) {
			throw new QueryVariableNotSetException();
		}
		//subtreeRoot.setParent(subtreeRoot); // Mark as root

		queryVariables.clear();
		for (TransientVariable tv : subtreeRoot.getMembers().values()) {
			if (tv.isQuery()) {
				queryVariables.put(tv.getId(), tv);
			}
		}

		Map<String, TransientVariable> descendentQueryVariables = new HashMap<String, TransientVariable>();

		pushQueryToSubroot(model, 1, subtreeRoot, descendentQueryVariables);
		// subtreeRoot contains query

		descendentQueryVariables.putAll(queryVariables);
		TransientClique parent = null;
		while (subtreeRoot.getFactor().getTailDependencies().size() > 0) {
			TransientCliqueSeparator tcs = subtreeRoot.getParentSeparator();
			if (tcs.getCliqueA() == subtreeRoot) {
				parent = tcs.getCliqueB();
			} else {
				parent = tcs.getCliqueA();
			}
			JunctionTree.push(subtreeRoot, parent, descendentQueryVariables);
			subtreeRoot = parent;
		}
		Factor ret = subtreeRoot.getFactor();
		if (subtreeRoot.getParentSeparator() != null) {
			ret = ret.combine(subtreeRoot.getParentSeparator().getFactor());
		}
		ret = ret.marginalize(descendentQueryVariables.values());

		ConditionalDistribution cd = ret.getDistribution();
		if (cd instanceof Table) {
			((Table) cd).normalize();
		}

		return ret;
	}

	private void pushQueryToSubroot(DynamicBayesNetModel model, int time,
			TransientClique parent,
			Map<String, TransientVariable> queryVariables) throws Exception {

		TransientClique child = null;

		for (TransientCliqueSeparator tcs : parent.getSeparators()) {
			if (tcs.getCliqueA() == parent) {
				child = tcs.getCliqueB();
			} else {
				child = tcs.getCliqueA();
			}
			if (parent.getParent() != child) {
				child.setParent(parent);
				if (tcs.isCut()) {
					continue;
				}
				Map<String, TransientVariable> childQueryVariables = new HashMap<String, TransientVariable>();
				for (TransientVariable tv : child.getMembers().values()) {
					if (tv.isQuery()) {
						childQueryVariables.put(tv.getId(), tv);
					}
				}
				
				pushQueryToSubroot(model, time, child, childQueryVariables);
				
				queryVariables.putAll(childQueryVariables);

				JunctionTree.push(child, parent, queryVariables);
			}
		}

	}

	private TransientClique markMinimalSubtree(DynamicBayesNetModel model,
			int time, TransientClique parent,
			Map<String, TransientVariable> parentQueryVariables,
			Set<TransientVariable> descendentQueryVariables,
			Set<TransientVariable> overallQueryVariables) {

		TransientClique child = null;
		TransientClique tmpRoot1 = null;
		TransientClique tmpRoot2 = null;

		for (TransientCliqueSeparator tcs : parent.getSeparators()) {
			if (tcs.getCliqueA() == parent) {
				child = tcs.getCliqueB();
			} else {
				child = tcs.getCliqueA();
			}
			if (child.getParent() == null) {
				tcs.setCut(false);
				child.setParent(parent);
				child.setParentSeparator(tcs);

				Set<TransientVariable> dqv = new HashSet<TransientVariable>();

				Map<String, TransientVariable> myQueryVariables = new HashMap<String, TransientVariable>();
				for (TransientVariable tv : child.getMembers().values()) {
					if (tv.isQuery()) {
						myQueryVariables.put(tv.getId(), tv);
						overallQueryVariables.add(tv);
					}
				}

				if (child.getReference().isForwardInterface()
						&& child.getReference().getForwardInterface() == 0) {
					if (time < model.getMaxNumberSlices())
					// We need to go to the future
					{
						JunctionTree future = model.getJunctionTreeTemplate()
								.getJunctionTreeSlices().get(time);
						future.clearParents();
						TransientClique futureRoot = future.getRoot();
						futureRoot.setParent(child);
						tmpRoot1 = markMinimalSubtree(model, time + 1,
								futureRoot, myQueryVariables, dqv,
								overallQueryVariables);
					}
				} else {
					tmpRoot1 = markMinimalSubtree(model, time, child,
							myQueryVariables, dqv, overallQueryVariables);
				}
				if (tmpRoot1 != null) {
					if (tmpRoot2 == null) { // One child clique means that we
						// potentially can trim out ancestors
						tmpRoot2 = tmpRoot1;
					} else { // Two children mean parent must be root of subtree
						tmpRoot2 = parent;
					}
				}

				boolean allChildrenVariablesInParent = true;
				for (TransientVariable gv : dqv) {
					if (!parentQueryVariables.containsKey(gv.getId())) {
						allChildrenVariablesInParent = false;
						break;
					}
				}
				descendentQueryVariables.addAll(dqv);

				if (allChildrenVariablesInParent) {
					tcs.setCut(true);
				}
			}
		}
		descendentQueryVariables.addAll(parentQueryVariables.values());
		if (parentQueryVariables.size() > 0) { // If we have query variables
			tmpRoot2 = parent;
		}
		return tmpRoot2;
	}

	public String getName() {
		return "Junction Tree";
	}

	public boolean isRandom() {
		return false;
	}

	public Factor getFactorForSufficientStatistics(DynamicBayesNetModel model,
			TransientVariable variable, Map<String, Object> additionalProperties)
			throws Exception {
		if (!model.getJunctionTreeTemplate().isCalibrated()) {
			CalibrateJunctionTreeSerial.getInstance().execute(model, null);
		}

		int time = variable.getTime();
		List<TransientVariable> needVars = new ArrayList<TransientVariable>(
				variable.getParents());
		needVars.add(variable);

		JunctionTree jt = model.getJunctionTreeTemplate()
				.getJunctionTreeSlices().get(time);

		List<TransientClique> cliques = jt.getCliques();
		for (TransientClique clique : cliques) {
			Map<String, TransientVariable> members = clique.getMembers();
			List<TransientVariable> tvMembers = new ArrayList<TransientVariable>();

			for (TransientVariable tv : members.values()) {
				if (needVars.contains(tv)) {
					tvMembers.add(tv);
				}
			}
			if (tvMembers.containsAll(needVars)) {
				return clique.getFactor().marginalize(tvMembers);
			}
		}
		throw new Exception(
				"Did not find a clique with correct parents for transient variable");
	}

	@Override
	public void evidenceSet(DynamicBayesNetModel model) {
		model.getJunctionTreeTemplate().setCalibratedFalse();
	}

	@Override
	public void parameterLearningDone(DynamicBayesNetModel model) {
		if (!model.getJunctionTreeTemplate().isStale()) {
			GenerateEliminationCliques.execute(model.getJunctionTreeTemplate());
			GenerateJunctionTreeFromCliques.execute(model
					.getJunctionTreeTemplate());
		}
	}
}
