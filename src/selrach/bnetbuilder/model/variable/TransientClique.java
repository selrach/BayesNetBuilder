package selrach.bnetbuilder.model.variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is a short lived object that is created for a run of the junction tree.
 * This mirrors the cliques created out of the GraphVariables by is specifically
 * tied to a particular time in the future. As soon as any additional data gets
 * incorporated into the network, this clique probably becomes invalid and is
 * regenerated as a consequence.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class TransientClique implements Comparable<TransientClique> {

	final private Clique reference;
	private Factor factor;
	private final Set<TransientClique> neighbors = new TreeSet<TransientClique>();
	private final Set<TransientCliqueSeparator> separators = new TreeSet<TransientCliqueSeparator>();

	final private Map<String, TransientVariable> members = new HashMap<String, TransientVariable>();;

	private TransientClique parent = null;
	private TransientCliqueSeparator parentSeparator = null;

	public TransientClique(int slice, Clique reference) throws Exception {
		this.reference = reference;
		for (GraphVariable gv : reference.getMembers().values()) {
			TransientVariable tv = gv.getReference().getTransientVariable(
					slice - gv.getSlice());
			members.put(tv.getId(), tv);
		}
		this.factor = new Factor(slice, reference.getPotential());
	}

	public void setupConnections(Map<Clique, TransientClique> cMap,
			Map<CliqueSeparator, TransientCliqueSeparator> csMap) {
		for (Clique c : reference.getNeighbors()) {
			neighbors.add(cMap.get(c));
		}
		for (CliqueSeparator cs : reference.getSeparators()) {
			separators.add(csMap.get(cs));
		}
	}

	public Clique getReference() {
		return reference;
	}

	public Factor getFactor() {
		return factor;
	}

	public void setFactor(Factor potential) {
		this.factor = potential;
	}

	public TransientClique getParent() {
		return parent;
	}

	public void setParent(TransientClique parent) {
		this.parent = parent;
	}

	public TransientCliqueSeparator getParentSeparator() {
		return parentSeparator;
	}

	public void setParentSeparator(TransientCliqueSeparator parent) {
		this.parentSeparator = parent;
	}

	/**
	 * @return the neighbors
	 */
	public Set<TransientClique> getNeighbors() {
		return neighbors;
	}

	public void setEvidence(TransientVariable value) throws Exception {
		this.factor.setEvidence(value);
	}

	public Map<String, TransientVariable> getMembers() {
		return members;
	}

	/**
	 * @return the separators
	 */
	public Set<TransientCliqueSeparator> getSeparators() {
		return separators;
	}

	public String toDetailedString(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend);
		sb.append("Transient Clique:\n");
		sb.append(prepend);
		sb.append("\tReference:\n");
		sb.append(prepend);
		sb.append(reference.toDetailedString(prepend + "\t"));

		sb.append("\tPotential:\n");
		sb.append(factor.toString());

		return sb.toString();
	}

	@Override
	public String toString() {
		return toDetailedString("");
	}

	public int compareTo(TransientClique tc) {
		return reference.compareTo(tc.reference);
	}

}
