package selrach.bnetbuilder.gui.tables;

import javax.swing.JTable;

/**
 * Creates a table with no header information set up by default
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class HeadlessJTable extends JTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8275268243579576185L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JTable#configureEnclosingScrollPane()
	 */
	@Override
	protected void configureEnclosingScrollPane() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JTable#unconfigureEnclosingScrollPane()
	 */
	@Override
	protected void unconfigureEnclosingScrollPane() {
	}
}
