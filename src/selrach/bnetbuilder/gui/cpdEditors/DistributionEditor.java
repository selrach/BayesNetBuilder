package selrach.bnetbuilder.gui.cpdEditors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;

import selrach.bnetbuilder.gui.tables.HeaderRenderer;
import selrach.bnetbuilder.gui.utilities.GuiUtility;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * This shows a distribution that can be edited by hand.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DistributionEditor extends javax.swing.JDialog {

	private static final long serialVersionUID = 5118832555504109725L;
	private JScrollPane distributionTableView;
	private JSpinner jSpinnerTime;
	private JTable distributionTable;
	private JComboBox jCBDistribution;
	private JLabel jLabelName;

	private RandomVariable variable = null;
	private int time = 0;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				frame.setSize(624, 325);
				DistributionEditor inst = new DistributionEditor(frame, null, 0);
				inst.setVisible(true);
			}
		});
	}

	public DistributionEditor(JFrame frame, RandomVariable variable, int time) {
		super(frame);
		this.variable = variable;
		this.time = time;
		initGUI();
	}

	private class ColumnHeaderToolTips extends MouseMotionAdapter {
		// Current column index
		int curIndex = -1;

		List<String> tips = new ArrayList<String>();
		String masterTip = "<html>";

		public void setMasterTooltip(String tip) {
			masterTip = "<html>" + tip;
		}

		public void setToolTip(int index, String tip) {
			while (index >= tips.size()) {
				tips.add("");
			}
			tips.set(index, tip);
		}

		public void setToolTips(List<String> tips) {
			this.tips = new ArrayList<String>(tips);
		}

		@Override
		public void mouseMoved(MouseEvent evt) {
			JTableHeader header = (JTableHeader) evt.getSource();
			JTable table = header.getTable();
			TableColumnModel colModel = table.getColumnModel();
			int vColIndex = colModel.getColumnIndexAtX(evt.getX());

			if (vColIndex != curIndex) {
				if (vColIndex < tips.size() && vColIndex > -1) {
					String tip = masterTip;
					tip += tips.get(vColIndex);
					curIndex = vColIndex;
					tip += "</html>";
					header.setToolTipText(tip);
				} else {
					header.setToolTipText(null);
				}
			}
		}
	}

	private class DistributionTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -508289233735243065L;
		RandomVariable variable = null;
		int time = 0;
		List<List<DistributionDescriptor>> data = null;
		JList rowHeader = null;
		ColumnHeaderToolTips columnTooltips = null;

		private boolean hasBeenEdited = false;

		public boolean getHasBeenEdited() {
			return hasBeenEdited;
		}

		public int getColumnCount() {
			if (data.size() == 0) {
				return 0;
			}
			return data.get(0).size();
		}

		public int getRowCount() {
			return data.size();
		}

		public Object getValueAt(int row, int col) {
			return data.get(row).get(col).getValue(); // String.format("%1$06f",
			// data.get(row).get(col).getValue());
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return true;
		}

		@Override
		public void setValueAt(Object arg0, int arg1, int arg2) {
			hasBeenEdited = true;
			data.get(arg1).get(arg2).setValue((Double) arg0); // data.get(arg1).
			// get(arg2).
			// setValue
			// (Double
			// .parseDouble
			// ((
			// String)arg0)
			// );
		}

		public DistributionTableModel(RandomVariable variable, int time) {
			setRandomVariable(variable, time);
		}

		@Override
		public String getColumnName(int column) {
			if (data.size() == 0) {
				return "";
			}
			return data.get(0).get(column).getKey();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@Override
		public Class<?> getColumnClass(int arg0) {
			return Double.class;
		}

		public void setRandomVariable(final RandomVariable variable,
				final int time) {
			try {
				this.time = time;
				this.variable = variable;
				this.data = variable.getDistributionDescriptor(time);
				this.rowHeader = new JList(new AbstractListModel() {

					private static final long serialVersionUID = 1L;
					String[] headers = new String[getRowCount()];

					{
						List<RandomVariable> parents = new ArrayList<RandomVariable>(
								variable.getParents(time));
						// constructHeader(parents.size() - 1, parents, 0, " ");
						constructHeader(0, parents, 0, " ");
						if (headers[0].length() == 0) {
							headers[0] = "";
						}
					}

					private int constructHeader(int parentIndex,
							List<RandomVariable> parents, int headerIndex,
							String str) {
						RandomVariable var = null;
						while (var == null && parentIndex < parents.size()) {
							var = parents.get(parentIndex++);
							if (!(var instanceof DiscreteVariable)) {
								var = null;
							}
						}
						if (var == null) {
							headers[headerIndex] = str.substring(0, str
									.length() - 1);
							return headerIndex + 1;
						}
						DiscreteVariable dv = (DiscreteVariable) var;
						List<String> states = dv.getStates();
						for (String s : states) {
							headerIndex = constructHeader(parentIndex, parents,
									headerIndex, str + s + '/');
						}
						return headerIndex;
					}

					public Object getElementAt(int arg0) {
						return headers[arg0];
					}

					public int getSize() {
						return headers.length;
					}

				}) {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					private String tooltip = null;

					@Override
					public String getToolTipText(MouseEvent evt) {
						if (tooltip == null) {
							StringBuilder sb = new StringBuilder("<html> ");
							boolean addit = false;
							for (int i = 0; i <= time; i++) {
								List<RandomVariable> parents = variable
										.getParentsAt(i);
								for (int j = 0; j < parents.size(); j++) {
									RandomVariable rv = parents.get(j);
									if (rv instanceof DiscreteVariable) {
										if (addit) {
											sb.append(" / ");
										}
										addit = true;
										sb.append("[");
										sb.append(rv.getName());
										sb.append("] time [");
										sb.append(i);
										sb.append("]");
									}
								}
							}
							sb.append(" <br /> ");
							tooltip = sb.toString();
						}
						int row = locationToIndex(evt.getPoint());
						return tooltip + getModel().getElementAt(row)
								+ " </html>";
					}

				};
				this.columnTooltips = new ColumnHeaderToolTips();
				List<String> continuousVariableNames = new ArrayList<String>();
				List<String> times = new ArrayList<String>();
				for (int i = 0; i <= time; i++) {
					List<RandomVariable> parents = variable.getParents(i);
					for (int j = 0; j < parents.size(); j++) {
						RandomVariable rv = parents.get(j);
						if (rv instanceof ContinuousVariable) {
							continuousVariableNames.add(rv.getName());
							times.add(String.valueOf(i));
						}
					}
				}
				List<DistributionDescriptor> cols = this.data.get(0);
				List<String> tips = new ArrayList<String>();
				List<String> states = null;
				if (variable instanceof DiscreteVariable) {
					states = ((DiscreteVariable) variable).getStates();
				}
				for (DistributionDescriptor sd : cols) {
					String info = sd.getInfo();
					String key = sd.getKey();
					for (int i = 0; i < continuousVariableNames.size(); i++) {
						String replace = "\\$\\$" + i + "\\$\\$";
						String timeReplace = "\\$#\\$";
						info = info.replaceAll(replace, continuousVariableNames
								.get(i));
						info = info.replaceAll(timeReplace, times.get(i));
						key = key.replaceAll(replace, continuousVariableNames
								.get(i));
						key = key.replaceAll(timeReplace, times.get(i));
					}
					if (states != null) {
						for (int i = 0; i < states.size(); i++) {
							String replace = "\\$@" + i + "\\$\\$";
							info = info.replaceAll(replace, states.get(i));
							key = key.replaceAll(replace, states.get(i));
						}
					}
					tips.add(info);
					sd.setKey(key);
				}
				this.columnTooltips.setToolTips(tips);

			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(ex.getMessage());
			}
		}

		public void updateModel() {
			try {
				this.variable.getCpd(this.time).setDistributionDescriptor(
						this.data);
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}

		public void randomizeModelCpd() {
			try {
				this.variable.getCpd(this.time).randomize();
				setRandomVariable(variable, time);
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}

		public void resetModelCpd() {
			try {
				this.variable.getCpd(this.time).reset();
				setRandomVariable(variable, time);
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}

		public JList getRowHeader() {
			return rowHeader;
		}

		public ColumnHeaderToolTips getColumnTooltips() {
			return columnTooltips;
		}

	}

	private void initGUI() {
		try {
			{
				JScrollPane scrollpane = new JScrollPane();
				getContentPane().add(scrollpane, BorderLayout.CENTER);
				scrollpane.setPreferredSize(new java.awt.Dimension(475, 225));
				{
					JPanel mainPanel = new JPanel();
					GridBagLayout layout = new GridBagLayout();
					scrollpane.setViewportView(mainPanel);
					GridBagConstraints c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 0;
					c.weightx = 0.0;
					c.weighty = 0.0;
					c.anchor = GridBagConstraints.EAST;
					Insets normalInsets = new Insets(2, 2, 2, 2);
					c.insets = normalInsets;
					mainPanel.setLayout(layout);
					{
						mainPanel.add(GuiUtility.makeLabel("nameHeader",
								layout, c));

						c.gridx++;
						c.anchor = GridBagConstraints.WEST;
						jLabelName = new JLabel(variable.getName());
						mainPanel.add(jLabelName, c);

						c.gridx = 0;
						c.gridy++;
						c.anchor = GridBagConstraints.EAST;
						mainPanel.add(GuiUtility.makeLabel("distTypeHeader",
								layout, c));

						c.gridx++;
						c.fill = GridBagConstraints.HORIZONTAL;
						ComboBoxModel jCBDistributionModel = new DefaultComboBoxModel(
								DistributionFactory.getPotentialCPD(variable,
										time).toArray());
						jCBDistribution = new JComboBox();
						jCBDistribution.setModel(jCBDistributionModel);
						jCBDistribution.setAction(getAppActionMap().get(
								"switchDistribution"));

						mainPanel.add(jCBDistribution, c);

						c.fill = GridBagConstraints.NONE;
						c.gridx++;
						c.insets = new Insets(2, 9, 2, 2);
						mainPanel.add(GuiUtility.makeLabel("timeHeader",
								layout, c));

						c.gridx++;
						c.insets = normalInsets;
						c.anchor = GridBagConstraints.WEST;
						mainPanel.add(getJSpinnerTime(), c);

						c.gridx = 0;
						c.gridy++;
						mainPanel.add(GuiUtility.makeButton(
								"resetDistribution", getAppActionMap(), layout,
								c));

						c.gridx++;
						mainPanel.add(GuiUtility.makeButton(
								"randomizeDistribution", getAppActionMap(),
								layout, c));

						c.gridx = 0;
						c.gridy++;
						c.weightx = 1.0;
						c.weighty = 1.0;
						c.fill = GridBagConstraints.BOTH;
						c.gridwidth = 8;
						distributionTableView = new JScrollPane();

						mainPanel.add(distributionTableView, c);
						{
							setupDistributionTableModel(distributionTableView);
						}

						c.gridwidth = 6;
						c.fill = GridBagConstraints.NONE;
						c.weighty = 0.0;
						c.gridy++;
						mainPanel.add(new JPanel(), c);

						c.gridx += c.gridwidth;
						c.gridwidth = 1;
						c.weightx = 0.0;
						c.anchor = GridBagConstraints.EAST;
						mainPanel.add(GuiUtility.makeButton("save",
								getAppActionMap(), layout, c));

						c.gridx++;
						c.anchor = GridBagConstraints.WEST;
						mainPanel.add(GuiUtility.makeButton("cancel",
								getAppActionMap(), layout, c));

					}
				}
			}
			this.setSize(690, 372);
			Application.getInstance().getContext().getResourceMap(getClass())
					.injectComponents(getContentPane());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final Map<Integer, DistributionTableModel> models = new HashMap<Integer, DistributionTableModel>();

	private void setupDistributionTableModel(JScrollPane view) {

		DistributionTableModel distributionTableModel;

		if (models.containsKey(time)) {
			distributionTableModel = models.get(time);
		} else {
			distributionTableModel = new DistributionTableModel(variable, time);
			models.put(time, distributionTableModel);
		}

		if (distributionTable == null) {
			distributionTable = new JTable();
			view.setViewportView(distributionTable);

			final JTextField tf = new JTextField();
			tf.setBorder(BorderFactory.createEmptyBorder());

			DefaultCellEditor dce = new DefaultCellEditor(tf);

			distributionTable.setDefaultEditor(Object.class, dce);
			distributionTable.putClientProperty("terminateEditOnFocusLost",
					Boolean.TRUE);
		}
		distributionTable.setModel(distributionTableModel);

		distributionTable.setPreferredScrollableViewportSize(new Dimension(10,
				4 * distributionTable.getRowHeight()));

		Enumeration<TableColumn> e = distributionTable.getColumnModel()
				.getColumns();
		while (e.hasMoreElements()) {
			e.nextElement().setMaxWidth(75);
		}
		JList rowHeader = (distributionTableModel).getRowHeader();
		rowHeader.setFixedCellWidth(100);
		rowHeader.setFixedCellHeight(distributionTable.getRowHeight());
		rowHeader.setCellRenderer(new HeaderRenderer(distributionTable));
		distributionTable.getTableHeader().addMouseMotionListener(
				(distributionTableModel).getColumnTooltips());

		view.setRowHeaderView(rowHeader);
	}

	@Action
	public void save() {
		for (DistributionTableModel model : models.values()) {
			model.updateModel();
		}
		dispose();
	}

	@Action
	public void resetDistribution(ActionEvent event) {
		((DistributionTableModel) distributionTable.getModel()).resetModelCpd();
		distributionTable.updateUI();
	}

	@Action
	public void randomizeDistribution(ActionEvent event) {
		((DistributionTableModel) distributionTable.getModel())
				.randomizeModelCpd();
		distributionTable.updateUI();
	}

	/**
	 * Returns the action map used by this application. Actions defined using
	 * the Action annotation are returned by this method
	 */
	private ApplicationActionMap getAppActionMap() {
		return Application.getInstance().getContext().getActionMap(this);
	}

	@Action
	public void cancel() {
		boolean edited = false;
		for (DistributionTableModel model : models.values()) {
			if (model.getHasBeenEdited()) {
				edited = true;
				break;
			}
		}
		if (edited) {
			if (JOptionPane
					.showConfirmDialog(
							this,
							"Are you sure you want to cancel all editing you have done?",
							"Are you sure?", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
				return;
			}
		}
		dispose();
	}

	@Action
	public void switchDistribution() {
		String cpdTypeName = (String) jCBDistribution.getSelectedItem();
		try {
			ConditionalDistribution cpd = DistributionFactory.getCPD(variable,
					time, cpdTypeName);
			if (!cpd.getType().equals(variable.getCpd(time))) {
				variable.setCpd(cpd, time);
				setupDistributionTableModel(distributionTableView);
			}
		} catch (Exception ex) {

		}
	}

	private JSpinner getJSpinnerTime() {
		if (jSpinnerTime == null) {
			SpinnerNumberModel jSpinnerTimeModel = new SpinnerNumberModel(time,
					0, variable.getNumberPotentialCpds(), 1);
			jSpinnerTime = new JSpinner();
			jSpinnerTime.setModel(jSpinnerTimeModel);
			jSpinnerTime.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent arg0) {
					time = (Integer) jSpinnerTime.getValue();
					try {
						DefaultComboBoxModel jCBDistributionModel = new DefaultComboBoxModel(
								DistributionFactory.getPotentialCPD(variable,
										time).toArray());
						jCBDistribution.setModel(jCBDistributionModel);

						ConditionalDistribution cpd = variable.getCpd(time);
						if (cpd == null) {
							cpd = DistributionFactory.getDefaultCPD(variable,
									time); // $hide$
							if (cpd != null) {
								variable.setCpd(cpd, time);
							}
						}
						if (cpd == null) {
							JOptionPane
									.showMessageDialog(getOwner(),
											"There is no default cpd for this type of relation yet!");
							return;
						}
						setupDistributionTableModel(distributionTableView);
					} catch (Exception ex) {
						JOptionPane
								.showMessageDialog(getOwner(),
										"There is no default cpd for this type of relation yet!");
					}
				}

			});
		}
		return jSpinnerTime;
	}

}
