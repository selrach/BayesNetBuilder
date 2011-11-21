package selrach.bnetbuilder.model.distributions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import selrach.bnetbuilder.model.Utility;
import selrach.bnetbuilder.model.distributions.conditional.LinearGaussianMix;
import selrach.bnetbuilder.model.distributions.interfaces.ConditionalDistribution;
import selrach.bnetbuilder.model.distributions.unconditional.Table;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Handles several calculations that would be awkward to place directly in the
 * corresponding distribution class. Primarily because bookkeeping needs to be
 * done
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class Operation {

	private static final double EPS = 0.00000001;

	static public class Quadruple implements Comparable<Quadruple> {
		public Quadruple(int r, int a, int b, String id) {
			this.r = r;
			this.a = a;
			this.b = b;
			this.id = id;
		}

		public int r, a, b;
		public String id;

		@Override
		public int compareTo(Quadruple o) {
			return r - o.r;
		}

		public static Map<String, Quadruple> combine(Map<String, Tuple> mA,
				Map<String, Tuple> mB) {
			Map<String, Quadruple> ret = new HashMap<String, Quadruple>();
			Set<String> keys = new HashSet<String>(mA.keySet());
			keys.addAll(mB.keySet());
			for (String key : keys) {
				int r = -1, a = -1, b = -1;
				if (mA.containsKey(key)) {
					r = mA.get(key).r;
					a = mA.get(key).a;
				}
				if (mB.containsKey(key)) {
					r = mB.get(key).r;
					b = mB.get(key).a;
				}
				ret.put(key, new Quadruple(r, a, b, key));
			}
			return ret;
		}

		public Tuple getTuple(boolean aorb) {
			if (aorb) {
				return new Tuple(r, a, id);
			}
			return new Tuple(r, b, id);
		}

		@Override
		public String toString() {
			return "{" + r + ", " + a + ", " + b + "} - " + id;
		}
	}

	static public class Tuple implements Comparable<Tuple> {
		public Tuple(int r, int a, String id) {
			this.r = r;
			this.a = a;
			this.id = id;
		}

		public int r, a;
		public String id;

		@Override
		public int compareTo(Tuple o) {
			return r - o.r;
		}

		public Pair getPair(boolean rora) {
			if (rora) {
				return new Pair(r, id);
			}
			return new Pair(a, id);
		}

		@Override
		public String toString() {
			return "{" + r + ", " + a + "} - " + id;
		}
	}

	static public class Pair implements Comparable<Pair> {
		public Pair(int r, String id) {
			this.r = r;
			this.id = id;
		}

		public int r;
		public String id;

		@Override
		public int compareTo(Pair o) {
			return r - o.r;
		}

		@Override
		public String toString() {
			return "{" + r + "} - " + id;
		}
	}

	static public int[][] makeIndexMaps(Map<String, Quadruple> map) {
		int[][] indices = new int[2][];
		indices[0] = new int[map.size()];
		indices[1] = new int[map.size()];
		for (Quadruple q : map.values()) {
			indices[0][q.r] = q.a;
			indices[1][q.r] = q.b;
		}

		return indices;
	}

	static public int[] makeIndexMap(Map<String, Tuple> map) {
		int[] indices = new int[map.size()];
		List<Tuple> t = new ArrayList<Tuple>(map.values());
		//Collections.sort(t);
		for (int i = 0; i < t.size(); i++) {
			indices[i] = t.get(i).a;
		}
		return indices;
	}

	static public ConditionalDistribution combine(ConditionalDistribution a,
			ConditionalDistribution b, List<Quadruple> discreteOverlapIndices,
			List<Quadruple> headOverlapIndices,
			List<Quadruple> tailOverlapIndices) throws Exception {
		Collections.sort(discreteOverlapIndices);
		if (headOverlapIndices != null) {
			Collections.sort(headOverlapIndices);
		}
		if (tailOverlapIndices != null) {
			Collections.sort(tailOverlapIndices);
		}
		if (a instanceof Table && b instanceof Table) {
			return combine((Table) a, (Table) b, discreteOverlapIndices);
		}
		if (a instanceof LinearGaussianMix && !(b instanceof LinearGaussianMix)) {
			b = new LinearGaussianMix(b);
		}
		if (b instanceof LinearGaussianMix && !(a instanceof LinearGaussianMix)) {
			a = new LinearGaussianMix(a);
		}

		if (a instanceof LinearGaussianMix && b instanceof LinearGaussianMix) {

			return combine((LinearGaussianMix) a, (LinearGaussianMix) b,
					discreteOverlapIndices, headOverlapIndices,
					tailOverlapIndices);
		}

		return null;
	}

	/**
	 * This is point-wise multiplication between two factors
	 * 
	 * @param a
	 * @param b
	 * @param indices
	 * @return
	 * @throws Exception
	 */
	static private Table combine(Table a, Table b, List<Quadruple> indices)
			throws Exception {

		DoubleMatrix1D aindices = DoubleFactory1D.dense.make(a
				.getNumberDimensions(), 0);
		DoubleMatrix1D bindices = DoubleFactory1D.dense.make(b
				.getNumberDimensions(), 0);
		DoubleMatrix1D retindices = DoubleFactory1D.dense.make(indices.size(),
				0);

		int[] stateLayout = new int[indices.size()];
		for (int i = 0; i < stateLayout.length; i++) {
			Quadruple t = indices.get(i);
			if (t.a != -1) {
				stateLayout[i] = a.getStateLayout()[t.a];
			} else {
				stateLayout[i] = b.getStateLayout()[t.b];
			}
		}

		ArrayList<Double> dbl = new ArrayList<Double>();

		do {
			Utility.setupIndices(stateLayout, retindices, aindices, bindices,
					indices);
			double p = a.getProbability(aindices) * b.getProbability(bindices);
			dbl.add(p);
		} while (Utility.incrementIndice(retindices, stateLayout));

		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(dbl.size());
		for (int i = 0; i < probabilities.size(); i++) {
			probabilities.setQuick(i, dbl.get(i));
		}

		if (stateLayout.length == 0) {
			stateLayout = new int[1];
			stateLayout[0] = 1;
		}

		return new Table(stateLayout, probabilities);

	}

	/**
	 * 
	 * @param a
	 * @param b
	 * @param discreteIndices
	 * @param headIndices
	 * @param tailIndices
	 * @return
	 * @throws Exception
	 */
	static private LinearGaussianMix combine(LinearGaussianMix a,
			LinearGaussianMix b, List<Quadruple> discreteIndices,
			List<Quadruple> headIndices, List<Quadruple> tailIndices)
			throws Exception {

		Map<String, Tuple> discrete1 = new HashMap<String, Tuple>();
		Map<String, Tuple> discrete2 = new HashMap<String, Tuple>();
		Map<String, Tuple> head1 = new HashMap<String, Tuple>();
		Map<String, Tuple> head2 = new HashMap<String, Tuple>();
		Map<String, Tuple> tail1 = new HashMap<String, Tuple>();
		Map<String, Tuple> tail2 = new HashMap<String, Tuple>();
		Map<String, Tuple> newDiscrete = new HashMap<String, Tuple>();
		Map<String, Tuple> newHead = new HashMap<String, Tuple>();
		Map<String, Tuple> newTail = new HashMap<String, Tuple>();

		for (Quadruple t : discreteIndices) {
			if (t.a != -1) {
				discrete1.put(t.id, t.getTuple(true));
			}
			if (t.b != -1) {
				discrete2.put(t.id, t.getTuple(false));
			}
		}
		for (Quadruple t : headIndices) {
			if (t.a != -1) {
				head1.put(t.id, t.getTuple(true));
			}
			if (t.b != -1) {
				head2.put(t.id, t.getTuple(false));
			}
		}
		for (Quadruple t : tailIndices) {
			if (t.a != -1) {
				tail1.put(t.id, t.getTuple(true));
			}
			if (t.b != -1) {
				tail2.put(t.id, t.getTuple(false));
			}
		}

		return recursiveCombination(a, b, discrete1, discrete2, head1, head2,
				tail1, tail2, newDiscrete, newHead, newTail);
	}

	private static LinearGaussianMix recursiveCombination(LinearGaussianMix a,
			LinearGaussianMix b, Map<String, Tuple> discrete1,
			Map<String, Tuple> discrete2, Map<String, Tuple> head1,
			Map<String, Tuple> head2, Map<String, Tuple> tail1,
			Map<String, Tuple> tail2, Map<String, Tuple> newDiscrete,
			Map<String, Tuple> newHead, Map<String, Tuple> newTail)
			throws Exception {

		HashMap<String, Tuple> intersection = new HashMap<String, Tuple>(head2);
		intersection.keySet().retainAll(tail1.keySet());
		if (intersection.isEmpty()) {
			// H_1 \intersect D_2 == EMPTY
			return doCombine(a, b, discrete1, discrete2, head1, head2, tail1,
					tail2, newDiscrete, newHead, newTail);
		} else {
			intersection = new HashMap<String, Tuple>(head1);
			intersection.keySet().retainAll(tail2.keySet());
			if (intersection.isEmpty()) {
				// H_2 \intersect D_1 == EMPTY
				return doCombine(b, a, discrete2, discrete1, head2, head1,
						tail2, tail1, newDiscrete, newHead, newTail);
			} else {

				HashMap<String, Tuple> D_1 = new HashMap<String, Tuple>(head1);
				D_1.putAll(tail1);

				HashMap<String, Tuple> D_2 = new HashMap<String, Tuple>(head2);
				D_2.putAll(tail2);

				HashMap<String, Tuple> D_12 = new HashMap<String, Tuple>(head1);
				D_12.keySet().removeAll(D_2.keySet());

				HashMap<String, Tuple> D_21 = new HashMap<String, Tuple>(head2);
				D_21.keySet().removeAll(D_1.keySet());

				// Neither condition true, select a distribution to recurse upon
				if (!D_12.isEmpty()) {

					// D_12 should be the head set I am marginalizing out,
					// consequentially it will be my complement's head set;

					LinearGaussianMix a_1 = (LinearGaussianMix) a.marginalize(
							null, D_12.values());

					HashMap<String, Tuple> mHead = new HashMap<String, Tuple>(
							head1);
					mHead.keySet().removeAll(D_12.keySet());

					HashMap<String, Tuple> cTail = new HashMap<String, Tuple>(
							tail1);
					cTail.putAll(mHead);

					LinearGaussianMix a_2 = (LinearGaussianMix) a.complement(
							null, D_12.values());

					HashMap<String, Tuple> nHead = new HashMap<String, Tuple>();
					HashMap<String, Tuple> nTail = new HashMap<String, Tuple>();
					HashMap<String, Tuple> nDiscrete = new HashMap<String, Tuple>();

					return recursiveCombination(recursiveCombination(a_1, b,
							discrete1, discrete2, mHead, head2, tail1, tail2,
							nDiscrete, nHead, nTail), a_2, nDiscrete,
							discrete1, nHead, D_12, nTail, cTail, newDiscrete,
							newHead, newTail);

				} else if (!D_21.isEmpty()) {

					LinearGaussianMix b_1 = (LinearGaussianMix) b.marginalize(
							null, D_21.values());

					HashMap<String, Tuple> mHead = new HashMap<String, Tuple>(
							head2);
					mHead.keySet().removeAll(D_21.keySet());
					HashMap<String, Tuple> cTail = new HashMap<String, Tuple>(
							tail2);
					cTail.putAll(mHead);

					LinearGaussianMix b_2 = (LinearGaussianMix) b.complement(
							null, D_21.values());

					HashMap<String, Tuple> nHead = new HashMap<String, Tuple>();
					HashMap<String, Tuple> nTail = new HashMap<String, Tuple>();
					HashMap<String, Tuple> nDiscrete = new HashMap<String, Tuple>();

					return recursiveCombination(recursiveCombination(b_1, a,
							discrete2, discrete1, mHead, head1, tail2, tail1,
							nDiscrete, nHead, nTail), b_2, nDiscrete,
							discrete2, nHead, D_21, nTail, cTail, newDiscrete,
							newHead, newTail);
				} else {
					// This should not be possible for Bayesian networks, but
					// might happen for completely arbitrary combinations
					throw new Exception(
							"Cannot create combination for this distribution.");
				}
			}
		}
	}

	/**
	 * 
	 * For this function to work we must have the head domain of a be
	 * independent of the domain of b, this should be checked before this
	 * function is called
	 * 
	 * @param a
	 * @param b
	 * @param discrete1
	 * @param discrete2
	 * @param head1
	 * @param head2
	 * @param tail1
	 * @param tail2
	 * @param newHead
	 * @param newTail
	 * @return
	 * @throws Exception
	 */
	private static LinearGaussianMix doCombine(LinearGaussianMix a,
			LinearGaussianMix b, Map<String, Tuple> discrete1,
			Map<String, Tuple> discrete2, Map<String, Tuple> head1,
			Map<String, Tuple> head2, Map<String, Tuple> tail1,
			Map<String, Tuple> tail2, Map<String, Tuple> newDiscrete,
			Map<String, Tuple> newHead, Map<String, Tuple> newTail)
			throws Exception {

		newDiscrete.clear();
		newHead.clear();
		newTail.clear();

		tail1 = new HashMap<String, Tuple>(tail1);
		tail2 = new HashMap<String, Tuple>(tail2);

		Map<String, Tuple> missingT_1 = new HashMap<String, Tuple>(tail2);
		missingT_1.keySet().removeAll(head1.keySet());
		missingT_1.keySet().removeAll(tail1.keySet());

		for (Tuple t : missingT_1.values()) {
			tail1.put(t.id, new Tuple(t.r, a.extend(-1), t.id));
		}
		// tail1 is now fully extended

		Map<String, Tuple> missingT_2 = new HashMap<String, Tuple>(tail1);
		missingT_2.putAll(head1);
		missingT_2.keySet().removeAll(tail2.keySet());

		for (Tuple t : missingT_2.values()) {
			tail2.put(t.id, new Tuple(t.r, b.extend(-1), t.id));
		}
		// tail2 is now fully extended

		Map<String, Tuple> tmp1 = new HashMap<String, Tuple>(discrete1);
		discrete1 = new HashMap<String, Tuple>(discrete1);
		tmp1.keySet().removeAll(discrete2.keySet());

		Map<String, Tuple> tmp2 = new HashMap<String, Tuple>(discrete2);
		discrete2 = new HashMap<String, Tuple>(discrete2);
		tmp2.keySet().removeAll(discrete1.keySet());

		for (Tuple t : tmp1.values()) {
			discrete2.put(t.id, new Tuple(t.r,
					b.extend(a.getDensityProbabilityDistribution()
							.getStateLayout()[t.a]), t.id));
		}

		for (Tuple t : tmp2.values()) {
			discrete1.put(t.id, new Tuple(t.r,
					a.extend(b.getDensityProbabilityDistribution()
							.getStateLayout()[t.a]), t.id));
		}
		// Discrete domain is now fully extended. They should have the same
		// domain

		return (LinearGaussianMix) a.combine(b, discrete1, discrete2, head1,
				head2, tail1, tail2);
	}

	/**
	 * This is point-wise division between two factors
	 * 
	 * @param a
	 * @param b
	 * @param indices
	 * @return
	 * @throws Exception
	 */
	static public Table divide(Table a, Table b, List<Quadruple> indices)
			throws Exception {

		DoubleMatrix1D aindices = DoubleFactory1D.dense.make(a
				.getNumberDimensions(), 0);
		DoubleMatrix1D bindices = DoubleFactory1D.dense.make(b
				.getNumberDimensions(), 0);
		DoubleMatrix1D retindices = DoubleFactory1D.dense.make(indices.size(),
				0);

		int[] stateLayout = new int[indices.size()];
		for (int i = 0; i < stateLayout.length; i++) {
			Quadruple t = indices.get(i);
			if (t.a != -1) {
				stateLayout[t.r] = a.getStateLayout()[t.a];
			} else {
				stateLayout[t.r] = b.getStateLayout()[t.b];
			}
		}

		ArrayList<Double> dbl = new ArrayList<Double>();

		do {
			Utility.setupIndices(stateLayout, retindices, aindices, bindices,
					indices);
			double pA = a.getProbability(aindices);
			double pB = b.getProbability(bindices);
			if (pB > EPS) {
				double p = pA / pB;
				dbl.add(p);
			} else {
				dbl.add(0.0);
			}
		} while (Utility.incrementIndice(retindices, stateLayout));

		DoubleMatrix1D probabilities = DoubleFactory1D.dense.make(dbl.size());
		for (int i = 0; i < probabilities.size(); i++) {
			probabilities.setQuick(i, dbl.get(i));
		}

		return new Table(stateLayout, probabilities);
	}
}
