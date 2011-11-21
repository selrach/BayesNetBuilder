package selrach.bnetbuilder.model;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import selrach.bnetbuilder.model.dao.DynamicBayesNetDao;
import selrach.bnetbuilder.model.dao.IncorrectFileFormatException;
import selrach.bnetbuilder.model.distributions.DistributionFactory;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.variable.ContinuousVariable;
import selrach.bnetbuilder.model.variable.DiscreteVariable;
import selrach.bnetbuilder.model.variable.RandomVariable;

/**
 * This is the parser and printer of the Dynamic Extended XBN file format
 * created for this program.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DynamicBNIFExtendedDynamicBayesNetDao extends DefaultHandler2
		implements DynamicBayesNetDao {

	private static Logger logger = Logger
			.getLogger(DynamicBNIFExtendedDynamicBayesNetDao.class);
	private DynamicBayesNetModel model = null;

	public interface XMLHandler {
		public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
				Attributes attr) throws Exception;

		public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
				String chars) throws Exception;
	}

	final class FormatConstants {
		final static public String FORMAT = "Dynamic Extended XBN";
		final static public String VERSION = "0.1";
		final static public String CREATOR = "Selrach";
	}

	boolean variablesCreated = false;
	boolean arcsCreated = false;
	boolean distributionsCreated = false;
	RandomVariable currentVariable = null;
	Integer currentVariableTime;
	List<RandomVariable> conditionOrder = new ArrayList<RandomVariable>();
	List<Integer> conditionOrderTimes = new ArrayList<Integer>();
	String dpi = null;
	Map<String, String> dpiMap = new HashMap<String, String>();

	List<String> currentStates = new ArrayList<String>();

	public enum XMLConstants {
		ANALYSISNOTEBOOK("analysisnotebook", null), DBNMODEL("dbnmodel", null), STATICPROPERTIES(
				"staticproperties", null), FORMAT("format", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				String value = attr.getValue("", "value");
				if (value == null || !value.equals(FormatConstants.FORMAT)) {
					throw new IncorrectFileFormatException("Format value was "
							+ value);
				}
			}

		}), VERSION("version", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				// TODO Auto-generated method stub

			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				String value = attr.getValue("", "value");
				if (value == null || !value.equals(FormatConstants.VERSION)) {
					throw new IncorrectFileFormatException(
							"Network version not supported: " + value);
				}

			}

		}), CREATOR("creator", null), VARIABLES("variables", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				dao.variablesCreated = true;
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
			}

		}), VAR("var", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				dao.model.createNewVariable(dao.currentVariable);
				dao.currentVariable = null;
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				String type = attr.getValue("", "type");
				if (type.equals("discrete")) {
					dao.currentVariable = new DiscreteVariable(dao.model);
				} else if (type.equals("continuous")) {
					dao.currentVariable = new ContinuousVariable(dao.model);
				}
				dao.currentVariable.setId(attr.getValue("", "id"));
				dao.currentVariable.setName(attr.getValue("", "name"));
				dao.currentVariable.setOrder(Integer.parseInt(attr.getValue("",
						"order")));
				dao.currentVariable.setLocation(new Point(Integer.parseInt(attr
						.getValue("", "xpos")), Integer.parseInt(attr.getValue(
						"", "ypos"))));

			}

		}), DESCRIPTION("description", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				dao.currentVariable.setDescription(chars);
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {

			}

		}), STATESET("stateset", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				if (dao.currentVariable instanceof DiscreteVariable) {
					((DiscreteVariable) dao.currentVariable)
							.setStates(dao.currentStates);
				}
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				dao.currentStates.clear();
			}

		}), STATENAME("statename", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				dao.currentStates.add(chars);
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {

				int i = 0;
				i++;
			}

		}), STRUCTURE("structure", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				dao.arcsCreated = true;
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				if (!dao.variablesCreated) {
					throw new Exception("Variables have not been set up yet");
				}
			}

		}), ARC("arc", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				dao.model.createEdge(attr.getValue("", "parent"), attr
						.getValue("", "child"), Integer.parseInt(attr.getValue(
						"", "time")));
			}

		}), DISTRIBUTIONS("distributions", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				if (!dao.variablesCreated) {
					throw new Exception("Variables have not been created yet.");
				}
				if (!dao.arcsCreated) {
					throw new Exception("Arcs have not been created yet!");
				}
			}

		}), DIST("dist", new XMLHandler() {
			String type = "";

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				ConditionalDistribution cpd = DistributionFactory.getCPD(
						dao.currentVariable, dao.currentVariableTime, type);
				cpd.setup(dao.currentVariable
						.getParents(dao.currentVariableTime),
						dao.conditionOrder, dao.conditionOrderTimes, dao.dpi,
						dao.dpiMap);
				dao.currentVariable.setCpd(cpd, dao.currentVariableTime);
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				dao.dpiMap.clear();
				dao.dpi = null;
				dao.conditionOrder.clear();
				dao.conditionOrderTimes.clear();
				dao.currentVariable = null;
				type = attr.getValue("", "type");
			}

		}), CONDSET("condset", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				// Maybe verify that we have the correct number of elements
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				// Nothing to do
			}

		}), COND("cond", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				// Nothing to do
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				String id = attr.getValue("", "id");
				if (id == null) {
					throw new Exception("id in cond element null");
				}
				Integer time = Integer.parseInt(attr.getValue("", "time")
						.trim());
				if (time == null) {
					throw new Exception("time in cond element null");
				}

				dao.conditionOrder.add(dao.model.getVariableMap().get(id));
				dao.conditionOrderTimes.add(time);
			}

		}), PRIVATE("private", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				// Nothing to do
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				String id = attr.getValue("", "name");
				if (id == null) {
					throw new Exception("name in private element null");
				}
				Integer time = Integer.parseInt(attr.getValue("", "time")
						.trim());
				if (time == null) {
					throw new Exception("time in private element null");
				}

				dao.currentVariable = dao.model.getVariableMap().get(id);
				dao.currentVariableTime = time;
			}

		}), DPIS("dpis", new XMLHandler() {

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				// Nothing to do
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				// Nothing to do
			}

		}), DPI("dpi", new XMLHandler() {
			String indices = null;

			public void endElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					String chars) throws Exception {
				if (indices == null) {
					dao.dpi = chars.trim();
				} else {
					dao.dpiMap.put(indices.trim(), chars.trim());
				}
			}

			public void startElement(DynamicBNIFExtendedDynamicBayesNetDao dao,
					Attributes attr) throws Exception {
				indices = null;
				indices = attr.getValue("", "index");
			}

		});

		private final String value;
		private final XMLHandler handler;

		XMLConstants(String value, XMLHandler handler) {
			this.value = value;
			this.handler = handler;

		}

		public void startElement(DynamicBNIFExtendedDynamicBayesNetDao model,
				Attributes attr) throws Exception {
			if (handler != null) {
				handler.startElement(model, attr);
			}
		}

		public void endElement(DynamicBNIFExtendedDynamicBayesNetDao model,
				String buffer) throws Exception {
			if (handler != null) {
				handler.endElement(model, buffer);
			}
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	public DynamicBNIFExtendedDynamicBayesNetDao() {
	}

	public void loadModel(String filename) throws Exception {

		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(this);
			parser.setEntityResolver(this);
			parser.setErrorHandler(this);
			FileReader fr = new FileReader(filename);
			parser.parse(new InputSource(fr));
		} catch (Exception ex) {
			throw new IncorrectFileFormatException(ex);
		}
	}

	public void saveModel(String filename) throws Exception {

		String trimmedFilename = filename
				.substring(filename.lastIndexOf('/') + 1);
		trimmedFilename = trimmedFilename.substring(trimmedFilename
				.lastIndexOf('\\') + 1);
		if (trimmedFilename.indexOf('.') > 0) {
			trimmedFilename = trimmedFilename.substring(0, trimmedFilename
					.indexOf('.'));
		}

		StringBuilder sb = new StringBuilder("<");
		sb.append(XMLConstants.ANALYSISNOTEBOOK);
		sb.append(" name=\"DBN Builder Network\" root=\"");
		sb.append(trimmedFilename);
		sb.append("\">\n<");
		sb.append(XMLConstants.DBNMODEL);
		sb.append(" name=\"");
		sb.append(trimmedFilename);
		sb.append("\">\n<");
		sb.append(XMLConstants.STATICPROPERTIES);
		sb.append("><");
		sb.append(XMLConstants.FORMAT);
		sb.append(" value=\"");
		sb.append(FormatConstants.FORMAT);
		sb.append("\" />\n<");
		sb.append(XMLConstants.VERSION);
		sb.append(" value=\"");
		sb.append(FormatConstants.VERSION);
		sb.append("\" />\n<");
		sb.append(XMLConstants.CREATOR);
		sb.append(" value=\"");
		sb.append(FormatConstants.CREATOR);
		sb.append("\" />\n</");
		sb.append(XMLConstants.STATICPROPERTIES);
		sb.append(">\n<");
		sb.append(XMLConstants.VARIABLES);
		sb.append(">\n");

		for (RandomVariable var : model.variables.values()) {
			sb.append(var.getXMLDescription());
		}

		sb.append("</");
		sb.append(XMLConstants.VARIABLES);
		sb.append(">\n<");
		sb.append(XMLConstants.STRUCTURE);
		sb.append(">\n");

		for (RandomVariable var : model.variables.values()) {
			for (int i = 0; i < model.numberTemplateSlices; i++) {
				for (RandomVariable to : var.getChildrenAt(i)) {
					sb.append("<");
					sb.append(XMLConstants.ARC);
					sb.append(" parent=\"");
					sb.append(var.getId());
					sb.append("\" child=\"");
					sb.append(to.getId());
					sb.append("\" time=\"");
					sb.append(i);
					sb.append("\" />\n");
				}
			}
		}

		sb.append("</");
		sb.append(XMLConstants.STRUCTURE);
		sb.append(">\n<");
		sb.append(XMLConstants.DISTRIBUTIONS);
		sb.append(">\n");

		for (RandomVariable var : model.variables.values()) {
			sb.append(var.getXMLDistributionDescription());
		}

		sb.append("</");
		sb.append(XMLConstants.DISTRIBUTIONS);
		sb.append(">\n</");
		sb.append(XMLConstants.DBNMODEL);
		sb.append(">\n</");
		sb.append(XMLConstants.ANALYSISNOTEBOOK);
		sb.append(">");
		FileWriter writer = new FileWriter(filename);
		BufferedWriter buff = new BufferedWriter(writer);
		buff.write(sb.toString());
		buff.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {

		try {
			model.clear();
			variablesCreated = false;
			arcsCreated = false;
			distributionsCreated = false;
		} catch (Exception ex) {
			logger.info(ex);
			throw new SAXException(ex);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		// Excellent it parsed fine
	}

	private final StringBuilder buffer = new StringBuilder();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String name, String qName,
			Attributes attr) throws SAXException {
		try {
			XMLConstants tag = XMLConstants.valueOf(name.toUpperCase());
			tag.startElement(this, attr);
			buffer.setLength(0);
		} catch (Exception ex) {
			logger.info(ex);
			throw new SAXException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String name, String qName)
			throws SAXException {
		try {
			XMLConstants tag = XMLConstants.valueOf(name.toUpperCase());
			tag.endElement(this, buffer.toString());
		} catch (Exception ex) {
			if (logger.isInfoEnabled()) {
				logger.info(ex);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(ex.toString(), ex);
			}
			throw new SAXException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] chars, int start, int length)
			throws SAXException {
		buffer.append(chars, start, length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
	 */
	@Override
	public void error(SAXParseException arg0) throws SAXException {
		super.error(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException
	 * )
	 */
	@Override
	public void fatalError(SAXParseException arg0) throws SAXException {
		super.fatalError(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
	 */
	@Override
	public void warning(SAXParseException arg0) throws SAXException {
		logger.debug(arg0);
		super.warning(arg0);
	}

	@Override
	public String getName() {
		return FormatConstants.FORMAT + " " + FormatConstants.VERSION;
	}

	@Override
	public void loadModel(DynamicBayesNetModel model, String filename)
			throws Exception {
		this.model = model;
		loadModel(filename);
	}

	@Override
	public void saveModel(DynamicBayesNetModel model, String filename)
			throws Exception {
		this.model = model;
		saveModel(filename);
	}

}
