package selrach.bnetbuilder.model.variable;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

/**
 * A ContinuousVariable has a continuous range of values.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ContinuousVariable extends RandomVariable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8842853426701308078L;

	public ContinuousVariable(DynamicBayesNetModel model) {
		super(model);
		name = "New Continuous Variable";
	}

	@Override
	public String getType() {
		return "continuous";
	}

	double value = 0.0;
	boolean valueSet = false;

	/**
	 * Gets the value of this cv
	 * 
	 * @return
	 */
	@Override
	public double getValue() throws Exception {
		if (!valueSet) {
			throw new Exception("Bad Value!");
		}
		return value;
	}

	/**
	 * Sets the value of this cv
	 * 
	 * @param v
	 */
	@Override
	public void setValue(double v) {
		value = v;
		valueSet = true;
	}

	/**
	 * Clears the value of this cv. Essentially says that this value is unknown.
	 */
	@Override
	public void clearValue() {
		valueSet = false;
	}

	@Override
	public boolean hasValue() {
		return valueSet;
	}

	@Override
	public RandomVariable copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getVariableSpecificXML() {
		return "";
	}

}
