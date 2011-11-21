package selrach.bnetbuilder.model.algorithms.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

/**
 * Factory that manages the different inference algorithms. If another inference
 * algorithm is created, this class should be updated to reflect this
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class InferenceAlgorithmFactory {
	private InferenceAlgorithmFactory() {
	}

	static private final Map<String, InferenceAlgorithm> algorithms = new HashMap<String, InferenceAlgorithm>();
	//Additional algorithms should be added here.
	static {
		algorithms.put(QueryJunctionTree.getInstance().getName(),
				QueryJunctionTree.getInstance());
		algorithms.put(VariableElimination.getInstance().getName(),
				VariableElimination.getInstance());
		algorithms.put(GibbsSampler.getInstance().getName(), GibbsSampler
				.getInstance());
	}

	public static List<String> getAlgorithmNameList() {
		return Collections.unmodifiableList(new ArrayList<String>(algorithms
				.keySet()));
	}

	public static InferenceAlgorithm getAlgorithm(String name) {
		return algorithms.get(name);
	}

	public static void parameterLearningDone(DynamicBayesNetModel model) {
		for (InferenceAlgorithm ia : algorithms.values()) {
			ia.parameterLearningDone(model);
		}
	}

}
