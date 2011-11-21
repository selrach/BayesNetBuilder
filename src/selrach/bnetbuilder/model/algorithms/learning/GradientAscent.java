package selrach.bnetbuilder.model.algorithms.learning;

import java.io.PrintStream;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

public class GradientAscent implements LearningAlgorithm {

	private static GradientAscent instance = new GradientAscent();
	
	private GradientAscent() {}
	
	public static GradientAscent getInstance()
	{
		return instance;
	}
	
	private PrintStream out;
	
	public void execute(DynamicBayesNetModel model,
			Map<String, Object> additionalProperties, PrintStream updateTracking) throws Exception {
		// TODO Auto-generated method stub
		this.out = updateTracking;

	}

	public String getName() {
		return "Gradient Ascent";
	}

	public boolean isRandom() {
		return false;
	}

}
