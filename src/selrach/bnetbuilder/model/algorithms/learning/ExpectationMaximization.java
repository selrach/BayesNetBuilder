package selrach.bnetbuilder.model.algorithms.learning;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.data.TrialDao;
import selrach.bnetbuilder.model.BayesNetSlice;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.inference.InferenceAlgorithm;
import selrach.bnetbuilder.model.algorithms.inference.InferenceAlgorithmFactory;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * Implements the basic ExpectationMaximization algorithm. Any inference engine
 * can be used as the subroutine.
 * 
 * Optional parameters include: 
 * LearningConstants.MAX_ITERATIONS Maximum times we loop EM before we stop, defaults to 1000 
 * LearningConstants.TOLERANCE What is the convergence threshold, defaults to 0.0001
 * LearningConstants.INFERENCE_ALGORITHM What inference algorithm, defaults to Junction Tree
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ExpectationMaximization implements LearningAlgorithm {

	private static ExpectationMaximization instance = new ExpectationMaximization();

	private ExpectationMaximization() {
	}

	public static ExpectationMaximization getInstance() {
		return instance;
	}

	PrintStream out;
	boolean doOut = false;

	public void execute(DynamicBayesNetModel model,
			Map<String, Object> additionalProperties, PrintStream updateTracking)
			throws Exception {
		int maxSteps = 1000;
		double tolerance = 0.0001;
		this.out = updateTracking;
		this.doOut = this.out != null;
		InferenceAlgorithm inferenceAlgorithm = InferenceAlgorithmFactory
				.getAlgorithm("Junction Tree");
		if (additionalProperties.containsKey(LearningConstants.MAX_ITERATIONS
				.toString())) {
			maxSteps = (Integer) additionalProperties
					.get(LearningConstants.MAX_ITERATIONS.toString());
		}
		if (additionalProperties.containsKey(LearningConstants.TOLERANCE
				.toString())) {
			tolerance = (Double) additionalProperties
					.get(LearningConstants.TOLERANCE.toString());
		}
		if (additionalProperties
				.containsKey(LearningConstants.INFERENCE_ALGORITHM.toString())) {
			inferenceAlgorithm = InferenceAlgorithmFactory
					.getAlgorithm((String) additionalProperties
							.get(LearningConstants.INFERENCE_ALGORITHM
									.toString()));
		}

		Date start = new Date();
		if (doOut) {
			out.println("Starting Expectation Maximization...");
		}
		expectationMaximization(model, inferenceAlgorithm, maxSteps, tolerance);
		if (doOut) {
			out.println("Learning done, took "
					+ (((new Date()).getTime() - start.getTime()) / 1000)
					+ " seconds");
		}

	}

	private void expectationMaximization(DynamicBayesNetModel model,
			InferenceAlgorithm inferenceAlgorithm, int maxSteps,
			double tolerance) throws Exception {

		boolean notConverged = true;
		int stepNumber = 0;
		model.storeState();

		while (notConverged && stepNumber < maxSteps) {
			if (doOut) {
				out.println("Step " + stepNumber);
			}
			computeExpectedSufficientStatistics(model, inferenceAlgorithm);
			notConverged = maximization(model, tolerance);
			inferenceAlgorithm.parameterLearningDone(model);
			stepNumber++;
		}

		model.restoreState();
		inferenceAlgorithm.evidenceSet(model);
		if (doOut && notConverged) {
			out
					.println("Stopping due to maximum steps reach, solution has not converged!");
		}
	}

	/**
	 * 
	 * @param model
	 */
	private void computeExpectedSufficientStatistics(
			DynamicBayesNetModel model, InferenceAlgorithm inferenceAlgorithm)
			throws Exception {
		TrialDao trialDao = model.getTrialDao();
		int numTrials = trialDao.getNumberTrials();
		// Initialize statistics
		Map<String, Object> inferenceOptions = Collections.emptyMap();

		model.initializeSufficientStatistics();

		if (doOut) {
			out.println("Computing Expectation...");
		}

		List<BayesNetSlice> slices = model.getTransientSlices();
		for (int i = 0; i < numTrials; i++) {
			if (doOut && i % 100 == 0) {
				out.println("  Trial " + i);
			}
			trialDao.setAllEvidence(i, false);
			inferenceAlgorithm.evidenceSet(model);
			for (BayesNetSlice slice : slices) {
				for (TransientVariable tv : slice.getVariables()) {
					Factor f = inferenceAlgorithm
							.getFactorForSufficientStatistics(model, tv,
									inferenceOptions);
					tv.getDistribution().updateSufficientStatisticsWithFactor(
							tv, f);
				}
			}
		}
	}

	private boolean maximization(DynamicBayesNetModel model, double tolerance)
			throws Exception {

		if (doOut) {
			out.println("Computing Maximization...");
		}

		List<BayesNetSlice> slices = model.getTransientSlices();
		boolean notConverged = false;
		for (BayesNetSlice slice : slices) {
			for (TransientVariable tv : slice.getVariables()) {
				double difference = tv.getDistribution()
						.updateDistributionWithSufficientStatistic();
				if (difference > tolerance) {
					notConverged = true;
				}
			}
		}
		return notConverged;
	}

	public String getName() {
		return "Expectation Maximization";
	}

	public boolean isRandom() {
		return false;
	}

}
