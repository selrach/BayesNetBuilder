package selrach.bnetbuilder.gui.jgraph;

import org.jgraph.graph.DefaultGraphModel;

/**
 * This is to facilitate custom drag and drop behaviours
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DynamicBayesNetJGraphModel extends DefaultGraphModel {

	private static final long serialVersionUID = 5134758073141319966L;

	@Override
	public boolean acceptsSource(Object edge, Object port) {
		// TODO Auto-generated method stub
		return super.acceptsSource(edge, port);
	}

	@Override
	public boolean acceptsTarget(Object edge, Object port) {
		// TODO Auto-generated method stub
		return super.acceptsTarget(edge, port);
	}

}
