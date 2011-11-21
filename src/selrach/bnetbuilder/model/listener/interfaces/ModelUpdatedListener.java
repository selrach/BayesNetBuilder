package selrach.bnetbuilder.model.listener.interfaces;

import selrach.bnetbuilder.model.DynamicBayesNetModel;

/**
 * Notification of different overarching model events.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public interface ModelUpdatedListener {

	public void modelLoaded(DynamicBayesNetModel model);

	public void modelUnloaded(DynamicBayesNetModel model);

	public void modelFinishedLearning(DynamicBayesNetModel model);

	public void modelFinishedInfering(DynamicBayesNetModel model);
}
