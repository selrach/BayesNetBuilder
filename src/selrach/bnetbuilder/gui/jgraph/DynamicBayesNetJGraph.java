package selrach.bnetbuilder.gui.jgraph;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortView;
import org.jgraph.util.ParallelEdgeRouter;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.listener.interfaces.EdgeUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.ModelUpdatedListener;
import selrach.bnetbuilder.model.listener.interfaces.VariableUpdatedListener;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * Handles the network front end rendering
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DynamicBayesNetJGraph extends JGraph implements
		EdgeUpdatedListener, VariableUpdatedListener, ModelUpdatedListener {

	private static final long serialVersionUID = -3610743889691735375L;

	private Map<Object, Object> setCellPosition(JGraph graph, Point2D location,
			Color gradient) {
		Map<Object, Object> map = new Hashtable<Object, Object>();
		if (graph != null) {
			location = graph.snap((Point2D) location.clone());
		} else {
			location = (Point2D) location.clone();
		}

		GraphConstants.setBounds(map, new Rectangle2D.Double(location.getX(),
				location.getY(), 0, 0));
		// Add a nice looking gradient background
		GraphConstants.setGradientColor(map, gradient);
		// Add a Border Color Attribute to the Map
		GraphConstants.setBorderColor(map, Color.black);
		// Add a White Background
		GraphConstants.setBackground(map, Color.white);
		// Make Vertex Opaque
		GraphConstants.setOpaque(map, true);
		// Make sure the cell is resized on insert
		GraphConstants.setResize(map, true);
		// GraphConstants.setAutoSize(map, true);

		return map;
	}

	private DynamicBayesNetModel dbnModel = null;
	private final Map<RandomVariable, Port> variableToPort = new Hashtable<RandomVariable, Port>();

	public DynamicBayesNetJGraph(DynamicBayesNetModel dbnModel, GraphModel model) {
		this(dbnModel, model, null);
	}

	/**
	 * This should set up the defaults for our graph
	 * 
	 * @param model
	 * @param cache
	 */
	public DynamicBayesNetJGraph(DynamicBayesNetModel dbnModel,
			GraphModel model, GraphLayoutCache cache) {
		super(model, cache);
		if (cache == null) {
			cache = new GraphLayoutCache(model, new DefaultCellViewFactory(),
					true);
		}
		this.setGraphLayoutCache(cache);
		setPortsVisible(false);
		setGridEnabled(true);
		setGridSize(10);
		setEdgeLabelsMovable(false);
		setGridVisible(true);
		setTolerance(2);
		setCloneable(false);
		setJumpToDefaultPort(true);
		setAutoscrolls(true);
		setEditable(true);
		setMarqueeHandler(new MarqueeHandler(this));
		addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			RandomVariable selectedVariable = null;

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				selectedVariable = null;
				try {

					DefaultPort port = (DefaultPort) getPortForLocation(e
							.getPoint().x, e.getPoint().y);
					selectedVariable = ((RandomVariable) ((DefaultGraphCell) getModel()
							.getParent(port)).getUserObject());
				} catch (Exception ex) {
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (selectedVariable != null) {
					selectedVariable.setLocation(e.getPoint());
				}
			}
		});
		this.dbnModel = dbnModel;
		modelLoaded(this.dbnModel);
		this.dbnModel.subscribe(this);

		this.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_DELETE) {
					tryRemoveSelected();
				}
			}
		});
	}

	/**
	 * If time = -1, we show all edges, otherwise we only show the edges
	 * corresponding to the given timeslice
	 * 
	 * @param time
	 */
	public void setTimeSliceVisible(int time) {
		GraphLayoutCache cache = this.getGraphLayoutCache();
		Object[] cells = DefaultGraphModel.getAll(getModel());
		for (Object c : cells) {
			if (c instanceof DefaultEdge) {
				if (time == -1) {
					cache.setVisible(c, true);
				} else {
					if ((Integer) ((DefaultEdge) c).getUserObject() == time) {
						cache.setVisible(c, true);
					} else {
						cache.setVisible(c, false);
					}
				}
			}
		}
	}

	public void connect(Port source, Port target) {
		RandomVariable from = (RandomVariable) ((DefaultGraphCell) getModel()
				.getParent(source)).getUserObject();
		RandomVariable to = (RandomVariable) ((DefaultGraphCell) getModel()
				.getParent(target)).getUserObject();
		try {
			dbnModel.createEdge(from, to, dbnModel.getTemplateSlice());
		} catch (Exception ex) {
			repaint();
		}
	}

	public void tryRemoveSelected() {

		if (!isSelectionEmpty()) {
			Object[] cells = getSelectionCells();
			// cells = getDescendants(cells);
			for (Object c : cells) {
				if (c instanceof DefaultEdge) {
					DefaultEdge e = (DefaultEdge) c;
					GraphModel m = getModel();
					try {
						RandomVariable from = (RandomVariable) ((DefaultGraphCell) m
								.getParent(e.getSource())).getUserObject();
						RandomVariable to = (RandomVariable) ((DefaultGraphCell) m
								.getParent(e.getTarget())).getUserObject();
						dbnModel.removeEdge(from, to, (Integer) e
								.getUserObject());
					} catch (Exception ex) {

					}
				} else if (c instanceof DefaultPort) {
				} else if (c instanceof DefaultGraphCell) {
					DefaultGraphCell v = (DefaultGraphCell) c;
					try {
						RandomVariable rv = (RandomVariable) v.getUserObject();
						dbnModel.removeVariable(rv);
					} catch (Exception ex) {

					}
				}
			}
			// getGraphLayoutCache().remove(cells);
		}
	}

	final static float[] dash = { 5, 5 };

	private Map<Object, Object> createEdgeAttributes(boolean inter) {
		Map<Object, Object> map = new Hashtable<Object, Object>();
		GraphConstants.setDisconnectable(map, false);
		GraphConstants.setLineEnd(map, GraphConstants.ARROW_SIMPLE);
		GraphConstants.setLineStyle(map, GraphConstants.STYLE_SPLINE);
		GraphConstants.setRouting(map, ParallelEdgeRouter.getSharedInstance());
		if (inter) {
			GraphConstants.setDashPattern(map, dash);
		}
		return map;
	}

	// MarqueeHandler that Connects Vertices and Displays PopupMenus
	private class MarqueeHandler extends BasicMarqueeHandler {

		DynamicBayesNetJGraph graph;

		MarqueeHandler(DynamicBayesNetJGraph graph) {
			super();
			this.graph = graph;
		}

		// Holds the Start and the Current Point
		protected Point2D start, current;

		// Holds the First and the Current Port
		protected PortView port, firstPort;

		// Override to Gain Control (for PopupMenu and ConnectMode)
		@Override
		public boolean isForceMarqueeEvent(MouseEvent e) {
			// if (e.isShiftDown())
			// return false;
			// If Right Mouse Button we want to Display the PopupMenu
			port = getSourcePortAt(e.getPoint());

			if (SwingUtilities.isRightMouseButton(e)) {
				// Return Immediately
				return true;
			}

			return super.isForceMarqueeEvent(e);
		}

		// Display PopupMenu or Remember Start Location and First Port
		@Override
		public void mousePressed(final MouseEvent e) {
			// If Right Mouse Button
			if (SwingUtilities.isRightMouseButton(e) && port != null) {
				// Remember Start Location
				start = graph.toScreen(port.getLocation());
				// Remember First Port
				firstPort = port;
			} else {
				// Call Superclass
				super.mousePressed(e);
			}
		}

		// Find Port under Mouse and Repaint Connector
		@Override
		public void mouseDragged(MouseEvent e) {
			// If remembered Start Point is Valid
			if (start != null) {
				// Fetch Graphics from Graph
				Graphics g = graph.getGraphics();
				// Reset Remembered Port
				PortView newPort = getTargetPortAt(e.getPoint());
				// Do not flicker (repaint only on real changes)
				if (newPort == null || newPort != port) {
					// Xor-Paint the old Connector (Hide old Connector)
					paintConnector(Color.black, graph.getBackground(), g);
					// If Port was found then Point to Port Location
					port = newPort;
					if (port != null) {
						current = graph.toScreen(port.getLocation());
					// Else If no Port was found then Point to Mouse Location
					} else {
						current = graph.snap(e.getPoint());
					}
					// Xor-Paint the new Connector
					paintConnector(graph.getBackground(), Color.black, g);
				}
			}
			// Call Superclass
			super.mouseDragged(e);
		}

		public PortView getSourcePortAt(Point2D point) {
			// Disable jumping
			graph.setJumpToDefaultPort(true);
			PortView result;
			try {
				// Find a Port View in Model Coordinates and Remember
				result = graph.getPortViewAt(point.getX(), point.getY());
			} finally {
				// graph.setJumpToDefaultPort(true);
			}
			return result;
		}

		// Find a Cell at point and Return its first Port as a PortView
		protected PortView getTargetPortAt(Point2D point) {
			// Find a Port View in Model Coordinates and Remember
			return graph.getPortViewAt(point.getX(), point.getY());
		}

		// Connect the First Port and the Current Port in the Graph or Repaint
		@Override
		public void mouseReleased(MouseEvent e) {
			// If Valid Event, Current and First Port
			if (firstPort != null) {
				GraphModel m = graph.getModel();
				((RandomVariable) ((DefaultGraphCell) m.getParent(firstPort
						.getCell())).getUserObject()).setLocation(e.getPoint());
			}
			if (e != null && port != null && firstPort != null) {// && firstPort
																	// != port)
																	// {
				// Then Establish Connection
				try {
					GraphModel m = graph.getModel();
					dbnModel
							.createEdge((RandomVariable) ((DefaultGraphCell) m
									.getParent(firstPort.getCell()))
									.getUserObject(),
									(RandomVariable) ((DefaultGraphCell) m
											.getParent(port.getCell()))
											.getUserObject(), dbnModel
											.getTemplateSlice());
				} catch (Exception ex) {

				}
				graph.repaint();
				e.consume();
				// Else Repaint the Graph
			} else {
				graph.repaint();
			}
			// Reset Global Vars
			firstPort = port = null;
			start = current = null;
			// Call Superclass
			super.mouseReleased(e);
		}

		// Show Special Cursor if Over Port
		@Override
		public void mouseMoved(MouseEvent e) {
			// Check Mode and Find Port
			if (e != null && getSourcePortAt(e.getPoint()) != null
					&& graph.isPortsVisible()) {
				// Set Cusor on Graph (Automatically Reset)
				graph.setCursor(new Cursor(Cursor.HAND_CURSOR));
				// Consume Event
				// Note: This is to signal the BasicGraphUI's
				// MouseHandle to stop further event processing.
				e.consume();
			} else {
				// Call Superclass
				super.mouseMoved(e);
			}
		}

		// Use Xor-Mode on Graphics to Paint Connector
		protected void paintConnector(Color fg, Color bg, Graphics g) {
			// Set Foreground
			g.setColor(fg);
			// Set Xor-Mode Color
			g.setXORMode(bg);
			// Highlight the Current Port
			paintPort(graph.getGraphics());
			// If Valid First Port, Start and Current Point
			if (firstPort != null && start != null && current != null) {
				// Then Draw A Line From Start to Current Point
				g.drawLine((int) start.getX(), (int) start.getY(),
						(int) current.getX(), (int) current.getY());
			}
		}

		// Use the Preview Flag to Draw a Highlighted Port
		protected void paintPort(Graphics g) {
			// If Current Port is Valid
			if (port != null) {
				// If Not Floating Port...
				boolean o = (GraphConstants.getOffset(port.getAllAttributes()) != null);
				// ...Then use Parent's Bounds
				Rectangle2D r = (o) ? port.getBounds() : port.getParentView()
						.getBounds();
				// Scale from Model to Screen
				r = graph.toScreen((Rectangle2D) r.clone());
				// Add Space For the Highlight Border
				r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
						.getHeight() + 6);
				// Paint Port in Preview (=Highlight) Mode
				graph.getUI().paintCell(g, port, r, true);
			}
		}

	} // End of Editor.MyMarqueeHandler

	public void addEdge(RandomVariable fromVariable, RandomVariable toVariable,
			int timeSeparation) {

		// Construct Edge with no label
		DefaultEdge edge = new DefaultEdge();
		edge.setUserObject(timeSeparation);
		if (this.getModel().acceptsSource(edge,
				variableToPort.get(fromVariable))
				&& this.getModel().acceptsTarget(edge,
						variableToPort.get(toVariable))) {
			// Create a Map that holds the attributes for the edge

			edge.getAttributes().applyMap(
					createEdgeAttributes(timeSeparation != 0));
			// Insert the Edge and its Attributes
			this.getGraphLayoutCache().insertEdge(edge,
					variableToPort.get(fromVariable),
					variableToPort.get(toVariable));
		}
	}

	@SuppressWarnings("unchecked")
	public void removeEdge(RandomVariable fromVariable,
			RandomVariable toVariable, int timeSeparation) {
		Port p = variableToPort.get(fromVariable);
		Iterator i = p.edges();
		DefaultEdge toRemove = null;
		while (i.hasNext()) {
			toRemove = (DefaultEdge) i.next();
			if (timeSeparation == ((Integer) toRemove.getUserObject())) {
				break;
			}
		}
		if (toRemove != null) {
			Object[] cells = { toRemove };
			cells = getDescendants(cells);
			this.getGraphLayoutCache().remove(cells);
		}
	}

	public void addVariable(RandomVariable variable) {
		Color color;
		if (variable instanceof ContinuousVariable) {
			color = Color.yellow;
		} else {
			color = Color.blue;
		}

		DefaultGraphCell cell = new DefaultGraphCell(variable);
		cell.addPort();
		cell.getAttributes().applyMap(
				setCellPosition(this, variable.getLocation(), color));
		this.getGraphLayoutCache().insert(cell);
		variableToPort.put(variable, (Port) cell.getChildAt(0));
	}

	@SuppressWarnings("unchecked")
	public void removeVariable(RandomVariable variable) {
		Object[] cells = { getModel().getParent(variableToPort.get(variable)) };
		cells = getDescendants(cells);
		ArrayList<Object> toRemove = new ArrayList<Object>();
		for (Object c : cells) {
			if (c instanceof Port) {
				Port p = (Port) c;
				Iterator i = p.edges();
				while (i.hasNext()) {
					toRemove.add(i.next());
				}
			}
		}
		this.getGraphLayoutCache().remove(toRemove.toArray());
		this.getGraphLayoutCache().remove(cells);
		variableToPort.remove(variable);
	}

	@SuppressWarnings("unchecked")
	public void updateVariable(RandomVariable variable) {
		DefaultGraphCell cell = ((DefaultGraphCell) ((DefaultPort) variableToPort
				.get(variable)).getParent());
		Map map = new Hashtable();
		GraphConstants.setResize(map, true);
		getGraphLayoutCache().editCell(cell, map);
	}

	public void modelFinishedInfering(DynamicBayesNetModel model) {
		// TODO Auto-generated method stub

	}

	public void modelFinishedLearning(DynamicBayesNetModel model) {
		// TODO Auto-generated method stub

	}

	public void modelLoaded(DynamicBayesNetModel model) {
		// TODO Auto-generated method stub

	}

	public void modelUnloaded(DynamicBayesNetModel model) {
		this.removeAll();
		List<Object> roots = new ArrayList<Object>();
		GraphModel m = this.getModel();
		int cnt = m.getRootCount();
		for (int i = 0; i < cnt; i++) {
			roots.add(m.getRootAt(i));
		}
		this.getModel().remove(roots.toArray());
		this.variableToPort.clear();
	}

}
