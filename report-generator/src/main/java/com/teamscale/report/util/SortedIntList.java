package com.teamscale.report.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Performant implementation of a deduplicated sorted integer list that assumes that insertions mainly happen at the end
 * and that input is already sorted.
 */
public class SortedIntList {

	protected int[] list;

	private int count;

	public SortedIntList() {
		list = new int[64];
	}

	public boolean add(int key) {
		int high = count;
		int low = 0;

		if (isEmpty()) {
			list[0] = key;
			count = 1;
			return true;
		}

		// Perform binary search to find
		do {
			int p = (low + high) >>> 1;
			if (key < list[p]) {
				high = p;
			} else if (key == list[p]) {
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

		if (low < count)
			System.arraycopy(list, low, list, low + 1, count - low);
		list[low] = key;
		count++;
		return true;
	}

	public int size() {
		return count;
	}

	public List<Integer> getSortedList() {
		ArrayList<Integer> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			result.add(list[i]);
		}
		return result;
	}

	public void addAll(SortedIntList input) {
		int[] a = list;
		int aSize = count;
		int[] b = input.list;
		int bSize = input.count;
		list = new int[count + input.count];
		int aIndex = 0;
		int bIndex = 0;
		int index = 0;
		while (aIndex < aSize && bIndex < bSize) {
			if (a[aIndex] < b[bIndex]) {
				list[index++] = a[aIndex++];
			} else if (a[aIndex] == b[bIndex]) {
				list[index++] = a[aIndex++];
				bIndex++;
			} else {
				list[index++] = b[bIndex++];
			}
		}
		while (aIndex < aSize) {
			list[index++] = a[aIndex++];
		}
		while (bIndex < bSize) {
			list[index++] = b[bIndex++];
		}
		count = index;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public int get(int i) {
		return list[i];
	}
}