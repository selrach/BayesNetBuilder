package selrach.bnetbuilder.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;

import selrach.bnetbuilder.data.interfaces.DataAccessor;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * This is the trial data access object. It can manage multiple DataAccessors so
 * that only this object is needed to work with multiple data sources
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class TrialDao {

	private static final Logger logger = Logger.getLogger(TrialDao.class);

	// No matter how we access the data, we need it organized for consistency
	private final List<DataAccessor> dataPieces = new ArrayList<DataAccessor>();

	private Map<String, ElementMetadata> trialMetadataCache;
	private List<List<String>> trialCache;
	private int trialInData = -1;

	private final DynamicBayesNetModel model;

	/**
	 * Creates the DAO object
	 * 
	 * @param model
	 */
	public TrialDao(DynamicBayesNetModel model) {
		this.model = model;
	}

	/**
	 * Adds a file to manage
	 * 
	 * @param fileHandler
	 */
	public void addFile(FileAccessor fileHandler) {
		dataPieces.add(fileHandler);
	}

	/**
	 * Removes a file from the manager
	 * 
	 * @param fileHandler
	 */
	public void removeFile(FileAccessor fileHandler) {
		if (dataPieces.contains(fileHandler)) {
			dataPieces.remove(fileHandler);
		}
	}

	/**
	 * Returns a list of all data sources
	 * 
	 * @return
	 */
	public List<DataAccessor> getDataSources() {
		return Collections.unmodifiableList(dataPieces);
	}

	/**
	 * Sets up the evidence present in a trial on each of the variables they
	 * have been mapped to
	 * 
	 * @param trial
	 *            the trial to load
	 * @param announce
	 *            should this trigger an update to the world that evidence has
	 *            changed? This should probably be false if you are running an
	 *            algorithm that relys on data (such as learning) so that we
	 *            don't waste cpu cycles updating all the listeners
	 * @return was loading the evidence successful
	 */
	public boolean setAllEvidence(int trial, boolean announce) {
		int timeSteps = Math.min(getNumberTimesteps(trial), model
				.getMaxNumberSlices());

		for (int i = 0; i < timeSteps; i++) {
			try {
				final List<TransientVariable> vars = model.getSlice(i)
						.getVariables();
				for (final TransientVariable tv : vars) {
					setEvidence(tv, trial, i, announce);
				}
			} catch (final Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Problem setting evidence for timestep " + i,
							ex);
				}
			}
		}

		return true;
	}

	/**
	 * Sets up evidence and announces it.
	 * 
	 * @param trial
	 *            the trial to load
	 * @return
	 */
	public boolean setAllEvidence(int trial) {
		return setAllEvidence(trial, true);
	}

	/**
	 * Sets up the evidence on a particular variable in a particular time
	 * @param variable
	 * @param trial
	 * @param time
	 * @param announce
	 * @return
	 * @throws Exception
	 */
	public boolean setEvidence(TransientVariable variable, int trial, int time,
			boolean announce) throws Exception {
		trial--;
		if (!loadTrial(trial)) {
			return false; // trial does not exist
		}
		ElementMetadata meta = trialMetadataCache.get(variable.getReference()
				.getId());
		if (meta != null) {
			int pos = meta.getPositionInTrial();
			if (pos == -1) {

				variable.setHidden(announce);
				return true;
			}
			String data = trialCache.get(time).get(pos);
			if (data.length() == 0) {
				variable.setHidden(announce);
				return true;
			}
			if (variable.getReference() instanceof ContinuousVariable) {
				try {
					variable.setEvidence(Double.parseDouble(data), announce);
					return true;
				} catch (NumberFormatException nfe) {
					variable.setHidden(announce);
					if (logger.isDebugEnabled()) {
						logger
								.debug("Element can't be parsed as a number. Assuming hidden: "
										+ meta);
					}
					return false;
				}
			} else if (variable.getReference() instanceof DiscreteVariable) {
				String modelState = meta.getModelState(data);
				DiscreteVariable dv = (DiscreteVariable) variable
						.getReference();
				List<String> states = dv.getStates();
				int ind = states.indexOf(modelState);
				if (ind == -1) {
					// If no mapping, assume hidden
					variable.setHidden(announce);
					return true;
				}
				variable.setEvidence((double) ind, announce);
				return true;
			}
			return false;
		}

		return false;

	}

	public boolean setEvidence(TransientVariable variable, int trial, int time)
			throws Exception {
		return setEvidence(variable, trial, time, true);
	}

	private boolean loadTrial(int trial) {

		// if (whichTrialCached == trial) return true;

		MutableInt i = new MutableInt(trial);
		DataAccessor da = getAccessor(i);
		if (da == null) {
			return false;
		}

		trialInData = i.intValue();
		trialCache = da.getTrial(trialInData);
		trialMetadataCache = da.getVariableToElementMetadataMap();
		return true;
	}

	private DataAccessor getAccessor(MutableInt trial) {
		int tmpTrial = trial.intValue();
		for (int i = 0; i < dataPieces.size(); i++) {
			int t = dataPieces.get(i).getNumberTrials();
			if (tmpTrial - t <= 0) {
				trial.setValue(tmpTrial);
				return dataPieces.get(i);
			}
			tmpTrial -= t;
		}
		return null;
	}

	public int getNumberTimesteps(int trial) {
		MutableInt i = new MutableInt(trial - 1);
		DataAccessor da = getAccessor(i);
		if (da == null) {
			return -1;
		}
		return da.getNumberTimestepsPerTrial();
	}

	public String getTrialDataDescription(int trial) {
		MutableInt i = new MutableInt(trial);
		DataAccessor da = getAccessor(i);
		return da.getDescription() + ", Trial: " + i.intValue();
	}

	public int getNumberTrials() {
		int trialCount = 0;
		for (DataAccessor da : dataPieces) {
			trialCount += da.getNumberTrials();
		}
		return trialCount;
	}

}
