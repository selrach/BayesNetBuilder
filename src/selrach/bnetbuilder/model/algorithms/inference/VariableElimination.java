package selrach.bnetbuilder.model.algorithms.inference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.exceptions.QueryVariableNotSetException;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * Variable elimination algorithm
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class VariableElimination implements InferenceAlgorithm {

	private static final Logger logger = Logger
			.getLogger(VariableElimination.class);

	private static final VariableElimination instance = new VariableElimination();

	private VariableElimination() {
	}

	public static VariableElimination getInstance() {
		return instance;
	}

	PrintStream out;
	boolean doOut = false;

	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			PrintStream updateTracking) throws Exception {
		this.out = updateTracking;
		this.doOut = this.out != null;
		if (allMarginals) {
			// Here we run execute once for each non-evidence variable in the
			// model...this is really expensive
			model.storeState();
			for (int i = model.getMaxNumberSlices() - 1; i >= 0; i--) {
				List<TransientVariable> vars = new ArrayList<TransientVariable>(
						model.getSlice(i).getVariables());
				for (TransientVariable tv : vars) {
					if (!tv.isEvidence()) {
						tv.setHidden();
					}
				}
			}
			for (int i = model.getMaxNumberSlices() - 1; i >= 0; i--) {
				List<TransientVariable> vars = new ArrayList<TransientVariable>(
						model.getSlice(i).getVariables());
				for (TransientVariable tv : vars) {
					if (!tv.isEvidence()) {
						tv.setQuery();
						Factor f = doQuery(model);
						tv.setMarginal(DistributionFactory.downgradeCPD(f
								.getDistribution()));
						tv.setHidden();
					}
				}
			}
			model.restoreState();
			return null;
		} else {
			return doQuery(model);
		}
	}

	/**
	 * Run Variable elimination on the network Its assumed that the network has
	 * already been unrolled and we have a set of query and evidence values
	 * given by thier transient variable valueset
	 * 
	 * @param model
	 * @return a factor that represents the query variables
	 * @throws Exception
	 */
	public Factor execute(DynamicBayesNetModel model, PrintStream updateTracking)
			throws Exception {
		return execute(model, false, updateTracking);
	}

	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			Map<String, Object> additionalProperties, PrintStream updateTracking)
			throws Exception {
		return execute(model, allMarginals, updateTracking);
	}

	private Factor doQuery(DynamicBayesNetModel model) throws Exception {
		List<TransientVariable> variables = new ArrayList<TransientVariable>();
		for (int i = model.getMaxNumberSlices() - 1; i >= 0; i--) {
			List<TransientVariable> vars = new ArrayList<TransientVariable>(
					model.getSlice(i).getVariables());
			Collections.reverse(vars);
			variables.addAll(vars);
		}

		// Our variable list is sorted from leaves to roots, we can look and cut
		// any extraneous nodes. Also since continuous variables *never* have
		// discrete children, we push all the continuous variables up as high as
		// possible

		List<TransientVariable> discrete = new ArrayList<TransientVariable>();
		List<TransientVariable> continuous = new ArrayList<TransientVariable>();
		Set<TransientVariable> continuousEvidence = new HashSet<TransientVariable>();

		boolean hasQuery = false;
		for (TransientVariable tv : variables) {
			if(tv.isHidden())
			//if ((tv.isHidden() || (queryVariable != null && !tv.isEvidence()))
			//		&& queryVariable != tv)
			// We are hidden, or we are a non-evidence node and we don't care if
			// the query values are set
			{
				List<TransientVariable> children = tv.getChildren();
				boolean cut = true;
				for (TransientVariable child : children) {
					if (!child.isCut()) {
						cut = false;
						break;
					}
				}
				tv.setCut(cut);
			} else {
				// We are an evidence or query variable
				tv.setCut(false);
			}
			if (tv.isQuery()) {
				hasQuery = true;
			}
			if (tv.getReference() instanceof ContinuousVariable) {
				continuous.add(tv);
				if (tv.isEvidence()) {
					continuousEvidence.add(tv);
				}
			} else {
				discrete.add(tv);
			}
			logger.debug(tv);
		}

		variables.clear();
		variables.addAll(continuous);
		variables.addAll(discrete);
		if (!hasQuery) {
			throw new QueryVariableNotSetException("No Query Variables set!");
		}

		// We have trimmed out all unneccessary leafs and know that there is a
		// query

		List<Factor> factors = new ArrayList<Factor>();
		for (TransientVariable tv : variables) {
			if (tv.isCut()) {
				continue;
			}

			factors.add(new Factor(tv));

			if ((!tv.isQuery()) 
					// We are marginalizing over a particular variable
			) {
				factors = sumOut(tv, factors);
			}
			/*
			 * if ((tv.isHidden() || (queryVariable != null &&
			 * !tv.isEvidence())) && queryVariable != tv) // Its a hidden
			 * variable { factors = sumOut(tv, factors); }
			 */
		}
		Factor mult = null;
		for (Factor f : factors) {
			if (mult == null) {
				mult = f;
			} else {
				mult = Factor.combine(mult, f);
				if(mult.getTailDependencies().isEmpty())
				{
					List<TransientVariable> headCopy = new ArrayList<TransientVariable>(
							mult.getHeadDependencies());
					for (TransientVariable tv : headCopy) {
						if (tv.isHidden()) {
		
							if (logger.isDebugEnabled()) {
								logger.debug("Marginalizing out variable...");
								logger.debug(mult);
							}
							mult.marginalizeOut(tv);
						}
					}
				}
			}
		}

		// We have the final factor
		if (mult.getDistribution() instanceof Table) {
			((Table) mult.getDistribution()).normalize();
		}

		logger.debug(mult.toString());

		return mult;
	}

	private List<Factor> sumOut(TransientVariable tv, List<Factor> factors) throws Exception {
		Factor mult = null;
		List<Factor> newList = new ArrayList<Factor>();
		for (Factor f : factors) {
			// Push the variable in
			if (f.dependsOn(tv)) // We should have at least one dependency
			{
				if (mult == null) {
					mult = f;
				} else {
					mult = Factor.combine(mult, f);
				}

			} else // Push variable out of summation
			{
				newList.add(f);
			}
		}

		if (mult != null) // If we successfully grabbed and created a joint
		// factor
		{
			if (!tv.isQuery()) {
				mult.marginalizeOut(tv); // Sum it out
			}
			newList.add(mult); // Add it to the end
		}
		return newList; // Return the new factor list
	}

	public String getName() {
		return "Variable Elimination";
	}

	public boolean isRandom() {
		return false;
	}

	public Factor getFactorForSufficientStatistics(DynamicBayesNetModel model,
			TransientVariable variable, Map<String, Object> additionalProperties) {
		// not used for now, must be implemented
		return null;
	}

	@Override
	public void evidenceSet(DynamicBayesNetModel model) {
		// does not affect algorithm

	}

	@Override
	public void parameterLearningDone(DynamicBayesNetModel model) {
		// does not affect algorithm
	}
}
