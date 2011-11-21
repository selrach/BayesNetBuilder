package selrach.bnetbuilder.model.variable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A clique is a group of completely connected variables that is a subset of
 * some variables. Cliques in the terms of this program are templates to build
 * off of created from GraphVariables within a network.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Clique implements Comparable<Clique> {

	private GraphVariable originator = null;
	private final Map<String, GraphVariable> members = new TreeMap<String, GraphVariable>();
	private final Set<Clique> neighbors = new TreeSet<Clique>();
	private final Set<CliqueSeparator> separators = new TreeSet<CliqueSeparator>();

	private int forwardInterface = -1;

	final private String id;

	// Potential associated with this clique.
	private Potential potential = null;

	public Potential getPotential() throws Exception {
		if (potential == null) {
			generatePotential();
		}
		return potential;
	}

	private void generatePotential() throws Exception {
		
		// Potential ret = null;
		Potential ret = new Potential(members.values());
		for (GraphVariable gv : members.values()) {
			if (gv.isDistributionCounted()
					|| !(members.values().containsAll(gv.getParents())))
			// Make sure the clique contains the entire family of the
			// variable
			{
				continue;
			}
			gv.setDistributionCounted(true);
			if (ret == null) {
				ret = new Potential(gv);
			} else {
				ret = ret.combine(new Potential(gv));
			}

		}
		if (ret == null) {
			ret = new Potential(); // Default potential, a unary table
		}
		/*
		 * for(GraphVariable gv : members.values()) {
		 * if(gv.isDistributionCounted() || !(members.values().con }
		 */
		potential = ret;
	}

	public Clique(GraphVariable originator) {
		this.originator = originator;
		members.put(originator.getId(), originator);
		members.putAll(originator.getNeighborMap());
		StringBuilder sb = new StringBuilder();
		for (GraphVariable gv : members.values()) {
			sb.append(gv.getId());
		}
		id = sb.toString();
	}

	public Clique(Map<String, GraphVariable> members) {
		this.members.putAll(members);
		StringBuilder sb = new StringBuilder();
		for (GraphVariable gv : members.values()) {
			sb.append(gv.getId());
		}
		id = sb.toString();
	}

	public void addSeparator(CliqueSeparator cliqueSeparator) {
		this.separators.add(cliqueSeparator);
	}

	public void addNeighbor(Clique clique) {
		neighbors.add(clique);
	}

	public void removeNeighbor(Clique clique) {
		if (neighbors.contains(clique)) {
			neighbors.remove(clique);
		}
	}

	public int compareTo(Clique o) {

		return id.compareTo(o.id);
	}

	public Set<GraphVariable> intersect(Clique b) {
		Set<GraphVariable> intersection = new TreeSet<GraphVariable>();
		for (GraphVariable gv : members.values()) {
			if (b.members.containsKey(gv.getId())) {
				intersection.add(gv);
			}
		}
		return intersection;
	}

	public Set<GraphVariable> intersect(Set<GraphVariable> b) {
		Set<GraphVariable> intersection = new TreeSet<GraphVariable>();
		for (GraphVariable gv : members.values()) {
			if (b.contains(gv)) {
				intersection.add(gv);
			}
		}
		return intersection;
	}

	public Set<GraphVariable> union(Clique b) {
		Set<GraphVariable> union = new TreeSet<GraphVariable>(members.values());
		union.addAll(b.members.values());
		return union;
	}

	public Set<GraphVariable> union(Set<GraphVariable> b) {
		Set<GraphVariable> union = new TreeSet<GraphVariable>(members.values());
		union.addAll(b);
		return union;
	}

	public boolean isProperSubset(Clique b) {
		return b.members.values().containsAll(members.values());
	}

	public boolean isProperSubset(Set<GraphVariable> b) {
		return b.containsAll(members.values());
	}

	public static boolean isProperSubset(Set<GraphVariable> a, Clique b) {
		return b.members.values().containsAll(a);
	}

	@Override
	public String toString() {
		return toDetailedString("");
	}

	public String toDetailedString(String prepend) {
		StringBuilder sb = new StringBuilder();

		sb.append(prepend);
		sb.append("Clique originator: ");
		sb.append(originator);
		sb.append("\n");
		sb.append(prepend);
		sb.append("\tcontains:");
		sb.append("\n");
		sb.append(prepend);
		sb.append("[ ");
		for (GraphVariable gv : members.values()) {
			sb.append(gv);
			sb.append(", ");
		}
		sb.append(" ]");

		return sb.toString();
	}

	public Map<String, GraphVariable> getMembers() {
		return Collections.unmodifiableMap(members);
	}

	/**
	 * @return the forwardInterface
	 */
	public boolean isForwardInterface() {
		return forwardInterface != -1;
	}

	/**
	 * @param forwardInterface
	 *            the forwardInterface to set
	 */
	public void setForwardInterface(int slice) {
		this.forwardInterface = slice;
	}

	public int getForwardInterface() {
		return this.forwardInterface;
	}

	public Set<Clique> getNeighbors() {
		return this.neighbors;
	}

	public Set<CliqueSeparator> getSeparators() {
		return this.separators;
	}

}
