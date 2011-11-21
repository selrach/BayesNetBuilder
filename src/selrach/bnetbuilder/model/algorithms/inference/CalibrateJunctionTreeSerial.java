package selrach.bnetbuilder.model.algorithms.inference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import selrach.bnetbuilder.model.DynamicBayesNetModel;
import selrach.bnetbuilder.model.algorithms.graph.MakeJunctionTreeTemplate;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import selrach.bnetbuilder.model.variable.Factor;
import selrach.bnetbuilder.model.variable.JunctionTree;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;
import selrach.bnetbuilder.model.variable.TransientClique;
import selrach.bnetbuilder.model.variable.TransientCliqueSeparator;

/**
 * This is a serial implementation of message passing that basically runs in
 * O(n) on a constructed junction tree.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class CalibrateJunctionTreeSerial {

	private final static Logger logger = Logger
			.getLogger(CalibrateJunctionTreeSerial.class);

	private static final CalibrateJunctionTreeSerial instance = new CalibrateJunctionTreeSerial();

	private CalibrateJunctionTreeSerial() {
	}

	public static CalibrateJunctionTreeSerial getInstance() {
		return instance;
	}

	/**
	 * Runs the algorithm, utilizes any evidence set in the model.
	 * 
	 * @param model
	 */
	public void execute(DynamicBayesNetModel model, PrintStream updateTracking)
			throws Exception {
		JunctionTreeTemplate jtt = model.getJunctionTreeTemplate();
		if (jtt.isStale()) {
			MakeJunctionTreeTemplate.execute(model);
		}

		// For each junction tree created in the model 0...T, Go forward and do
		// the
		// distributions to each node
		// Copy the interface from the previous timeslice to the current
		// timeslice,
		// Run the serial message passing

		// now for each slice we need to set the evidence and run the algorithm

		List<JunctionTree> jtSlices = new ArrayList<JunctionTree>();

		final int slices = model.getMaxNumberSlices() - 1;
		final int numTemplateSlices = model.getNumberTemplateSlices() - 1;
		
		for (int i = 0; i < model.getMaxNumberSlices(); i++) 
			// Run this for each timeslice
		{
			int tSlice = Math.min(i, numTemplateSlices);
			JunctionTree jt = new JunctionTree(jtt.getCliqueSets().get(tSlice),
					jtt.getCliqueSeparatorSets().get(tSlice), i);
			jtSlices.add(jt);
			jt.setEvidence(model.getSlice(i));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("RUNNING COLLECT");
		}

		for (int i = slices; i >= 0; i--) {
			JunctionTree jt = jtSlices.get(i);
			
			for (int j = 0; j < Math.min(slices - i, numTemplateSlices); j++) {
				jt.linkInterface(0, jtSlices.get(i + j + 1)
						.getForwardInterface(numTemplateSlices - j));
				// I want to link the current slice with any slices in the
				// future
				// i+0+1, i+1+1, 2-0, 2-1
			}
			jt.clearParents();
			runCollect(jt);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("RUNNING DISTRIBUTE");
		}

		for (int i = 0; i < model.getMaxNumberSlices(); i++) // Run this for
																// each
		// timeslice
		{
			JunctionTree jt = jtSlices.get(i);
			for (int j = 0; j < Math.min(i, numTemplateSlices); j++) {
				jt.linkInterface(j + 1, jtSlices.get(i - j - 1)
						.getForwardInterface(0));
				// I want to link the current junction tree with any slices in
				// the past
				// 0+1, 1+1, i-0, i-1
				// Since we haven't added the slice yet we don't have to go back
				// further
				// than 0
			}
			jt.clearParents();
			runDistribute(jt);
		}
		for (int i = 0; i < model.getMaxNumberSlices(); i++) // Run this for
																// each
		// timeslice
		{
			JunctionTree jt = jtSlices.get(i);
			normalize(jt);
			if (logger.isDebugEnabled()) {
				logger.debug("-------NEW SLICE----------");
				logger.debug(jt);
			}
		}

		jtt.setJunctionTreeSlices(jtSlices);
	}
	

	private void runCollect(JunctionTree jt) throws Exception {
		// Pick a root:
		// Recursively descend tree in post order, generate the potential with
		// the
		// evidence set
		// Calculate the separator potential to the parent, which is simply the
		// projection/marginalization to the domain of the separator
		// Calculate the parents potential which is the separator potential *
		// prior
		// potential
		//

		TransientClique root = jt.getRoot();
		if (logger.isDebugEnabled()) {
			logger.debug("Root = " + root);
		}
		root.setParent(root);
		postOrderCollect(root);
	}

	private void postOrderCollect(TransientClique parent) throws Exception {
		for (TransientCliqueSeparator tcs : parent.getSeparators()) {
			TransientClique child = null;
			if (tcs.getCliqueA() == parent) {
				child = tcs.getCliqueB();
			} else if (tcs.getCliqueB() == parent) {
				child = tcs.getCliqueA();
			}

			if (child.getParent() == null) {
				child.setParent(parent);
				child.setParentSeparator(tcs);
				postOrderCollect(child);
				if (logger.isDebugEnabled()) {
					logger.debug("Running collect for child:\n"
							+ child.toDetailedString("") + "\nwith parent:\n"
							+ parent.toDetailedString(""));
				}
							

				Factor Tj = child.getFactor();
				Factor Tjk = Tj.marginalize(tcs.getMembers());
				//tcs.setPotential(Tjk);
				child.setFactor(Tj.complement(Tjk, tcs.getMembers()));
				parent.setFactor(parent.getFactor().combine(Tjk));

			}
		}
	}

	private void runDistribute(JunctionTree jt) throws Exception {
		// From the root:
		// Divide each child potential by the parent potential
		// Calculate separator potential of a child to parent
		// Calculate child potential which is separator potential * prior
		// potential
		// Normalize parent potential

		TransientClique root = jt.getRoot();
		root.setParent(root);
		if (logger.isDebugEnabled()) {
			logger.debug("Root = " + root);
		}
		preOrderDistribute(root);
	}

	private void preOrderDistribute(TransientClique parent) throws Exception {
		for (TransientCliqueSeparator tcs : parent.getSeparators()) {
			TransientClique child = null;
			if (tcs.getCliqueA() == parent) {
				child = tcs.getCliqueB();
			} else if (tcs.getCliqueB() == parent) {
				child = tcs.getCliqueA();
			}

			if (child.getParent() == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Running distribute for child:\n" + child
							+ "\nwith parent:\n" + parent);
				}

				child.setParent(parent);
				//child.setParentSeparator(tcs);
				

				/*
				 *Old Way
				Potential Ti = child.getPotential().divide(tcs.getPotential());
				Potential Tij = parent.getPotential().marginalize(
						tcs.getReference().getVariables());
				tcs.setPotential(Tij);
				child.setPotential(Ti.multiply(Tij));

				preOrderDistribute(child);
				*/
				
				Factor Cmarg = null;
				if(parent.getParentSeparator() == null)
				{
					Cmarg = parent.getFactor();
				}
				else
				{
					Cmarg = parent.getParentSeparator().getFactor().combine(parent.getFactor());
				}
				
				tcs.setFactor(Cmarg.marginalize(tcs.getMembers()));
				
				

				preOrderDistribute(child);
			}
		}
	}

	private void normalize(JunctionTree jt) {
		for (TransientClique clique : jt.getCliques()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Normalizing: " + clique);
			}
			ConditionalDistribution dist = clique.getFactor()
					.getDistribution();
			if (dist instanceof Table) {
				((Table) dist).normalize();
			}
		}
	}

}
