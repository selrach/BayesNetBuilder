package selrach.bnetbuilder.model.variable;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This is the transient representation of CliqueSeparators, specified for a
 * particular time.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class TransientCliqueSeparator implements
		Comparable<TransientCliqueSeparator> {

	static private Logger logger = Logger
			.getLogger(TransientCliqueSeparator.class);
	final private CliqueSeparator reference;
	private Factor factor;
	final private TransientClique cliqueA;
	final private TransientClique cliqueB;
	final private Set<TransientVariable> members = new HashSet<TransientVariable>();

	private boolean cut = false;
	private final int slice;

	public TransientCliqueSeparator(TransientClique a, TransientClique b)
			throws Exception {
		slice = -1;
		reference = null;
		this.cliqueA = a;
		this.cliqueB = b;
		this.members.addAll(a.getMembers().values());
		this.members.addAll(b.getMembers().values());
		this.factor = new Factor(members);
	}

	public TransientCliqueSeparator(int slice, Set<TransientVariable> members,
			TransientClique a, TransientClique b, Factor factor) {
		this.slice = slice;
		this.reference = null;
		this.factor = factor;
		this.cliqueA = a;
		this.cliqueB = b;
		this.members.addAll(members);
	}

	public TransientCliqueSeparator(int slice, CliqueSeparator reference,
			TransientClique a, TransientClique b) throws Exception {
		this.slice = slice;
		this.reference = reference;
		this.factor = new Factor(slice, reference.getPotential());
		this.cliqueA = a;
		this.cliqueB = b;
		for (GraphVariable gv : reference.getMembers()) {
			TransientVariable tv = gv.getReference().getTransientVariable(
					slice - gv.getSlice());
			members.add(tv);
		}
		if (cliqueA.getReference() != reference.getCliqueA()
				|| cliqueB.getReference() != reference.getCliqueB()) {
			throw new Exception(
					"clique separator and transientclique separator are refering to different cliques");
		}

	}

	public Factor getFactor() {
		return this.factor;
	}

	public void setFactor(Factor factor) {
		this.factor = factor;
	}

	public void resetPotential() {
		try {
			if (slice >= 0) {
				this.factor = new Factor(slice, reference.getPotential());
			} else {
				// this.factor = new Factor();
			}
		} catch (Exception ex) {
			logger.debug("Problem resetting factor.", ex);
		}
	}

	public CliqueSeparator getReference() {
		return reference;
	}

	public Set<TransientVariable> getMembers() {
		return members;
	}

	/**
	 * @return the cliqueA
	 */
	public TransientClique getCliqueA() {
		return cliqueA;
	}

	/**
	 * @return the cliqueB
	 */
	public TransientClique getCliqueB() {
		return cliqueB;
	}

	public int compareTo(TransientCliqueSeparator o) {
		if (reference == null) {
			if (o.reference == null) {
				return 0;
			} else {
				return -1;
			}
		} else {
			if (o.reference == null) {
				return 1;
			}
		}
		return reference.compareTo(o.reference);
	}

	public void setCut(boolean b) {
		cut = b;
	}

	public boolean isCut() {
		return cut;
	}

}
