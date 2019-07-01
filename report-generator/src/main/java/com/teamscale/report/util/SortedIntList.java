package com.teamscale.report.util;

/**
 * Performant implementation of a deduplicated sorted integer list that assumes that insertions mainly happen at the end
 * and that input is already sorted.
 */
public class SortedIntList {

	/**
	 * The list of values in sorted order and without duplicates. The list might be bigger than the number of elements.
	 */
	protected int[] list;

	/** The number of actual elements in the list. */
	private int count;

	public SortedIntList() {
		list = new int[64];
	}

	/** Adds the given value to the list at the correct location, ignoring duplicates. */
	public boolean add(int value) {
		int high = count;
		int low = 0;

		if (isEmpty()) {
			list[0] = value;
			count = 1;
			return true;
		}

		// Perform binary search to find target location
		do {
			int p = (low + high) >>> 1;
			if (value < list[p]) {
				high = p;
			} else if (value == list[p]) {
				// Element already exists in the list
				return false;
			} else {
				low = p + 1;
			}
		} while (low < high);

		if (count == list.length) {
			int[] n = new int[list.length * 2];
			System.arraycopy(list, 0, n, 0, count);
			list = n;
		}

		if (low < count) {
			System.arraycopy(list, low, list, low + 1, count - low);
		}
		list[low] = value;
		count++;
		return true;
	}

	/** Inserts all values from the given list, ignoring duplicates. */
	public void addAll(SortedIntList input) {
		for (int i = 0; i < input.size(); i++) {
			add(input.get(i));
		}
	}

	/** Returns the size of the list. */
	public int size() {
		return count;
	}

	/** Returns whether the list is empty. */
	public boolean isEmpty() {
		return count == 0;
	}

	/** Returns the i-th element of the list. */
	public int get(int i) {
		return list[i];
	}
}