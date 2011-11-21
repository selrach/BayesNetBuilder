package selrach.bnetbuilder.model.variable;

import java.util.Set;

/**
 * A CliqueSeparator contains the intersection of two cliques. This is also a
 * template class.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class CliqueSeparator implements Comparable<CliqueSeparator> {

	Set<GraphVariable> variables;
	final Clique cliqueA;
	final Clique cliqueB;

	Potential potential = null;

	public CliqueSeparator(Set<GraphVariable> variables, Clique a, Clique b) {
		this.cliqueA = a;
		this.cliqueB = b;
		this.variables = variables;
	}

	public int compareTo(CliqueSeparator c) {
		int a = Math.abs(cliqueA.compareTo(c.cliqueA))
				+ Math.abs(cliqueB.compareTo(c.cliqueB));
		int b = Math.abs(cliqueA.compareTo(c.cliqueB))
				+ Math.abs(cliqueB.compareTo(c.cliqueA));
		if (a == 0 || b == 0) {
			return 0;
		}
		return a + b;
	}

	@Override
	public String toString() {
		return cliqueA + "\nis neighbor of\n" + cliqueB + "\nby variables\n"
				+ variables;
	}

	public Potential getPotential() throws Exception {
		if (potential == null) {
			generatePotential();
		}
		return potential;
	}

	private void generatePotential() throws Exception {
		potential = new Potential(this);
	}

	/**
	 * @return the cliqueA
	 */
	public Clique getCliqueA() {
		return cliqueA;
	}

	/**
	 * @return the cliqueB
	 */
	public Clique getCliqueB() {
		return cliqueB;
	}

	/**
	 * @return the variables
	 */
	public Set<GraphVariable> getMembers() {
		return variables;
	}

}
