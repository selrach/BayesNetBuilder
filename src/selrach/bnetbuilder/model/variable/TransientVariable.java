package selrach.bnetbuilder.model.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Gaussian;
import selrach.bnetbuilder.model.distributions.unconditional.Table;

/**
 * Realization variable of a template random variable for a particular time.
 * 
 * @author Charles Robertson
 */
public class TransientVariable implements Comparable<TransientVariable> {

	/**
	 * reference to the template randomvariable that this transientvariable is
	 * wrapping.
	 */
	private final RandomVariable reference;

	/**
	 * Parents of this transient variable. Includes both inter and intra slices
	 */
	private final List<TransientVariable> parents;

	/**
	 * Children of this transient variable. Includes intra slices and inter slices
	 * with the caveat that inter slice links are only created if the slice that
	 * contains the variables that the interlink goes to is created. This allows
	 * us to fully expand a 0:T network and not have to worry about dropping off
	 * the world by trying to access T+1 (T is max time). It also allows us to
	 * utilize the same network realization in a filtering or smoothing atmosphere
	 */
	private final List<TransientVariable> children;

	/**
	 * This is the current evidence that this transient variable is set to.
	 */
	private Double value = null;

	/**
	 * This is the marginal distribution in respect to the current setting of
	 * variables in the network.
	 */
	private UnconditionalDistribution marginal = null;

	/**
	 * This is the most recently computed marginal in the absence of evidence of
	 * this variable. P(this variable)
	 */
	private UnconditionalDistribution prior;

	/**
	 * This is a list of all samples that we have taken for this variable with
	 * each entry corresponding to the sample number...This is for use in things
	 * like Gibbs Sampling in order to compute the expectation of this variable
	 */
	private final List<Double> samples = new ArrayList<Double>();

	private int time = -1;
	private int templateSlice = -1;
	private boolean cut = false;

	private enum Type {
		EVIDENCE, QUERY, HIDDEN
	}

	private Type type = Type.HIDDEN;
	

	private Double storedValue = null;
	private Type storedType = Type.HIDDEN;

	/**
	 * Default constructor for a transient variable
	 * 
	 * @param reference
	 *          the variable that we are mirroring
	 * @param time
	 *          which timeslice are we generating this for?
	 */
	public TransientVariable(RandomVariable reference, int time) {
		this.time = time;
		this.reference = reference;
		int maxTemp = reference.getNumberPotentialCpds();
		if (maxTemp > time) {
			maxTemp = time;
		}
		this.templateSlice = maxTemp;
		this.parents = new ArrayList<TransientVariable>();
		this.children = new ArrayList<TransientVariable>();
		for (int i = 0; i <= maxTemp; i++) {
			List<RandomVariable> sliceParents = reference.getParentsAt(i);
			for (RandomVariable p : sliceParents) {
				TransientVariable parent = p.getTransientVariable(time - i);
				parents.add(parent);
				parent.children.add(this);
			}
		}
	}

	/**
	 * Sets the evidence on this variable and notifies listeners
	 * 
	 * @param value
	 */
	public void setEvidence(Double value) {
		setEvidence(value, true);
	}

	/**
	 * Sets evidence on this variable
	 * 
	 * @param value
	 * @param announce
	 *          should we notify listeners?
	 */
	public void setEvidence(Double value, boolean announce) {

		if (this.value != value) {
			if (value == null) {
				this.value = null;
				this.type = Type.HIDDEN;
			} else {
				this.value = new Double(value);
				this.type = Type.EVIDENCE;
			}
			if (announce) {
				reference.notifyEvidenceSet();
			}
		}
	}

	/**
	 * This sets the variable as a query variable and announces it to interested
	 * parties
	 * 
	 */
	public void setQuery() {
		setQuery(true);
	}

	/**
	 * This sets the variable as a query variable
	 * 
	 * @param announce
	 *          should we notify the listeners?
	 */
	public void setQuery(boolean announce) {
		this.type = Type.QUERY;
		if (this.value != null) {
			this.value = null;
			if (announce) {
				reference.notifyEvidenceSet();
			}
		}
	}

	/**
	 * Is this variable a query variable?
	 * 
	 * @return
	 */
	public boolean isQuery() {
		return type == Type.QUERY;
	}

	/**
	 * Is this variable a hidden variable?
	 * 
	 * @return
	 */
	public boolean isHidden() {
		return this.type == Type.HIDDEN;
	}

	/**
	 * Gets the evidence on this variable
	 * 
	 * @return
	 */
	public Double getEvidence() {
		return value;
	}

	/**
	 * Checks to see if there has been evidence assigned to this variable
	 * 
	 * @return
	 */
	public boolean isEvidence() {
		return type == Type.EVIDENCE;
	}

	/**
	 * Clears any evidence that is on this transient variable and notifies the
	 * listeners
	 */
	public void setHidden() {
		setHidden(true);
	}

	/**
	 * Clears the evidence
	 * 
	 * @param announce
	 *          should we notify the listeners?
	 */
	public void setHidden(boolean announce) {
		type = Type.HIDDEN;
		if (this.value != null) {
			this.value = null;
			if (announce) {
				reference.notifyEvidenceSet();
			}
		}
	}

	/**
	 * Adds a sample to this transient variable
	 * 
	 * @param value
	 */
	public void addSample(Double value) {
		this.samples.add(new Double(value));
	}

	/**
	 * Gets a particular sample from the sample set.
	 * 
	 * @param sampleNumber
	 * @return
	 */
	public Double getSample(int sampleNumber) {
		if (sampleNumber > samples.size() || sampleNumber < 0) {
			return null;
		}
		return samples.get(sampleNumber);
	}

	/**
	 * Gets all the samples generated
	 * 
	 * @return
	 */
	public List<Double> getSamples() {
		return this.samples;
	}

	/**
	 * Clears out the sample list.
	 */
	public void clearSamples() {
		samples.clear();
	}

	/**
	 * Grabs a value from this variable. If there has been evidence assigned to it
	 * it returns that value, otherwise it returns a random sample from the prior.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Double getValue() throws Exception {
		if (value != null) {
			return value;
		}
		if (prior != null) {
			return prior.sample().getQuick(0);
		}
		return null;
	}

	/**
	 * @return the reference
	 */
	public RandomVariable getReference() {
		return reference;
	}

	public ConditionalDistribution getDistribution() throws Exception {
		return reference.getCpd(this.templateSlice);
	}

	/**
	 * @return the parents
	 */
	public List<TransientVariable> getParents() {
		return Collections.unmodifiableList(parents);
	}

	/**
	 * @return the children
	 */
	public List<TransientVariable> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * @return the timeslice
	 */
	public int getTime() {
		return time;
	}

	public int compareTo(TransientVariable tv) {
		if (tv.time == this.time) {
			return reference.compareTo(tv.reference);
			//return tv.reference.getId().compareTo(reference.id);
		} else {
			return time - tv.time;
		}
	}

	public void setCut(boolean cut) {
		this.cut = cut;
	}

	public boolean isCut() {
		return this.cut;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Transient Variable:\n  name:\t");
		sb.append(reference.getName());
		sb.append("__");
		sb.append(time);
		sb.append("\n  isCut:\t");
		sb.append(cut);
		sb.append("\n  Type:\t");
		sb.append(type);

		return sb.toString();
	}

	/**
	 * @return the marginal
	 */
	public UnconditionalDistribution getMarginal() {
		return marginal;
	}

	/**
	 * @param marginal
	 *          the marginal to set
	 */
	public void setMarginal(UnconditionalDistribution marginal) {
		this.marginal = marginal;
	}
	
	
	public void storeEvidenceState()
	{
		this.storedValue = value;
		this.storedType = type;
	}
	
	public void restoreEvidenceState()
	{
		this.value = storedValue;
		this.type = storedType;
	}

	public void generateMarginalFromSamples() throws Exception {

		if(samples.size() < 3)
		{
			throw new Exception("Sample size too small.");
		}
		
		if (reference instanceof DiscreteVariable) {
			double[] probabilites = new double[((DiscreteVariable) reference)
					.getStates().size()];
			for (Double sample : samples) {
				int index = sample.intValue();
				probabilites[index] += 0.001;
			}

			UnconditionalDistribution marginal = new Table(probabilites);
			((Table) marginal).normalize();
			setMarginal(marginal);
		} else if (reference instanceof ContinuousVariable) {
			//From Knuth
			double n = 0;
			double mean = 0;
			double m_2 = 0;
			for(Double smp : samples)
			{
				n++;
				double delta = smp - mean;
				mean += delta / n;
				m_2 += delta*(smp - mean);
			}
			double unbiased_variance = m_2 / (n-1);			
			setMarginal(new Gaussian(mean, unbiased_variance));
		}
	}

	public String getId() {
		return reference.getId() + "_" + time;
	}
	
}
