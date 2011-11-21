package selrach.bnetbuilder.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * 
 * This is the metadata for each element in a data source. An element is a
 * potential mapping to a variable within the model
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ElementMetadata {

	/**
	 * This is a constant that determines how many unique states we have until
	 * we know for certain that the data cannot be discrete
	 */
	private final int MAX_DISCRETE_STATES = 100;

	/**
	 * Creates an ElementMetadata object
	 * 
	 * @param position
	 *            The visual location of the metadata
	 * @param example
	 *            An example of what the data looks like
	 */
	public ElementMetadata(int position, String example) {
		this.position = position;
		this.example = example;
	}

	/**
	 * This is the position of the variable within a single line.
	 */
	private int position = -1;

	/**
	 * This is the header information from the file...could match up with the
	 * variable name in the file.
	 */
	private String header = "";

	/**
	 * This is an example of the data
	 */
	private String example = "";

	/**
	 * Flag to say if we think this variable is continuous
	 */
	private boolean continuous = false;

	/**
	 * Flag that says the data is not in a discrete format (probably has too
	 * many unique states)
	 */
	private boolean cannotBeDiscrete = false;

	/**
	 * Flag to know if the mapping has been set up automagically or the user has
	 * set up the metadata
	 */
	private boolean beenSetup = false;

	/**
	 * This is the min value that we have seen
	 */
	private double minValue = Double.MAX_VALUE;

	/**
	 * This is the max value that we have seen
	 */
	private double maxValue = Double.MIN_VALUE;

	/**
	 * This is to store discretizing range boundary separators
	 */
	private final List<Double> rangeBoundaries = new ArrayList<Double>();

	/**
	 * This is which state maps to a particular range in range boundaries. This
	 * should always be one element bigger than rangeBoundaries.
	 */
	private final List<String> rangeStates = new ArrayList<String>();

	/**
	 * The variable states inside the model do not necessarily match up with the
	 * states in the model. This is to match them up. Null entries are unmatched
	 * as of yet. this has the potential to get rather large for continuous
	 * spaces so will stop growing if we reach MAX_DISCRETE_STATES distinct
	 * strings.
	 */
	private final Map<String, String> potentialStateMap = new HashMap<String, String>();

	/**
	 * These are the potential states as specified automatically by the data
	 * source
	 */
	private final List<String> potentialStates = new ArrayList<String>();

	private RandomVariable currentMapTo = null;

	/**
	 * Adds a file value that needs to be mapped at some time in the future.
	 * 
	 * @param fileValue
	 */
	public void addState(String fileValue) {
		try {
			double value = Double.parseDouble(fileValue);
			if (value > maxValue) {
				maxValue = value;
			}
			if (value < minValue) {
				minValue = value;
			}
		} catch (NumberFormatException nfe) {
			continuous = false;
		}

		if (cannotBeDiscrete) {
			return;
		}

		if (potentialStateMap.size() > MAX_DISCRETE_STATES) {
			potentialStateMap.clear();
			cannotBeDiscrete = true;
			continuous = true;
			return;
		}
		if (potentialStateMap.containsKey(fileValue)) {
			return;
		}
		potentialStateMap.put(fileValue, null);
	}

	/**
	 * Maps a state defined in the file to a state in a particular model
	 * variable this should be blown away if we switch which variable we are
	 * mapping to.
	 * 
	 * @param dataState
	 *            This is the state name in the data source.
	 * @param modelState
	 *            This is the state name in the model.
	 */
	public void setStateMapping(String modelState, String dataState)
			throws Exception {
		if (cannotBeDiscrete) {
			throw new Exception(
					"There are too many states for us to work with.");
		}
		if (potentialStateMap.size() > MAX_DISCRETE_STATES
				&& !potentialStateMap.containsKey(dataState)) {
			try {
				Double.parseDouble(modelState);
				cannotBeDiscrete = true;
				continuous = true;
				potentialStateMap.clear();
				return;
			} catch (NumberFormatException nfe) {
				System.err.println("We have more than " + MAX_DISCRETE_STATES
						+ " states and the value state value (" + modelState
						+ ") is not continuous.");
			}
		}
		potentialStateMap.put(dataState, modelState);
	}

	/**
	 * Inserts a new state mapping at the range before the new specified
	 * boundary value
	 * 
	 * @param modelState
	 *            The state to add
	 * @param newBoundaryValue
	 *            The boundary value to add
	 */
	public void insertBeforeRangeBoundary(String modelState,
			double newBoundaryValue) {
		double prev = rangeBoundaries.get(0);
		int i;
		if (newBoundaryValue <= prev) {
			rangeBoundaries.add(0, newBoundaryValue);
			rangeStates.add(0, modelState);
			return;
		}
		for (i = 1; i < rangeBoundaries.size(); i++) {
			if (newBoundaryValue > prev
					&& newBoundaryValue <= (prev = rangeBoundaries.get(i))) {
				rangeBoundaries.add(i, newBoundaryValue);
				rangeStates.add(i, modelState);
				return;
			}
		}
		rangeBoundaries.add(newBoundaryValue);
		rangeStates.add(i, modelState);
	}

	/**
	 * Inserts a new state mapping at the range after the new specified boundary
	 * value
	 * 
	 * @param modelState
	 *            The state to add
	 * @param newBoundaryValue
	 *            The boundary value to add
	 */
	public void insertAfterRangeBoundary(String modelState,
			double newBoundaryValue) {

		double prev = rangeBoundaries.get(0);
		int i;
		if (newBoundaryValue <= prev) {
			rangeBoundaries.add(0, newBoundaryValue);
			rangeStates.add(1, modelState);
			return;
		}
		for (i = 1; i < rangeBoundaries.size(); i++) {
			if (newBoundaryValue > prev
					&& newBoundaryValue <= (prev = rangeBoundaries.get(i))) {
				rangeBoundaries.add(i, newBoundaryValue);
				rangeStates.add(i + 1, modelState);
				return;
			}
		}
		rangeBoundaries.add(newBoundaryValue);
		rangeStates.add(modelState);
	}

	/**
	 * Sets the state mapping for a particular range to the given modelState
	 * 
	 * @param whichRange
	 *            The range to map the state to
	 * @param modelState
	 *            The state to map
	 */
	public void setStateMapping(int whichRange, String modelState) {
		rangeStates.set(whichRange, modelState);
	}

	/**
	 * Moves the upper boundary of a range to the new value, if other,
	 * supposedly higher boundaries are included in that range, they get smashed
	 * up to the top
	 * 
	 * @param whichRange
	 *            The range to map the boundary to
	 * @param newBoundaryValue
	 *            The new boundary value
	 */
	public void moveRangeUpperBoundary(int whichRange, double newBoundaryValue) {
		if (whichRange > rangeBoundaries.size()) {
			return;
		}
		rangeBoundaries.set(whichRange, newBoundaryValue);
		for (int i = whichRange + 1; i < rangeBoundaries.size(); i++) {
			if (rangeBoundaries.get(i) < newBoundaryValue) {
				rangeBoundaries.set(i, newBoundaryValue += 0.0000001);
			} else {
				return;
			}
		}
	}

	/**
	 * Moves a range boundary and keeps its associative position in the data
	 * source
	 * 
	 * @param whichRange
	 *            The range to map the boundary to
	 * @param newBoundaryValue
	 *            The new boundary value
	 */
	public void moveRangeBoundary(int whichBoundary, double newBoundaryValue) {

		rangeBoundaries.set(whichBoundary, newBoundaryValue);
		for (int i = whichBoundary + 1; i < rangeBoundaries.size(); i++) {
			if (rangeBoundaries.get(i) < newBoundaryValue) {
				rangeBoundaries.set(i, newBoundaryValue += 0.0000001);
			} else {
				break;
			}
		}
		for (int i = whichBoundary - 1; i >= 0; i--) {
			if (rangeBoundaries.get(i) > newBoundaryValue) {
				rangeBoundaries.set(i, newBoundaryValue -= 0.0000001);
			} else {
				return;
			}
		}
	}

	/**
	 * Moves the lower boundary of a range to the new value, if other,
	 * supposedly lower boundaries are included in that range, they get smashed
	 * up to the bottom
	 * 
	 * @param whichRange
	 * @param newBoundaryValue
	 */
	public void moveRangeLowerBoundary(int whichRange, double newBoundaryValue) {
		if (whichRange == 0) {
			return;
		}
		whichRange--;
		rangeBoundaries.set(whichRange, newBoundaryValue);
		for (int i = whichRange - 1; i >= 0; i--) {
			if (rangeBoundaries.get(i) < newBoundaryValue) {
				rangeBoundaries.set(i, newBoundaryValue -= 0.0000001);
			} else {
				return;
			}
		}
	}

	/**
	 * Tries to set up default ranges for the different model states, its
	 * probably incorrect and needs human intervention to make sure the mappings
	 * are proper.
	 * 
	 * @param modelStates
	 */
	public void setupDefaultRanges(List<String> modelStates) {
		if (isProbablyContinuous(modelStates)) {
			setContinuous(true);
		}
		if (maxValue >= minValue) {
			rangeStates.clear();
			rangeBoundaries.clear();

			rangeStates.addAll(modelStates);
			double dif = (maxValue - minValue) / rangeStates.size();
			double newValue = minValue;
			for (int i = 0; i < rangeStates.size() - 1; i++) {
				newValue += dif;
				rangeBoundaries.add(newValue);
			}
			beenSetup = true;
		}
		if (modelStates.size() == potentialStateMap.size()) {
			int i = 0;
			for (String key : potentialStateMap.keySet()) {
				if (potentialStateMap.get(key) == null) {
					potentialStateMap.put(key, modelStates.get(i));
				}
				i++;
			}
			beenSetup = true;
		}
	}

	/**
	 * Gets the list of range states in the metadata, there should be one more
	 * state than boundary, the upper and lower values have the max double and
	 * min double values respectively as their boundaries
	 * 
	 * @return
	 */
	public List<String> getRangeStates() {
		return Collections.unmodifiableList(rangeStates);
	}

	/**
	 * Gets the range boundaries for each state, this is monotonically
	 * increasing
	 * 
	 * @return
	 */
	public List<Double> getRangeBoundaries() {
		return Collections.unmodifiableList(rangeBoundaries);
	}

	/**
	 * Gets the potential state mapping
	 * 
	 * @return
	 */
	public Map<String, String> getPotentialStateMap() {
		return Collections.unmodifiableMap(potentialStateMap);
	}

	/**
	 * Gets the state for a particular value of the data
	 * 
	 * @param dataValue
	 * @return
	 */
	public String getModelState(String dataValue) {
		if (continuous) {
			try {
				double value = Double.parseDouble(dataValue);
				double prev = rangeBoundaries.get(0);
				int i;
				if (value <= prev) {
					return rangeStates.get(0);
				}
				for (i = 1; i < rangeBoundaries.size(); i++) {
					if (value > prev
							&& value <= (prev = rangeBoundaries.get(i))) {
						return rangeStates.get(i);
					}
				}
				return rangeStates.get(i);
			} catch (NumberFormatException nfe) {
				return null;
			}
		} else {
			return potentialStateMap.get(dataValue);
		}
	}

	/**
	 * Clears out all mappings made
	 */
	public void clearMappings() {
		for (String key : potentialStateMap.keySet()) {
			potentialStateMap.put(key, null);
		}
		potentialStates.clear();
		rangeBoundaries.clear();
		rangeStates.clear();

		cannotBeDiscrete = false;
		minValue = Double.MAX_VALUE;
		maxValue = Double.MIN_VALUE;
		beenSetup = false;
	}

	/**
	 * @return the position in the mapping
	 */
	public int getPositionInTrial() {
		return position;
	}

	/**
	 * Sets up the header text
	 * 
	 * @param header
	 */
	public void setHeader(String header) {
		this.header = header;
	}

	/**
	 * @return the header
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Sets up the example data value
	 * 
	 * @param example
	 */
	public void setExample(String example) {
		this.example = example;
	}

	/**
	 * @return the example
	 */
	public String getExample() {
		return example;
	}

	/**
	 * @return the continuous
	 */
	public boolean isContinuous() {
		return continuous;
	}

	/**
	 * Has this metadata object been mapped out?
	 * 
	 * @return
	 */
	public boolean hasBeenSetup() {
		return beenSetup;
	}

	/**
	 * 
	 * @param beenSetup
	 */
	public void setBeenSetup(boolean beenSetup) {
		this.beenSetup = beenSetup;
	}

	/**
	 * Lets us know if the metadata is probably continuous so that we can guess
	 * this for the user when they try and set things up.
	 * 
	 * @param states
	 * @return
	 */
	public boolean isProbablyContinuous(List<String> states) {
		return (beenSetup && continuous)
				|| states.size() < potentialStateMap.size();
	}

	/**
	 * Could the metadata have simple state mappings for discrete variables
	 * 
	 * @return
	 */
	public boolean isCannotBeDiscrete() {
		return cannotBeDiscrete;
	}

	/**
	 * Could the metadata represent a continuous element
	 * 
	 * @return
	 */
	public boolean isCannotBeContinuous() {
		return minValue > maxValue;
	}

	/**
	 * @return the currentMapTo
	 */
	public RandomVariable getCurrentMapTo() {
		return currentMapTo;
	}

	/**
	 * @param currentMapTo
	 *            the currentMapTo to set
	 */
	public void setCurrentMapTo(RandomVariable currentMapTo) {
		this.currentMapTo = currentMapTo;
	}

	/**
	 * @param continuous
	 *            the continuous to set
	 */
	public void setContinuous(boolean continuous) {
		if (cannotBeDiscrete) {
			continuous = true;
		} else if (isCannotBeContinuous()) {
			continuous = false;
		}
		this.continuous = continuous;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ElementMetaData properties:\nPosition:");
		sb.append(this.position);
		sb.append("\nheader:");
		sb.append(this.header);
		sb.append("\nexample:");
		sb.append(this.example);
		sb.append("\nContinuous? ");
		sb.append(this.continuous);
		sb.append("\nState map:\n");
		for (String s : this.potentialStateMap.keySet()) {
			sb.append('\t');
			sb.append(s);
			sb.append("->");
			sb.append(this.potentialStateMap.get(s));
			sb.append('\n');
		}
		return sb.toString();
	}

	public List<String> getDataStates() {
		if (potentialStates.size() != potentialStateMap.size()) {
			potentialStates.clear();
			potentialStates.addAll(potentialStateMap.keySet());
		}
		return Collections.unmodifiableList(potentialStates);
	}
}