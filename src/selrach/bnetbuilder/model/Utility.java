package selrach.bnetbuilder.model;

import java.util.Collections;
import java.util.List;

import selrach.bnetbuilder.model.distributions.Operation.Quadruple;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * A collection of different, generic functionality that is used all over the
 * place
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public abstract class Utility {

	private Utility() {
	}

	static public final double eps = 0.0000000001;

	static public int[] convert1DoubleToIntArray(DoubleMatrix1D state) {
		int[] ret = new int[state.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (int) state.getQuick(i);
		}
		return ret;
	}

	static public int[] convert(List<Integer> list, int shift) {
		int[] arr = new int[list.size()];
		int j = 0;
		for (Integer i : list) {
			arr[j++] = i + shift;
		}
		return arr;
	}

	static public int[] convert(List<Integer> list) {
		return convert(list, 0);
	}

	static public double[] convert(List<Double> list) {
		double[] arr = new double[list.size()];
		int j = 0;
		for (Double i : list) {
			arr[j++] = i;
		}
		return arr;
	}

	/**
	 * Calculates the index of a discrete entry given the state layout and the
	 * value set. The state layout can contain continuous values (-1), they are
	 * simply ignored. The vector of state indices to be calculated on can only
	 * contain the discrete entries. Basically this converts a multidimensional
	 * discrete index into a single value index
	 * 
	 * @param e
	 *            The vector that contains the index of interest for each state
	 *            for each dimension
	 * @param stateLayout
	 *            The statelayout that corresponds to this vector in each
	 *            dimension
	 * @return The index for a 1-d object
	 */
	static public int calculateIndex(String[] e, int[] stateLayout) {
		int ind = 0;
		int i, k;
		int factor = 1;
		for (k = e.length, i = stateLayout.length - 1; i >= 0; i--) {
			if (stateLayout[i] < 0) {
				continue;
			}
			ind += factor * Integer.valueOf(e[k--]);
			factor *= stateLayout[i];
		}
		return ind;
	}

	/**
	 * Calculates the index of a discrete entry given the state layout and the
	 * value set. The state layout can contain continuous values (-1), they are
	 * simply ignored. The vector of state indices to be calculated on can only
	 * contain the discrete entries. Basically this converts a multidimensional
	 * discrete index into a single value index
	 * 
	 * @param e
	 *            The vector that contains the index of interest for each state
	 *            for each dimension
	 * @param stateLayout
	 *            The statelayout that corresponds to this vector in each
	 *            dimension
	 * @return The index for a 1-d object
	 */
	static public int calculateIndex(DoubleMatrix1D e, int[] stateLayout) {
		if (stateLayout.length == 1 && stateLayout[0] == 1) {
			return 0;
		}
		int ind = 0;
		int i, k;
		int factor = 1;
		for (k = e.size() - 1, i = stateLayout.length - 1; i >= 0; i--) {
			if (stateLayout[i] < 0) {
				continue;
			}
			ind += factor * (int) e.getQuick(k--);
			factor *= stateLayout[i];
		}
		return ind;
	}

	/**
	 * Calculates the index of a discrete entry given the state layout and the
	 * value set. The state layout can contain continuous values (-1), they are
	 * simply ignored. The vector of state indices to be calculated on can only
	 * contain the discrete entries. Basically this converts a multidimensional
	 * discrete index into a single value index
	 * 
	 * @param e
	 *            The vector that contains the index of interest for each state
	 *            for each dimension
	 * @param stateLayout
	 *            The statelayout that corresponds to this vector in each
	 *            dimension
	 * @return The index for a 1-d object
	 */
	static public int calculateIndex(int[] e, int[] stateLayout) {
		int ind = 0;
		int i, k;
		int factor = 1;
		for (k = e.length - 1, i = stateLayout.length - 1; i >= 0; i--) {
			if (stateLayout[i] < 0) {
				continue;
			}
			ind += factor * e[k--];
			factor *= stateLayout[i];
		}
		return ind;
	}

	/**
	 * Assumes ascending order of independent indices. Basically returns the
	 * indices that are not present in the current indices set given a total
	 * number of dimensions.
	 * 
	 * @param indices
	 * @return
	 */
	static public int[] indexComplement(int[] indices, int totalDimensions) {
		int j = 0, i = 0;
		int[] comp = new int[totalDimensions - indices.length];
		for (int k = 0; k < totalDimensions; k++) {
			if (indices.length == 0 || j >= indices.length || k != indices[j]) {
				comp[i++] = k;
			} else {
				j++;
			}
		}
		return comp;
	}

	/**
	 * increments the values array in accordance with the stateLayout specified.
	 * Returns true if the index was successfully updated and false if the index
	 * rolled over to the 0 state
	 * 
	 * @param values
	 * @param stateLayout
	 * @param ignore
	 *            set of indices to ignore while incrementing
	 * @return
	 */
	public static boolean incrementIndice(DoubleMatrix1D values,
			int[] stateLayout, List<Integer> ignore) {

		for (int i = values.size() - 1, k = stateLayout.length - 1; k >= 0
				&& i >= 0; k--) {
			if (stateLayout[k] == -1) {
				continue;
			}
			if (ignore.contains(i)) {
				i--;
				continue;
			}
			values.setQuick(i, values.getQuick(i) + 1.0);
			if (values.getQuick(i) < stateLayout[k]) {
				return true;
			}
			values.setQuick(i--, 0);
		}
		return false;
	}

	/**
	 * increments the values array in accordance with the stateLayout specified.
	 * Returns true if the index was successfully updated and false if the index
	 * rolled over to the 0 state
	 * 
	 * @param values
	 * @param stateLayout
	 * @return
	 */
	static public boolean incrementIndice(DoubleMatrix1D values,
			int[] stateLayout) {
		List<Integer> l = Collections.emptyList();
		return incrementIndice(values, stateLayout, l);
	}

	/**
	 * increments the values array in accordance with the stateLayout specified.
	 * Returns true if the index was successfully updated and false if the index
	 * rolled over to the 0 state
	 * 
	 * @param values
	 * @param stateLayout
	 * @param ignore
	 *            set of indices to ignore while incrementing
	 * @return
	 */
	static public boolean incrementIndice(int[] values, int[] stateLayout,
			List<Integer> ignore) {

		for (int i = values.length - 1, k = stateLayout.length - 1; k >= 0
				&& i >= 0; k--) {
			if (stateLayout[k] == -1) {
				continue;
			}
			if (ignore.contains(i)) {
				i--;
				continue;
			}
			values[i]++;
			if (values[i] < stateLayout[i]) {
				return true;
			}
			values[i--] = 0;
		}
		return false;
	}

	/**
	 * increments the values array in accordance with the stateLayout specified.
	 * Returns true if the index was successfully updated and false if the index
	 * rolled over to the 0 state
	 * 
	 * @param values
	 * @param stateLayout
	 * @return
	 */
	static public boolean incrementIndice(int[] values, int[] stateLayout) {
		List<Integer> l = Collections.emptyList();
		return incrementIndice(values, stateLayout, l);
	}

	/**
	 * Uses the mapping set up in indices to set up the proper multi-state index
	 * corresponding to overlapping variables. With two disjoint distributions,
	 * we need to make sure our mappings get laid out properly otherwise we will
	 * be processing the incorrect parts of the distribution. For example, when
	 * multiplying two distributions to get a third, we may have distribution A
	 * mapping to variables c,a and distribution B mapping to variables a,b,e,f.
	 * When iterating through the new distribution R, we know, for the case of
	 * multiplication, that R will contain a,b,c,e,f. We need to map accordingly
	 * so that when we increment an index, we increment the proper dimension.
	 * 
	 * @param stateLayout
	 *            This is the layout of the r matrix index, generally the layout
	 *            of the distribution we are mapping to
	 * @param r
	 *            This will become the index mapping to a particular
	 *            distribution layout
	 * @param a
	 * @param b
	 * @param indices
	 */
	static public void setupIndices(int[] stateLayout, DoubleMatrix1D r,
			DoubleMatrix1D a, DoubleMatrix1D b, List<Quadruple> indices) {
		for (int i = 0; i < stateLayout.length; i++) {
			Quadruple t = indices.get(i);
			if (t.a != -1) {
				a.setQuick(t.a, r.getQuick(t.r));
			}
			if (t.b != -1) {
				b.setQuick(t.b, r.getQuick(t.r));
			}
		}
	}

}
