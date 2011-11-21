package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import selrach.bnetbuilder.model.BayesNetSlice;

/**
 * Represents a junction tree slice in the dynamic network. Each JunctionTree is
 * an instantiation of the original JunctionTree specified by the template graph
 * variables.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class JunctionTree {

	private final int slice;
	private final List<TransientClique> interfaces;
	private final Map<Clique, TransientClique> cliques = new HashMap<Clique, TransientClique>();
	private final Map<CliqueSeparator, TransientCliqueSeparator> cliqueSeparators = new HashMap<CliqueSeparator, TransientCliqueSeparator>();

	public JunctionTree(List<Clique> cliques,
			List<CliqueSeparator> cliqueSeparators, int slice) throws Exception {
		this.slice = slice;
		TreeMap<Integer, TransientClique> interfaceMap = new TreeMap<Integer, TransientClique>();
		for (Clique c : cliques) {
			TransientClique tc = new TransientClique(slice, c);
			this.cliques.put(c, tc);
			if (c.isForwardInterface()) {
				interfaceMap.put(c.getForwardInterface(), tc);
			}
		}
		interfaces = new ArrayList<TransientClique>(interfaceMap.values());
		for (CliqueSeparator cs : cliqueSeparators) {
			this.cliqueSeparators.put(cs, new TransientCliqueSeparator(slice,
					cs, this.cliques.get(cs.getCliqueA()), this.cliques.get(cs
							.getCliqueB())));
		}
		for (TransientClique tc : this.cliques.values()) {
			tc.setupConnections(this.cliques, this.cliqueSeparators);
		}
	}

	public List<TransientClique> getCliques() {
		return Collections.unmodifiableList(new ArrayList<TransientClique>(
				this.cliques.values()));
	}

	public List<TransientCliqueSeparator> getCliqueSeparators() {
		return Collections
				.unmodifiableList(new ArrayList<TransientCliqueSeparator>(
						this.cliqueSeparators.values()));
	}

	public int getSlice() {
		return slice;
	}

	public void setEvidence(BayesNetSlice bnslice) throws Exception {
		List<TransientVariable> variables = bnslice.getVariables();
		for (TransientVariable variable : variables) {
			if (variable.isEvidence() && slice == variable.getTime()) {
				String key = variable.getId();
				for (TransientClique clique : cliques.values()) {
					if (clique.getMembers().containsKey(key)) {
						clique.setEvidence(variable);
					}
				}
			}
		}
	}

	/**
	 * This pushes the potential's distribution into a higher clique, useful if
	 * we need the condition that there can be no continuous "tail" variables in
	 * a distribution, if we push enough times we end up in the strong root and
	 * by definition it has no "tail" variables
	 * 
	 * @param child
	 *            the child clique, who we are pushing from
	 * @param parent
	 *            the parent clique, who we are pushing to
	 * @param pushVariables
	 *            the set of variables we are pushing from U to W, must be a
	 *            subset of U
	 * @throws Exception
	 */
	static public void push(TransientClique child, TransientClique parent,
			Map<String, TransientVariable> pushVariables) throws Exception {
		/*
		 * Acts on group of variables M contained in clique W with neighbor U
		 * towards root Separator S = U \\intersect W \\gamma_W =
		 * (\\gamma_W)^{\\downarrow M\\union S} \\ox (\\gamma_W)^{\\vertline
		 * \\union S} clique U extended to U^ = U \\union M, S^ = S \\union M
		 * \\gamma_{U^} = \\gamma_U \\ox (\\gamma_{W})^{\\downarrow M\\union S}
		 * \\gamma_{S^} = \\gamma_S \\ox (\\gamma_W)^{\\downarrow M\\union S}
		 * \\gamma_{W} = (\\gamma_W)^{\\vertline M\\union S}
		 */

		TransientCliqueSeparator S = null;
		for (TransientCliqueSeparator tcs : child.getSeparators()) {
			if (tcs.getCliqueA() == parent || tcs.getCliqueB() == parent) {
				S = tcs;
				break;
			}
		}

		Set<TransientVariable> MunionS = new HashSet<TransientVariable>(pushVariables
				.values());
		MunionS.addAll(S.getMembers());

		Factor Wmarg = child.getFactor().marginalize(MunionS);
		Factor Wcomp = child.getFactor().complement(Wmarg, MunionS);

		parent.getMembers().putAll(pushVariables);
		S.getMembers().addAll(pushVariables.values());

		parent.setFactor(parent.getFactor().combine(Wmarg));
		S.setFactor(S.getFactor().combine(Wmarg));
		child.setFactor(Wcomp);

	}

	@Override
	public String toString() {
		return toDetailedString("");
	}

	public String toDetailedString(String prepend) {
		StringBuilder sb = new StringBuilder();

		for (TransientClique tc : cliques.values()) {
			sb.append("\n");
			sb.append(tc.toDetailedString(prepend + "\t"));
		}

		return sb.toString();

	}

	public void clearParents() {
		for (TransientClique tc : cliques.values()) {
			tc.setParent(null);
		}
	}

	public void resetSeparators() {
		for (TransientCliqueSeparator tcs : cliqueSeparators.values()) {
			tcs.resetPotential();
		}
	}

	/**
	 * @return the futureInterface
	 */
	public TransientClique getForwardInterface(int slice) {
		return interfaces.get(slice);
	}

	TransientClique rootCache = null;

	public TransientClique getRoot() {
		if (rootCache != null) {
			return rootCache;
		}
		if (interfaces.size() <= 1) {
			Iterator<TransientClique> iter = cliques.values().iterator();
			while (iter.hasNext()) {
				TransientClique c = iter.next();
				if (!interfaces.contains(c)) {
					// Need to pick a strong root.
					if (isStrongRoot(c)) {
						clearParents();
						return rootCache = c;
					}
				}
			}
		}
		return rootCache = interfaces.get(interfaces.size() - 1); // Pick the
		// interface furthest in the past
	}

	private boolean isStrongRoot(TransientClique c) {
		clearParents();
		boolean leafMustBeContinuous = false;
		for (TransientVariable tv : c.getMembers().values()) {
			if (tv.getReference() instanceof ContinuousVariable) {
				leafMustBeContinuous = true;
				break;
			}
		}
		return isStrongRootConditionsValid(c, leafMustBeContinuous);
	}

	private boolean isStrongRootConditionsValid(TransientClique parent,
			boolean leafMustBeContinuous) {

		if (leafMustBeContinuous) {
			// we need to see if we are a leaf, if not continue down each path.
			int cnt = 0;
			for (TransientCliqueSeparator sep : parent.getSeparators()) {
				TransientClique child = sep.getCliqueA() == parent ? sep
						.getCliqueB() : sep.getCliqueA();
				if (cliques.containsValue(child) && child.getParent() == null) {
					child.setParent(parent);
					cnt++;
					if (!isStrongRootConditionsValid(child,
							leafMustBeContinuous)) {
						return false;
					}
				}

			}
			if (cnt > 0) { // We are a leaf...are we completely continuous?
				for (TransientVariable gv : parent.getMembers().values()) {
					if (gv.getReference() instanceof DiscreteVariable) {
						return false;
					}
				}
			}

		} else {
			for (TransientCliqueSeparator sep : parent.getSeparators()) {
				TransientClique child = sep.getCliqueA() == parent ? sep
						.getCliqueB() : sep.getCliqueA();
				if (child.getParent() == null) {
					child.setParent(parent);
					leafMustBeContinuous = false;
					for (TransientVariable gv : sep.getMembers()) {
						if (gv.getReference() instanceof ContinuousVariable) {
							leafMustBeContinuous = true;
							break;
						}
					}

					if (!isStrongRootConditionsValid(child,
							leafMustBeContinuous)) {
						return false;
					}
				}

			}
		}
		return true;
	}

	/**
	 * @param slice
	 *            the slice that the forwardInterface belongs to.
	 * @param forwardInterface
	 *            the futureInterface to set
	 */
	public void linkInterface(int slice, TransientClique forwardInterface)
			throws Exception {
		// Do things to make sure the
		TransientClique iFace = interfaces.get(slice);
		iFace.setFactor(forwardInterface.getFactor());

		TransientCliqueSeparator p = forwardInterface.getParentSeparator();

		iFace.setParentSeparator(p);

	}

}
