package selrach.bnetbuilder.model.algorithms.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.GraphVariable;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * 
 * Assumes a network has been constructed, will set up the template slice
 * moralization. This constructs forward interface strong moral graphs.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class GenerateMoralization {

	/**
	 * Produces the moral graph.
	 * @param model
	 * @throws Exception
	 */
	public static void execute(final DynamicBayesNetModel model)
			throws Exception {
		final List<RandomVariable> variables = model.getVariables();
		final List<GraphVariable> gvInInterface = new ArrayList<GraphVariable>();
		final Set<RandomVariable> inInterface = new HashSet<RandomVariable>();
		// Only questionable thing, which variables are in the forward interface
		// for
		// a n-T DBN?
		for (final RandomVariable rv : variables) {
			if (rv.hasChildrenAt(1)) {
				inInterface.add(rv);
			}
		}

		final int numberTemplateSlice = model.getNumberTemplateSlices();
		final List<GraphVariable> needMarrying = new ArrayList<GraphVariable>();

		final List<Map<String, GraphVariable>> moralizedGraphs = new ArrayList<Map<String, GraphVariable>>();

		// For each template timeslice we need to create a set of moralized
		// variables
		for (int slice = 0; slice < numberTemplateSlice; slice++) {
			// Initialize the current graph
			final Map<String, GraphVariable> moralizedGraph = new TreeMap<String, GraphVariable>();
			moralizedGraphs.add(moralizedGraph);

			// Okay starting as far in the past as we go for this slice
			for (int workingSlice = slice; workingSlice >= 0; workingSlice--) {
				gvInInterface.clear();
				// Grab all the variables
				for (final RandomVariable rv : variables) {
					// If this variable connects up to the present as a parent
					// or is in
					// the present
					if (workingSlice == 0 || rv.hasChildrenAt(workingSlice)) {
						// Create the GraphVariable associated with it
						final GraphVariable gv = new GraphVariable(rv,
								workingSlice, slice);
						// if we are not at the current timeslice, add us to the
						// interface
						if (inInterface.contains(rv)) {
							gvInInterface.add(gv);
							gv.setInInterface(true);
						}

						// Clear out marriage list
						needMarrying.clear();
						// For each timeslice in the current set
						for (int tmpSlice = workingSlice; tmpSlice <= slice; tmpSlice++) {
							// We need to inspect the parents of this guy and
							// see if a parent
							// exists in the moralized graph pattern, if he does
							// then add
							// them to the sets of neighbors
							// Since we start in the past and work our way
							// forward, we should
							// collect all valid parents for this graph.
							for (final RandomVariable parent : rv
									.getParentsAt(tmpSlice)) {
								final GraphVariable p = moralizedGraph
										.get(parent.getId() + "_" + (tmpSlice));
								if (p != null) {
									// potentially needs to be married to other
									// parents of this
									// variable
									needMarrying.add(p);
									gv.addParent(p);
									p.addChild(gv);
								}
							}
						}
						// Marry all parents
						final int sz = needMarrying.size();
						for (int i = 0; i < sz; i++) {
							final GraphVariable p1 = needMarrying.get(i);
							for (int j = i + 1; j < sz; j++) {
								final GraphVariable p2 = needMarrying.get(j);
								p1.addNeighbor(p2);
								p2.addNeighbor(p1);
							}
						}
						moralizedGraph.put(gv.getId(), gv);
					}
				}
				if (gvInInterface.size() != inInterface.size()) {
					throw new Exception(
							"Invalid construction of moralized graph, forward interface has differing sizes!");
				}
				final int sz = gvInInterface.size();
				for (int i = 0; i < sz; i++) {
					final GraphVariable p1 = gvInInterface.get(i);
					for (int j = i + 1; j < sz; j++) {
						final GraphVariable p2 = gvInInterface.get(j);
						p1.addNeighbor(p2);
						p2.addNeighbor(p1);
					}
				}

				// Finally in order to construct a strong junction tree for
				// continuous variables, we need to explore continuous paths and
				// connect discrete variables connected to that path
				marryDiscreteOnContinuousPath(moralizedGraph.values());
			}
		}

		final JunctionTreeTemplate jtt = model.getJunctionTreeTemplate();
		jtt.reset();
		jtt.setVariableSets(moralizedGraphs);
	}

	/**
	 * This produces links between discrete variables on a continuous path. This
	 * is a requirement for strong junction trees in marked graphs.
	 * \cite{Propagation of Probabilities, Means, and Variances in Mixed
	 * Graphical Association Models by Steffen L. Lauritzen}
	 * 
	 * @param variables
	 */
	static private void marryDiscreteOnContinuousPath(
			Collection<GraphVariable> variables) {

		TreeSet<GraphVariable> discreteVars = new TreeSet<GraphVariable>();
		for (GraphVariable gv : variables) {
			discreteVars.clear();
			if (!gv.isCut() && gv.getReference() instanceof ContinuousVariable) {
				gv.setCut(true);
				getDiscreteVars(gv, discreteVars);
				for (GraphVariable dvA : discreteVars) {
					for (GraphVariable dvB : discreteVars.tailSet(dvA)) {
						dvA.addNeighbor(dvB);
						dvB.addNeighbor(dvA);
					}
				}
			}
		}
		unmarkVariables(variables);
	}

	/**
	 * Recursive search
	 * @param gv
	 * @param discreteVars
	 */
	static private void getDiscreteVars(GraphVariable gv,
			Set<GraphVariable> discreteVars) {
		for (GraphVariable neighbor : gv.getNeighborList()) {
			if (neighbor.isCut()) {
				continue;
			}
			neighbor.setCut(true);
			if (neighbor.getReference() instanceof ContinuousVariable) {
				getDiscreteVars(neighbor, discreteVars);
			} else {
				discreteVars.add(neighbor);
			}

		}
	}

	/**
	 * Clears out cut markers on graph variables.
	 * @param variables
	 */
	static private void unmarkVariables(Collection<GraphVariable> variables) {
		for (GraphVariable gv : variables) {
			gv.setCut(false);
		}
	}
}
