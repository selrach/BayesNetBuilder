package selrach.bnetbuilder.gui.notification;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Sets up a modal warning that something is happening.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ModalWarning extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7136379674837427770L;
	private final String message;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				ModalWarning inst = new ModalWarning(frame, "this is a test");
				inst.setVisible(true);
			}
		});
	}

	public ModalWarning(JFrame frame, String message) {
		super(frame);
		this.message = message;
		initGUI();
	}

	private void initGUI() {
		BoxLayout thisLayout = new BoxLayout(getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
		getContentPane().setLayout(thisLayout);
		JTextPane text = new JTextPane();
		text.setText(message);
		text.setEditable(false);
		text.setBackground(this.getBackground());
		getContentPane().add(text);
		this.setSize(new Dimension(150, 75));
		this.setTitle("Warning");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
}
