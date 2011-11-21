package selrach.bnetbuilder.model.algorithms.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that manages the different learning algorithms. If another learning
 * algorithm is created, this class should be updated to reflect this
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class LearningAlgorithmFactory {

	private LearningAlgorithmFactory() {}
	
	static private final Map<String, LearningAlgorithm> algorithms = new HashMap<String, LearningAlgorithm>();
	static
	{
		//algorithms.put(GradientAscent.getInstance().getName(), GradientAscent.getInstance());
		algorithms.put(ExpectationMaximization.getInstance().getName(), ExpectationMaximization.getInstance());
	}
	
	public static List<String> getAlgorithmNameList()
	{
		return Collections.unmodifiableList(new ArrayList<String>(algorithms.keySet()));
	}
	
	public static LearningAlgorithm getAlgorithm(String name)
	{
		return algorithms.get(name);
	}
}
