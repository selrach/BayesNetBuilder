package selrach.bnetbuilder.gui.evidence;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ResourceMap;

import selrach.bnetbuilder.data.ElementMetadata;
import selrach.bnetbuilder.data.FileAccessor;
import selrach.bnetbuilder.data.interfaces.DataAccessor;
import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * This is the evidence loader, it basically handles how we want to import data
 * files into our system by specifying the mapping between the variables and the
 * data files, only handles your basic text files at the moment
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class EvidenceLoader extends javax.swing.JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9221989142344510361L;

	private static final Logger logger = Logger.getLogger(EvidenceLoader.class);

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			}
		});
	}

	private DynamicBayesNetModel model = null;
	private JSpinner jSpinnerRowsPerTrial;
	private JCheckBox jCheckBoxHeader;
	private JRadioButton jRadioButtonSpace;
	private JTextField jTextCustomDeliminator;
	private JList fileList;
	private JRadioButton jRadioButtonCustom;
	private JRadioButton jRadioButtonTab;
	private JRadioButton jRadioButtonComma;
	private JTable evidenceTable;
	private JButton jButtonStateMappings;

	private String deliminator = ",";
	private String filename = null;

	private final Color inactiveColor = new Color(239, 239, 239);

	private boolean loadingFileDefaults = false;

	final private ResourceMap resource;

	public EvidenceLoader(JFrame frame, DynamicBayesNetModel model) {
		super(frame);
		resource = Application.getInstance().getContext().getResourceMap(
				getClass());
		this.model = model;
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		initGUI();

		resource.injectComponents(getContentPane());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Dialog#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean arg0) {
		super.setVisible(arg0);
		reset();
	}

	private void reset() {

	}

	private void initGUI() {
		try {
			BoxLayout thisLayout = new BoxLayout(getContentPane(),
					javax.swing.BoxLayout.Y_AXIS);
			getContentPane().setLayout(thisLayout);
			getContentPane().add(getTopPanel());
			getContentPane().add(getBottomPanel());
			

			fileList.clearSelection();
			fileList.setSelectedIndex(0);
			
			this.setSize(721, 526);
			this.setLocationRelativeTo(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JPanel getTopPanel() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS);
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 192));
		panel.add(getFileListingPanel());
		panel.add(getParsingOptionsPanel());
		panel.add(getTemporalOptionsPanel());
		panel.add(getEvidenceOptionsPanel());
		return panel;
	}

	private JPanel getBottomPanel() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS);
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 451));
		panel.add(new JScrollPane(getEvidenceTable()));
		panel.add(getLoadButtonPanel());
		return panel;
	}

	private JPanel getLoadButtonPanel() {
		JPanel panel = new JPanel();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.RIGHT);
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 23));
		panel.add(makeButton("loadEvidence"));
		return panel;
	}

	private JTable getEvidenceTable() {
		if (evidenceTable == null) {
			int ind = 0;
			if (fileList != null) {
				ind = fileList.getSelectedIndex();
			}
			final EvidenceTableModel evidenceTableModel = new EvidenceTableModel(
					model, ind);
			evidenceTable = new JTable();
			evidenceTable.setModel(evidenceTableModel);
			evidenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			evidenceTable.setCellSelectionEnabled(true);
			ListSelectionListener listener = new ListSelectionListener() {

				public void valueChanged(ListSelectionEvent e) {
					int row = evidenceTable.getSelectedRow();
					if(row>=0)
					{
						FileAccessor fa = ((EvidenceTableModel) evidenceTable
								.getModel()).getFileAccessor();
						String id = fa.getVariablesInList().get(row);
						if (id != null) {
							RandomVariable rv = model.getVariableMap().get(id);
							if (rv instanceof DiscreteVariable) {
								EvidenceLoader.this.jButtonStateMappings
										.setEnabled(true);
								return;
							}
						}
						EvidenceLoader.this.jButtonStateMappings.setEnabled(false);
					}
				}

			};
			evidenceTable.getSelectionModel()
					.addListSelectionListener(listener);

			evidenceTable.setTransferHandler(new TransferHandler() {

				/**
				 * 
				 */
				private static final long serialVersionUID = -2034603149432218606L;

				DataFlavor variableFlavor = new DataFlavor(
						RandomVariable.class,
						DataFlavor.javaJVMLocalObjectMimeType + "; class="
								+ RandomVariable.class.getName());
				DataFlavor dataEntryFlavor = new DataFlavor(
						ElementMetadata.class,
						DataFlavor.javaJVMLocalObjectMimeType + "; class="
								+ ElementMetadata.class.getName());

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.TransferHandler#canImport(javax.swing.TransferHandler
				 * .TransferSupport)
				 */
				@Override
				public boolean canImport(TransferSupport support) {
					if (!(support.isDataFlavorSupported(variableFlavor) || support
							.isDataFlavorSupported(dataEntryFlavor))) {
						return false;
					}
					return shouldAcceptDropLocation(support.getDropLocation());
				}

				private boolean shouldAcceptDropLocation(DropLocation at) {
					Point loc = at.getDropPoint();
					int row = evidenceTable.rowAtPoint(loc);
					int col = evidenceTable.columnAtPoint(loc);
					return row != -1 && col != -1;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.TransferHandler#createTransferable(javax.swing
				 * .JComponent)
				 */
				@Override
				protected Transferable createTransferable(JComponent c) {
					if (c instanceof JTable) {
						JTable tbl = (JTable) c;
						int col = tbl.getSelectedColumn();
						int row = tbl.getSelectedRow();
						if (col == -1 || row == -1) {
							return null;
						}

						EvidenceTableModel etm = (EvidenceTableModel) tbl
								.getModel();
						Object val = etm.getObjectAt(row, col);
						if (val == null) {
							return null;
						}
						Transferable ret = null;
						if (val instanceof ElementMetadata) {
							ret = new ElementMetadataTransferable(
									(ElementMetadata) val);
						} else if (val instanceof String) {
							ret = new RandomVariableTransferable((String) val);
						} else if (val instanceof RandomVariable) {
							ret = new RandomVariableTransferable(
									(RandomVariable) val);
						}
						return ret;
					}
					return null;
				}

				class RandomVariableTransferable implements Transferable {

					private RandomVariable data;
					private String id;

					RandomVariableTransferable(RandomVariable variable) {
						data = variable;
					}

					RandomVariableTransferable(String id) {
						this.id = id;
					}

					public Object getTransferData(DataFlavor flavor)
							throws UnsupportedFlavorException, IOException {
						if (!isDataFlavorSupported(flavor)) {
							throw new UnsupportedFlavorException(flavor);
						}
						if (data == null) {
							return id;
						}
						return data;
					}

					public DataFlavor[] getTransferDataFlavors() {
						return new DataFlavor[] { variableFlavor };
					}

					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return variableFlavor.equals(flavor);
					}

				}

				class ElementMetadataTransferable implements Transferable {
					ElementMetadata data;

					ElementMetadataTransferable(ElementMetadata data) {
						this.data = data;
					}

					public Object getTransferData(DataFlavor flavor)
							throws UnsupportedFlavorException, IOException {
						if (!isDataFlavorSupported(flavor)) {
							throw new UnsupportedFlavorException(flavor);
						}
						return data;
					}

					public DataFlavor[] getTransferDataFlavors() {
						return new DataFlavor[] { dataEntryFlavor };
					}

					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return dataEntryFlavor.equals(flavor);
					}

				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.TransferHandler#exportDone(javax.swing.JComponent
				 * , java.awt.datatransfer.Transferable, int)
				 */
				@Override
				protected void exportDone(JComponent source, Transferable data,
						int action) {
					// TODO Auto-generated method stub
					// super.exportDone(source, data, action);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javaxevidence1.txt.swing.TransferHandler#getSourceActions
				 * (javax.swing.JComponent)
				 */
				@Override
				public int getSourceActions(JComponent c) {
					return TransferHandler.COPY_OR_MOVE;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @seejavax.swing.TransferHandler#importData(javax.swing.
				 * TransferHandler.TransferSupport)
				 */
				@Override
				public boolean importData(TransferSupport support) {
					if (!canImport(support)) {
						return false;
					}

					JTable tbl = (JTable) support.getComponent();
					Point loc = support.getDropLocation().getDropPoint();
					int row = evidenceTable.rowAtPoint(loc);
					int col = evidenceTable.columnAtPoint(loc);

					// int col = tbl.getSelectedColumn();
					// int row = tbl.getSelectedRow();
					if (col == -1 || row == -1) {
						return false;
					}

					EvidenceTableModel etm = (EvidenceTableModel) tbl
							.getModel();

					Transferable t = support.getTransferable();

					try {
						if (t.isDataFlavorSupported(variableFlavor)) {
							Object val = t.getTransferData(variableFlavor);
							boolean ret = etm.putObjectAt(row, col, val);
							tbl.updateUI();
							return ret;
						} else if (t.isDataFlavorSupported(dataEntryFlavor)) {
							ElementMetadata de = (ElementMetadata) t
									.getTransferData(dataEntryFlavor);
							boolean ret = etm.putObjectAt(row, col, de);
							tbl.updateUI();
							return ret;
						} else {
							return false;
						}

					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						ex.printStackTrace(System.err);
						return false;
					}
				}

			});

			evidenceTable.setDragEnabled(true);
		}

		return evidenceTable;
	}

	private JPanel getFileListingPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		layout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
		layout.rowHeights = new int[] { 7, 7, 7, 7 };
		layout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.1 };
		layout.columnWidths = new int[] { 85, 406, 82, 7, 20 };
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 59));
		panel.add(makeLabel("loadFilesHeader", layout, new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(2, 0, 0, 5), 0, 0)));
		panel.add(getFileList(), new GridBagConstraints(1, 0, 1, 4, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
						5, 5, 5, 5), 0, 0));
		panel.add(makeButton("openEvidenceFile", layout,
				new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.NONE,
						new Insets(5, 10, 0, 0), 0, 0)));
		panel.add(makeButton("removeFile", layout, new GridBagConstraints(2, 1,
				1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0)));
		return panel;
	}

	private JPanel getParsingOptionsPanel() {
		JPanel panel = new JPanel();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.LEFT);
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 32));
		panel.add(makeLabel("deliminatorHeader"));
		panel.add(getJRadioButtonComma());
		panel.add(getJRadioButtonTab());
		panel.add(getJRadioButtonSpace());
		panel.add(getJRadioButtonCustom());
		panel.add(getJTextCustomDeliminator());
		ButtonGroup bg = new ButtonGroup();
		bg.add(jRadioButtonComma);
		bg.add(jRadioButtonTab);
		bg.add(jRadioButtonSpace);
		bg.add(jRadioButtonCustom);
		return panel;
	}

	private JRadioButton getJRadioButtonComma() {
		if (jRadioButtonComma == null) {
			jRadioButtonComma = new JRadioButton();
			jRadioButtonComma.setName("jRadioButtonComma");
			jRadioButtonComma.setSelected(true);
			jRadioButtonComma.setAction(getAppActionMap().get(
					"changeDeliminator"));
		}
		return jRadioButtonComma;
	}

	private JRadioButton getJRadioButtonTab() {
		if (jRadioButtonTab == null) {
			jRadioButtonTab = new JRadioButton();
			jRadioButtonTab.setName("jRadioButtonTab");
			jRadioButtonTab.setAction(getAppActionMap()
					.get("changeDeliminator"));
		}
		return jRadioButtonTab;
	}

	private JRadioButton getJRadioButtonCustom() {
		if (jRadioButtonCustom == null) {
			jRadioButtonCustom = new JRadioButton();
			jRadioButtonCustom.setName("jRadioButtonCustom");
			jRadioButtonCustom.setAction(getAppActionMap().get(
					"changeDeliminator"));
		}
		return jRadioButtonCustom;
	}

	private JTextField getJTextCustomDeliminator() {
		if (jTextCustomDeliminator == null) {
			jTextCustomDeliminator = new JTextField();
			jTextCustomDeliminator.setPreferredSize(new java.awt.Dimension(108,
					22));
			jTextCustomDeliminator.setEnabled(false);
			jTextCustomDeliminator.setBackground(inactiveColor);
		}
		return jTextCustomDeliminator;
	}

	private JRadioButton getJRadioButtonSpace() {
		if (jRadioButtonSpace == null) {
			jRadioButtonSpace = new JRadioButton();
			jRadioButtonSpace.setName("jRadioButtonSpace");
			jRadioButtonSpace.setAction(getAppActionMap().get(
					"changeDeliminator"));
		}
		return jRadioButtonSpace;
	}

	private JPanel getTemporalOptionsPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		layout.rowWeights = new double[] { 0.1 };
		layout.rowHeights = new int[] { 7 };
		layout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		layout.columnWidths = new int[] { 30, 40, 20, 40, 20, 100 };
		panel.setLayout(layout);
		panel.setPreferredSize(new java.awt.Dimension(711, 27));
		panel.add(jCheckBoxHeader = makeCheckbox("swapHeaderFlag", layout,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 5, 0, 0), 0, 0)));
		panel.add(makeLabel("numTimeStepsHeader", layout,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						new Insets(0, 15, 0, 0), 0, 0)));
		panel.add(getJSpinnerRowsPerTrial(), new GridBagConstraints(2, 0, 1, 1,
				0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
		return panel;
	}

	private JSpinner getJSpinnerRowsPerTrial() {
		if (jSpinnerRowsPerTrial == null) {
			SpinnerNumberModel jSpinnerRowsPerTrialModel = new SpinnerNumberModel(
					1, 1, 99999, 1);
			jSpinnerRowsPerTrial = new JSpinner();
			jSpinnerRowsPerTrial.setModel(jSpinnerRowsPerTrialModel);
			jSpinnerRowsPerTrial.getEditor().setEnabled(false);
			jSpinnerRowsPerTrial.setBackground(inactiveColor);
						
			jSpinnerRowsPerTrial.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					FileAccessor fa = ((EvidenceTableModel) evidenceTable
							.getModel()).getFileAccessor();
					if (fa.getNumberDataLines() < (Integer) jSpinnerRowsPerTrial
							.getValue()) {
						jSpinnerRowsPerTrial.setValue(fa.getNumberDataLines());
					}
					fa.setNumberTimesteps((Integer) jSpinnerRowsPerTrial
							.getValue());
				}
			});
		}
		return jSpinnerRowsPerTrial;
	}

	private JPanel getEvidenceOptionsPanel() {
		JPanel panel = new JPanel();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.LEFT);
		panel.setLayout(layout);
		panel.add(makeButton("addBlankRow"));
		panel.add(jButtonStateMappings = makeButton("setupStateMappings"));
		jButtonStateMappings.setEnabled(false);
		return panel;
	}

	@Action
	public void openEvidenceFile() {
		try {
			JFileChooser jFileChooser;
			if (filename == null) {
				jFileChooser = new JFileChooser(System.getProperty("user.dir"));
			} else {
				jFileChooser = new JFileChooser(filename);
			}
			jFileChooser.setMultiSelectionEnabled(true);
			if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				for (File file : jFileChooser.getSelectedFiles()) {
					filename = file.getAbsolutePath();
					FileAccessor fa = new FileAccessor(model, filename,
							deliminator, jCheckBoxHeader.isSelected());
					model.getTrialDao().addFile(fa);
				}
				fileList.updateUI();
				evidenceTable.updateUI();
				fileList.clearSelection();
				fileList.setSelectedIndex(0);
			}
		} catch (Exception ex) {
			System.out.print(ex.getMessage());
			ex.printStackTrace(System.err);
		}

	}

	@Action
	public void changeDeliminator() {
		if (loadingFileDefaults) {
			return;
		}
		if (jRadioButtonCustom.isSelected()) {
			jTextCustomDeliminator.setEnabled(true);
			jTextCustomDeliminator.setBackground(Color.white);
			deliminator = jTextCustomDeliminator.getText();
			if (deliminator.length() == 0) {
				deliminator = " ";
			}
		} else {
			jTextCustomDeliminator.setEnabled(false);
			jTextCustomDeliminator.setBackground(inactiveColor);
			if (jRadioButtonComma.isSelected()) {
				deliminator = ",";
			} else if (jRadioButtonTab.isSelected()) {
				deliminator = "\t";
			} else if (jRadioButtonSpace.isSelected()) {
				deliminator = " ";
			}
		}

		EvidenceTableModel etm = (EvidenceTableModel) evidenceTable.getModel();
		etm.getFileAccessor().setVariableDeliminator(deliminator);
		jSpinnerRowsPerTrial.setValue(etm.getFileAccessor()
				.getNumberTimestepsPerTrial());
		evidenceTable.updateUI();

	}

	@Action
	public void swapHeaderFlag() {
		// tryLoadFileTableModel();
		if (loadingFileDefaults) {
			return;
		}

		EvidenceTableModel etm = (EvidenceTableModel) evidenceTable.getModel();
		etm.getFileAccessor().setHasHeader(jCheckBoxHeader.isSelected());
		jSpinnerRowsPerTrial.setValue(etm.getFileAccessor()
				.getNumberTimestepsPerTrial());
		evidenceTable.updateUI();
	}

	@Action
	public void addBlankRow() {
		((EvidenceTableModel) evidenceTable.getModel()).addRow();
		evidenceTable.updateUI();
	}

	@Action
	public void setupStateMappings(ActionEvent event) {
		int row = evidenceTable.getSelectedRow();
		FileAccessor fa = ((EvidenceTableModel) evidenceTable.getModel())
				.getFileAccessor();
		String id = fa.getVariablesInList().get(row);
		if (id != null) {
			RandomVariable rv = model.getVariableMap().get(id);
			if (rv instanceof DiscreteVariable) {
				ElementMetadata metadata = fa.getVariableToElementMetadataMap()
						.get(id);
				if (metadata.isCannotBeContinuous()
						&& metadata.isCannotBeDiscrete()) {
					JOptionPane.showMessageDialog(this, resource
							.getString("mixedDataError"), resource
							.getString("mixedDataErrorHeader"),
							JOptionPane.ERROR_MESSAGE);
					if (logger.isInfoEnabled()) {

					}
				} else {
					StateMapper sm = new StateMapper(this,
							(DiscreteVariable) rv, metadata);
					sm.setModalityType(ModalityType.APPLICATION_MODAL);
					sm.setVisible(true);
				}
			}
		}

	}

	@Action
	public void loadEvidence() {

		for (DataAccessor fa : model.getTrialDao().getDataSources()) {
			List<String> vars = fa.getVariablesInList();
			Map<String, ElementMetadata> map = fa
					.getVariableToElementMetadataMap();
			for (String key : vars) {
				if (key != null) {
					RandomVariable rv = model.getVariableMap().get(key);
					if (rv instanceof DiscreteVariable) {
						ElementMetadata meta = map.get(key);
						if (meta.isCannotBeContinuous()
								&& meta.isCannotBeDiscrete()) {
							JOptionPane.showMessageDialog(this, resource
									.getString("mixedDataError"), resource
									.getString("mixedDataErrorHeader"),
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						if (!meta.hasBeenSetup()) {
							meta.setupDefaultRanges(((DiscreteVariable) rv)
									.getStates());
							if (!meta.hasBeenSetup()) {
								JOptionPane.showMessageDialog(this, resource
										.getString("unmappedDataError")
										+ fa.getDescription(),
								resource.getString("unmappedDataErrorHeader"),
										JOptionPane.WARNING_MESSAGE);
								return;
							}
						}
					}
				}
			}
		}
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setVisible(false);
	}

	@Action
	public void removeFile() {
		if (fileList.getModel().getSize() > 0) {
			int[] ind = fileList.getSelectedIndices();
			List<DataAccessor> ds = model.getTrialDao().getDataSources();
			for (int i = ind.length - 1; i >= 0; i--) {
				FileAccessor fa = (FileAccessor) ds.get(ind[i]);
				model.getTrialDao().removeFile(fa);
			}
			fileList.setSelectedIndex(-1);
			fileList.updateUI();
			if (fileList.getModel().getSize() > 0) {
				((EvidenceTableModel) evidenceTable.getModel())
						.changeFileAccessor(fileList.getSelectedIndex());
			} else {

				((EvidenceTableModel) evidenceTable.getModel())
						.changeFileAccessor(-1);
			}
			evidenceTable.updateUI();
		}
	}

	/**
	 * Returns the action map used by this application. Actions defined using
	 * the Action annotation are returned by this method
	 */
	private ApplicationActionMap getAppActionMap() {
		return Application.getInstance().getContext().getActionMap(this);
	}

	private JList getFileList() {
		if (fileList == null) {
			ListModel fileListModel = new ListModel() {
				private static final long serialVersionUID = 1L;

				List<ListDataListener> ldls = new ArrayList<ListDataListener>();

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.ListModel#addListDataListener(javax.swing.event
				 * .ListDataListener)
				 */
				public void addListDataListener(ListDataListener arg0) {
					if (!ldls.contains(arg0)) {
						ldls.add(arg0);
					}
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.ListModel#getElementAt(int)
				 */
				public Object getElementAt(int arg0) {
					return model.getTrialDao().getDataSources().get(arg0)
							.getDescription();
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see javax.swing.ListModel#getSize()
				 */
				public int getSize() {
					return model.getTrialDao().getDataSources().size();
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * javax.swing.ListModel#removeListDataListener(javax.swing.
				 * event.ListDataListener)
				 */
				public void removeListDataListener(ListDataListener arg0) {
					if (ldls.contains(arg0)) {
						ldls.remove(arg0);
					}

				}

			};
			fileList = new JList();
			fileList.setModel(fileListModel);
			fileList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent evt) {
					// System.out.println("fileList.valueChanged, event=" +
					// evt);
					if (evidenceTable != null) {
						if (fileList.getModel().getSize() > 0) {
							((EvidenceTableModel) evidenceTable.getModel())
									.changeFileAccessor(fileList
											.getSelectedIndex());
						} else {

							((EvidenceTableModel) evidenceTable.getModel())
									.changeFileAccessor(-1);
						}
						loadFileAccessor(((EvidenceTableModel) evidenceTable
								.getModel()).getFileAccessor());
						evidenceTable.updateUI();
					}
				}

				private void loadFileAccessor(FileAccessor fileAccessor) {
					if(fileAccessor!=null)
					{
						jCheckBoxHeader.setEnabled(true);
						jCheckBoxHeader.setSelected(fileAccessor.isHasHeader());
						deliminator = fileAccessor.getVariableDeliminator();
						loadingFileDefaults = true;
						jTextCustomDeliminator.setEnabled(false);
						jTextCustomDeliminator.setBackground(inactiveColor);
						jRadioButtonSpace.setEnabled(true);
						jRadioButtonTab.setEnabled(true);
						jRadioButtonComma.setEnabled(true);
						jRadioButtonCustom.setEnabled(true);
						if (deliminator == ",") {
							jRadioButtonComma.setSelected(true);
						} else if (deliminator == "\t") {
							jRadioButtonTab.setSelected(true);
						} else if (deliminator == " ") {
							jRadioButtonSpace.setSelected(true);
						} else {
							jTextCustomDeliminator.setEnabled(true);
							jTextCustomDeliminator.setBackground(Color.white);
							jRadioButtonCustom.setSelected(true);
							jTextCustomDeliminator.setText(deliminator);
						}
						jCheckBoxHeader.setSelected(fileAccessor.isHasHeader());
						jSpinnerRowsPerTrial.setEnabled(true);
						jSpinnerRowsPerTrial.setValue(fileAccessor
								.getNumberTimestepsPerTrial());
	
						loadingFileDefaults = false;
					}
					else
					{
						jSpinnerRowsPerTrial.setEnabled(false);
						jTextCustomDeliminator.setEnabled(false);
						jCheckBoxHeader.setEnabled(false);
						jRadioButtonCustom.setEnabled(false);
						jRadioButtonSpace.setEnabled(false);
						jRadioButtonTab.setEnabled(false);
						jRadioButtonComma.setEnabled(false);
						
					}
				}
			});
			if (fileList.getModel().getSize() > 0) {
				fileList.setSelectedIndex(0);
			}
		}
		return fileList;
	}

	private class EvidenceTableModel extends DefaultTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7522280957901332513L;

		DynamicBayesNetModel model;
		FileAccessor fa = null;

		// List<RandomVariable> variables;
		// List<DataEntry> data;

		public EvidenceTableModel(DynamicBayesNetModel model, int which) { // List
			// <
			// DataEntry
			// >
			// data)
			// {
			this.model = model;
			changeFileAccessor(which);
			// this.variables = new
			// ArrayList<RandomVariable>(model.getVariables());
			// this.data = data;
			// while (this.variables.size() < this.data.size()) {
			// this.variables.add(null);
			// }
			// while (this.data.size() < this.variables.size()) {
			// this.data.add(new DataEntry("", "", -1));
			// }

		}

		public void changeFileAccessor(int which) {
			this.fa = null;
			try {
				if (which < 0) {
					fa = null;
				} else {
					fa = (FileAccessor) model.getTrialDao().getDataSources()
							.get(which);
				}
			} catch (Exception ex) {
			}
		}

		public FileAccessor getFileAccessor() {
			return fa;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.DefaultTableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 3;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.DefaultTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int arg0) {
			String name = null;
			switch (arg0) {
			case 0:
				name = "Model Variable";
				break;
			case 1:
				name = "Data Column Name";
				break;
			case 2:
				name = "Data Example";
				break;
			default:
				name = "Should not exist";
			}
			return name;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.DefaultTableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			if (fa == null || fa.getVariablesInList() == null) {
				return 0;
			}
			return fa.getVariablesInList().size();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.DefaultTableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				if (fa.getVariablesInList().get(row) == null) {
					return "";
				}
				return model.getVariableMap().get(
						fa.getVariablesInList().get(row)).getName();
			case 1:
				return fa.getFileElementsInList().get(row).getHeader();
			case 2:
				return fa.getFileElementsInList().get(row).getExample();
			}
			return "Should not exist";
		}

		public Object getObjectAt(int row, int col) {
			switch (col) {
			case 0:
				if (fa.getVariablesInList().get(row) == null) {
					return String.valueOf(row);
				}
				return fa.getVariablesInList().get(row);
			case 1:
			case 2:
				return fa.getFileElementsInList().get(row);
			}
			return null;
		}

		public boolean putObjectAt(int row, int col, Object obj) {
			int oldRow = -1;
			if (obj instanceof ElementMetadata) {
				fa.moveFileElementInList((ElementMetadata) obj, row);
			} else {
				if (obj instanceof String) {
					if (!model.getVariableMap().containsKey(obj)) {
						oldRow = Integer.parseInt((String) obj);
						fa.moveVariableInList(oldRow, row);
					} else {
						fa.moveVariableInList((String) obj, row);
					}
				} else {
					return false;
				}
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
		 */
		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}

		public void addRow() {
			fa.addNullFileElementInList();
		}

	}

	private JLabel makeLabel(String name, GridBagLayout gridbag,
			GridBagConstraints c) {
		final JLabel label = new JLabel();
		label.setName(name);
		gridbag.setConstraints(label, c);
		return label;
	}

	private JLabel makeLabel(String name) {
		final JLabel label = new JLabel();
		label.setName(name);
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

	private JButton makeButton(String name) {
		final JButton button = new JButton();
		button.setAction(getAppActionMap().get(name));
		button.setBorder(BorderFactory.createEtchedBorder(BevelBorder.RAISED));
		return button;
	}

	private JCheckBox makeCheckbox(String name, GridBagLayout gridbag,
			GridBagConstraints c) {
		final JCheckBox checkbox = new JCheckBox();
		checkbox.setAction(getAppActionMap().get(name));
		gridbag.setConstraints(checkbox, c);
		return checkbox;
	}

}

/**
 * Class to enable drag drop interface to move around what entry is mapped to
 * what variable
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
class DataEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4829124639468633388L;
	private final String value;
	private final String example;
	private final Integer position;

	public DataEntry(String value, String example, int position) {
		this.value = value;
		this.example = example;
		this.position = position;
	}

	public String getValue() {
		return value;
	}

	public String getExample() {
		return example;
	}

	public int getPosition() {
		return position;
	}

}
