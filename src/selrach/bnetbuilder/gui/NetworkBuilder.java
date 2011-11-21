package selrach.bnetbuilder.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultGraphCell;

import selrach.bnetbuilder.data.TrialDao;
import selrach.bnetbuilder.gui.cpdEditors.DistributionEditor;
import selrach.bnetbuilder.gui.evidence.EvidenceLoader;
import selrach.bnetbuilder.gui.jgraph.DynamicBayesNetJGraph;
import selrach.bnetbuilder.gui.jgraph.DynamicBayesNetJGraphModel;
import selrach.bnetbuilder.gui.notification.ProgressTrackerDialog;
import selrach.bnetbuilder.gui.queryDisplays.FactorDisplay;
import selrach.bnetbuilder.gui.tables.StateTableModel;
import selrach.bnetbuilder.gui.utilities.GuiUtility;
import selrach.bnetbuilder.model.BayesNetSlice;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.exceptions.QueryVariableNotSetException;
import selrach.bnetbuilder.model.algorithms.inference.InferenceAlgorithmFactory;
import selrach.bnetbuilder.model.algorithms.inference.RandomizedAlgorithmConstants;
import selrach.bnetbuilder.model.algorithms.learning.LearningAlgorithmFactory;
import selrach.bnetbuilder.model.dao.DynamicBayesNetDao;
import selrach.bnetbuilder.model.dao.DynamicBayesNetDaoFactory;
import selrach.bnetbuilder.model.dao.IncorrectFileFormatException;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.interfaces.UnconditionalDistribution;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.RandomVariable;
import selrach.bnetbuilder.model.variable.TransientVariable;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Main gui for the bayes net builder, handles the overarching view of
 * constructing a network, learning over a network and infering over a network
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class NetworkBuilder extends SingleFrameApplication {

	private static final Logger logger = Logger.getLogger(NetworkBuilder.class);

	public static void main(final String[] args) {
		launch(NetworkBuilder.class, args);
	}

	// GUI objects
	private JPanel topPanel;
	private JMenuBar menuBar;
	private JMenuItem jMenuItemSaveAs;
	private JMenuItem jMenuItemExit;
	private JMenuItem jMenuItemSave;
	private JMenuItem jMenuItemOpen;
	private JMenuItem jMenuItemNewFile;
	private JMenuItem jMenuItemLoadFiles;
	private JTable jListStates;
	private JRadioButton jRadioBtnSpecificSlice;
	private JRadioButton jRadioBtnShowAllConnections;
	private JPanel jPanelBuildNetworkProperties;
	private JButton jBtnEditCPD;
	private JLabel jLblCPDDescription;
	private JTextArea jTextDescription;
	private JRadioButton jRBIsHidden;
	private JRadioButton jRBIsQuery;
	private JRadioButton jRBIsEvidence;
	private JSpinner jSpinnerTimeslice;
	private JSpinner jSpinner1;
	private JSpinner jSpinnerVariableValue;
	private JSpinner jSpinnerCurrentTimestep;
	private JSpinner jSpinnerMaxTimestep;
	private JSpinner jSpinnerMaxSamples;
	private JSpinner jSpinnerBurnInSamples;
	private JSpinner jSpinnerAvailableTrials;
	private JButton jBtnLoadTrial;
	private JLabel jLblAvailableTrials;
	private JMenu evidenceMenu;
	private JComboBox jCBInferenceAlgorithm;
	private JComboBox jCBLearningAlgorithm;
	private JPanel jInfer;
	private JTabbedPane jTabbedPane1;
	private JPanel jPanelBuildVariableProperties;
	private JTextField jTextName;
	private JPanel jPanelBuild;
	private JSplitPane jSplitPane1;
	private JMenu fileMenu;
	private final DynamicBayesNetModel dynamicBayesNet;
	private JPanel jPanelStates;
	private final DynamicBayesNetJGraph graphDisplay;
	private ChartPanel jPanelVariableChart;
	private JFreeChart variableChart;
	private JPanel jPanelLearnNetworkEvidencePlaceholder;
	private JPanel jPanelInferNetworkEvidencePlaceholder;
	private JPanel jPanelEvidenceLoader;

	private final Color inactiveColor = new Color(239, 239, 239);

	// Data Objects we might manipulate
	private DefaultXYDataset variableChartData;
	private String filename = null;
	private final Map<String, Object> inferencePropertiesMap = new HashMap<String, Object>();
	private final Map<String, Object> learningPropertiesMap = new HashMap<String, Object>();
	private RandomVariable selectedVariable = null;

	// Listeners that we may have to attach and detach at certain points

	private final ChangeListener jSpinnerVariableValueListener = new ChangeListener() {

		public void stateChanged(ChangeEvent e) {
			BayesNetSlice slice = null;
			TransientVariable transientVariable = null;
			try {
				slice = dynamicBayesNet
						.getSlice((Integer) jSpinnerCurrentTimestep.getValue());
				transientVariable = slice.getVariable(selectedVariable.getId());
				if (selectedVariable instanceof DiscreteVariable) {
					transientVariable.setEvidence(Double
							.valueOf(((DiscreteVariable) selectedVariable)
									.getStates().indexOf(
											jSpinnerVariableValue.getValue())));
				} else {
					transientVariable
							.setEvidence((Double) jSpinnerVariableValue
									.getValue());
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}
	};

	private final ItemListener jRBVariableTypeListener = new ItemListener() {

		public void itemStateChanged(ItemEvent e) {
			final JRadioButton btn = (JRadioButton) e.getSource();
			BayesNetSlice slice = null;
			TransientVariable transientVariable = null;
			try {
				slice = dynamicBayesNet
						.getSlice((Integer) jSpinnerCurrentTimestep.getValue());
				transientVariable = slice.getVariable(selectedVariable.getId());
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
			if (transientVariable == null) {
				return;
			}

			jSpinnerVariableValue.setEnabled(false);
			boolean updateFace = false;
			if (btn == jRBIsEvidence) {
				if (btn.isSelected()) {
					try {
						jSpinnerVariableValue.setEnabled(true);
						if (selectedVariable instanceof DiscreteVariable) {
							transientVariable.setEvidence(new Double(0));
							jSpinnerVariableValue
									.setValue(((DiscreteVariable) selectedVariable)
											.getStates().get(0));
						} else {
							final ConditionalDistribution d = selectedVariable
									.getCpd((Integer) jSpinnerCurrentTimestep
											.getValue());
							if (d != null
									&& d instanceof UnconditionalDistribution) {
								transientVariable
										.setEvidence(((UnconditionalDistribution) d)
												.getExpectedValue().getQuick(0));
							} else {
								transientVariable.setEvidence(new Double(0));
							}
							jSpinnerVariableValue.setValue(transientVariable
									.getEvidence());
						}
						updateFace = true;
					} catch (final Exception ex) {
						ex.printStackTrace();
					}
				}
			} else if (btn == jRBIsHidden) {
				if (btn.isSelected()) {
					transientVariable.setHidden();
					updateFace = true;
				}
			} else if (btn == jRBIsQuery) {
				if (btn.isSelected()) {
					transientVariable.setQuery();
					updateFace = true;
				}
			}
			if (updateFace) {
				setupVariableChartData(selectedVariable);
			}
		}
	};

	public NetworkBuilder() {
		dynamicBayesNet = new DynamicBayesNetModel(DynamicBayesNetDaoFactory
				.getDao(DynamicBayesNetDaoFactory.getDaoNameList().get(0)));
		graphDisplay = new DynamicBayesNetJGraph(dynamicBayesNet,
				new DynamicBayesNetJGraphModel());
	}

	/*
	 * Actions in system
	 */

	@Action
	/*
	 * Adds a continuous variable to the network
	 */
	public void addContinuousVariable() {
		dynamicBayesNet.createNewVariable(true);
	}

	@Action
	/*
	 * Adds a discrete variable to the network
	 */
	public void addDiscreteVariable() {
		dynamicBayesNet.createNewVariable(false);
	}

	@Action
	/*
	 * Adds another state to a discrete variable
	 */
	public void addState() {
		if (selectedVariable instanceof DiscreteVariable) {
			final DiscreteVariable var = (DiscreteVariable) selectedVariable;
			var.addState("New State" + var.getStates().size());
		}
	}

	public void deactivatePanel() {
		selectedVariable = null;
		// Builder Panel
		jTextName.setText("");
		jTextName.setBackground(inactiveColor);
		jTextDescription.setEnabled(false);
		jTextDescription.setText("");
		jTextDescription.setBackground(inactiveColor);
		jSpinnerTimeslice.setEnabled(false);
		jBtnEditCPD.setEnabled(false);
		jPanelStates.setVisible(false);
		// End Builder Panel

		// Inference Panel
		jRBIsEvidence.setEnabled(false);
		jRBIsHidden.setEnabled(false);
		jRBIsQuery.setEnabled(false);
		jSpinnerVariableValue.setEnabled(false);
		jPanelVariableChart.setVisible(false);

		// End Inference Panel
	}

	@Action
	/*
	 * Opens up the Distribution editor for a variable so we can make changes by
	 * hand
	 */
	public void editCPD() {
		if (selectedVariable != null) {
			final int time = (Integer) jSpinnerTimeslice.getValue();
			try {
				// ConditionalDistribution cpd = selectedVariable.getCpd(time);

				final DistributionEditor de = new DistributionEditor(this
						.getMainFrame(), selectedVariable, time);
				de.pack();
				de.setLocationRelativeTo(null);
				de.setModalityType(ModalityType.APPLICATION_MODAL);
				de.setVisible(true);
			} catch (final Exception ex) {
				JOptionPane.showMessageDialog(this.getMainFrame(),
						"There was a problem accessing the selected variables cpd, "
								+ ex.getMessage());

			}

		}
	}

	@Action
	/*
	 * Exits the application
	 */
	public void exitApp() {
		this.exit();
	}

	private void generateMenu() {
		menuBar = new JMenuBar();
		{
			fileMenu = new JMenu();
			menuBar.add(fileMenu);
			fileMenu.setName("fileMenu");
			{
				jMenuItemNewFile = new JMenuItem();
				fileMenu.add(jMenuItemNewFile);
				jMenuItemNewFile.setAction(getAppActionMap().get("newFile"));
			}
			{
				jMenuItemOpen = new JMenuItem();
				fileMenu.add(jMenuItemOpen);
				jMenuItemOpen.setAction(getAppActionMap().get("open"));
				jMenuItemOpen.setName("jMenuItem2");
			}
			{
				jMenuItemSave = new JMenuItem();
				fileMenu.add(jMenuItemSave);
				jMenuItemSave.setAction(getAppActionMap().get("save"));
			}
			{
				jMenuItemSaveAs = new JMenuItem();
				fileMenu.add(jMenuItemSaveAs);
				jMenuItemSaveAs.setAction(getAppActionMap().get("saveAs"));
			}
			{
				fileMenu.add(new JSeparator());
			}
			{
				jMenuItemExit = new JMenuItem();
				fileMenu.add(jMenuItemExit);
				jMenuItemExit.setAction(getAppActionMap().get("exitApp"));
			}

			evidenceMenu = new JMenu();
			menuBar.add(evidenceMenu);
			evidenceMenu.setName("evidenceMenu");
			{
				jMenuItemLoadFiles = new JMenuItem();
				evidenceMenu.add(jMenuItemLoadFiles);
				jMenuItemLoadFiles.setAction(getAppActionMap().get(
						"loadEvidence"));
			}
		}
		getMainFrame().setJMenuBar(menuBar);
	}

	private ActionMap getAppActionMap() {
		return Application.getInstance().getContext().getActionMap(this);
	}

	/**
	 * Sets up the build network panel, where you can edit things like the CPD
	 * distributions, names etc.
	 * 
	 * @return
	 */
	private JPanel getJBuild() {
		jPanelBuild = new JPanel();
		final GridBagLayout jPanel1Layout = new GridBagLayout();
		jPanel1Layout.columnWidths = new int[] { 7, 7, 7, 7, 7 };
		jPanel1Layout.rowHeights = new int[] { 7, 7, 7, 7, 7, 7, 7 };
		jPanel1Layout.columnWeights = new double[] { 0.1, 0.1, 0.1, 0.1, 0.1 };
		jPanel1Layout.rowWeights = new double[] { 0, 0, 0, 0, 0, 0, 1 };
		jPanelBuild.setLayout(jPanel1Layout);
		{
			jPanelBuildVariableProperties = new JPanel();
			jPanelBuildVariableProperties.setBorder(BorderFactory
					.createLineBorder(Color.black));
			final GridBagLayout layout = new GridBagLayout();
			jPanelBuildVariableProperties.setLayout(layout);
			jPanelBuild.add(jPanelBuildVariableProperties,
					new GridBagConstraints(0, 4, 5, 1, 0.5, 0.0,
							GridBagConstraints.NORTHEAST,
							GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0,
									0), 0, 0));
			{
				jPanelBuildVariableProperties.add(GuiUtility.makeLabel(
						"nameHeader", layout, new GridBagConstraints(1, 1, 1,
								1, 0.0, 0.0, GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(5, 0, 0, 0), 0, 0)));
			}
			{
				jTextName = new JTextField();
				jPanelBuildVariableProperties.add(jTextName,
						new GridBagConstraints(3, 1, 2, 1, 0.0, 0.0,
								GridBagConstraints.CENTER,
								GridBagConstraints.HORIZONTAL, new Insets(5, 0,
										0, 20), 0, 0));
				jTextName.setEnabled(false);
				jTextName.setBackground(inactiveColor);
				jTextName.setName("jTextName");
				jTextName
						.setMaximumSize(new java.awt.Dimension(120, 2147483647));
				jTextName.setPreferredSize(new java.awt.Dimension(120, 20));
				jTextName.addKeyListener(new KeyAdapter() {

					@Override
					public void keyReleased(final KeyEvent arg0) {
						if (selectedVariable != null) {
							selectedVariable
									.setName(jTextName.getText().trim());
						}
					}

				});
			}
			{
				jPanelBuildVariableProperties.add(GuiUtility.makeLabel(
						"descriptionHeader", layout, new GridBagConstraints(1,
								2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(0, 0, 0, 0), 0, 0)));
			}
			{
				jTextDescription = new JTextArea();
				jTextDescription.setBorder(BorderFactory
						.createLineBorder(Color.lightGray));

				jTextDescription.setLineWrap(true);
				jTextDescription.setWrapStyleWord(true);
				jTextDescription.setEnabled(false);
				jTextDescription.setBackground(inactiveColor);
				jTextDescription.setName("jTextDescription");
				jTextDescription.addKeyListener(new KeyAdapter() {

					@Override
					public void keyReleased(final KeyEvent arg0) {
						if (selectedVariable != null) {
							selectedVariable.setDescription(jTextDescription
									.getText());
						}
					}

				});

				final JScrollPane areaScrollPane = new JScrollPane(
						jTextDescription);
				areaScrollPane
						.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				areaScrollPane.setPreferredSize(new Dimension(30, 100));

				jPanelBuildVariableProperties.add(areaScrollPane,
						new GridBagConstraints(3, 2, 2, 1, 0.4, 0.0,
								GridBagConstraints.EAST,
								GridBagConstraints.BOTH,
								new Insets(5, 0, 5, 1), 0, 0));

			}
			{
				jPanelBuildVariableProperties.add(GuiUtility.makeLabel(
						"distributionHeader", layout, new GridBagConstraints(1,
								4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(5, 0, 0, 0), 0, 0)));
			}
			{
				jLblCPDDescription = GuiUtility.makeLabel("jLblCPDDescription",
						layout, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
								GridBagConstraints.WEST,
								GridBagConstraints.NONE,
								new Insets(5, 0, 0, 0), 0, 0));
				jPanelBuildVariableProperties.add(jLblCPDDescription);
			}
			{
				jBtnEditCPD = GuiUtility.makeButton("editCPD",
						getAppActionMap(), layout, new GridBagConstraints(3, 5,
								1, 1, 0.0, 0.0, GridBagConstraints.WEST,
								GridBagConstraints.NONE,
								new Insets(0, 0, 0, 0), 0, 0));
				jPanelBuildVariableProperties.add(jBtnEditCPD);
				jBtnEditCPD.setEnabled(false);
			}
			{
				jPanelBuildVariableProperties.add(GuiUtility.makeLabel(
						"timeSliceHeader", layout, new GridBagConstraints(1, 3,
								1, 1, 0.0, 0.0, GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(5, 0, 0, 0), 0, 0)));
			}
			{
				final SpinnerNumberModel jSpinner2Model = new SpinnerNumberModel(
						0, 0, 1, 1);
				jSpinnerTimeslice = new JSpinner();
				jPanelBuildVariableProperties.add(jSpinnerTimeslice,
						new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
								GridBagConstraints.WEST,
								GridBagConstraints.NONE,
								new Insets(0, 0, 0, 0), 0, 0));
				jSpinnerTimeslice.setModel(jSpinner2Model);
				jSpinnerTimeslice.setEnabled(false);
				jSpinnerTimeslice.addChangeListener(new ChangeListener() {

					public void stateChanged(final ChangeEvent e) {
						try {
							String cpdstr = "Not yet set";
							if (selectedVariable != null) {
								final ConditionalDistribution cd = selectedVariable
										.getCpd((Integer) jSpinnerTimeslice
												.getValue());
								if (cd != null) {
									cpdstr = cd.getType();
								}
							}
							jLblCPDDescription.setText(cpdstr);

						} catch (final Exception ex) {

							if (logger.isInfoEnabled()) {
								logger
										.info(
												"Problem setting variable distribution notice",
												ex);
							}
						}
					}
				});
			}
			{
				jPanelStates = new JPanel();
				final GridBagLayout jPanelStatesLayout = new GridBagLayout();
				jPanelStates.setLayout(jPanelStatesLayout);
				jPanelBuildVariableProperties.add(jPanelStates,
						new GridBagConstraints(1, 6, 4, 2, 0.0, 0.0,
								GridBagConstraints.CENTER,
								GridBagConstraints.BOTH,
								new Insets(0, 0, 0, 0), 0, 0));

				{
					jPanelStates.add(GuiUtility.makeLabel("statesHeader",
							jPanelStatesLayout, new GridBagConstraints(0, 0, 1,
									1, 0.0, 0.0, GridBagConstraints.EAST,
									GridBagConstraints.NONE, new Insets(5, 0,
											0, 0), 0, 0)));
				}
				{
					jPanelStates.add(GuiUtility.makeButton("addState",
							getAppActionMap(), jPanelStatesLayout,
							new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
									GridBagConstraints.EAST,
									GridBagConstraints.NONE, new Insets(0, 0,
											0, 0), 0, 0)));
				}
				{
					jPanelStates.add(GuiUtility.makeButton("removeState",
							getAppActionMap(), jPanelStatesLayout,
							new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
									GridBagConstraints.WEST,
									GridBagConstraints.NONE, new Insets(0, 0,
											0, 0), 0, 0)));

				}
				{
					final AbstractTableModel jListStatesModel = new StateTableModel();
					jListStates = new JTable();
					jListStates.setModel(jListStatesModel);
					jListStates
							.setPreferredScrollableViewportSize(new Dimension(
									150, 5 * jListStates.getRowHeight()));
					jListStates.getColumnModel().getColumn(0).setWidth(140);
					jListStates.setTableHeader(null);
					jPanelStates.add(new JScrollPane(jListStates),
							new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,
									GridBagConstraints.CENTER,
									GridBagConstraints.BOTH, new Insets(5, 0,
											0, 0), 0, 0));
				}
				jPanelStates.setVisible(false);

			}
		}
		{
			jPanelBuild.add(GuiUtility.makeLabel("varPropHeader",
					jPanel1Layout, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE,
							new Insets(9, 5, 0, 0), 0, 0)));
		}
		{
			jPanelBuild.add(GuiUtility.makeLabel("networkPropHeader",
					jPanel1Layout, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE,
							new Insets(9, 5, 0, 0), 0, 0)));
		}
		{
			jPanelBuildNetworkProperties = new JPanel();
			final GridBagLayout layout = new GridBagLayout();
			jPanelBuild.add(jPanelBuildNetworkProperties,
					new GridBagConstraints(0, 2, 5, 1, 0.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.BOTH,
							new Insets(0, 0, 0, 0), 0, 0));
			jPanelBuildNetworkProperties.setBorder(BorderFactory
					.createLineBorder(Color.black));
			layout.rowWeights = new double[] { 0, 0, 0, 0, 0.1 };
			layout.rowHeights = new int[] { 7, 7, 7, 7, 7 };
			layout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.1, 0.1, 1 };
			layout.columnWidths = new int[] { 7, 75, 7, 7, 7, 7 };
			jPanelBuildNetworkProperties.setLayout(layout);
			{
				jPanelBuildNetworkProperties.add(GuiUtility.makeButton(
						"addDiscreteVariable", getAppActionMap(), layout,
						new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
								GridBagConstraints.NORTHEAST,
								GridBagConstraints.NONE,
								new Insets(5, 5, 5, 5), 0, 0)));
			}
			{
				jPanelBuildNetworkProperties.add(GuiUtility.makeButton(
						"addContinuousVariable", getAppActionMap(), layout,
						new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
								GridBagConstraints.NORTHWEST,
								GridBagConstraints.NONE,
								new Insets(5, 5, 5, 5), 0, 0)));
			}
			{
				jPanelBuildNetworkProperties.add(GuiUtility.makeLabel(
						"editConnectionsHeader", layout,
						new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
								GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(0, 4, 0, 0), 0, 0)));
			}
			{
				jRadioBtnSpecificSlice = new JRadioButton();
				jPanelBuildNetworkProperties.add(jRadioBtnSpecificSlice,
						new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
								GridBagConstraints.WEST,
								GridBagConstraints.NONE,
								new Insets(0, 4, 0, 0), 0, 0));
				jRadioBtnSpecificSlice.setName("jRadioBtnSpecificSlice");
				jRadioBtnSpecificSlice.setSelected(true);
			}
			{
				jRadioBtnShowAllConnections = new JRadioButton();
				jPanelBuildNetworkProperties.add(jRadioBtnShowAllConnections,
						new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
								GridBagConstraints.WEST,
								GridBagConstraints.NONE,
								new Insets(0, 4, 0, 0), 0, 0));
				jRadioBtnShowAllConnections
						.setName("jRadioBtnShowAllConnections");
			}
			{
				final ButtonGroup timesliceGroup = new ButtonGroup();
				timesliceGroup.add(jRadioBtnShowAllConnections);
				timesliceGroup.add(jRadioBtnSpecificSlice);
				jRadioBtnSpecificSlice.setAction(getAppActionMap().get(
						"timesliceSelect"));
				jRadioBtnShowAllConnections.setAction(getAppActionMap().get(
						"timesliceSelect"));
			}
			{
				final SpinnerNumberModel jSpinner1Model = new SpinnerNumberModel(
						0, 0, 1, 1);
				jSpinner1 = new JSpinner();
				jPanelBuildNetworkProperties.add(jSpinner1,
						new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
								GridBagConstraints.EAST,
								GridBagConstraints.NONE,
								new Insets(0, 0, 0, 0), 0, 0));
				jSpinner1.setPreferredSize(new Dimension(70, 20));
				jSpinner1.setModel(jSpinner1Model);
				jSpinner1.setEnabled(true);
				jSpinner1.addChangeListener(new ChangeListener() {

					public void stateChanged(final ChangeEvent e) {
						if (jRadioBtnSpecificSlice.isSelected()) {
							dynamicBayesNet
									.setTemplateSlice((Integer) jSpinner1
											.getValue());
							graphDisplay
									.setTimeSliceVisible((Integer) jSpinner1
											.getValue());
						}
					}
				});
			}
		}
		return jPanelBuild;
	}

	private JComboBox getJCBInferenceAlgorithm() {
		if (jCBInferenceAlgorithm == null) {
			final ComboBoxModel jCBAlgorithmModel = new DefaultComboBoxModel(
					InferenceAlgorithmFactory.getAlgorithmNameList().toArray());
			jCBInferenceAlgorithm = new JComboBox();
			jCBInferenceAlgorithm.setModel(jCBAlgorithmModel);
		}
		return jCBInferenceAlgorithm;
	}

	private JComboBox getJCBLearningAlgorithm() {
		if (jCBLearningAlgorithm == null) {
			final ComboBoxModel jCBAlgorithmModel = new DefaultComboBoxModel(
					LearningAlgorithmFactory.getAlgorithmNameList().toArray());
			jCBLearningAlgorithm = new JComboBox();
			jCBLearningAlgorithm.setModel(jCBAlgorithmModel);
		}
		return jCBLearningAlgorithm;
	}

	/**
	 * Sets up the inference panel, where various queries can be run from
	 * 
	 * @return
	 */
	private JPanel getJInfer() {
		jInfer = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		jInfer.setLayout(layout);
		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.weightx = 1.0;

		jInfer.add(GuiUtility.makeLabel("networkHeader", layout, c));

		c.fill = GridBagConstraints.BOTH;
		c.gridy++;

		jInfer.add(getJPanelInferNetworkInfo(), c);

		c.fill = GridBagConstraints.NONE;
		c.gridy++;

		jInfer.add(GuiUtility.makeLabel("varInfoHeader", layout, c));

		c.gridy++;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;

		jInfer.add(getJPanelInferVariableInfo(), c);

		c.gridy++;
		jInfer.add(new JPanel(), c);

		return jInfer;
	}

	private JPanel getJLearn() {
		jInfer = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		jInfer.setLayout(layout);
		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.weightx = 1.0;

		jInfer.add(GuiUtility.makeLabel("networkHeader", layout, c));

		c.fill = GridBagConstraints.BOTH;
		c.gridy++;

		jInfer.add(getJPanelLearnNetworkInfo(), c);

		c.fill = GridBagConstraints.NONE;
		c.gridy++;

		jInfer.add(GuiUtility.makeLabel("varInfoHeader", layout, c));

		c.gridy++;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;

		jInfer.add(getJPanelLearnVariableInfo(), c);

		c.gridy++;
		jInfer.add(new JPanel(), c);

		return jInfer;
	}

	private JPanel getJPanelEvidenceLoader() {
		if (jPanelEvidenceLoader == null) {
			final GridBagLayout layout = new GridBagLayout();
			final GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.gridheight = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1.0;
			c.weighty = 0.0;
			c.insets = new Insets(2, 2, 2, 2);

			jPanelEvidenceLoader = new JPanel();

			jPanelEvidenceLoader.setLayout(layout);

			jPanelEvidenceLoader.add(GuiUtility.makeButton("loadEvidence",
					getAppActionMap(), layout, c));

			c.gridy++;
			c.gridwidth = 1;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.EAST;

			jPanelEvidenceLoader.add(jLblAvailableTrials = GuiUtility
					.makeLabel("availableTrialsHeader", layout, c));

			c.gridx++;
			c.anchor = GridBagConstraints.WEST;

			jPanelEvidenceLoader.add(getJSpinnerAvailableTrials(), c);

			c.gridy++;

			jPanelEvidenceLoader.add(jBtnLoadTrial = GuiUtility.makeButton(
					"loadTrial", getAppActionMap(), layout, c));

			c.gridx = 0;
			c.gridy++;
			c.anchor = GridBagConstraints.EAST;

			jPanelEvidenceLoader.add(GuiUtility.makeLabel("maxTimestepsHeader",
					layout, c));

			c.gridx++;
			c.anchor = GridBagConstraints.WEST;

			jPanelEvidenceLoader.add(getJSpinnerMaxTimestep(), c);

		}
		return jPanelEvidenceLoader;
	}

	private JPanel getJPanelInferNetworkInfo() {
		final JPanel jPanelInferNetworkInfo = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridheight = 1;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(2, 2, 2, 2);

		jPanelInferNetworkInfo.setLayout(layout);
		jPanelInferNetworkInfo.setBorder(new LineBorder(new java.awt.Color(0,
				0, 0), 1, false));

		jPanelInferNetworkEvidencePlaceholder = new JPanel();

		jPanelInferNetworkInfo.add(jPanelInferNetworkEvidencePlaceholder, c);

		jPanelInferNetworkEvidencePlaceholder.add(getJPanelEvidenceLoader());

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.EAST;

		jPanelInferNetworkInfo.add(GuiUtility.makeLabel("algorithmHeader",
				layout, c));

		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;

		jPanelInferNetworkInfo.add(getJCBInferenceAlgorithm(), c);

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;

		jPanelInferNetworkInfo.add(GuiUtility.makeLabel("maxSamplesHeader",
				layout, c));

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;

		jPanelInferNetworkInfo.add(getJSpinnerMaxSamples(), c);

		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.EAST;

		jPanelInferNetworkInfo.add(GuiUtility.makeLabel("burnInSamplesHeader",
				layout, c));

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		jPanelInferNetworkInfo.add(getJSpinnerBurnInSamples(), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		jPanelInferNetworkInfo.add(GuiUtility.makeButton("runQuery",
				getAppActionMap(), layout, c));

		c.gridx++;

		jPanelInferNetworkInfo.add(GuiUtility.makeButton(
				"runAllMarginalization", getAppActionMap(), layout, c));

		return jPanelInferNetworkInfo;
	}

	private JPanel getJPanelInferVariableInfo() {
		final JPanel jPanelVariableInfo = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		jPanelVariableInfo.setLayout(layout);
		jPanelVariableInfo.setBorder(new LineBorder(
				new java.awt.Color(0, 0, 0), 1, false));

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2, 2, 2, 2);

		jPanelVariableInfo.add(GuiUtility.makeLabel("currentTimestepHeader",
				layout, c));

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;

		jPanelVariableInfo.add(getJSpinnerCurrentTimestep(), c);

		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.CENTER;

		jPanelVariableInfo.add(getJRBIsEvidence(), c);

		c.gridx++;

		jPanelVariableInfo.add(getJRBIsQuery(), c);

		c.gridx++;

		jPanelVariableInfo.add(getJRBIsHidden(), c);

		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.EAST;

		jPanelVariableInfo.add(GuiUtility.makeLabel("variableValueHeader",
				layout, c));

		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;

		jPanelVariableInfo.add(getJSpinnerVariableValue(), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		jPanelVariableInfo.add(new JPanel());

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 3;

		jPanelVariableInfo.add(getJScrollVariableChart(), c);

		final ButtonGroup rbg = new ButtonGroup();
		rbg.add(jRBIsEvidence);
		rbg.add(jRBIsQuery);
		rbg.add(jRBIsHidden);
		return jPanelVariableInfo;
	}

	private JPanel getJPanelLearnNetworkInfo() {
		final JPanel panel = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridheight = 1;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(2, 2, 2, 2);

		panel.setLayout(layout);
		panel.setBorder(new LineBorder(new java.awt.Color(0, 0, 0), 1, false));

		jPanelLearnNetworkEvidencePlaceholder = new JPanel();

		panel.add(jPanelLearnNetworkEvidencePlaceholder, c);

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		panel.add(GuiUtility.makeLabel("algorithmHeader", layout, c));

		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(getJCBLearningAlgorithm(), c);

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		panel.add(GuiUtility.makeButton("runLearningAlgorithm",
				getAppActionMap(), layout, c));

		return panel;
	}

	private JPanel getJPanelLearnVariableInfo() {
		final JPanel panel = new JPanel();

		return panel;
	}

	private JRadioButton getJRBIsEvidence() {
		if (jRBIsEvidence == null) {
			jRBIsEvidence = new JRadioButton();
			jRBIsEvidence.setName("jRBIsEvidence");
			jRBIsEvidence.addItemListener(jRBVariableTypeListener);
		}
		return jRBIsEvidence;
	}

	private JRadioButton getJRBIsHidden() {
		if (jRBIsHidden == null) {
			jRBIsHidden = new JRadioButton();
			jRBIsHidden.setName("jRBIsHidden");
			jRBIsHidden.setSelected(true);
			jRBIsHidden.addItemListener(jRBVariableTypeListener);
		}
		return jRBIsHidden;
	}

	// Build the Gui

	private JRadioButton getJRBIsQuery() {
		if (jRBIsQuery == null) {
			jRBIsQuery = new JRadioButton();
			jRBIsQuery.setName("jRBIsQuery");
			jRBIsQuery.addItemListener(jRBVariableTypeListener);
		}
		return jRBIsQuery;
	}

	// Build network GUI Components

	private JScrollPane getJScrollVariableChart() {

		variableChartData = new DefaultXYDataset();
		final double data[][] = new double[2][];
		data[0] = new double[1];
		data[0][0] = 0.0;
		data[1] = new double[1];
		data[1][0] = 0.0;

		variableChartData.addSeries("None", data);

		variableChart = ChartFactory.createXYLineChart(
				"Marginal Probabilities", "Time", "Probability",
				variableChartData, PlotOrientation.VERTICAL, true, true, false);

		final XYPlot cp = ((XYPlot) variableChart.getPlot());

		// cp.getRangeAxis().setDefaultAutoRange(new Range(0, 1));
		// cp.getRangeAxis().setAutoRangeMinimumSize(1.0);
		// cp.getRangeAxis().setLowerBound(0.0);
		// cp.getRangeAxis().setUpperBound(1.0);
		// cp.getRangeAxis().setFixedAutoRange(1.0);
		// cp.getRangeAxis().setRange(new Range(0, 1), true, true);

		cp.getDomainAxis().setLowerMargin(0.1);
		cp.getDomainAxis().setUpperMargin(0.1);
		cp.getDomainAxis().setAutoRangeMinimumSize(1.0);
		cp.getDomainAxis().setAutoRange(true);
		cp.setRangeCrosshairVisible(true);
		((XYLineAndShapeRenderer) cp.getRenderer()).setBaseShapesVisible(true);

		jPanelVariableChart = new ChartPanel(variableChart, true, true, true,
				false, true) {
			private static final long serialVersionUID = -2984754571610707548L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.jfree.chart.ChartPanel#restoreAutoRangeBounds()
			 */
			@Override
			public void restoreAutoRangeBounds() {
				super.restoreAutoRangeBounds();
				// cp.getRangeAxis().setRange(new Range(0, 1), false, true);
			}

		};
		jPanelVariableChart.setDomainZoomable(true);
		jPanelVariableChart.setRangeZoomable(false);
		jPanelVariableChart.setPreferredSize(new Dimension(80, 200));

		final JScrollPane scrollpane = new JScrollPane(jPanelVariableChart);
		scrollpane.setPreferredSize(new Dimension(80, 220));

		return scrollpane;
	}

	// Learning GUI Panel

	private JSpinner getJSpinnerAvailableTrials() {
		if (jSpinnerAvailableTrials == null) {
			final SpinnerModel jSpinnerAvailableTrialsModel = new SpinnerNumberModel(
					1, 1, 1, 1) {

				private static final long serialVersionUID = 1923142687240437309L;

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.SpinnerNumberModel#getMaximum()
				 */
				@SuppressWarnings("unchecked")
				@Override
				public Comparable getMaximum() {
					if (dynamicBayesNet.getTrialDao() == null) {
						return 0;
					}
					this.setMaximum(dynamicBayesNet.getTrialDao()
							.getNumberTrials());
					return dynamicBayesNet.getTrialDao().getNumberTrials();
				}

			};
			jSpinnerAvailableTrials = new JSpinner();
			jSpinnerAvailableTrials.setModel(jSpinnerAvailableTrialsModel);
			jSpinnerAvailableTrials.setEnabled(false);
		}
		return jSpinnerAvailableTrials;
	}

	private JSpinner getJSpinnerBurnInSamples() {
		if (jSpinnerBurnInSamples == null) {
			final SpinnerModel jSpinnerBurnInSamplesModel = new SpinnerNumberModel(
					10000, 0, 999999999, 100) {
				private static final long serialVersionUID = -7546247507516084543L;

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.SpinnerNumberModel#setMaximum(java.lang.Comparable
				 * )
				 */
				@SuppressWarnings("unchecked")
				@Override
				public void setMaximum(Comparable maximum) {
					if ((Integer) maximum < (Integer) this.getValue()) {
						this.setValue(maximum);
					}
					super.setMaximum(maximum);
				}
			};

			jSpinnerBurnInSamples = new JSpinner(jSpinnerBurnInSamplesModel);
			jSpinnerBurnInSamples.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					try {
						inferencePropertiesMap.put(
								RandomizedAlgorithmConstants.BURN_IN_TIME
										.toString(), jSpinnerBurnInSamples
										.getValue());
					} catch (final Exception ex) {
						logger.error("Problem setting inference property map",
								ex);
					}
				}
			});
		}
		return jSpinnerBurnInSamples;
	}

	private JSpinner getJSpinnerCurrentTimestep() {
		if (jSpinnerCurrentTimestep == null) {
			final SpinnerModel jSpinnerCurrentTimestepModel = new SpinnerNumberModel(
					0, 0, 99999, 1) {

				private static final long serialVersionUID = -3834424105363567421L;

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.SpinnerNumberModel#getMaximum()
				 */
				@SuppressWarnings("unchecked")
				@Override
				public Comparable getMaximum() {
					final int max = dynamicBayesNet.getMaxNumberSlices() - 1;
					this.setMaximum(max);
					return super.getMaximum();
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.SpinnerNumberModel#getNextValue()
				 */
				@Override
				public Object getNextValue() {
					final int max = dynamicBayesNet.getMaxNumberSlices() - 1;
					Integer val = (Integer) super.getNextValue();
					if (val == null) {
						return val;
					}
					val = val > max ? max : val;
					return val;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.SpinnerNumberModel#getPreviousValue()
				 */
				@Override
				public Object getPreviousValue() {
					final int max = dynamicBayesNet.getMaxNumberSlices() - 1;
					Integer val = (Integer) super.getPreviousValue();
					if (val == null) {
						return val;
					}
					val = val > max ? max : val;
					return val;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.SpinnerNumberModel#setMaximum(java.lang.Comparable
				 * )
				 */
				@SuppressWarnings("unchecked")
				@Override
				public void setMaximum(Comparable maximum) {
					if ((Integer) maximum < (Integer) this.getValue()) {
						this.setValue(maximum);
					}
					super.setMaximum(maximum);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.SpinnerNumberModel#setValue(java.lang.Object)
				 */
				@Override
				public void setValue(Object value) {
					final int max = dynamicBayesNet.getMaxNumberSlices() - 1;
					value = (Integer) value > max ? max : value;
					super.setValue(value);
				}

			};
			jSpinnerCurrentTimestep = new JSpinner();
			jSpinnerCurrentTimestep.setModel(jSpinnerCurrentTimestepModel);
			jSpinnerCurrentTimestep.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {

					final RandomVariable v = selectedVariable;
					deactivatePanel();
					selectVariable(v);

				}
			});
		}
		return jSpinnerCurrentTimestep;
	}

	private JSpinner getJSpinnerMaxSamples() {
		if (jSpinnerMaxSamples == null) {
			final SpinnerModel jMaxSamplesModel = new SpinnerNumberModel(
					100000, 100, 999999999, 100) {
				private static final long serialVersionUID = 840093422836931251L;

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.SpinnerNumberModel#setValue(java.lang.Object)
				 */
				@Override
				public void setValue(Object value) {
					super.setValue(value);
					((SpinnerNumberModel) jSpinnerBurnInSamples.getModel())
							.setMaximum((Integer) value - 1);
				}

			};

			jSpinnerMaxSamples = new JSpinner(jMaxSamplesModel);
			jSpinnerMaxSamples.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					try {
						inferencePropertiesMap.put(
								RandomizedAlgorithmConstants.MAX_SAMPLES
										.toString(), jSpinnerMaxSamples
										.getValue());
					} catch (final Exception ex) {
						logger.error("Problem setting inference property map",
								ex);
					}
				}
			});
		}
		return jSpinnerMaxSamples;
	}

	// Inference GUI Panel

	private JSpinner getJSpinnerMaxTimestep() {
		if (jSpinnerMaxTimestep == null) {
			final SpinnerModel jMaxTimestepModel = new SpinnerNumberModel(1, 1,
					9999, 1) {

				/**
				 * 
				 */
				private static final long serialVersionUID = -3991325324758031460L;

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.SpinnerNumberModel#setValue(java.lang.Object)
				 */
				@Override
				public void setValue(Object value) {
					super.setValue(value);
					dynamicBayesNet.setMaxNumberSlices((Integer) value);
					((SpinnerNumberModel) jSpinnerCurrentTimestep.getModel())
							.setMaximum((Integer) value - 1);
				}

			};
			jSpinnerMaxTimestep = new JSpinner();
			jSpinnerMaxTimestep.setModel(jMaxTimestepModel);
			jSpinnerMaxTimestep.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					try {
						dynamicBayesNet.getJunctionTreeTemplate()
								.setCalibratedFalse();

					} catch (final Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}
		return jSpinnerMaxTimestep;
	}

	private JSpinner getJSpinnerVariableValue() {
		if (jSpinnerVariableValue == null) {
			final SpinnerModel jSpinnerVariableValueModel = new SpinnerNumberModel(
					0, 0, 1, 1);
			jSpinnerVariableValue = new JSpinner();
			jSpinnerVariableValue.setModel(jSpinnerVariableValueModel);
			jSpinnerVariableValue
					.addChangeListener(jSpinnerVariableValueListener);
			jSpinnerVariableValue.setPreferredSize(new Dimension(100, 25));
			jSpinnerVariableValue.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					setupVariableChartData(selectedVariable);
				}
			});
		}
		return jSpinnerVariableValue;
	}

	@Action
	/*
	 * Opens up the evidence loader dialog
	 */
	public void loadEvidence() {
		try {
			final EvidenceLoader evidenceLoader = new EvidenceLoader(this
					.getMainFrame(), dynamicBayesNet);
			evidenceLoader.setModalityType(ModalityType.APPLICATION_MODAL);
			evidenceLoader.setVisible(true);

			final int avail = dynamicBayesNet.getTrialDao().getNumberTrials();
			jLblAvailableTrials.setText("Available Trials(" + avail + "):");
			jSpinnerAvailableTrials.setEnabled(avail > 0);
			jBtnLoadTrial.setEnabled(avail > 0);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	@Action
	/*
	 * Loads up a particular trial from the evidence data files loaded up
	 */
	public void loadTrial() {
		if (!jSpinnerAvailableTrials.isEnabled()) {
			return;
		}
		final int trial = (Integer) jSpinnerAvailableTrials.getValue();
		final TrialDao dao = dynamicBayesNet.getTrialDao();
		jSpinnerMaxTimestep.setValue(dao.getNumberTimesteps(trial));
		dao.setAllEvidence(trial);

	}

	@Action
	public void newFile() {
		try {
			dynamicBayesNet.clear();
			selectedVariable = null;
			filename = null;
		} catch (final Exception ex) {

		}
	}

	@Action
	public void open() {
		try {
			JFileChooser jFileChooser;
			if (filename == null) {
				jFileChooser = new JFileChooser(System.getProperty("user.dir"));
			} else {
				jFileChooser = new JFileChooser(filename);
			}
			if (jFileChooser.showOpenDialog(this.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
				filename = jFileChooser.getSelectedFile().getAbsolutePath();
				if (filename != null) {
					try {
						dynamicBayesNet.loadModel(filename);
					} catch (final IncorrectFileFormatException iffe) {
						final DynamicBayesNetDao oldDao = dynamicBayesNet
								.getDao();
						boolean successful = false;
						for (final DynamicBayesNetDao dao : DynamicBayesNetDaoFactory
								.getDaoList()) {
							if (dao != oldDao) {
								try {
									dynamicBayesNet.setDao(dao);
									dynamicBayesNet.loadModel(filename);
									successful = true;
								} catch (final IncorrectFileFormatException iffe2) {

								}
								if (successful) {
									break;
								}
							}
						}
						if (!successful) {
							dynamicBayesNet.setDao(oldDao);
							// Perhaps put a loop that tries out different daos
							throw iffe;
						}
					}
				}
			}
		} catch (final Exception ex) {
			JOptionPane.showMessageDialog(this.getMainFrame(),
					"There was a problem loading the model file, "
							+ ex.getMessage(), "Error Loading File",
					JOptionPane.WARNING_MESSAGE);
			if (logger.isInfoEnabled()) {
				logger.debug("Problem loading model file: " + filename, ex);
			}
		}
	}

	@Action
	public void removeState() {
		if (selectedVariable instanceof DiscreteVariable) {
			final DiscreteVariable var = (DiscreteVariable) selectedVariable;
			var.removeState(jListStates.getSelectedRow());
		}
	}

	@Action
	public void runAllMarginalization() {

		//final ModalWarning warning = new ModalWarning(getMainFrame(),
		//		"Running marginalization, please be patient.");

		final ProgressTrackerDialog warning = new ProgressTrackerDialog(
				getMainFrame(), "Running marginalization, please be patient.");
		final String selected = (String) jCBInferenceAlgorithm
				.getSelectedItem();
		warning.setModalityType(ModalityType.APPLICATION_MODAL);

		final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {

			@Override
			protected Object doInBackground() throws Exception {

				try {
					Thread.sleep(1);
					
					Date start = new Date();
					InferenceAlgorithmFactory.getAlgorithm(selected).execute(
							dynamicBayesNet, true, inferencePropertiesMap,
							warning.getTrackingPrintStream());
					Date end = new Date();
					
					warning.getTrackingPrintStream().println("Took " + ((double)(end.getTime() - start.getTime()) / 1000) + " seconds...");

					setupVariableChartData(selectedVariable);

					warning.enableClosing();
					/*
					warning.dispose();
					JOptionPane
							.showMessageDialog(getMainFrame(),
									"Inference Done", "Finished",
									JOptionPane.OK_OPTION);
					*/

				} catch (final Exception ex) {
					warning.getTrackingPrintStream().println("There was a problem with the query.");
					logger.debug("Problem with query. ", ex);
				}

				return null;
			}

			@Override
			protected void done() {
				warning.getTrackingPrintStream().println("Marginalization Done!");
				warning.enableClosing();
				//warning.dispose();
			}

		};
		worker.execute();
		warning.setVisible(true);

	}

	@Action
	public void runLearningAlgorithm(ActionEvent event) {

		final ProgressTrackerDialog warning = new ProgressTrackerDialog(
				getMainFrame(), "Running learning task, please be patient.");
		final String selected = (String) jCBLearningAlgorithm.getSelectedItem();
		warning.setModalityType(ModalityType.APPLICATION_MODAL);

		final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {

			@Override
			protected Object doInBackground() throws Exception {

				try {
					Thread.sleep(1);

					Date start = new Date();
					LearningAlgorithmFactory.getAlgorithm(selected).execute(
							dynamicBayesNet, learningPropertiesMap,
							warning.getTrackingPrintStream());
					Date end = new Date();
					
					warning.getTrackingPrintStream().println("Took " + ((double)(end.getTime() - start.getTime()) / 1000) + " seconds...");

					warning.enableClosing();

				} catch (final Exception ex) {
					logger.info("Problem with learning. ", ex);
				}

				return null;
			}

			@Override
			protected void done() {
				warning.getTrackingPrintStream().println("Learning Done!");
				warning.enableClosing();
			}

		};
		worker.execute();
		warning.setVisible(true);
	}

	@Action
	public void runQuery() {
		
		//final ModalWarning warning = new ModalWarning(this.getMainFrame(),
		//		"Running query, please be patient.");

		final ProgressTrackerDialog warning = new ProgressTrackerDialog(
				getMainFrame(), "Running query, please be patient.");
		final String selected = (String) jCBInferenceAlgorithm
				.getSelectedItem();
		warning.setModalityType(ModalityType.APPLICATION_MODAL);

		final SwingWorker<Factor, Void> worker = new SwingWorker<Factor, Void>() {

			@Override
			protected Factor doInBackground() throws Exception {

				Factor f = null;
				try {
					Thread.sleep(1);

					Date start = new Date();
					f = InferenceAlgorithmFactory.getAlgorithm(selected)
							.execute(dynamicBayesNet, false,
									inferencePropertiesMap, warning.getTrackingPrintStream());
					Date end = new Date();					
					warning.getTrackingPrintStream().println("Took " + ((double)(end.getTime() - start.getTime()) / 1000) + " seconds...");

				} catch (final QueryVariableNotSetException ex) {

					warning.dispose();
					JOptionPane.showMessageDialog(getMainFrame(), ex
							.getMessage());

				} catch (final Exception ex) {
					logger.debug(ex,ex);
				}

				return f;
			}

			@Override
			protected void done() {
				warning.getTrackingPrintStream().println("Query Done!");
				warning.enableClosing();
				//warning.dispose();
				try {
					final Factor f = get();
					if (f != null) {
						final FactorDisplay factorDisplay = new FactorDisplay(
								getMainFrame(), f);
						factorDisplay
								.setModalityType(ModalityType.APPLICATION_MODAL);
						factorDisplay.setVisible(true);
					} else {
						warning.getTrackingPrintStream().println("There was a problem running " + selected + ".");
					}
				} catch (final Exception ex) {
					warning.getTrackingPrintStream().println("There was a problem running " + selected + ".");
					logger.debug("Problem with query. ", ex);
				}

			}

		};
		worker.execute();
		warning.setVisible(true);

	}

	@Action
	public void save() {
		try {
			if (filename == null) {
				final JFileChooser jFileChooser = new JFileChooser(System
						.getProperty("user.dir"));
				if (jFileChooser.showSaveDialog(this.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
					filename = jFileChooser.getSelectedFile().getAbsolutePath();
				} else {
					return;
				}
			}
			if (filename != null) {
				dynamicBayesNet.saveModel(filename);
			}
		} catch (final Exception ex) {
			logger.debug("Problem with save. ", ex);
		}
	}

	@Action
	public void saveAs() {
		try {
			JFileChooser jFileChooser;
			if (filename == null) {
				jFileChooser = new JFileChooser(System.getProperty("user.dir"));
			} else {
				jFileChooser = new JFileChooser(filename);
			}
			if (jFileChooser.showSaveDialog(this.getMainFrame()) == JFileChooser.APPROVE_OPTION) {

				filename = jFileChooser.getSelectedFile().getAbsolutePath();
				dynamicBayesNet.saveModel(filename);
			}
		} catch (final Exception ex) {

		}
	}

	private void selectVariable(final RandomVariable variable) {
		if (variable == null) {
			return;
		}
		selectedVariable = variable;
		BayesNetSlice slice = null;
		TransientVariable transientVariable = null;
		try {
			slice = dynamicBayesNet.getSlice((Integer) jSpinnerCurrentTimestep
					.getValue());
			transientVariable = slice.getVariable(variable.getId());
		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		// BuilderPanel
		jTextName.setText(variable.getName());
		jTextName.setEnabled(true);
		jTextName.setBackground(Color.white);
		jTextDescription.setText(variable.getDescription());
		jTextDescription.setEnabled(true);
		jTextDescription.setBackground(Color.white);
		final int value = (Integer) jSpinnerTimeslice.getValue();
		final int nCpds = selectedVariable.getNumberPotentialCpds();
		jSpinnerTimeslice.setModel(new SpinnerNumberModel(
				value <= nCpds ? value : 0, 0, nCpds, 1));
		jSpinnerTimeslice.setEnabled(true);

		try {
			String cpdstr = "Not yet set";
			final ConditionalDistribution cd = variable
					.getCpd((Integer) jSpinnerTimeslice.getValue());
			if (cd != null) {
				cpdstr = cd.getType();
			}
			jLblCPDDescription.setText(cpdstr);

		} catch (final Exception ex) {

			ex.printStackTrace();
		}
		jBtnEditCPD.setEnabled(true);

		final boolean stateVisible = variable instanceof DiscreteVariable;
		jPanelStates.setVisible(stateVisible);
		// End Builder Panel
		// Inference Panel

		jRBIsEvidence.setEnabled(true);
		jRBIsHidden.setEnabled(true);
		jRBIsQuery.setEnabled(true);
		jRBIsEvidence.removeItemListener(jRBVariableTypeListener);
		jRBIsHidden.removeItemListener(jRBVariableTypeListener);
		jRBIsQuery.removeItemListener(jRBVariableTypeListener);

		if (transientVariable != null) {
			if (transientVariable.isEvidence()) {
				jRBIsEvidence.setSelected(true);
			} else if (transientVariable.isHidden()) {
				jRBIsHidden.setSelected(true);
			} else if (transientVariable.isQuery()) {
				jRBIsQuery.setSelected(true);
			} else {
				logger.debug("Unknown transient variable state");
			}
		}

		jRBIsEvidence.addItemListener(jRBVariableTypeListener);
		jRBIsHidden.addItemListener(jRBVariableTypeListener);
		jRBIsQuery.addItemListener(jRBVariableTypeListener);

		// End Inference Panel
		jSpinnerVariableValue
				.removeChangeListener(jSpinnerVariableValueListener);

		if (stateVisible) {
			// Builder Panel
			jListStates.clearSelection();
			((StateTableModel) jListStates.getModel())
					.setDiscreteVariable((DiscreteVariable) variable);
			final DiscreteVariable dv = (DiscreteVariable) variable;

			// Inference Panel

			jSpinnerVariableValue
					.setModel(new SpinnerListModel(dv.getStates()));
			if (transientVariable != null && transientVariable.isEvidence()) {
				try {
					jSpinnerVariableValue.setValue(dv.getStates().get(
							transientVariable.getValue().intValue()));
					jSpinnerVariableValue.setEnabled(true);
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			}

		} else {
			// ContinuousVariable cv = (ContinuousVariable) variable;
			jSpinnerVariableValue.setModel(new SpinnerNumberModel(0.0,
					-99999999, 99999999, 1.0));
			if (transientVariable.isEvidence()) {
				try {
					jSpinnerVariableValue
							.setValue(transientVariable.getValue());
					jSpinnerVariableValue.setEnabled(true);
				} catch (final Exception ex) {
					if(logger.isDebugEnabled())
					{
						logger.debug("Problem setting evidence on jSpinnerVariableValues");
					}
				}
			} else {
				try {
					jSpinnerVariableValue
							.setValue(((UnconditionalDistribution) transientVariable
									.getDistribution()).getExpectedValue());
				} catch (final Exception ex) {
					if(logger.isDebugEnabled())
					{
						logger.debug("No expected value for distribution on  " + transientVariable.getReference().getName());
					}
				}
			}

		}

		jSpinnerVariableValue.addChangeListener(jSpinnerVariableValueListener);
		setupVariableChartData(variable);
	}

	private void setupVariableChartData(final RandomVariable variable) {
		// variableChartData.clear();
		double[][] probabilities;
		double[] times = new double[dynamicBayesNet.getMaxNumberSlices()];
		if (variable instanceof DiscreteVariable) {
			final DiscreteVariable dv = (DiscreteVariable) variable;
			final List<String> states = dv.getStates();
			final int numStates = states.size();
			probabilities = new double[numStates][];
			for (int i = 0; i < numStates; i++) {
				probabilities[i] = new double[dynamicBayesNet
						.getMaxNumberSlices()];
			}
			for (int i = 0; i < dynamicBayesNet.getMaxNumberSlices(); i++) {
				times[i] = i + 1;
				try {
					final BayesNetSlice bns = dynamicBayesNet.getSlice(i);
					final TransientVariable tv = bns.getVariable(dv.getId());
					int setState = -1;
					if (tv.isEvidence()) {
						setState = tv.getEvidence().intValue();

						for (int j = 0; j < numStates; j++) {
							probabilities[j][i] = (j == setState ? 1.0 : 0.0);
							/*
							 * variableChartData.addValue((Number) (j ==
							 * setState ? 1.0 : 0.0), states.get(j), i);
							 */
						}
					} else {
						final DoubleMatrix1D dmd = DoubleFactory1D.dense.make(
								1, 0);
						final UnconditionalDistribution ud = tv.getMarginal();

						if (ud != null) {
							for (int j = 0; j < numStates; j++) {
								dmd.set(0, j);
								probabilities[j][i] = ud.getProbability(dmd);
								/*
								 * variableChartData.addValue((Number)
								 * ud.getProbability(dmd), states.get(j), i);
								 */
							}
						}
					}
				} catch (final Exception ex) {

					final DoubleMatrix1D dmd = DoubleFactory1D.dense.make(1, 0);
					try {
						final ConditionalDistribution cd = dv.getCpd(i);
						if (cd instanceof UnconditionalDistribution) {
							final UnconditionalDistribution ucd = (UnconditionalDistribution) cd;
							for (int j = 0; j < numStates; j++) {
								dmd.set(0, j);
								probabilities[j][i] = ucd.getProbability(dmd);
								/*
								 * variableChartData.addValue((Number)
								 * ucd.getProbability(dmd), states.get(j), i);
								 */

							}
						}
					} catch (final Exception ex2) {
						ex2.printStackTrace();
					}
				}
			}

			while (variableChartData.getSeriesCount() > 0) {
				variableChartData.removeSeries(variableChartData
						.getSeriesKey(0));
			}
			for (int i = 0; i < numStates; i++) {
				final double[][] series = new double[2][];
				series[0] = times;
				series[1] = probabilities[i];

				variableChartData.addSeries(states.get(i), series);
			}
		} else if (variable instanceof ContinuousVariable) {

			ContinuousVariable cv = (ContinuousVariable) variable;
			String[] labels = { "Mean", "1 Std. Higher", "1 Std. Lower" };
			double[][] values = new double[3][];
			for (int i = 0; i < 3; i++) {
				values[i] = new double[times.length];
			}

			for (int i = 0; i < times.length; i++) {
				times[i] = i + 1;

				try {
					final BayesNetSlice bns = dynamicBayesNet.getSlice(i);
					final TransientVariable tv = bns.getVariable(cv.getId());
					if (tv.isEvidence()) {
						values[0][i] = values[1][i] = values[2][i] = tv
								.getEvidence();

					} else {
						final DoubleMatrix1D dmd = DoubleFactory1D.dense.make(
								1, 0);
						final UnconditionalDistribution ud = tv.getMarginal();

						if (ud != null) {
							values[0][i] = ud.getExpectedValue().getQuick(0);
							values[1][i] = values[0][i]
									+ ud.getCovariance().getQuick(0, 0);
							values[2][i] = values[0][i]
									- ud.getCovariance().getQuick(0, 0);
						}
					}
				} catch (final Exception ex) {

					final DoubleMatrix1D dmd = DoubleFactory1D.dense.make(1, 0);
					try {
						final ConditionalDistribution cd = cv.getCpd(i);
						if (cd instanceof UnconditionalDistribution) {
							final UnconditionalDistribution ucd = (UnconditionalDistribution) cd;

							values[0][i] = ucd.getExpectedValue().getQuick(0);
							values[1][i] = values[0][i]
									+ ucd.getCovariance().getQuick(0, 0);
							values[2][i] = values[0][i]
									- ucd.getCovariance().getQuick(0, 0);
						}
					} catch (final Exception ex2) {
						ex2.printStackTrace();
					}
				}
			}

			while (variableChartData.getSeriesCount() > 0) {
				variableChartData.removeSeries(variableChartData
						.getSeriesKey(0));
			}
			for (int i = 0; i < 3; i++) {
				final double[][] series = new double[2][];
				series[0] = times;
				series[1] = values[i];

				variableChartData.addSeries(labels[i], series);
			}
		} else {
			// huh?
			return;
		}

		jPanelVariableChart.restoreAutoBounds();

		jPanelVariableChart.setVisible(true);
		// int tmpWidth = Math.max(80, dynamicBayesNet.getMaxNumberSlices() *
		// 30);
		// jPanelVariableChart.setPreferredSize(new Dimension(tmpWidth, 200));
		// jPanelVariableChart.setMaximumDrawWidth(tmpWidth)

	}

	@Override
	protected void startup() {
		{
			getMainFrame().setSize(680, 600);
		}
		{
			topPanel = new JPanel();
			getMainFrame().getContentPane().add(topPanel, BorderLayout.CENTER);
			final BorderLayout panelLayout = new BorderLayout();
			topPanel.setLayout(panelLayout);
			topPanel.setPreferredSize(new java.awt.Dimension(600, 300));
			{
				jSplitPane1 = new JSplitPane();
				topPanel.add(jSplitPane1, BorderLayout.CENTER);
				jSplitPane1.setDividerSize(4);
				jSplitPane1.setResizeWeight(1);
				jSplitPane1.setPreferredSize(new java.awt.Dimension(700, 265));
				{
					final JScrollPane jScrollInterGraphPane = new JScrollPane(
							graphDisplay);
					jSplitPane1.add(jScrollInterGraphPane, JSplitPane.LEFT);
					jScrollInterGraphPane
							.setPreferredSize(new java.awt.Dimension(350, 438));

				}
				{
					jTabbedPane1 = new JTabbedPane();
					jSplitPane1.add(jTabbedPane1, JSplitPane.RIGHT);
					jTabbedPane1.setPreferredSize(new java.awt.Dimension(300,
							438));
					{
						jTabbedPane1.addTab("Build", null, new JScrollPane(
								getJBuild()), null);
					}
					{
						jTabbedPane1.addTab("Learn", null, new JScrollPane(
								getJLearn()), null);
					}
					{
						jTabbedPane1.addTab("Infer", null, new JScrollPane(
								getJInfer()), null);
					}
					jTabbedPane1.addChangeListener(new ChangeListener() {

						public void stateChanged(ChangeEvent e) {
							switch (jTabbedPane1.getSelectedIndex()) {
							case 1:
								jPanelLearnNetworkEvidencePlaceholder
										.add(getJPanelEvidenceLoader());
								break;
							case 2:
								jPanelInferNetworkEvidencePlaceholder
										.add(getJPanelEvidenceLoader());
								break;
							}
						}

					});
				}
			}
		}

		generateMenu();

		graphDisplay.addGraphSelectionListener(new GraphSelectionListener() {

			public void valueChanged(final GraphSelectionEvent e) {
				deactivatePanel();
				RandomVariable rv = null;
				for (final Object c : e.getCells()) {
					if (e.isAddedCell(c) && c instanceof DefaultGraphCell) {
						final Object obj = ((DefaultGraphCell) c)
								.getUserObject();
						if (rv != null) {
							rv = null;
							break;
						}
						if (obj instanceof RandomVariable) {
							rv = (RandomVariable) obj;
						}
					}
				}
				if (rv != null) {
					selectVariable(rv);
				}
			}

		});
		deactivatePanel();
		show(topPanel);
	}

	@Action
	public void timesliceSelect() {
		if (jRadioBtnShowAllConnections.isSelected()) {
			jSpinner1.setEnabled(false);
			dynamicBayesNet.setTemplateSlice(0);
			graphDisplay.setTimeSliceVisible(-1);
		} else {
			jSpinner1.setEnabled(true);
			dynamicBayesNet.setTemplateSlice((Integer) jSpinner1.getValue());
			graphDisplay.setTimeSliceVisible((Integer) jSpinner1.getValue());
		}
	}

}
