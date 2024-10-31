package com.teamscale.report.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SortedIntListTest {
	@Test
	fun emptyList() {
		val sortedIntList = sortedSetOf<Int>()
		assertThat(sortedIntList.isEmpty()).isTrue()
	}

	@Test
	fun addSorted() {
		val sortedIntList = sortedSetOf(1, 3, 4, 7, 10)
		assertThat(sortedIntList).startsWith(1, 3, 4, 7, 10)
		assertThat(sortedIntList.size).isEqualTo(5)
	}

	@Test
	fun addReversed() {
		val sortedIntList = sortedSetOf(6, 5, 2, 0)
		assertThat(sortedIntList).startsWith(0, 2, 5, 6)
		assertThat(sortedIntList.size).isEqualTo(4)
	}

	@Test
	fun add() {
		val sortedIntList = sortedSetOf(7, 4, 9, 11, 1)
		assertThat(sortedIntList).startsWith(1, 4, 7, 9, 11)
		assertThat(sortedIntList.size).isEqualTo(5)
	}

	@Test
	fun mergeIntoEmptyList() {
		val sortedIntList = sortedSetOf<Int>()
		sortedIntList.addAll(listOf(1, 2, 5, 8, 9))
		assertThat(sortedIntList).startsWith(1, 2, 5, 8, 9)
		assertThat(sortedIntList.size).isEqualTo(5)
	}

	@Test
	fun mergeWithEmptyList() {
		val sortedIntList = sortedSetOf(1, 2, 5, 8, 9)
		sortedIntList.addAll(listOf())
		assertThat(sortedIntList).startsWith(1, 2, 5, 8, 9)
		assertThat(sortedIntList.size).isEqualTo(5)
	}

	@Test
	fun mergeWithOverlap() {
		val sortedIntList = sortedSetOf(1, 2, 5, 8, 9)
		sortedIntList.addAll(sortedSetOf(3, 4, 5))
		assertThat(sortedIntList).startsWith(1, 2, 3, 4, 5, 8, 9)
		assertThat(sortedIntList.size).isEqualTo(7)
	}
}