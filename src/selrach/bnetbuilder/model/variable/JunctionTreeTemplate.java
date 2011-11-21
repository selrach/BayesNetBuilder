package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.listener.interfaces.EdgeUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.ModelUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.VariableUpdatedListener;

/**
 * This contains the information needed to print off more JunctionTree as the
 * time horizon extends past the original slice.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class JunctionTreeTemplate implements VariableUpdatedListener,
		EdgeUpdatedListener, ModelUpdatedListener {
	private final List<Map<String, GraphVariable>> variableSets = new ArrayList<Map<String, GraphVariable>>();
	private final List<List<Clique>> cliqueSets = new ArrayList<List<Clique>>();
	private final List<List<CliqueSeparator>> cliqueSeparatorSets = new ArrayList<List<CliqueSeparator>>();

	private final List<JunctionTree> junctionTreeSlices = new ArrayList<JunctionTree>();

	private boolean stale = true;

	/**
	 * Is the junction tree template stale (ie we
	 * 
	 * @return
	 */
	public boolean isStale() {
		return stale;
	}

	/**
	 * Set the junction tree template to be stale
	 * 
	 * @param stale
	 */
	public void setStale(boolean stale) {
		this.stale = stale;
	}

	/**
	 * Have we calibrated this junction tree?
	 * 
	 * @return
	 */
	public boolean isCalibrated() {
		return junctionTreeSlices.size() > 0;
	}

	/**
	 * Use this if we put evidence on the junction tree and need to recalculate
	 * the calibration
	 */
	public void setCalibratedFalse() {
		boolean tmp = stale;
		resetInstance();
		stale = tmp;
	}

	public void reset() {
		if (variableSets != null) {
			for (Map<String, GraphVariable> map : variableSets) {
				map.clear();
			}
			variableSets.clear();
		}
		if (cliqueSets != null) {
			for (List<Clique> map : cliqueSets) {
				map.clear();
			}
			cliqueSets.clear();
		}
		if (cliqueSeparatorSets != null) {
			for (List<CliqueSeparator> map : cliqueSeparatorSets) {
				map.clear();
			}
			cliqueSeparatorSets.clear();
		}
		resetInstance();
	}

	public void resetCutVariables() {

		if (variableSets != null) {
			for (Map<String, GraphVariable> map : variableSets) {
				for (GraphVariable gv : map.values()) {
					gv.setCut(false);
				}
			}
		}
	}

	/**
	 * @return the cliqueSeparatorSets
	 */
	public List<List<CliqueSeparator>> getCliqueSeparatorSets() {
		return cliqueSeparatorSets;
	}

	/**
	 * @param cliqueSeparatorSets
	 *            the cliqueSeparatorSets to set
	 */
	public void setCliqueSeparatorSets(
			List<List<CliqueSeparator>> cliqueSeparatorSets) {
		this.cliqueSeparatorSets.clear();
		for (List<CliqueSeparator> list : cliqueSeparatorSets) {
			this.cliqueSeparatorSets.add(list);
		}
	}

	/**
	 * @return the cliqueSets
	 */
	public List<List<Clique>> getCliqueSets() {
		return cliqueSets;
	}

	/**
	 * @param cliqueSets
	 *            the cliqueSets to set
	 */
	public void setCliqueSets(List<List<Clique>> cliqueSets) {
		this.cliqueSets.clear();
		for (List<Clique> list : cliqueSets) {
			this.cliqueSets.add(list);
		}
	}

	/**
	 * @return the variableSets
	 */
	public List<Map<String, GraphVariable>> getVariableSets() {
		return variableSets;
	}

	/**
	 * @param variableSets
	 *            the variableSets to set
	 */
	public void setVariableSets(List<Map<String, GraphVariable>> variableSets) {
		this.variableSets.clear();
		for (Map<String, GraphVariable> map : variableSets) {
			this.variableSets.add(map);
		}
	}

	public void addVariable(RandomVariable variable) {

		resetInstance();
	}

	public void removeVariable(RandomVariable variable) {

		resetInstance();
	}

	public void updateVariable(RandomVariable variable) {
		// TODO: Does our network become invalid because of a cpd change?
		// stale = true;
		boolean tmp = stale;
		resetInstance();
		stale = tmp;
	}

	public void modelFinishedInfering(DynamicBayesNetModel model) {
	}

	public void modelFinishedLearning(DynamicBayesNetModel model) {
	}

	public void modelLoaded(DynamicBayesNetModel model) {

		resetInstance();
	}

	public void modelUnloaded(DynamicBayesNetModel model) {

		resetInstance();
	}

	public void addEdge(RandomVariable fromVariable, RandomVariable toVariable,
			int timeSeparation) {
		resetInstance();
	}

	public void removeEdge(RandomVariable fromVariable,
			RandomVariable toVariable, int timeSeparation) {
		resetInstance();
	}

	private void resetInstance() {
		stale = true;
		this.junctionTreeSlices.clear();
		for (Map<String, GraphVariable> map : this.variableSets) {
			for (GraphVariable gv : map.values()) {
				gv.setDistributionCounted(false);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Junction Tree Information:");
		if (cliqueSeparatorSets.size() > 0) {
			sb.append("\n\tClique Separator Sets: ");
			for (List<CliqueSeparator> cliques : cliqueSeparatorSets) {
				sb.append("\n++++++++++++++++++++++++++++++\n");
				for (CliqueSeparator clique : cliques) {
					sb.append("\n-----------------------------------\n");
					sb.append(clique);
				}
			}
		} else if (cliqueSets.size() > 0) {
			sb.append("\n\tClique Sets: ");
			for (List<Clique> cliques : cliqueSets) {
				sb.append("\n++++++++++++++++++++++++++++++\n");
				for (Clique clique : cliques) {
					sb.append("\n-----------------------------------\n");
					sb.append(clique);
				}
			}
		} else if (variableSets.size() > 0) {
			sb.append("\n\tVariables: ");
			for (Map<String, GraphVariable> map : variableSets) {
				sb.append("\n++++++++++++++++\n");
				for (GraphVariable gv : map.values()) {
					sb.append("\n----------\n");
					sb.append(gv);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * @return the junctionTreeSlices
	 */
	public List<JunctionTree> getJunctionTreeSlices() {
		return junctionTreeSlices;
	}

	/**
	 * @param junctionTreeSlices
	 *            the junctionTreeSlices to set
	 */
	public void setJunctionTreeSlices(List<JunctionTree> junctionTreeSlices) {
		this.junctionTreeSlices.clear();
		this.junctionTreeSlices.addAll(junctionTreeSlices);
	}

}
