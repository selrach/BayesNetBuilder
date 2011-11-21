package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GraphVariable represents a RandomVariable in the network, but its
 * connections can be manipulated without affecting the original network. This
 * is useful for several different graph algorithms.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class GraphVariable implements Comparable<GraphVariable> {

	private RandomVariable reference = null;
	private int slice = -1;
	private int cpdSlice = -1;
	private final String id;
	private boolean isCut = false;
	private boolean inInterface = false;
	private boolean distributionCounted = false;

	public boolean isInInterface() {
		return inInterface;
	}

	public void setInInterface(boolean b) {
		this.inInterface = b;
	}

	// Directed connections
	private final Map<String, GraphVariable> parents = new HashMap<String, GraphVariable>();
	private final Map<String, GraphVariable> children = new HashMap<String, GraphVariable>();

	// Undirected connections
	private final Map<String, GraphVariable> neighbors = new HashMap<String, GraphVariable>();

	public GraphVariable(RandomVariable reference, int slice,
			int pastSliceNumber) throws Exception {
		this.reference = reference;
		this.slice = slice;
		pastSliceNumber = Math.min(pastSliceNumber, reference
				.getMaxParentTemplateSlices() - 1);
		this.cpdSlice = Math.max(pastSliceNumber - slice, 0);
		this.id = this.reference.getId() + "_" + this.slice;
		int maxTemp = reference.getMaxChildrenTemplateSlices() - 1;
		if (slice > maxTemp) {
			throw new Exception(
					"Cannot create graphvariable extending out of template slices reach");
		}

	}

	public String getId() {
		return this.id;
	}

	public boolean addParent(GraphVariable variable) {
		String id = variable.getId();
		if (!neighbors.containsKey(id)) {
			neighbors.put(id, variable);
		}
		if (!parents.containsKey(id)) {
			parents.put(id, variable);
			return true;
		}
		return false;
	}

	public boolean addChild(GraphVariable variable) {
		String id = variable.getId();
		if (!neighbors.containsKey(id)) {
			neighbors.put(id, variable);
		}
		if (!children.containsKey(id)) {
			children.put(id, variable);
			return true;
		}
		return false;
	}

	public boolean addNeighbor(GraphVariable variable) {
		String id = variable.getId();
		if (!neighbors.containsKey(id)) {
			neighbors.put(id, variable);
			return true;
		}
		return false;
	}

	public void removeNeighbor(GraphVariable variable) {
		String id = variable.getId();
		if (children.containsKey(id)) {
			children.remove(id);
		}
		if (parents.containsKey(id)) {
			parents.remove(id);
		}
		if (neighbors.containsKey(id)) {
			neighbors.remove(id);
		}
	}

	public boolean hasNeighbor(GraphVariable variable) {
		if (variable.isCut()) {
			return false;
		}
		return neighbors.containsKey(variable.getId());
	}

	public List<GraphVariable> getNeighborList() {
		List<GraphVariable> list = new ArrayList<GraphVariable>();
		for (GraphVariable gv : neighbors.values()) {
			if (!gv.isCut) {
				list.add(gv);
			}
		}
		return list;
	}

	public Map<String, GraphVariable> getNeighborMap() {
		Map<String, GraphVariable> map = new HashMap<String, GraphVariable>();
		for (String key : neighbors.keySet()) {
			GraphVariable gv = neighbors.get(key);
			if (!gv.isCut()) {
				map.put(key, gv);
			}
		}
		return map;
	}

	public List<GraphVariable> getParents() {
		List<GraphVariable> list = new ArrayList<GraphVariable>(parents.size());
		for (GraphVariable gv : parents.values()) {
			if (!gv.isCut) {
				list.add(gv);
			}
		}
		return list;
	}

	public int compareTo(GraphVariable tv) {
		if (tv.slice == this.slice) {
			return tv.reference.getId().compareTo(reference.id);
		} else {
			return tv.slice - slice;
		}
	}

	/**
	 * @return the isCut
	 */
	public boolean isCut() {
		return isCut;
	}

	/**
	 * @param isCut
	 *            the isCut to set
	 */
	public void setCut(boolean isCut) {
		this.isCut = isCut;
	}

	public RandomVariable getReference() {
		return reference;
	}

	/**
	 * GraphVariable This is how far in the past this graph variable exists
	 * 
	 * @return
	 */
	public int getSlice() {
		return slice;
	}

	public int getCpdSlice() {
		return cpdSlice;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(reference.toString());
		sb.append(" from slice ");
		sb.append(slice);
		sb.append("\t isCut? ");
		sb.append(isCut);

		return sb.toString();
	}

	/**
	 * @return the distributionCounted
	 */
	public boolean isDistributionCounted() {
		return distributionCounted;
	}

	/**
	 * @param distributionCounted
	 *            the distributionCounted to set
	 */
	public void setDistributionCounted(boolean distributionCounted) {
		this.distributionCounted = distributionCounted;
	}

}
