package com.teamscale.report.util

/**
 * Performant implementation of a deduplicated sorted integer list that assumes that insertions mainly happen at the end
 * and that input is already sorted.
 */
class SortedIntList {
	/**
	 * The list of values in sorted order and without duplicates. The list might be bigger than the number of elements.
	 */
	@JvmField
	var list: IntArray

	/** The number of actual elements in the list.  */
	private var count = 0

	init {
		list = IntArray(64)
	}

	/** Adds the given value to the list at the correct location, ignoring duplicates.  */
	fun add(value: Int): Boolean {
		var high = count
		var low = 0

		if (isEmpty) {
			list[0] = value
			count = 1
			return true
		}

		// Perform binary search to find target location
		do {
			val p = (low + high) ushr 1
			if (value < list[p]) {
				high = p
			} else if (value == list[p]) {
				// Element already exists in the list
				return false
			} else {
				low = p + 1
			}
		} while (low < high)

		if (count == list.size) {
			val n = IntArray(list.size * 2)
			System.arraycopy(list, 0, n, 0, count)
			list = n
		}

		if (low < count) {
			System.arraycopy(list, low, list, low + 1, count - low)
		}
		list[low] = value
		count++
		return true
	}

	/** Inserts all values from the given list, ignoring duplicates.  */
	fun addAll(input: SortedIntList) {
		for (i in 0 until input.size()) {
			add(input.get(i))
		}
	}

	/** Returns the size of the list.  */
	fun size(): Int {
		return count
	}

	val isEmpty: Boolean
		/** Returns whether the list is empty.  */
		get() = count == 0

	/** Returns the i-th element of the list.  */
	operator fun get(i: Int): Int {
		return list[i]
	}
}