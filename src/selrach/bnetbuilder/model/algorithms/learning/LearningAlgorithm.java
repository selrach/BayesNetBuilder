package selrach.bnetbuilder.model.algorithms.learning;

import java.io.PrintStream;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

public interface LearningAlgorithm {
	public boolean isRandom();

	public String getName();

	public void execute(DynamicBayesNetModel model,
			Map<String, Object> additionalProperties, PrintStream updateTracking) throws Exception;

}
