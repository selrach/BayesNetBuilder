package selrach.bnetbuilder.model.algorithms.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import selrach.bnetbuilder.model.variable.Clique;
import selrach.bnetbuilder.model.variable.CliqueSeparator;
import selrach.bnetbuilder.model.variable.GraphVariable;
import selrach.bnetbuilder.model.variable.JunctionTreeTemplate;

/**
 * Generates a junction tree from the minimal RIP set.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class GenerateJunctionTreeFromCliques {

	public static void execute(JunctionTreeTemplate jtt)
	{
		//Grab the cliques, they should be a minimal RIP set
		List<List<Clique>> cliqueSets = jtt.getCliqueSets();
		List<List<CliqueSeparator>> cliqueSeparators = new ArrayList<List<CliqueSeparator>>();
		jtt.resetCutVariables();
		
		for(List<Clique> cliques : cliqueSets)
		{
			List<CliqueSeparator> cliqueSeparator = makeJTreeFromRIPCliques(cliques);
			cliqueSeparators.add(cliqueSeparator);
		}
		jtt.setCliqueSeparatorSets(cliqueSeparators);
	}
	
	private static List<CliqueSeparator> makeJTreeFromRIPCliques(List<Clique> cliques)
	{
		List<CliqueSeparator> cliqueSeparators = new ArrayList<CliqueSeparator>();
		if(cliques.size() <= 1) {
			return cliqueSeparators;
		}
		
		Set<GraphVariable> vars = new TreeSet<GraphVariable>();
		vars = cliques.get(0).union(vars);
		for(int j=1; j<cliques.size(); j++)
		{
			Clique c1 = cliques.get(j);
			Set<GraphVariable> intersect = c1.intersect(vars);
			vars = c1.union(vars);
			for(int i=j-1; i>=0; i--)
			{
				Clique c2 = cliques.get(i);
				//if c2 and c1 have something in common
				if(Clique.isProperSubset(intersect, c2))
				{
					//Add an edge
					CliqueSeparator cs = new CliqueSeparator(intersect, c1, c2);
					c1.addNeighbor(c2);
					c2.addNeighbor(c1);
					c1.addSeparator(cs);
					c2.addSeparator(cs);
					cliqueSeparators.add(cs);
					break; //Connected one, don't need to look for any more
				}
			}
		}	
		
		return cliqueSeparators;
	}
	
}
