/*
 * Copyright (c) CQSE GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teamscale.report.util

import java.util.*

/**
 * A compact, serializable representation of line numbers using a BitSet. This class is designed to
 * efficiently store and manipulate sets of line numbers, which is particularly useful for tracking
 * coverage information, regions of a text, or any scenario where line-based data needs to be
 * compactly managed.
 *
 * Instances of this class can be created empty, from a collection of integers, or from a string
 * representation of line number ranges. It supports basic set operations such as addition, removal,
 * intersection, and union, as well as specialized operations like checking if any line number
 * within a range or specific region is present.
 *
 * This class also implements [Iterable], allowing for easy iteration over all stored line
 * numbers.
 *
 * @see BitSet
 */
data class CompactLines @JvmOverloads constructor(val bitSet: BitSet = BitSet()) : Iterable<Int> {

	companion object {
		fun compactLinesOf(vararg lines: Int) = CompactLines(*lines)
		fun compactLinesOf(lines: Iterable<Int>) = CompactLines(lines)
		fun compactLinesCopyOf(lines: CompactLines): CompactLines {
			return CompactLines().apply {
				this merge lines
			}
		}
		fun compactLinesOf() = CompactLines()
	}

	constructor(lines: Iterable<Int>) : this() {
		lines.forEach { line ->
			bitSet.set(line)
		}
	}

	constructor(vararg lines: Int) : this() {
		lines.forEach { line ->
			bitSet.set(line)
		}
	}

	/** Returns the number of line numbers in this set. */
	val size
		get() = bitSet.cardinality()

	/**
	 * Checks if this set of line numbers is empty.
	 *
	 * @return `true` if there are no line numbers in this set, `false` otherwise.
	 */
	val isEmpty: Boolean
		get() = bitSet.isEmpty

	/**
	 * Adds all line numbers from another [CompactLines] instance to this one.
	 */
	infix fun merge(lines: CompactLines) {
		bitSet.or(lines.bitSet)
	}

	/**
	 * Checks if a specific line number is present in this set.
	 *
	 * @param line The line number (1-based)
	 * @return `true` if the line number is present, `false` otherwise.
	 */
	fun contains(line: Int) = bitSet.get(line)

	/**
	 * Checks if any line number within a specified range is present in this set.
	 *
	 * @param start the start of the range (inclusive, 1-based).
	 * @param end the end of the range (inclusive, 1-based).
	 * @return `true` if any line number within the range is present, `false` otherwise.
	 */
	fun containsAny(start: Int, end: Int): Boolean {
		val nextSetBit = bitSet.nextSetBit(start)
		return nextSetBit != -1 && nextSetBit <= end
	}

	/**
	 * Checks if this set contains all the line numbers specified in an iterable collection.
	 *
	 * @return `true` if every line number in the collection is contained in this set,
	 *         `false` otherwise.
	 */
	fun containsAll(lines: Iterable<Int>) =
		lines.all { line -> bitSet[line] }

	/**
	 * Adds a specific line number to this set.
	 *
	 * @param line The line number (1-based)
	 */
	fun add(line: Int) {
		bitSet.set(line)
	}

	/**
	 * Adds a range of line numbers to this set.
	 *
	 * @param startLine the starting line number of the range to add (inclusive, 1-based)
	 * @param endLine the ending line number of the range to add (inclusive, 1-based)
	 */
	fun addRange(startLine: Int, endLine: Int) {
		bitSet.set(startLine, endLine + 1)
	}

	/** Removes a specific line number from this set. */
	fun remove(line: Int) {
		bitSet.clear(line)
	}

	/**
	 * Removes all line numbers that are present in another [CompactLines] instance from this one.
	 */
	fun removeAll(lines: CompactLines) {
		bitSet.andNot(lines.bitSet)
	}

	/** Clears all line numbers from this set. */
	fun clear() {
		bitSet.clear()
	}

	/**
	 * Retains only the line numbers that are present in both this and another [CompactLines]
	 * instance. This basically builds the intersection set between both.
	 */
	fun retainAll(lines: CompactLines) {
		bitSet.and(lines.bitSet)
	}

	/**
	 * Creates a new [CompactLines] object with the intersection of this and the other lines.
	 */
	fun intersection(other: CompactLines) =
		compactLinesCopyOf(this).apply {
			retainAll(other)
		}

	/**
	 * Checks if there is any overlap between the line numbers in this and another [CompactLines]
	 * instance.
	 *
	 * @return `true` if there is at least one common line number, `false` otherwise.
	 */
	fun intersects(lines: CompactLines) =
		bitSet.intersects(lines.bitSet)

	override fun toString() = joinToString(",")

	override fun iterator(): Iterator<Int> {
		return object : Iterator<Int> {
			private var currentIndex = -1

			override fun hasNext(): Boolean {
				val nextIndex = bitSet.nextSetBit(currentIndex + 1)
				return nextIndex != -1
			}

			override fun next(): Int {
				if (!hasNext()) {
					throw NoSuchElementException()
				}
				currentIndex = bitSet.nextSetBit(currentIndex + 1)
				return currentIndex
			}
		}
	}
}
