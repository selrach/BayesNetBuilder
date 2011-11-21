package selrach.bnetbuilder.model.algorithms.graph;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;

/**
 * Creates the junction tree templates, basically groups together all the steps
 * necessary to create a junction tree from the raw network
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class MakeJunctionTreeTemplate {

	private final static Logger logger = Logger
			.getLogger(MakeJunctionTreeTemplate.class);

	public static void execute(DynamicBayesNetModel model) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Constructing Junction tree...");
			}
			GenerateMoralization.execute(model);
			JunctionTreeTemplate jtt = model.getJunctionTreeTemplate();
			if (logger.isDebugEnabled()) {
				logger.debug("Junction Tree after Moralization:");
				logger.debug(jtt);
			}
			GenerateEliminationCliques.execute(jtt);
			if (logger.isDebugEnabled()) {
				logger.debug("Junction Tree after Elimination Cliques:");
				logger.debug(jtt);
			}
			GenerateJunctionTreeFromCliques.execute(jtt);
			if (logger.isDebugEnabled()) {
				logger.debug("Junction Tree after GenerateJunctionTree:");
				logger.debug(jtt);
			}
			jtt.setStale(false);
		} catch (Exception ex) {
			logger.error("Problem with Junction Tree Construction:", ex);
		}
	}

}
