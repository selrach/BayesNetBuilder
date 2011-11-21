package selrach.bnetbuilder.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.data.interfaces.DataAccessor;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * Implements a basic file accessor, this accessor keeps track of a single file
 * and its metadata mappings
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class FileAccessor implements DataAccessor {

	private final static Logger logger = Logger.getLogger(FileAccessor.class);

	/**
	 * Filename on system
	 */
	private File file = null;

	/**
	 * Delimination string between data entries
	 */
	private String variableDeliminator = null;

	/**
	 * This is the number of timesteps per trial in this file. This number
	 * should be the same across all files that we are loading evidence from.
	 */
	private int numberTimesteps = -1;

	/**
	 * This is the number of data lines in the file
	 */
	private int numberDataLines = -1;

	/**
	 * Flag to let us know if there is a header line in this file
	 */
	private boolean hasHeader = false;

	/**
	 * processed data in this file, if we are doing lazy loading this should be
	 * empty unless we are actually working on the file
	 */
	private final List<List<String>> parsedData = new ArrayList<List<String>>();

	/**
	 * Unparsed data lines for quick changes between deliminators.
	 */
	private final List<String> unparsedData = new ArrayList<String>();

	/**
	 * List of file element information
	 */
	private final List<ElementMetadata> positions = new ArrayList<ElementMetadata>();

	/**
	 * List of variable names to map to
	 */
	private final List<String> variables = new ArrayList<String>();

	/**
	 * Metadata mapping
	 */
	private final Map<String, ElementMetadata> variableToElementMetadataMap = new HashMap<String, ElementMetadata>();

	/**
	 * Handle to model
	 */
	private final DynamicBayesNetModel model;

	/**
	 * Creates a file accessor for the model
	 * 
	 * @param model
	 *            the bayes model that this data is supposed to be mapping to
	 * @param filename
	 *            the filename of the data we are mapping
	 * @param deliminator
	 *            the data deliminator string
	 * @param hasHeader
	 *            flag to specify that this file has a single line of header
	 *            information
	 */
	public FileAccessor(DynamicBayesNetModel model, String filename,
			String deliminator, boolean hasHeader) {
		this.model = model;
		loadFile(filename, deliminator, hasHeader);
		this.model.subscribe(this);
		DiscreteVariable.subscribe(this);

	}

	/**
	 * Adds a blank mapping
	 */
	public void addNullFileElementInList() {
		positions.add(new ElementMetadata(-1, ""));
		variables.add(null);
	}

	public void addVariable(RandomVariable variable) {
		variables.add(variable.getId());
		positions.add(new ElementMetadata(-1, ""));
		variableToElementMetadataMap.clear();
	}

	/**
	 * Discards the actual data of this file and keeps the metadata for later
	 * use, useful if we have massive data and we want to only keep the working
	 * set in memory.
	 */
	public void discardData() {
		parsedData.clear();
	}

	public String getDescription() {
		return file.getAbsolutePath();
	}

	/**
	 * @return the file element positions
	 */
	public List<ElementMetadata> getFileElementsInList() {
		return Collections.unmodifiableList(positions);
	}

	/**
	 * @return the maxPotentialTrials
	 */
	public int getNumberDataLines() {
		return numberDataLines;
	}

	/**
	 * @return the numberTimesteps
	 */
	public int getNumberTimestepsPerTrial() {
		return numberTimesteps;
	}

	/**
	 * Returns the number of trials present in this file given the number of
	 * timesteps each trial is going to take
	 */
	public int getNumberTrials() {
		return (int) Math.floor((double) numberDataLines
				/ (double) numberTimesteps);
	}

	/**
	 * Gets a trial from the data, count starts at 0
	 * 
	 * @param which
	 *            the trial to grab
	 * @return an unmodifiable list representing the data, use the metadata to
	 *         figure out the actual mappings from data->variable
	 */
	public List<List<String>> getTrial(int which) {
		if (parsedData.size() == 0) {
			loadData();
		}
		final int from = which * getNumberTimestepsPerTrial()
				+ (hasHeader ? 1 : 0);
		return Collections.unmodifiableList(parsedData.subList(from, from
				+ getNumberTimestepsPerTrial()));
	}

	/**
	 * @return the variableDeliminator
	 */
	public String getVariableDeliminator() {
		return variableDeliminator;
	}

	/**
	 * @return the variable element positions
	 */
	public List<String> getVariablesInList() {
		return Collections.unmodifiableList(variables);
	}

	/**
	 * Grabs the current mapping from model variable to file element position.
	 * If a variable is not present in the list, it does not have a mapping
	 * 
	 * @return
	 */
	public Map<String, ElementMetadata> getVariableToElementMetadataMap() {
		if (variableToElementMetadataMap.size() == 0) {
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i) != null && positions.get(i) != null) {
					variableToElementMetadataMap.put(variables.get(i),
							positions.get(i));
				}
			}
		}
		return Collections.unmodifiableMap(variableToElementMetadataMap);
	}

	/**
	 * @return the hasHeader
	 */
	public boolean isHasHeader() {
		return hasHeader;
	}

	/**
	 * This function assumes that all the metadata for the file has been loaded
	 * previously, we just need to retrieve the file from disk again. This is
	 * useful if we are processing massive amounts of data and only want to keep
	 * the working set of data in memory.
	 */
	public void loadData() {
		if (file != null && file.exists()) {
			try {
				String line = null;
				final InputStreamReader isr = new FileReader(file);
				final BufferedReader br = new BufferedReader(isr);
				int offset = 0;
				parsedData.clear();
				while ((line = br.readLine()) != null) {
					final String[] items = line.split(variableDeliminator);
					final List<String> newline = Arrays.asList(items);
					if (newline.size() != positions.size()) {
						System.err
								.println("Line has different number of variables, ignoring line "
										+ (parsedData.size() + (offset++))
										+ ": \t" + line);
					} else {
						parsedData.add(newline);
					}
				}
				numberDataLines = parsedData.size() - (hasHeader ? 1 : 0);
				br.close();
				isr.close();
			} catch (final Exception ex) {
				parsedData.clear();
				System.err.println(ex.getMessage());
			}
		}
	}

	/**
	 * Loads the data in the file
	 * 
	 * @param file
	 *            the file itself
	 * @param deliminator
	 *            element deliminator
	 * @param hasHeader
	 *            does the file contain a header line?
	 */
	public void loadFile(File file, String deliminator, boolean hasHeader) {
		this.variableDeliminator = deliminator;
		this.hasHeader = hasHeader;
		if (file.exists()) {
			this.file = file;
			processMetadata();
		}
	}

	/**
	 * Loads the data located at filename
	 * 
	 * @param filename
	 *            location of the file
	 * @param deliminator
	 *            element deliminator
	 * @param hasHeader
	 *            does the file contain a header line?
	 */
	public void loadFile(String filename, String deliminator, boolean hasHeader) {
		loadFile(new File(filename), deliminator, hasHeader);
	}

	public void moveFileElementInList(ElementMetadata ele, int to) {
		final int from = positions.indexOf(ele);
		moveFileElementInList(from, to);
	}

	public void moveFileElementInList(int from, int to) {
		if (from >= 0 && to >= 0 && from < positions.size()
				&& to < positions.size() && to != from) {
			final ElementMetadata pi = positions.remove(from);
			// to = to > from ? to - 1 : to;
			positions.add(to, pi);
			variableToElementMetadataMap.clear();
		}
	}

	public void moveVariableInList(int from, int to) {
		if (from >= 0 && to >= 0 && from < variables.size()
				&& to < variables.size() && to != from) {
			final String str = variables.remove(from);
			// to = to > from ? to - 1 : to;
			variables.add(to, str);
			variableToElementMetadataMap.clear();
		}
	}

	public void moveVariableInList(String id, int to) {
		final int from = variables.indexOf(id);
		moveVariableInList(from, to);
	}

	private void processDataFromCache() {
		try {

			String line = null;
			parsedData.clear();
			int size = -1;
			int offset = 0;
			// get first line
			line = unparsedData.get(0);
			String[] items = line.split(variableDeliminator);
			List<String> newline = Arrays.asList(items);
			size = newline.size();

			positions.clear();
			variableToElementMetadataMap.clear();

			int i;

			for (i = 0; i < items.length; i++) {
				positions.add(new ElementMetadata(i, items[i]));
			}

			for (final String ln : unparsedData) {
				items = ln.split(variableDeliminator);
				newline = Arrays.asList(items);
				if (size < newline.size()) {
					if (logger.isInfoEnabled()) {
						logger
								.info("Line has different number of variables, ignoring line "
										+ (parsedData.size() + (offset++))
										+ ": \t" + ln);
					}
				} else {
					parsedData.add(newline);
					for (i = 0; i < newline.size(); i++) {
						positions.get(i).addState(newline.get(i));
					}
				}
			}

			final int maxSteps = parsedData.size() - (hasHeader ? 1 : 0);
			if (numberTimesteps == -1 || maxSteps < numberTimesteps) {
				numberTimesteps = maxSteps;
			}
			numberDataLines = parsedData.size() - (hasHeader ? 1 : 0);

			updateHeader();
		} catch (final Exception ex) {
			parsedData.clear();
			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Process the file and set up the metadata
	 */
	private void processDataFromFile() {
		if (file != null && file.exists()) {
			try {

				String line = null;
				parsedData.clear();
				unparsedData.clear();
				final InputStreamReader isr = new FileReader(file);
				final BufferedReader br = new BufferedReader(isr);
				int size = -1;
				int offset = 0;
				// get first line
				line = br.readLine();
				String[] items = line.split(variableDeliminator);
				List<String> newline = Arrays.asList(items);
				size = newline.size();

				unparsedData.add(line);
				parsedData.add(newline);
				positions.clear();
				variableToElementMetadataMap.clear();

				int i;

				for (i = 0; i < items.length; i++) {
					positions.add(new ElementMetadata(i, items[i]));
				}

				while ((line = br.readLine()) != null) {
					unparsedData.add(line);
					items = line.split(variableDeliminator);
					newline = Arrays.asList(items);
					if (size < newline.size()) {
						if (logger.isInfoEnabled()) {
							logger
									.info("Line has different number of variables, ignoring line "
											+ (parsedData.size() + (offset++))
											+ ": \t" + line);
						}
					} else {
						parsedData.add(newline);
						for (i = 0; i < newline.size(); i++) {
							positions.get(i).addState(newline.get(i));
						}
					}
				}

				final int maxSteps = parsedData.size() - (hasHeader ? 1 : 0);
				if (numberTimesteps == -1 || maxSteps < numberTimesteps) {
					numberTimesteps = maxSteps;
				}
				numberDataLines = parsedData.size() - (hasHeader ? 1 : 0);

				br.close();
				isr.close();

				updateHeader();
			} catch (final Exception ex) {
				parsedData.clear();
				unparsedData.clear();
				System.err.println(ex.getMessage());
				ex.printStackTrace(System.err);
			}
		}
	}

	private void processMetadata() {
		if (unparsedData.isEmpty()) {
			processDataFromFile();
		} else {
			processDataFromCache();
		}
	}

	public void removeVariable(RandomVariable variable) {
		final int ind = variables.indexOf(variable.getId());
		if (positions.get(ind) == null) { // They are both null, we can safely
			// remove them
			variables.remove(ind);
			positions.remove(ind);
		} else {
			variables.set(ind, null); // the position exists, just null out the
			// variable
		}
		variableToElementMetadataMap.clear();
	}

	/**
	 * @param hasHeader
	 *            the hasHeader to set
	 */
	public void setHasHeader(boolean hasHeader) {
		this.hasHeader = hasHeader;
		final boolean clearOutData = parsedData.size() == 0;
		if (clearOutData) {
			processMetadata();
			discardData();
		} else {
			try {
				updateHeader();
			} catch (final Exception ex) {
				processMetadata();
			}
		}
	}

	/**
	 * @param numberTimesteps
	 *            the numberTimesteps to set
	 */
	public void setNumberTimesteps(int numberTimesteps) {
		this.numberTimesteps = numberTimesteps;
	}

	/**
	 * @param variableDeliminator
	 *            the variableDeliminator to set
	 */
	public void setVariableDeliminator(String variableDeliminator) {
		if (this.variableDeliminator == variableDeliminator) {
			return;
		}
		this.variableDeliminator = variableDeliminator;
		final boolean clearOutData = parsedData.size() == 0;
		processMetadata();
		if (clearOutData) {
			discardData();
		}
	}

	public void statesUpdated(DiscreteVariable variable) {
		final int ind = variables.indexOf(variable.getId());
		if (positions.get(ind) != null) {
			positions.get(ind).clearMappings();
		}
	}

	private void updateHeader() throws Exception {
		if (parsedData.size() < 1) {
			return;
		}
		final List<RandomVariable> rvs = model.getVariables();
		variables.clear();
		int i;
		if (hasHeader && parsedData.size() > 1) {

			final List<String> headers = parsedData.get(0);
			final List<String> examples = parsedData.get(1);
			while (examples.size() < headers.size()) {
				examples.add("");
			}
			if (headers.size() > positions.size()) {
				throw new Exception("Headers cannot be setup");
			}
			for (i = 0; i < headers.size(); i++) {
				positions.get(i).setHeader(headers.get(i));
				positions.get(i).setExample(examples.get(i));
				if (rvs.size() > i) {
					variables.add(rvs.get(i).getId());
				} else {
					variables.add(null);
				}
			}
		} else {
			final List<String> examples = parsedData.get(0);
			for (i = 0; i < examples.size(); i++) {
				positions.get(i).setHeader("" + i);
				positions.get(i).setExample(examples.get(i));
				if (rvs.size() > i) {
					variables.add(rvs.get(i).getId());
				} else {
					variables.add(null);
				}
			}
		}
		// Pump up position list to size of variable list.
		while (positions.size() < rvs.size()) {
			positions.add(new ElementMetadata(-1, ""));
			variables.add(rvs.get(i++).getId());
		}
	}

	public void updateVariable(RandomVariable variable) {
		// We don't need to do anything.
	}

}
