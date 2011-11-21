package selrach.bnetbuilder.model.algorithms.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import selrach.bnetbuilder.model.variable.Clique;
import selrach.bnetbuilder.model.variable.GraphVariable;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;

/**
 * Generate the elimination cliques from a set of moralized graphs
 * As a precondition, the moralized graphs for each time step that we are
 * interested in must be set up in the JunctionTreeTemplate 
 *
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class GenerateEliminationCliques {

	public static void execute(final JunctionTreeTemplate jtt)
	{
		final List<Map<String, GraphVariable>> moralizedGraphs = jtt.getVariableSets();
				
		final List<List<Clique>> cliqueSets = new ArrayList<List<Clique>>();
		
		for(final Map<String, GraphVariable> map : moralizedGraphs)
		{
			jtt.resetCutVariables();
			final List<Clique> cliqueSet = doCliqueElimination(map);
			cliqueSets.add(cliqueSet);
		}
		jtt.setCliqueSets(cliqueSets);
	}
	
	private static List<Clique> doCliqueElimination(final Map<String, GraphVariable> variables)
	{
		final List<Clique> cliques = new ArrayList<Clique>(variables.size());
		final Set<GraphVariable> variableCopy = new TreeSet<GraphVariable>(variables.values());
		final List<Clique> interfaces = new ArrayList<Clique>();
		final List<Map<String, GraphVariable>> iVars = new ArrayList<Map<String, GraphVariable>>();
		
		for(GraphVariable gv : variableCopy)
		{
			if(gv.isInInterface())
			{
				while(gv.getSlice() >= iVars.size())
				{
					iVars.add(new TreeMap<String, GraphVariable>());
				}
				iVars.get(gv.getSlice()).put(gv.getId(), gv);
			}
		}
		
		int i=0;
		for(Map<String, GraphVariable> map : iVars)
		{
			Clique c;
			interfaces.add(c = new Clique(map));
			c.setForwardInterface(i++);
		}
		
		
		while(variableCopy.size() > 0)
		{	
		/* minCliqueCardinality <-- MAX
		 * foreach variable:
		 *   cliqueCardinality <-- calculate clique cardinality
		 *   if(v.cliqueCardinality < minCliqueCardinality
		 *     useVariable <-- variable
		 *     minCliqueCardinality <-- cliqueCardinality
		 *   fi
		 * end
		 */
			GraphVariable toEliminate=null;
			int minCardinality = Integer.MAX_VALUE;
			for(final GraphVariable variable : variableCopy)
			{
				final int cardinality = calculateCliqueCardinality(variable);
				if(cardinality < minCardinality)
				{
					toEliminate = variable;
					minCardinality = cardinality;
					if(minCardinality==0) {
						break;
					}
				}
			}
			//Connect all neighbors of the elimination node together
			final List<GraphVariable> neighbors = toEliminate.getNeighborList();
			
			final int sz = neighbors.size();
			for(i=0; i<sz; i++)
			{
				final GraphVariable gv1 = neighbors.get(i);
				for(int j=i; j<sz; j++)
				{
					final GraphVariable gv2 = neighbors.get(j);
					gv1.addNeighbor(gv2);
					gv2.addNeighbor(gv1);
				}
			}
			
			final Clique clique = new Clique(toEliminate);
			cliques.add(clique);
			
			//Remove node from graph
			toEliminate.setCut(true);
			variableCopy.remove(toEliminate);
		}
		//We now have a list of RIP cliques, we should prune out unnecessary
		//cliques and maintain the RIP (Running Intersection Property)
		
		
		final int sz=cliques.size();
		//Eliminate redundant cliques by deleting any subset cliques of cliques further down the list
		//and replacing them by the super-set clique
		// C1,...,Ct-1,Cp,Ct+1,...Cp-1,Cp+1,Ck  where k is the number of cliques
		final List<Clique> minCliques = new ArrayList<Clique>(sz);
		for(i=sz-1; i>=0; i--)
		{
			Clique c1 = cliques.get(i);
			if(c1==null) {
				continue;
			}
			for(int j=i-1;j>=0; j--)
			{
				final Clique c2 = cliques.get(j);
				if(c2==null || interfaces.contains(c1)) {
					continue;
				}
				if(c1.isProperSubset(c2))
				{
					cliques.set(j, null);
					c1 = c2;
				}
			}
			minCliques.add(c1);
		}
		
		//Add interfaces 
		for(Clique c : interfaces)
		{
			if(c.isForwardInterface())
			{
				minCliques.add(c);
			}
		}
		
		//Return the minimum set of RIP cliques
		return minCliques;
	}
	
	private static int calculateCliqueCardinality(final GraphVariable variable)
	{
		/*
		//Min-Weight Heuristic for eliminaton ordering
		RandomVariable var = variable.getReference();
		int card = var.getCpd(variable.getSlice()).getNumberStates();
		if(card==-1) card = 1;
		for(GraphVariable gv : variable.getNeighborList())
		{
			RandomVariable rv = gv.getReference();
			int v = rv.getCpd(gv.getSlice()).getNumberStates();
			if(v==-1) v = 1;
			card *= v;
		}
		*/
		  
		//Min-Fill Heuristic for elimination ordering
		int card = 0;
		final List<GraphVariable> neighbors = variable.getNeighborList();
		final int sz = neighbors.size();
		for(int i=0; i<sz; i++)
		{
			final GraphVariable gv1 = neighbors.get(i);
			for(int j=i+1; j<sz; j++)
			{
				final GraphVariable gv2 = neighbors.get(j);
				if(!gv1.hasNeighbor(gv2)) {
					card++;
				}
			}
		}
		return card;
	}
}
