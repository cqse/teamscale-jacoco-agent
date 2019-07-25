package com.teamscale.report.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortedIntListTest {

	@Test
	void emptyList() {
		SortedIntList sortedIntList = new SortedIntList();
		assertThat(sortedIntList.isEmpty()).isTrue();
	}

	@Test
	void addSorted() {
		SortedIntList sortedIntList = listOf(1, 3, 4, 7, 10);
		assertThat(sortedIntList.list).startsWith(1, 3, 4, 7, 10);
		assertThat(sortedIntList.size()).isEqualTo(5);
	}

	@Test
	void addReversed() {
		SortedIntList sortedIntList = listOf(6, 5, 2, 0);
		assertThat(sortedIntList.list).startsWith(0, 2, 5, 6);
		assertThat(sortedIntList.size()).isEqualTo(4);
	}

	@Test
	void add() {
		SortedIntList sortedIntList = listOf(7, 4, 9, 11, 1);
		assertThat(sortedIntList.list).startsWith(1, 4, 7, 9, 11);
		assertThat(sortedIntList.size()).isEqualTo(5);
	}

	@Test
	void mergeIntoEmptyList() {
		SortedIntList sortedIntList = listOf();
		sortedIntList.addAll(listOf(1, 2, 5, 8, 9));
		assertThat(sortedIntList.list).startsWith(1, 2, 5, 8, 9);
		assertThat(sortedIntList.size()).isEqualTo(5);
	}

	@Test
	void mergeWithEmptyList() {
		SortedIntList sortedIntList = listOf(1, 2, 5, 8, 9);
		sortedIntList.addAll(listOf());
		assertThat(sortedIntList.list).startsWith(1, 2, 5, 8, 9);
		assertThat(sortedIntList.size()).isEqualTo(5);
	}

	@Test
	void mergeWithOverlap() {
		SortedIntList sortedIntList = listOf(1, 2, 5, 8, 9);
		sortedIntList.addAll(listOf(3, 4, 5));
		assertThat(sortedIntList.list).startsWith(1, 2, 3, 4, 5, 8, 9);
		assertThat(sortedIntList.size()).isEqualTo(7);
	}

	private SortedIntList listOf(int... values) {
		SortedIntList sortedIntList = new SortedIntList();
		for (int value : values) {
			sortedIntList.add(value);
		}
		return sortedIntList;
	}
}