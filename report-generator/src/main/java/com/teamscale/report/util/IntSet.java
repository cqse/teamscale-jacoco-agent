package com.teamscale.report.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IntSet {

	private int[] set;

	private int count;

	public IntSet() {
		set = new int[64];
	}

	public boolean add(int key) {
		int high = count;
		int low = 0;

		if (high == 0) {
			set[0] = key;
			count = 1;
			return true;
		}

		do {
			int p = (low + high) >>> 1;
			if (key < set[p])
				high = p;
			else if (key == set[p])
				return false;
			else
				low = p + 1;
		} while (low < high);

		if (count == set.length) {
			int[] n = new int[set.length * 2];
			System.arraycopy(set, 0, n, 0, count);
			set = n;
		}

		if (low < count)
			System.arraycopy(set, low, set, low + 1, count - low);
		set[low] = key;
		count++;
		return true;
	}

	public int size() {
		return count;
	}

	public List<Integer> getSortedList() {
		ArrayList<Integer> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			result.add(set[i]);
		}
		return result;
	}

	public void addAll(Set<Integer> input) {
		for (Integer integer : input) {
			add(integer);
		}
	}
}