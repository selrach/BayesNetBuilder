package selrach.bnetbuilder.model.variable;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;

/**
 * Represents the generic commonalities between the different types of variables
 * in the network. Essentially it embodies the connectivity as well as some
 * additional metadata
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class RandomVariable implements Serializable,
		Comparable<RandomVariable> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2121317451095856183L;

	final static private Logger logger = Logger.getLogger(RandomVariable.class);

	protected String name = null;
	protected String id = null;
	protected String description = "";
	protected int order = 0;

	/**
	 * list of cpds ordered by slice that they effect
	 */
	final protected List<ConditionalDistribution> cpds;

	/**
	 * This is a linked list of all the transient variables associated with this
	 * variable. When we add a new transient variable, we push out old ones if
	 * they can no longer be referenced within the current time slice
	 */
	final protected LinkedList<TransientVariable> transientVariables = new LinkedList<TransientVariable>();

	DynamicBayesNetModel model = null;

	protected ArrayList<GraphVariable> graphVariables = new ArrayList<GraphVariable>();

	public TransientVariable getTransientVariable(int time) {
		while (transientVariables.size() <= time) {
			createNewTransientSlice(transientVariables.size());
		}
		return transientVariables.get(time);
	}

	/**
	 * This creates a new transient slice that assumes we want the next slice in
	 * whatever series we are calculating. It keeps only the few necessary
	 * slices. When you move to a new slice you may will lose any information
	 * that is not necessary for that slice (For example if you have a 2DBN,
	 * only two slices will only ever be present and one of of those slices can
	 * just be a partial).
	 * 
	 * @param slice
	 * @return
	 */
	private TransientVariable createNewTransientSlice(int slice) {

		// Which template slice do we want? The size of the parents vector is
		// exactly
		// to the number of template slices we have
		TransientVariable ret = new TransientVariable(this, slice);

		transientVariables.add(slice, ret);
		// while(transientVariables.size() > parents.size())
		// transientVariables.pop();
		return ret;
	}

	public void clearTransientVariables() {
		transientVariables.clear();
	}

	/**
	 * These are the conditional parents of this particular node. This lists all
	 * the parents, intra and inter slice. First list wrapper is timeslice,
	 * second list wrapper is the parental references from that timeslice. The
	 * higher in the list the further back in the past it references.
	 */
	protected List<List<RandomVariable>> parents = null;

	/**
	 * This is a list of all variables that depend on this particular variable.
	 * The first list wrapper is the timeslice, the second is the child
	 * reference to that timeslice. Thi higher in the list, the further in the
	 * future it references.
	 */
	protected List<List<RandomVariable>> children = null;

	/*
	 * If we change how variables are stored, all we have to do is change this
	 * function
	 */
	private List<List<RandomVariable>> createRVList(
			List<List<RandomVariable>> from) {
		List<List<RandomVariable>> ret = new ArrayList<List<RandomVariable>>();
		if (from != null) {
			while (ret.size() != from.size()) {
				ret
						.add(new ArrayList<RandomVariable>(from
								.get(ret.size() - 1)));
			}
		} else {
			ret.add(new ArrayList<RandomVariable>());
		}
		return ret;
	}

	/*
	 * This just wraps how we create our list, arraylist right now
	 */
	private List<ConditionalDistribution> createCPDList(
			List<ConditionalDistribution> from) {
		return new ArrayList<ConditionalDistribution>(from);
	}

	protected Point location = new Point();

	/**
	 * Creates a new randomvariable with some default settings.
	 */
	public RandomVariable(DynamicBayesNetModel model) {
		id = UUID.randomUUID().toString();
		parents = createRVList(null);
		children = createRVList(null);
		cpds = new ArrayList<ConditionalDistribution>();
		this.model = model;
	}

	/**
	 * Creates a random variable with the specific settings
	 * 
	 * @param id
	 * @param name
	 * @param description
	 * @param location
	 * @param cpds
	 * @param parents
	 * @param children
	 */
	public RandomVariable(DynamicBayesNetModel model, String id, String name,
			String description, Point location,
			List<ConditionalDistribution> cpds,
			List<List<RandomVariable>> parents,
			List<List<RandomVariable>> children) throws Exception {
		if (model == null) {
			throw new Exception(
					"Cannot create a variable without a model reference");
		}
		this.model = model;
		this.id = id;
		this.name = name;
		this.description = description;
		this.cpds = createCPDList(cpds);
		this.location = (Point) location.clone();
		this.parents = createRVList(parents);
		this.children = createRVList(children);
	}

	/**
	 * Sets the suggested display location of this variable
	 * 
	 * @param where
	 */
	public void setLocation(Point where) {
		location.setLocation(where);
	}

	/**
	 * Gets the suggested display location of this variable
	 * 
	 * @return
	 */
	public Point getLocation() {
		return (Point) location.clone();
	}

	/**
	 * This returns the distribution for the desired timeslice, it is assumed
	 * that if we query for a higher timeslice than the ones that we have
	 * stored, we want the latest timeslice available. It will also generate a
	 * default cpd if a cpd is requested and there are currently no cpds defined
	 * 
	 * @param timeslice
	 * @return
	 */
	public ConditionalDistribution getCpd(int timeslice) throws Exception {
		if (cpds.size() <= timeslice) {
			while (cpds.size() <= getNumberPotentialCpds()
					&& cpds.size() <= timeslice) {
				cpds.add(DistributionFactory.getDefaultCPD(this, cpds.size()));
			}
			if (cpds.size() <= timeslice) {
				throw new Exception("CPD does not exist for " + name
						+ " at time " + timeslice);
			}
		}
		ConditionalDistribution cpd = cpds.get(timeslice);
		if (cpd == null) {
			cpds.set(timeslice, cpd = DistributionFactory.getDefaultCPD(this,
					cpds.size()));
			if (cpd == null) {
				throw new Exception("There was a problem creating a cpd for "
						+ name + " at time " + timeslice);
			}
		}
		return cpd;
	}

	/**
	 * This adds a different distribution at the given timeslice, note that the
	 * minimal amount of cpds to describe the entire timeflow should be put in
	 * here. For example, if we are describing a 2-DBN, then there should be 2
	 * cpds for each variable that relies on a prior timestep and 1 cpd for each
	 * variable that is independent in time.
	 * 
	 * At timeslice 0, there are only intraslice dependencies, for each
	 * additional timeslice, you can only have dependencies from previous
	 * timeslices. So, inherantly cpds get "bigger" as they get higher in time.
	 * 
	 * @param cpd
	 * @param timeslice
	 */
	public void setCpd(ConditionalDistribution cpd, int timeslice) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting CPD for " + name + " at time " + timeslice
					+ " as " + cpd);
		}
		while (cpds.size() <= timeslice) {
			cpds.add(null);
		}
		cpds.set(timeslice, cpd);
		model.notifyVariableUpdate(this);
	}

	public void clearCpd(int time) {
		if (logger.isDebugEnabled()) {
			logger.debug("Clearing CPD for " + name + " at time " + time);
		}
		while (cpds.size() > time) {
			cpds.remove(time);
		}
		model.notifyVariableUpdate(this);
	}

	/**
	 * This is the number of potential distributions - 1 that this variable can
	 * have associated with it, it is equal to the number of slices back that we
	 * potential have to look
	 * 
	 * @return
	 */
	public int getNumberPotentialCpds() {
		return parents.size() - 1;
	}

	/**
	 * Return all the parents of a particular timeslice, includes
	 * interdependencies Parents are stored temporally, so intratimeslice
	 * variables come before intertimeslice variables
	 * 
	 * @param timeslice
	 * @return
	 */
	public List<RandomVariable> getParents(int timeslice) {
		ArrayList<RandomVariable> ret = new ArrayList<RandomVariable>();
		for (int i = 0; i <= timeslice && parents.size() > i; i++) {
			ret.addAll(parents.get(i));
		}
		return Collections.unmodifiableList(ret);
	}

	/**
	 * Only returns parents from the desired time slice
	 * 
	 * @param timeslice
	 * @return
	 */
	public List<RandomVariable> getParentsAt(int timeslice) {
		if (timeslice < parents.size()) {
			return Collections.unmodifiableList(parents.get(timeslice));
		}
		return Collections.emptyList();
	}

	public boolean hasParentsAt(int timeslice) {
		if (timeslice < parents.size()) {
			return parents.get(timeslice).size() > 0;
		}
		return false;
	}

	public int getMaxChildrenTemplateSlices() {
		return children.size();
	}

	public int getMaxParentTemplateSlices() {
		return parents.size();
	}

	public void setParents(List<List<RandomVariable>> parents) {
		this.parents = createRVList(parents);
		for (List<RandomVariable> list : parents) {
			Collections.sort(list);
		}
	}

	/**
	 * Removing a parent invalidates the cpd for the time that parent existed in
	 * and all following times because one of our dependencies has left. We
	 * don't regenerate anything CPD-wise, we rely on the model to set our CPDs
	 * up again to get to a consistent state
	 * 
	 * @param parent
	 * @param timeslice
	 */
	public void removeParent(RandomVariable parent, int timeslice)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to remove parent " + parent + " for "
					+ name + " at time " + timeslice);
		}
		if (timeslice > parents.size()) {
			throw new Exception("There are no parents at that timeslice: "
					+ timeslice);
		}
		if (!parents.get(timeslice).contains(parent)) {
			throw new Exception(
					"That parent does not exist at this timeslice: "
							+ timeslice);
		}
		parents.get(timeslice).remove(parent);
		boolean moreHigher = false;
		for (int i = parents.size() - 1; i >= timeslice; i--) {
			if (cpds.size() > i) {
				cpds.set(i, null);
			}
			if (!moreHigher && i != 0) {
				if (parents.size() == 0) {
					parents.remove(i);
					if (cpds.size() > i) {
						cpds.remove(i);
					}
				} else {
					moreHigher = true;
				}
			}
		}
		parent.removeChild(this, timeslice);

		if (logger.isDebugEnabled()) {
			logger.debug("successful");
		}
	}

	/**
	 * Adding a parent invalidates any cpds for that timeslice or higher.
	 * 
	 * @param parent
	 * @param timeslice
	 */
	public void addParent(RandomVariable parent, int timeslice) {

		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to add parent " + parent + " for " + name
					+ " at time " + timeslice);
		}
		while (parents.size() < timeslice + 1) {
			parents.add(new ArrayList<RandomVariable>());
		}
		parents.get(timeslice).add(parent);
		Collections.sort(parents.get(timeslice));
		/*
		 * Collections.sort(parents.get(timeslice), new
		 * Comparator<RandomVariable>() { public int compare(RandomVariable o1,
		 * RandomVariable o2) { return o1.getOrder() - o2.getOrder(); } });
		 */
		parent.addChild(this, timeslice);

		if (logger.isDebugEnabled()) {
			logger.debug("successful");
		}
	}

	private void addChild(RandomVariable child, int timeslice) {
		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to add child " + child + " for " + name
					+ " at time " + timeslice);
		}
		while (children.size() < timeslice + 1) {
			children.add(new ArrayList<RandomVariable>());
		}
		children.get(timeslice).add(child);
		Collections.sort(children.get(timeslice));
		/*
		 * Collections.sort(children.get(timeslice), new
		 * Comparator<RandomVariable>() { public int compare(RandomVariable o1,
		 * RandomVariable o2) { return o1.getOrder() - o2.getOrder(); } });
		 */
		if (logger.isDebugEnabled()) {
			logger.debug("successful");
		}
	}

	private void removeChild(RandomVariable child, int timeslice)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to remove child " + child + " for " + name
					+ " at time " + timeslice);
		}
		if (timeslice > children.size()) {
			throw new Exception("There are no parents at that timeslice: "
					+ timeslice);
		}
		if (!children.get(timeslice).contains(child)) {
			throw new Exception(
					"That parent does not exist at this timeslice: "
							+ timeslice);
		}
		children.get(timeslice).remove(child);

		for (int i = children.size() - 1; i >= timeslice; i--) {
			if (children.get(i).size() == 0 && i != 0) {
				children.remove(i);
			} else {
				break;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("successful");
		}
	}

	public List<RandomVariable> getChildren(int timeslice) {
		ArrayList<RandomVariable> ret = new ArrayList<RandomVariable>();
		for (int i = 0; i <= timeslice && children.size() > i; i++) {
			ret.addAll(children.get(i));
		}
		return Collections.unmodifiableList(ret);

	}

	public List<RandomVariable> getChildrenAt(int timeslice) {
		if (timeslice < children.size()) {
			return Collections.unmodifiableList(children.get(timeslice));
		}
		return Collections.emptyList();
	}

	public boolean hasChildrenAt(int timeslice) {
		if (timeslice < children.size()) {
			return children.get(timeslice).size() > 0;
		}
		return false;
	}

	public boolean isValid() throws Exception {
		if (cpds.size() == 0) {
			return false;
		}
		if (cpds.size() != parents.size()) {
			return false;
		}
		int sz = cpds.size();
		for (int i = 0; i < sz; i++) {
			ConditionalDistribution d = cpds.get(i);
			if (d == null) {
				return false;
			}
			if (d.getNumberParentDimensions() != parents.get(i).size()) {
				throw new Exception(
						"CPD dimension variable mismatch.  How did this happen?");
			}
		}
		return true;
	}

	/**
	 * Makes sure that parents and children remove links to this variable
	 */
	public void clearRelations() throws Exception {
		while (parents.get(0).size() > 0) {
			removeParent(parents.get(0).get(0), 0);
		}
		while (children.get(0).size() > 0) {
			children.get(0).get(0).removeParent(this, 0);
		}
		parents.clear();
		children.clear();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<List<DistributionDescriptor>> getDistributionDescriptor(
			int timeslice) {

		List<List<DistributionDescriptor>> stat = null;
		try {
			stat = getCpd(timeslice).getDistributionDescriptor();

		} catch (Exception ex) {

			if (logger.isDebugEnabled()) {
				logger.debug("", ex);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(stat);
		}

		return stat;
	}

	public void setDistributionDescriptor(int timeslice,
			List<List<DistributionDescriptor>> descriptor) {

		if (logger.isDebugEnabled()) {
			logger.debug(descriptor);
		}

		try {
			getCpd(timeslice).setDistributionDescriptor(descriptor);

		} catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("", ex);
			}
		}

	}

	@Override
	public String toString() {
		return name;
	}

	public abstract RandomVariable copy();

	public abstract String getType();

	public abstract double getValue() throws Exception;

	public abstract void setValue(double v) throws Exception;

	public abstract void clearValue();

	public abstract boolean hasValue();

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getXMLDescription() {
		StringBuilder sb = new StringBuilder("<var id=\"");
		sb.append(id);
		sb.append("\" name=\"");
		sb.append(name);
		sb.append("\" type=\"");
		sb.append(getType());
		sb.append("\" xpos=\"");
		sb.append(getLocation().x);
		sb.append("\" ypos=\"");
		sb.append(getLocation().y);
		sb.append("\" order=\"");
		sb.append(getOrder());
		sb.append("\">\n");
		sb.append("<description>");
		sb.append(getDescription());
		sb.append("</description>\n");
		sb.append(getVariableSpecificXML());
		sb.append("</var>\n");
		if (logger.isDebugEnabled()) {
			logger.debug("\n" + sb.toString() + "\n");
		}
		return sb.toString();
	}

	public String getXMLDistributionDescription() throws Exception {
		StringBuilder sb = new StringBuilder();
		if (this.cpds != null && this.cpds.size() != 0) {
			StringBuilder pSb = new StringBuilder();
			for (int i = 0; i < this.cpds.size(); i++) {
				ConditionalDistribution cpd = this.cpds.get(i);
				if (cpd != null) {
					sb.append("<dist type=\"");
					sb.append(cpd.getType());
					sb.append("\">\n");

					sb.append("<condset>\n");
					List<RandomVariable> parents = this.getParentsAt(i);
					for (int j = 0; j < parents.size(); j++) {
						pSb.append("<cond id=\"");
						pSb.append(parents.get(j).getId());
						pSb.append("\" time=\"");
						pSb.append(i);
						pSb.append("\" />\n");
					}
					sb.append(pSb.toString());
					sb.append("</condset>\n");
					sb.append("<private name=\"");
					sb.append(id);
					sb.append("\" time=\"");
					sb.append(i);
					sb.append("\" />\n");
					sb.append(cpd.getXMLDescription());
					sb.append("</dist>\n");
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("\n" + sb.toString() + "\n");
		}
		return sb.toString();
	}

	protected String getVariableSpecificXML() {
		return "";
	};

	public void notifyEvidenceSet() {

		model.notifyEvidenceSet(this);
	}

	public void initializeSufficientStatistic() throws Exception {
		for (ConditionalDistribution d : cpds) {
			d.initializeSufficientStatistics();
		}
	}

	public int compareTo(RandomVariable rv) {
		return order - rv.order;// id.compareTo(rv.id);
	}
}