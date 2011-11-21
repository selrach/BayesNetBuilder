package selrach.bnetbuilder.model.dao;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

/**
 * Interface to implement for different model file formats
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface DynamicBayesNetDao {

	public void loadModel(DynamicBayesNetModel model, String filename)
			throws Exception;

	public void saveModel(DynamicBayesNetModel model, String filename)
			throws Exception;

	public String getName();
}
