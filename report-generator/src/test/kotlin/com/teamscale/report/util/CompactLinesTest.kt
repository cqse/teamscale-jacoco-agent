package com.teamscale.report.util

import com.teamscale.report.util.CompactLines.Companion.compactLinesOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CompactLinesTest {
	@Test
	fun emptyList() {
		val compactLines = compactLinesOf()
		assertThat(compactLines.isEmpty).isTrue()
	}

	@Test
	fun testContains() {
		val lines = compactLinesOf(5)
		assertThat(lines.contains(5)).isTrue()
		assertThat(lines.contains(4)).isFalse()
	}

	@Test
	fun testAddAndRemove() {
		val lines = compactLinesOf()
		lines.add(10)
		assertThat(lines.contains(10)).isTrue()
		lines.remove(10)
		assertThat(lines.contains(10)).isFalse()
	}

	@Test
	fun testSize() {
		val lines = compactLinesOf(1, 2)
		assertThat(lines.size).isEqualTo(2)
	}

	@Test
	fun testIsEmpty() {
		val lines = compactLinesOf()
		assertThat(lines.isEmpty).isTrue()
		lines.add(1)
		assertThat(lines.isEmpty).isFalse()
	}

	@Test
	fun testMerging() {
		val lines1 = compactLinesOf(1, 2)
		val lines2 = compactLinesOf()
		lines2 merge lines1

		assertThat(lines2.contains(1)).isTrue()
		assertThat(lines2.contains(2)).isTrue()
	}

	@Test
	fun testRemoveAll() {
		val lines1 = compactLinesOf(1, 2)
		val lines2 = compactLinesOf(1, 2, 3)
		lines2.removeAll(lines1)

		assertThat(lines2.contains(1)).isFalse()
		assertThat(lines2.contains(2)).isFalse()
		assertThat(lines2.contains(3)).isTrue()
	}

	@Test
	fun testIntersects() {
		val lines1 = compactLinesOf(1, 2)
		val lines2 = compactLinesOf(2, 3)

		assertThat(lines1.intersects(lines2)).isTrue()

		lines2.remove(2)
		assertThat(lines1.intersects(lines2)).isFalse()
	}

	@Test
	fun testContainsAny() {
		val lines = compactLinesOf(5, 10)

		assertThat(lines.containsAny(3, 4)).isFalse()
		assertThat(lines.containsAny(3, 5)).isTrue()
		assertThat(lines.containsAny(4, 6)).isTrue()
		assertThat(lines.containsAny(10, 15)).isTrue()
		assertThat(lines.containsAny(11, 15)).isFalse()
	}

	@Test
	fun testAddRange() {
		val lines = compactLinesOf()
		lines.addRange(5, 7)
		assertThat(lines).containsExactly(5, 6, 7)
	}

	@Test
	fun testContainsAllTrue() {
		val lines = compactLinesOf(1, 3)
		assertThat(lines.containsAll(listOf(1, 2, 3))).isFalse()
		assertThat(lines.containsAll(listOf(1, 3))).isTrue()
		assertThat(lines.containsAll(compactLinesOf(1, 2, 3))).isFalse()
		assertThat(lines.containsAll(compactLinesOf(1, 3))).isTrue()
	}
}
