package selrach.bnetbuilder.model.algorithms.inference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.BayesNetSlice;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.exceptions.QueryVariableNotSetException;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.Random;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.random.Uniform;

/**
 * Implements a Gibbs Sampler on our DBN. Technically this will always be a
 * smoother, but depending on how we set this up the slices before and after the
 * one passed into the system will essentially make this either a smoother, a
 * predictor, or a filter. Fixed-interval smoother - have n:T slices set up
 * already. Fixed-lag smoother - have n:t slices set up with slice L being the
 * slice of interest Predictor - have n:t+H slices set up with H being the slice
 * of interest Filter - have n:T slices set up. the nth slice is simply a slice
 * that has completely separated the past from the future through
 * marginalization. H slices is how far in the future we want to predict. These
 * future slices won't have evidence on them. L is how far back we want to lag
 * in our estimations
 * 
 * All examples need evidence for the current run set up before starting.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class GibbsSampler implements InferenceAlgorithm {

	private static final Logger logger = Logger.getLogger(GibbsSampler.class);

	private static final GibbsSampler instance = new GibbsSampler();

	private GibbsSampler() {
	}

	public static GibbsSampler getInstance() {
		return instance;
	}

	PrintStream out;
	boolean doOut = false;

	/**
	 * Does the gibbs sampler
	 * 
	 * @param slices
	 *            - an instantiation of all slices of interest in temporal order
	 * @param maxSamples
	 *            - maximum amount of times the sampler should sample each
	 *            variable in the network.
	 */
	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			int burnInTime, int maxSamples, PrintStream updateTracking)
			throws Exception {
		// all variables should be topologically sorted.
		this.out = updateTracking;
		this.doOut = this.out != null;

		if (burnInTime >= maxSamples) {
			throw new Exception(
					"burn in time must be less than max sample size.");
		}

		List<TransientVariable> nonevidenceVariables = new ArrayList<TransientVariable>();
		List<TransientVariable> evidenceVariables = new ArrayList<TransientVariable>();
		List<TransientVariable> queryVariables = new ArrayList<TransientVariable>();
		List<List<Double>> querySamples = new ArrayList<List<Double>>();

		for (int i = 0; i < model.getMaxNumberSlices(); i++) {
			BayesNetSlice slice = model.getSlice(i);
			for (TransientVariable tv : slice.getVariables()) {
				if (tv.getTime() != i) {
					continue;
				}
				tv.clearSamples(); // We need to clear out any samples that may
									// have
				// been generated before us.
				if (tv.isEvidence()) {
					evidenceVariables.add(tv);
				} else {
					nonevidenceVariables.add(tv);
					if (tv.isQuery()) {
						queryVariables.add(tv);
					}
					try {
						// We need to do an initial assignment of evidence
						// So we make sure everything is conditioned right on
						// this matrix then set the evidence.
						// Since this should be topologically sorted and we are
						// doing
						// Gibbs sampling, there should be evidence at every
						// parent.
						// We don't even care that its using any children
						// evidence right now
						// which makes this assignment really easy. This is
						// because we just
						// want an initial assignment that is in the ballpark of
						// the actual
						// answer.
						DoubleMatrix1D parentValues = DoubleFactory1D.dense
								.make(tv.getDistribution()
										.getNumberParentDimensions());
						int k = 0;
						for (TransientVariable p : tv.getParents()) {
							Double ev = p.getEvidence(); // This should always
															// work,
							// topologically sorted variables,
							// randomly assigned evidence
							parentValues.setQuick(k++, ev);
						}
						double sample = tv.getDistribution().sample(
								parentValues).getQuick(0);
						tv.setEvidence(sample, false);
					} catch (Exception ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Problem setting up sampler", ex);
						} else if (logger.isInfoEnabled()) {
							logger.info("Problem setting up sampler: "
									+ ex.getMessage());
						}
					}
				}
			}
		}

		// Okay we have initialized our network to have randomly generated
		// evidence...or static evidence if the evidence was set
		// before we came into this function. We now need to go through the
		// non-evidence nodes, sample the posterior in respect
		// to all other set nodes are repeat until convergence.

		final int numNonevidence = nonevidenceVariables.size() - 1;
		if (numNonevidence < 0) {
			return null;
		}

		if (queryVariables.size() == 0 && !allMarginals) {
			cleanup(nonevidenceVariables, queryVariables, allMarginals);
			throw new QueryVariableNotSetException();
		}

		int j = -1;
		TransientVariable nonevidence;

		Uniform random = Random.getUniform();

		boolean hasNotConverged = true;
		for (int i = 0; i < maxSamples && hasNotConverged; i++) {
			j = random.nextIntFromTo(0, numNonevidence);

			if (doOut && i % 1000 == 0) {
				out.println("Sample #" + i);
			}
			nonevidence = nonevidenceVariables.get(j);
			nonevidence.setEvidence(null, false);
			nonevidence.setQuery();
			if (logger.isDebugEnabled()) {
				logger.debug("Current Sampling Variable\n" + nonevidence);
			}
			try {
				// We know that all evidence on parents are set, so lets
				// grab the density that corresponds to being conditioned on the
				// parents
				DoubleMatrix1D pValues = DoubleFactory1D.dense.make(nonevidence
						.getDistribution().getNumberParentDimensions());
				int x = 0;
				for (TransientVariable p : nonevidence.getParents()) {
					Double pEv = p.getEvidence();
					pValues.setQuick(x++, pEv);
				}

				// UnconditionalDistribution d =
				// nonevidence.getDistribution().getDensity(pValues);
				// Now we need to take into account children, so we should
				// modify the partially modified distribution that has taken
				// into
				// account the parents with information about the children.

				Factor factor = new Factor(nonevidence);

				for (TransientVariable c : nonevidence.getChildren()) {
					factor = factor.combine(new Factor(c));
				}

				factor.normalize();

				// Now we can sample and grab the new value
				double sample = (DistributionFactory.downgradeCPD(factor
						.getDistribution())).sample().get(0);

				nonevidence.setEvidence(sample, false);

				if (i > burnInTime) {
					if (allMarginals) {
						nonevidence.addSample(sample);
					} else {
						if (queryVariables.contains(nonevidence)) {
							List<Double> sampleList = new ArrayList<Double>();
							for (TransientVariable qv : queryVariables) {
								sampleList.add(new Double(qv.getEvidence()));
							}
							querySamples.add(sampleList);
						}
					}
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Problem sampling", ex);
				} else if (logger.isInfoEnabled()) {
					logger.info("Problem sampling: " + ex.getMessage());
				}
			}
		}

		// Okay we should have a nice set of samples to analyze
		// and figure out what the answers we are looking for are.

		cleanup(nonevidenceVariables, queryVariables, allMarginals);

		Factor f = null;
		if (allMarginals) {
			for (TransientVariable tv : nonevidenceVariables) {
				tv.generateMarginalFromSamples();
			}
		} else {
			f = new Factor(queryVariables, querySamples);
		}

		return f;
	}

	// Let's put the network back to how we found it except for leaving the
	// samples around
	private void cleanup(List<TransientVariable> nonevidenceVariables,
			List<TransientVariable> queryVariables, boolean allMarginals) {
		for (int i = 0; i < nonevidenceVariables.size(); i++) {
			TransientVariable tv = nonevidenceVariables.get(i);
			if (queryVariables.contains(tv)) {
				tv.setQuery(false);
			} else {
				tv.setHidden(false);
			}
		}
	}

	/**
	 * Execute for Gibbs Sampler requires additional properties of:
	 * RandomizedAlgorithmConstants.BURN_IN_TIME
	 * RandomizedAlgorithmConstants.MAX_SAMPLES
	 */
	public Factor execute(DynamicBayesNetModel model, boolean allMarginals,
			Map<String, Object> additionalProperties, PrintStream updateTracking)
			throws Exception {
		int burnInTime = 10000;
		int maxSamples = 100000;
		if (additionalProperties
				.containsKey(RandomizedAlgorithmConstants.BURN_IN_TIME
						.toString())) {
			burnInTime = (Integer) additionalProperties
					.get(RandomizedAlgorithmConstants.BURN_IN_TIME.toString());
		}
		if (additionalProperties
				.containsKey(RandomizedAlgorithmConstants.MAX_SAMPLES
						.toString())) {
			maxSamples = (Integer) additionalProperties
					.get(RandomizedAlgorithmConstants.MAX_SAMPLES.toString());
		}
		return execute(model, allMarginals, burnInTime, maxSamples,
				updateTracking);
	}

	public String getName() {
		return "Gibbs Sampler";
	}

	public boolean isRandom() {
		return true;
	}

	public Factor getFactorForSufficientStatistics(DynamicBayesNetModel model,
			TransientVariable variable, Map<String, Object> additionalProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void evidenceSet(DynamicBayesNetModel model) {
		// Doesn't care
	}

	@Override
	public void parameterLearningDone(DynamicBayesNetModel model) {
		// Doesn't care
	}

}
