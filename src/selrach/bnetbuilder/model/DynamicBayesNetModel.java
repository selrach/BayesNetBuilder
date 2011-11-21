package selrach.bnetbuilder.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.data.TrialDao;
import selrach.bnetbuilder.model.dao.DynamicBayesNetDao;
import selrach.bnetbuilder.model.listener.interfaces.EdgeUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.ModelUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.VariableUpdatedListener;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import selrach.bnetbuilder.model.variable.interfaces.StateUpdatedListener;

/**
 * Main class for the dynamic Bayesian network model. Construction of model as
 * well as slice spawning happens in this class.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DynamicBayesNetModel implements StateUpdatedListener {

	private static Logger logger = Logger.getLogger(DynamicBayesNetModel.class);

	/**
	 * Handles EdgeUpdatedEvent subscribers
	 * 
	 * @author <a href="mailto:charleswrobertson@gmail.com">Charles
	 *         Robertson</a>
	 * 
	 */
	static private class EdgeUpdatedEvent {
		static private EdgeUpdatedEvent instance = null;

		static public EdgeUpdatedEvent getInstance() {
			if (instance == null) {
				instance = new EdgeUpdatedEvent();
			}
			return instance;
		}

		private final ArrayList<EdgeUpdatedListener> subscribers = new ArrayList<EdgeUpdatedListener>();

		private EdgeUpdatedEvent() {
		}

		protected void edgeAdded(RandomVariable fromVariable,
				RandomVariable toVariable, int time) {
			for (EdgeUpdatedListener l : subscribers) {
				l.addEdge(fromVariable, toVariable, time);
			}
		}

		protected void edgeRemoved(RandomVariable fromVariable,
				RandomVariable toVariable, int time) {
			for (EdgeUpdatedListener l : subscribers) {
				l.removeEdge(fromVariable, toVariable, time);
			}
		}

		public void subscribe(EdgeUpdatedListener listener) {
			if (!subscribers.contains(listener)) {
				subscribers.add(listener);
			}
		}

		public void unsubscribe(EdgeUpdatedListener listener) {
			if (subscribers.contains(listener)) {
				subscribers.remove(listener);
			}
		}
	}

	/**
	 * Handles ModelUpdateListener subscribers
	 * 
	 * @author <a href="mailto:charleswrobertson@gmail.com">Charles
	 *         Robertson</a>
	 * 
	 */
	static private class ModelUpdatedEvent {
		static private ModelUpdatedEvent instance = null;

		static public ModelUpdatedEvent getInstance() {
			if (instance == null) {
				instance = new ModelUpdatedEvent();
			}
			return instance;
		}

		private final ArrayList<ModelUpdatedListener> subscribers = new ArrayList<ModelUpdatedListener>();

		private ModelUpdatedEvent() {
		}

		public void modelFinishedInfering(DynamicBayesNetModel model) {
			for (ModelUpdatedListener l : subscribers) {
				l.modelFinishedInfering(model);
			}
		}

		public void modelFinishedLearning(DynamicBayesNetModel model) {
			for (ModelUpdatedListener l : subscribers) {
				l.modelFinishedLearning(model);
			}
		}

		public void modelLoaded(DynamicBayesNetModel model) {
			for (ModelUpdatedListener l : subscribers) {
				l.modelLoaded(model);
			}
		}

		public void modelUnloaded(DynamicBayesNetModel model) {
			for (ModelUpdatedListener l : subscribers) {
				l.modelUnloaded(model);
			}
		}

		public void subscribe(ModelUpdatedListener listener) {
			if (!subscribers.contains(listener)) {
				subscribers.add(listener);
			}
		}

		public void unsubscribe(ModelUpdatedListener listener) {
			if (subscribers.contains(listener)) {
				subscribers.remove(listener);
			}
		}
	}

	/**
	 * Allows for notification of different variable changes.
	 * 
	 * @author <a href="mailto:charleswrobertson@gmail.com">Charles
	 *         Robertson</a>
	 * 
	 */
	static private class VariableUpdatedEvent {
		static private VariableUpdatedEvent instance = null;

		static public VariableUpdatedEvent getInstance() {
			if (instance == null) {
				instance = new VariableUpdatedEvent();
			}
			return instance;
		}

		private final ArrayList<VariableUpdatedListener> subscribers = new ArrayList<VariableUpdatedListener>();

		private VariableUpdatedEvent() {
		}

		/**
		 * Subscribe to variable updated events, listener must implement
		 * VariableUpdatedListener interface
		 * 
		 * @param listener
		 */
		public void subscribe(VariableUpdatedListener listener) {
			if (!subscribers.contains(listener)) {
				subscribers.add(listener);
			}
		}

		/**
		 * Unsubscribe to variable updated events.
		 * 
		 * @param listener
		 */
		public void unsubscribe(VariableUpdatedListener listener) {
			if (subscribers.contains(listener)) {
				subscribers.remove(listener);
			}
		}

		/**
		 * Variable has been added, notify listeners
		 * 
		 * @param variable
		 */
		protected void variableAdded(RandomVariable variable) {
			for (VariableUpdatedListener l : subscribers) {
				l.addVariable(variable);
			}
		}

		/**
		 * Variable has been removed, notify listeners
		 * 
		 * @param variable
		 */
		protected void variableRemoved(RandomVariable variable) {
			for (VariableUpdatedListener l : subscribers) {
				l.removeVariable(variable);
			}
		}

		/**
		 * Something about the variable has been updated, notify listeners
		 * 
		 * @param variable
		 */
		protected void variableUpdated(RandomVariable variable) {
			for (VariableUpdatedListener l : subscribers) {
				l.updateVariable(variable);
			}
		}
	}

	/**
	 * This is just a table of all the variables that we can possibly have
	 * indexed by key
	 */
	Map<String, RandomVariable> variables = new Hashtable<String, RandomVariable>();

	private final JunctionTreeTemplate junctionTreeTemplate = new JunctionTreeTemplate();

	/**
	 * Topological sorting cache.
	 */
	List<RandomVariable> topologicallySortedVariables = null;

	/**
	 * This is the number of timeslices within this model, 1 corresponds to a
	 * non-temporal DBN, 2 corresponds to a 2-DBN network, etc.
	 */
	int numberTemplateSlices = 1;

	/**
	 * This is the current template slice that we are adding/removing
	 * information from
	 */
	private int templateSlice = 0;

	/**
	 * class that handles loading and saving of dbns
	 */
	private DynamicBayesNetDao dao = null;

	/**
	 * This handles accessing trials from different data sources.
	 */
	private TrialDao trialDao = null;

	/**
	 * List of slices in memory
	 */
	List<BayesNetSlice> slices = new ArrayList<BayesNetSlice>();

	/**
	 * This is the current upper bound on the timesteps that we can generate in
	 * equations this is generally represented at T
	 */
	int maxNumberSlices = 1;

	/**
	 * This is the maximum amount of slices allowed in memory...useful if we
	 * want to limit our memory footprint in filtering applications. Currently
	 * we are just going to keep everything in memory.
	 */
	int maxSlicesInMemory = Integer.MAX_VALUE;

	/**
	 * This should always be bigger than the largest order variable in the
	 * network We do this to impose a consistent outlay on all random variable
	 * lists so that we create consistent distribution mappings
	 */
	private int nextOrder = 0;

	/**
	 * List of roots, aka variables with no intra-parent ancestors.
	 */
	List<RandomVariable> roots = new ArrayList<RandomVariable>();

	private final VariableUpdatedEvent variableUpdatedEvent = VariableUpdatedEvent
			.getInstance();

	public void notifyVariableUpdate(RandomVariable variable) {
		clearTransientVariables();
		variableUpdatedEvent.variableUpdated(variable);
	}

	public void notifyEvidenceSet(RandomVariable variable) {
		variableUpdatedEvent.variableUpdated(variable);
	}

	private final EdgeUpdatedEvent edgeUpdatedEvent = EdgeUpdatedEvent
			.getInstance();

	private final ModelUpdatedEvent modelUpdatedEvent = ModelUpdatedEvent
			.getInstance();

	Point nextIntra = new Point(20, -20);

	public DynamicBayesNetModel(DynamicBayesNetDao dao) {
		this.dao = dao;
		trialDao = new TrialDao(this);
		subscribe(junctionTreeTemplate);
	}

	/**
	 * Sets the processing dao
	 * 
	 * @param dao
	 */
	public void setDao(DynamicBayesNetDao dao) {
		if (dao == null) {
			return;
		}
		this.dao = dao;
	}

	public DynamicBayesNetDao getDao() {
		return dao;
	}

	/**
	 * Clears all information stored in this model
	 * 
	 * @throws Exception
	 */
	public void clear() throws Exception {
		for (RandomVariable rv : variables.values()) {
			rv.clearRelations();
		}
		clearTransientVariables();
		variables.clear();
		roots.clear();
		templateSlice = 0;
		numberTemplateSlices = 1;
		nextOrder = 0;
		modelUpdatedEvent.modelUnloaded(this);
	}

	/**
	 * Creates an edge in the graph
	 * 
	 * @param from
	 *            the variable where the edge originates
	 * @param to
	 *            the variable where the edge terminates
	 * @param time
	 *            what timeslice this node comes from: 0 intra-edge >0
	 *            inter-edge with currentTime-time being the slice it originates
	 *            from
	 * @throws Exception
	 */
	public void createEdge(RandomVariable from, RandomVariable to, int time)
			throws Exception {
		topologicallySortedVariables = null;
		slices.clear();
		boolean exist = false;
		for (RandomVariable rv : from.getChildrenAt(time)) {
			if (rv.equals(to)) {
				exist = true;
			}
		}
		if (!exist) {
			to.addParent(from, time);
			if (time == 0) {
				if (roots.contains(to)) {
					roots.remove(to);
				}
				// Make sure graph is still a DAG, aka do topological sort on
				// slice 0
				// If it breaks DAG condition, remove the edge
				if (!introducesCycle(to)) {
					to.clearCpd(time);
					clearTransientVariables();
					edgeUpdatedEvent.edgeAdded(from, to, time);
				} else {
					to.removeParent(from, time);
					if (to.getParents(0).size() == 0 && !roots.contains(to)) {
						roots.add(to);
					}
					return;
				}
			} else {
				numberTemplateSlices = numberTemplateSlices < time + 1 ? time + 1
						: numberTemplateSlices;
				clearTransientVariables();
				to.clearCpd(time);
				edgeUpdatedEvent.edgeAdded(from, to, time);
			}
		}
	}

	/**
	 * Creates an edge in the graph
	 * 
	 * @param fromId
	 *            the id of the variable where the edge originates
	 * @param toId
	 *            the id of the variable where the edge terminates
	 * @param time
	 *            what timeslice this node comes from: 0 intra-edge >0
	 *            inter-edge with currentTime-time being the slice it originates
	 *            from
	 * @throws Exception
	 */
	public void createEdge(String fromId, String toId, int time)
			throws Exception {
		if (variables.containsKey(fromId) && variables.containsKey(toId)) {
			createEdge(variables.get(fromId), variables.get(toId), time);
		}
	}

	/**
	 * Creates a new continuous variable
	 * 
	 * @return
	 */
	public RandomVariable createNewVariable() {
		return createNewVariable(true);
	}

	/**
	 * Create a new variable and add it to the network
	 * 
	 * @param continuous
	 * @return
	 */
	public RandomVariable createNewVariable(boolean continuous) {
		RandomVariable ret = null;
		if (continuous) {
			ret = new ContinuousVariable(this);
		} else {
			ret = new DiscreteVariable(this);
		}
		ret.setLocation(getNextLocation());
		ret.setOrder(getNextOrder());
		variables.put(ret.getId(), ret);
		roots.add(ret);
		topologicallySortedVariables = null;
		slices.clear();
		clearTransientVariables();
		variableUpdatedEvent.variableAdded(ret);
		return ret;
	}

	/**
	 * Add an already constructed variable to the network
	 * 
	 * @param variable
	 */
	public RandomVariable createNewVariable(RandomVariable variable) {
		variable.setOrder(getNextOrder());
		variables.put(variable.getId(), variable);
		roots.add(variable);
		topologicallySortedVariables = null;
		clearTransientVariables();
		variableUpdatedEvent.variableAdded(variable);
		return variable;
	}

	/**
	 * introduces cycle helper, recursive depth-first search
	 * 
	 * @param v
	 * @param set
	 * @return
	 */
	private boolean cycleDive(RandomVariable v, Set<RandomVariable> set) {
		if (set.contains(v)) {
			return true;
		}
		Set<RandomVariable> nextSet = new HashSet<RandomVariable>(set);
		nextSet.add(v);
		for (RandomVariable c : v.getChildren(0)) {
			if (cycleDive(c, nextSet)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates the first slice in the model as well as reseting the internal
	 * mechanisms to the initial slice.
	 */
	public void generateFirstSlice() {
		clearTransientVariables();
		generateSlice(slices.size());
	}

	/**
	 * Generates the next set of transient variables and returns the timestep
	 * that we think we are on. Run generateFirstSlice to reset. We roll up
	 * priors, so iterating through time slices are really one way.
	 * 
	 * @return
	 */
	public int generateNextSlice() throws Exception {
		if (slices.size() >= maxNumberSlices) {
			throw new Exception(
					"We cannot generate the next slice as it is bigger than our upper bound");
		}
		generateSlice(slices.size());
		return slices.size();
	}

	/**
	 * Generates the current timeslice by setting up the proper
	 * TransientVariable set
	 * 
	 * @param timeslice
	 */
	private void generateSlice(int timeslice) {
		if (topologicallySortedVariables == null) {
			topologicallySortedVariables = getTopologicalSorting();
		}

		List<TransientVariable> currentVariables = new ArrayList<TransientVariable>();
		Map<String, TransientVariable> currentVariableMap = new HashMap<String, TransientVariable>();
		TransientVariable tv = null;
		for (RandomVariable rv : topologicallySortedVariables) {
			currentVariables.add(tv = rv.getTransientVariable(timeslice));
			currentVariableMap.put(rv.getId(), tv);
		}

		slices.add(new BayesNetSlice(currentVariables, currentVariableMap));
		if (slices.size() > maxSlicesInMemory) {
			// Do stuff to marginalize distributions in slice we are going
			// to remove to a slice that is going to remain (slices.get(1))
			slices.remove(0);
		}
	}

	public void clearTransientVariables() {
		slices.clear();
		for (RandomVariable rv : variables.values()) {
			rv.clearTransientVariables();
		}
	}

	public void initializeSufficientStatistics() throws Exception {

		for (RandomVariable rv : variables.values()) {
			rv.initializeSufficientStatistic();
		}

	}

	public void clearTransientMarginals() {
		for (BayesNetSlice slice : slices) {
			for (TransientVariable tv : slice.getVariables()) {
				tv.setMarginal(null);
			}
		}
	}

	/**
	 * Generates a location to add a new variable to, kinda dumb implementation,
	 * but it avoids overlapping when creating a new graph.
	 * 
	 * @return
	 */
	private Point getNextLocation() {
		nextIntra.setLocation(nextIntra.getX(), nextIntra.getY() + 30);
		return nextIntra;
	}

	/**
	 * This gets an order value so that we always have a consistent layout of
	 * parents when we construct them or pass them into any conditional
	 * distributions, essentially an artificially imposed ordering that makes
	 * our life a bit easier.
	 * 
	 * @return
	 */
	public int getNextOrder() {
		return nextOrder++;
	}

	public BayesNetSlice getSlice(int time) throws Exception {
		if (time > maxNumberSlices || time < 0) {
			throw new Exception("Slice outside of valid range.");
		}
		if (slices.size() == 0) {
			generateFirstSlice();
		}
		while (time >= slices.size()) {
			generateNextSlice();
		}
		return slices.get(time);
	}

	/**
	 * Gets the template slice we are working on
	 * 
	 * @return
	 */
	public int getTemplateSlice() {
		return templateSlice;
	}

	/**
	 * This does a topological sorting of variables within this model, starting
	 * at the roots.
	 * 
	 * @return
	 */
	private List<RandomVariable> getTopologicalSorting() {
		List<RandomVariable> sorted = new ArrayList<RandomVariable>();
		Set<String> visited = new HashSet<String>();
		for (RandomVariable rv : roots) {
			if (!visited.contains(rv.getId())) {
				visited.add(rv.getId());
				topologicalSortDive(rv, sorted, visited);
				sorted.add(rv);
			}
		}
		Collections.reverse(sorted);
		return sorted;
	}

	/**
	 * Gets the topologically sorted list of variables. Topology is only enforce
	 * intra-slice. All interslice nodes can essentially be said to be infront
	 * of intra-slice nodes in this ordering
	 * 
	 * @return
	 */
	public List<RandomVariable> getVariables() {

		if (topologicallySortedVariables == null) {
			topologicallySortedVariables = getTopologicalSorting();
		}
		return Collections.unmodifiableList(topologicallySortedVariables);
	}

	/**
	 * This gets an unmodifiable version of the variable map keyed to thier id
	 * 
	 * @return
	 */
	public Map<String, RandomVariable> getVariableMap() {
		return Collections.unmodifiableMap(variables);
	}

	/**
	 * worst-case linear search to see if this node introduces a cycle.
	 * 
	 * @param to
	 * @return
	 */
	private boolean introducesCycle(RandomVariable to) {
		Set<RandomVariable> set = new HashSet<RandomVariable>();
		return cycleDive(to, set);
	}

	/**
	 * Loads a model from a file
	 * 
	 * @param filename
	 */
	public void loadModel(String filename) throws Exception {
		dao.loadModel(this, filename);
	}

	/**
	 * Removes an edge from the graph
	 * 
	 * @param from
	 *            the variable where the edge originates
	 * @param to
	 *            the variable where the edge terminates
	 * @param time
	 *            what timeslice this node comes from: 0 intra-edge >0
	 *            inter-edge with currentTime-time being the slice it originates
	 *            from
	 * @throws Exception
	 */
	public void removeEdge(RandomVariable from, RandomVariable to, int time)
			throws Exception {
		if (to.getParents(time).contains(from)) {
			to.removeParent(from, time);
			if (to.getParents(0).size() == 0 && !roots.contains(to)) {
				roots.add(to);
			}
			slices.clear();
			topologicallySortedVariables = null;
			edgeUpdatedEvent.edgeRemoved(from, to, time);
		}
	}

	/**
	 * Removes an edge from the graph
	 * 
	 * @param fromId
	 *            the id of the variable where the edge originates
	 * @param toId
	 *            the id of the variable where the edge terminates
	 * @param time
	 *            what timeslice this node comes from: 0 intra-edge >0
	 *            inter-edge with currentTime-time being the slice it originates
	 *            from
	 * @throws Exception
	 */
	public void removeEdge(String fromId, String toId, int time)
			throws Exception {
		if (variables.containsKey(fromId) && variables.containsKey(toId)) {
			removeEdge(variables.get(fromId), variables.get(toId), time);
		}
	}

	/**
	 * Remove a variable from the network. This INVALIDATES all CPDs that were
	 * using this variable in some way! You must go through and recreate new
	 * CPDs for variables utilizing this edge
	 * 
	 * @param variable
	 * @throws Exception
	 */
	public void removeVariable(RandomVariable variable) throws Exception {
		if (variables.containsKey(variable.getId())) {
			variable.clearRelations();
			variables.remove(variable.getId());
			if (roots.contains(variable)) {
				roots.remove(variable);
			}
			topologicallySortedVariables = null;
			slices.clear();
			for (RandomVariable rv : variables.values()) {
				if (rv.getParentsAt(0).size() == 0) {
					if (!roots.contains(rv)) {
						roots.add(rv);
					}
				}
			}
			variableUpdatedEvent.variableRemoved(variable);
		} else {
			throw new Exception("Variable does not exist in model: "
					+ variable.getName());
		}
	}

	/**
	 * Removes a variable from the network by id. This INVALIDATES all CPDs that
	 * were using this variable in some way! You must go through and recreate
	 * new CPDs for variables utilizing this edge
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void removeVariable(String id) throws Exception {
		if (variables.containsKey(id)) {
			removeVariable(variables.get(id));
		}
	}

	/**
	 * Saves the model to a file.
	 * 
	 * @param filename
	 * @throws Exception
	 */
	public void saveModel(String filename) throws Exception {
		dao.saveModel(this, filename);
	}

	/**
	 * This is a protected member that essentially sets up the next order value
	 */
	protected void setNextOrder() {
		int m = -999999999;
		for (RandomVariable rv : variables.values()) {
			if (m <= rv.getOrder()) {
				m = rv.getOrder();
			}
		}
		nextOrder = m + 1;
		if (nextOrder < 0) {
			nextOrder = 0;
		}
	}

	/**
	 * Sets what template slice we are working on
	 * 
	 * @param time
	 */
	public void setTemplateSlice(int time) {
		templateSlice = time;
	}

	/**
	 * Subscribes an object to this model depending on what interfaces it has
	 * implemented. VariableUpdatedListener EdgeUpdatedListener
	 * ModelUpdatedListner
	 * 
	 * @param obj
	 */
	public void subscribe(Object obj) {
		if (obj instanceof VariableUpdatedListener) {
			variableUpdatedEvent.subscribe((VariableUpdatedListener) obj);
		}
		if (obj instanceof EdgeUpdatedListener) {
			edgeUpdatedEvent.subscribe((EdgeUpdatedListener) obj);
		}
		if (obj instanceof ModelUpdatedListener) {
			modelUpdatedEvent.subscribe((ModelUpdatedListener) obj);
		}
	}

	/**
	 * Recursive call for topological sort.
	 * 
	 * @param parent
	 * @param sorted
	 * @param visited
	 */
	private void topologicalSortDive(RandomVariable parent,
			List<RandomVariable> sorted, Set<String> visited) {
		for (RandomVariable rv : parent.getChildren(0)) {
			if (!visited.contains(rv.getId())) {
				visited.add(rv.getId());
				topologicalSortDive(rv, sorted, visited);
				sorted.add(rv);
			}
		}
	}

	/**
	 * Unsubscribe an object from this model
	 * 
	 * @param obj
	 */
	public void unsubscribe(Object obj) {
		if (obj instanceof VariableUpdatedListener) {
			variableUpdatedEvent.unsubscribe((VariableUpdatedListener) obj);
		}
		if (obj instanceof EdgeUpdatedListener) {
			edgeUpdatedEvent.unsubscribe((EdgeUpdatedListener) obj);
		}
		if (obj instanceof ModelUpdatedListener) {
			modelUpdatedEvent.unsubscribe((ModelUpdatedListener) obj);
		}
	}

	/**
	 * @return the trialAccessor
	 */
	public TrialDao getTrialDao() {
		return trialDao;
	}

	/**
	 * This is the current upper bound on the timesteps that we can generate in
	 * equations this is generally represented at T
	 * 
	 * @return the maxNumberSlices
	 */
	public int getMaxNumberSlices() {
		return maxNumberSlices;
	}

	/**
	 * Sets up the upper bound to the number of timeslices we should generate
	 * now
	 * 
	 * @param maxNumberSlices
	 *            the maxNumberSlices to set
	 */
	public void setMaxNumberSlices(int maxNumberSlices) {
		this.maxNumberSlices = maxNumberSlices;
		while (maxNumberSlices < slices.size()) {
			slices.remove(slices.size() - 1);
		}
	}

	/**
	 * @return the maxSlicesInMemory
	 */
	public int getMaxSlicesInMemory() {
		return maxSlicesInMemory;
	}

	/**
	 * @param maxSlicesInMemory
	 *            the maxSlicesInMemory to set
	 */
	public void setMaxSlicesInMemory(int maxSlicesInMemory) throws Exception {
		throw new Exception(
				"Marginalization of old timeslices not done yet, do not change the maxslices in memory");
		// this.maxSlicesInMemory = maxSlicesInMemory;
	}

	/**
	 * @return the numberSlices
	 */
	public int getNumberTemplateSlices() {
		return numberTemplateSlices;
	}

	/**
	 * @return the junctionTreeTemplate
	 */
	public JunctionTreeTemplate getJunctionTreeTemplate() {
		return junctionTreeTemplate;
	}

	public List<BayesNetSlice> getTransientSlices() {
		return Collections.unmodifiableList(slices);
	}

	public void storeState() {

		List<BayesNetSlice> slices = getTransientSlices();
		for (BayesNetSlice slice : slices) {
			for (TransientVariable tv : slice.getVariables()) {
				tv.storeEvidenceState();
			}
		}
	}

	public void restoreState() {

		List<BayesNetSlice> slices = getTransientSlices();
		for (BayesNetSlice slice : slices) {
			for (TransientVariable tv : slice.getVariables()) {
				tv.restoreEvidenceState();
			}
		}
	}

	@Override
	public void statesUpdated(DiscreteVariable variable) {
		for (RandomVariable rv : variable.getChildren(0)) {
			rv.clearCpd(0);
		}
		junctionTreeTemplate.setStale(true);
	}

}
