package selrach.bnetbuilder.gui.queryDisplays;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;

import selrach.bnetbuilder.gui.tables.ColumnHeaderModel;
import selrach.bnetbuilder.gui.tables.ColumnHeaderToolTips;
import selrach.bnetbuilder.gui.tables.HeaderRenderer;
import selrach.bnetbuilder.gui.tables.HeadlessJTable;
import selrach.bnetbuilder.model.distributions.DistributionDescriptor;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;

/**
 * This shows a factor query result.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class FactorDisplay extends JDialog {

	private static final long serialVersionUID = -1388910674309852042L;

	private final static Logger logger = Logger.getLogger(FactorDisplay.class);

	private final Factor factor;

	private JTable distributionTable;

	private JScrollPane distributionTableView;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				frame.setSize(624, 325);
				FactorDisplay inst = new FactorDisplay(frame, null);
				inst.setVisible(true);
			}
		});
	}

	public FactorDisplay(JFrame frame, Factor factor) {
		super(frame);
		this.factor = factor;
		initGUI();
	}

	private class DistributionTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -508289233735243065L;
		Factor factor = null;
		int time = 0;
		List<List<DistributionDescriptor>> data = null;
		JList rowHeader = null;
		JTable columnHeader = null;
		JList corner = null;
		ColumnHeaderToolTips columnTooltips = null;

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
			return data.get(row).get(col).getValue();
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}

		@Override
		public void setValueAt(Object arg0, int arg1, int arg2) {
		}

		public DistributionTableModel(Factor factor) {
			setFactor(factor);
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

		public void setFactor(final Factor factor) {
			try {
				this.factor = factor;
				this.data = factor.getDistribution()
						.getDistributionDescriptor();

				this.columnTooltips = new ColumnHeaderToolTips();

				this.columnHeader = new JTable(new ColumnHeaderModel(factor
						.getDependencies()));

				this.columnHeader.setDefaultRenderer(Object.class,
						new HeaderRenderer());

				List<String> rhstrings = new ArrayList<String>();
				rhstrings.add("probabilities");
				this.rowHeader = new JList(rhstrings.toArray());

				this.rowHeader.setCellRenderer(new HeaderRenderer());

				List<String> dependencyInfo = new ArrayList<String>();
				for (TransientVariable tv : factor.getDependencies()) {
					RandomVariable rv = tv.getReference();
					dependencyInfo.add(rv.getName() + " in time "
							+ tv.getTime());
				}

				this.corner = new JList(dependencyInfo.toArray());

				this.corner.setCellRenderer(new HeaderRenderer());
				this.corner.setFixedCellHeight(columnHeader.getRowHeight());

				StringBuilder tip = new StringBuilder();
				List<List<String>> states = new ArrayList<List<String>>();
				List<Integer> indices = new ArrayList<Integer>();
				boolean notFirst = false;
				for (TransientVariable tv : factor.getDependencies()) {
					if (notFirst) {
						tip.append("/");
						notFirst = true;
					}
					RandomVariable rv = tv.getReference();
					tip.append(rv.getName() + " in time " + tv.getTime());
					if (rv instanceof DiscreteVariable) {
						states.add(((DiscreteVariable) rv).getStates());
						indices.add(0);
					} else {
						states
								.add(new ArrayList<String>(Arrays
										.asList("none")));
					}
				}

				List<DistributionDescriptor> cols = this.data.get(0);
				List<String> tips = new ArrayList<String>();
				for (DistributionDescriptor sd : cols) {

					StringBuilder key = new StringBuilder();
					boolean notFirst2 = false;
					for (int i = 0; i < states.size(); i++) {
						if (notFirst2) {
							key.append("/");
							notFirst2 = true;
						}
						key.append(states.get(i).get(indices.get(i)));
					}
					{
						for (int i = 0; i < indices.size(); i++) {
							if (indices.get(i) < states.get(i).size() - 1) {
								indices.set(i, indices.get(i) + 1);
								break;
							} else {
								indices.set(i, 0);
							}
						}
					}

					tips.add(tip.toString());
					sd.setKey(key.toString());
				}
				this.columnTooltips.setToolTips(tips);

			} catch (Exception ex) {
				logger.error(ex);
			}
		}

		public JList getRowHeader() {
			return rowHeader;
		}

		public JTable getColumnHeader() {
			return columnHeader;
		}

		public JList getCornerHeader() {
			return corner;
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
					mainPanel.setLayout(layout);
					{

						mainPanel.add(makeLabel("queryDisplay", layout,
								new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
										GridBagConstraints.NORTHEAST,
										GridBagConstraints.BOTH, new Insets(5,
												5, 5, 5), 0, 0)));

						distributionTableView = new JScrollPane();
						mainPanel.add(distributionTableView,
								new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
										GridBagConstraints.CENTER,
										GridBagConstraints.BOTH, new Insets(5,
												5, 5, 5), 0, 0));
						{
							setupDistributionTableModel(distributionTableView);
						}

						mainPanel.add(new JPanel(), new GridBagConstraints(0,
								3, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(5, 5, 5, 5), 0, 0));

						mainPanel.add(makeButton("done", layout,
								new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
										GridBagConstraints.EAST,
										GridBagConstraints.NONE, new Insets(5,
												5, 5, 5), 0, 0)));

					}
				}
			}
			this.setSize(690, 372);
			this.setLocationRelativeTo(null);
			Application.getInstance().getContext().getResourceMap(getClass())
					.injectComponents(getContentPane());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupDistributionTableModel(JScrollPane view) {
		DistributionTableModel distributionTableModel = new DistributionTableModel(
				factor);

		distributionTable = new HeadlessJTable();

		distributionTable.setModel(distributionTableModel);

		distributionTable.setPreferredScrollableViewportSize(new Dimension(
				10000, 4 * distributionTable.getRowHeight()));

		Enumeration<TableColumn> e = distributionTable.getColumnModel()
				.getColumns();
		while (e.hasMoreElements()) {
			e.nextElement().setMaxWidth(75);
		}
		distributionTable.setTableHeader(null);
		view.setViewportView(distributionTable);
		JList cornerView = distributionTableModel.getCornerHeader();
		view.setCorner(JScrollPane.UPPER_LEFT_CORNER, cornerView);

		JTable columnView = distributionTableModel.getColumnHeader();
		e = columnView.getColumnModel().getColumns();
		while (e.hasMoreElements()) {
			e.nextElement().setMaxWidth(75);
		}
		columnView.setPreferredScrollableViewportSize(new Dimension(10,
				columnView.getRowCount() * columnView.getRowHeight()));
		view.setColumnHeaderView(columnView);

		JList rowView = distributionTableModel.getRowHeader();
		int width = (int) Math.max(cornerView.getMinimumSize().getWidth(),
				rowView.getMinimumSize().getWidth());

		rowView.setFixedCellHeight(distributionTable.getRowHeight());
		rowView.setPreferredSize(new Dimension(width, rowView.getHeight()));
		view.setRowHeaderView(rowView);
		view
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// distributionTable.getTableHeader().addMouseMotionListener(
		// ((DistributionTableModel)
		// distributionTableModel).getColumnTooltips());

	}

	@Action
	public void done() {
		dispose();
	}

	/**
	 * Returns the action map used by this application. Actions defined using
	 * the Action annotation are returned by this method
	 */
	private ApplicationActionMap getAppActionMap() {
		return Application.getInstance().getContext().getActionMap(this);
	}

	private JLabel makeLabel(String name, GridBagLayout gridbag,
			GridBagConstraints c) {
		final JLabel label = new JLabel();
		label.setName(name);
		gridbag.setConstraints(label, c);
		return label;
	}

	private JButton makeButton(String name, GridBagLayout gridbag,
			GridBagConstraints c) {
		final JButton button = new JButton();
		button.setAction(getAppActionMap().get(name));
		gridbag.setConstraints(button, c);
		button.setBorder(BorderFactory.createEtchedBorder(BevelBorder.RAISED));
		return button;
	}

}
