package selrach.bnetbuilder.gui.evidence;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;

import selrach.bnetbuilder.data.ElementMetadata;
import selrach.bnetbuilder.gui.evidence.tables.ComboBoxEditor;
import selrach.bnetbuilder.gui.evidence.tables.ComboBoxRenderer;
import selrach.bnetbuilder.gui.evidence.tables.ComponentRenderer;
import selrach.bnetbuilder.gui.evidence.tables.ContinuousFileDataJTableModel;
import selrach.bnetbuilder.gui.evidence.tables.DiscreteFileDataJTableModel;
import selrach.bnetbuilder.gui.evidence.tables.JTableButtonTableMouseListener;
import selrach.bnetbuilder.gui.evidence.tables.SpinnerEditor;
import selrach.bnetbuilder.gui.evidence.tables.SpinnerRenderer;
import selrach.bnetbuilder.gui.utilities.GuiUtility;
import selrach.bnetbuilder.model.variable.DiscreteVariable;

/**
 * This is the state mapper dialog that helps facilitate mapping states to data
 * for the metadata
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class StateMapper extends JDialog {

	private static final long serialVersionUID = 1213247854030193223L;
	final private DiscreteVariable variable;
	final private ElementMetadata metadata;
	private JPanel jPanelContinuousFileDataMapper;
	private JPanel jPanelDiscreteFileDataMapper;
	private JRadioButton rbContinuous;
	private JRadioButton rbDiscrete;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JDialog frame = new JDialog();
				StateMapper inst = new StateMapper(frame, null, null);
				inst.setVisible(true);
			}
		});
	}

	public StateMapper(JDialog parent, DiscreteVariable variable,
			ElementMetadata metadata) {
		super(parent);
		this.variable = variable;
		this.metadata = metadata;
		initGui();
	}

	@Action
	public void save(ActionEvent event) {
		this.metadata.setBeenSetup(true);
		this.dispose();
	}

	@Action
	public void cancel(ActionEvent event) {
		this.dispose();
	}

	@Action
	public void setMapperPanel(ActionEvent event) {
		jPanelContinuousFileDataMapper.setVisible(rbContinuous.isSelected());
		jPanelDiscreteFileDataMapper.setVisible(rbDiscrete.isSelected());
		metadata.setContinuous(rbContinuous.isSelected());
	}

	protected void initGui() {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.weightx = 0.0;
		c.weighty = 0.0;

		if (metadata.getCurrentMapTo() != variable) {
			metadata.setBeenSetup(false);
			metadata.setCurrentMapTo(variable);
		}
		if (!metadata.hasBeenSetup()) {
			if (metadata.isProbablyContinuous(variable.getStates())) {
				metadata.setContinuous(true);
			}
			metadata.setupDefaultRanges(variable.getStates());
		}

		add(GuiUtility.makeLabel("lblStateMapperHeader", layout, c));

		ButtonGroup buttonGroup = new ButtonGroup();

		c.gridy++;
		add(rbContinuous = GuiUtility.makeRadioButton("setMapperPanel",
				"rbContinuous", getAppActionMap(), buttonGroup, layout, c));
		c.gridx++;
		add(rbDiscrete = GuiUtility.makeRadioButton("setMapperPanel",
				"rbDiscrete", getAppActionMap(), buttonGroup, layout, c));
		c.gridx++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(new JPanel(), c);

		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		c.gridx = 0;
		c.weighty = 1.0;
		c.gridwidth = 3;
		add(getJPanelContinuousFileDataMapper(), c);
		add(getJPanelDiscreteFileDataMapper(), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridy++;
		add(new JPanel(), c);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		// c.gridx++;
		c.gridx += 2;
		add(GuiUtility.makeButton("save", getAppActionMap(), layout, c));
		// c.gridx++;
		// add(GuiUtility.makeButton("cancel", getAppActionMap(), layout, c));

		Application.getInstance().getContext().getResourceMap(getClass())
				.injectComponents(getContentPane());
		if (metadata.isContinuous()) {
			rbContinuous.setSelected(true);
			if (metadata.isCannotBeDiscrete()) {
				rbDiscrete.setEnabled(false);
			}
			jPanelDiscreteFileDataMapper.setVisible(false);
		} else {
			rbDiscrete.setSelected(true);
			if (metadata.isCannotBeContinuous()) {
				rbContinuous.setEnabled(false);
			}
			jPanelContinuousFileDataMapper.setVisible(false);
		}
		this.setSize(500, 400);
		this.setLocationRelativeTo(null);
	}

	private JPanel getJPanelContinuousFileDataMapper() {
		if (jPanelContinuousFileDataMapper == null) {
			jPanelContinuousFileDataMapper = new JPanel();
			jPanelContinuousFileDataMapper.setBorder(BorderFactory
					.createLineBorder(Color.black));
			GridBagLayout layout = new GridBagLayout();
			jPanelContinuousFileDataMapper.setLayout(layout);
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(2, 2, 2, 2);
			c.weightx = 0.0;
			jPanelContinuousFileDataMapper.add(GuiUtility.makeLabel(
					"lblContinuousFileDataHeader", layout, c));

			ContinuousFileDataJTableModel cfdm = new ContinuousFileDataJTableModel(
					variable, metadata);
			JTable continuousJTable = new JTable(cfdm);

			TableColumnModel columnModel = continuousJTable.getColumnModel();

			TableColumn column = columnModel.getColumn(0);
			column.setCellEditor(new SpinnerEditor(0.0,
					Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
			column.setCellRenderer(new SpinnerRenderer());

			column = columnModel.getColumn(1);
			column.setCellEditor(new ComboBoxEditor(variable.getStates()));
			column.setCellRenderer(new ComboBoxRenderer(variable.getStates()));

			column = columnModel.getColumn(2);
			column.setCellRenderer(new ComponentRenderer());

			continuousJTable
					.addMouseListener(new JTableButtonTableMouseListener(
							continuousJTable));

			continuousJTable.setRowSelectionAllowed(false);
			continuousJTable.setColumnSelectionAllowed(false);
			continuousJTable.setCellSelectionEnabled(false);
			continuousJTable.putClientProperty("terminateEditOnFocusLost",
					Boolean.TRUE);

			c.gridy++;
			c.gridwidth = 2;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.fill = GridBagConstraints.BOTH;
			jPanelContinuousFileDataMapper.add(
					new JScrollPane(continuousJTable), c);
			continuousJTable.setRowHeight(25);
		}
		return jPanelContinuousFileDataMapper;
	}

	private JPanel getJPanelDiscreteFileDataMapper() {
		if (jPanelDiscreteFileDataMapper == null) {
			jPanelDiscreteFileDataMapper = new JPanel();
			jPanelDiscreteFileDataMapper.setBorder(BorderFactory
					.createLineBorder(Color.black));
			GridBagLayout layout = new GridBagLayout();
			jPanelDiscreteFileDataMapper.setLayout(layout);
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(2, 2, 2, 2);
			c.weightx = 0.0;
			jPanelDiscreteFileDataMapper.add(GuiUtility.makeLabel(
					"lblDiscreteFileDataHeader", layout, c));

			DiscreteFileDataJTableModel dfdm = new DiscreteFileDataJTableModel(
					variable, metadata);
			JTable discreteJTable = new JTable(dfdm);

			TableColumnModel columnModel = discreteJTable.getColumnModel();

			TableColumn column = columnModel.getColumn(1);

			column = columnModel.getColumn(1);
			List<String> states = new ArrayList<String>(variable.getStates());
			states.add(0, "");
			column.setCellEditor(new ComboBoxEditor(states));
			column.setCellRenderer(new ComboBoxRenderer(states));

			discreteJTable.setRowSelectionAllowed(false);
			discreteJTable.setColumnSelectionAllowed(false);
			discreteJTable.setCellSelectionEnabled(false);
			discreteJTable.putClientProperty("terminateEditOnFocusLost",
					Boolean.TRUE);

			c.gridy++;
			c.gridwidth = 2;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.fill = GridBagConstraints.BOTH;
			jPanelDiscreteFileDataMapper
					.add(new JScrollPane(discreteJTable), c);
			discreteJTable.setRowHeight(25);

		}
		return jPanelDiscreteFileDataMapper;
	}

	private ActionMap getAppActionMap() {
		return Application.getInstance().getContext().getActionMap(this);
	}

}