package selrach.bnetbuilder.gui.notification;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import selrach.bnetbuilder.gui.notification.io.TextComponentPrintStream;

/**
 * This is a modal warning dialog that pops up when the application is doing
 * some long processes, such as learning. It has the option to grab the print
 * stream and show progress information to the user.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ProgressTrackerDialog extends JDialog {

	private static final long serialVersionUID = -5036630878594632229L;

	private TextComponentPrintStream noticeStream;
	private JButton jBtnClose;

	private final String message;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				ProgressTrackerDialog inst = new ProgressTrackerDialog(frame,
						"this is a test");
				inst.setVisible(true);
			}
		});
	}

	/**
	 * This is the print stream to use for messages to appear on the dialog
	 * window
	 * 
	 * @return
	 */
	public PrintStream getTrackingPrintStream() {
		return noticeStream;
	}

	public ProgressTrackerDialog(JFrame frame, String message) {
		super(frame);
		this.message = message;
		initGUI();
	}

	private void initGUI() {
		BoxLayout thisLayout = new BoxLayout(getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
		getContentPane().setLayout(thisLayout);
		JLabel text = new JLabel();
		text.setText(message);
		getContentPane().add(text);

		JTextArea trackingText = new JTextArea();
		noticeStream = new TextComponentPrintStream(trackingText);
		trackingText.setEditable(false);
		trackingText.setAutoscrolls(true);
		getContentPane().add(new JScrollPane(trackingText));

		jBtnClose = new JButton("Ok");
		jBtnClose.setEnabled(false);
		jBtnClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		getContentPane().add(jBtnClose);

		this.setSize(new Dimension(400, 300));
		this.setTitle("Warning");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	public void enableClosing() {
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		jBtnClose.setEnabled(true);
	}
}
