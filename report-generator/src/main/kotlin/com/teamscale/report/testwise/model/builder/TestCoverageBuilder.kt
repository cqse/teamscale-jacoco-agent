package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.PathCoverage

/**
 * Generic holder of test coverage of a single test based on line-ranges.
 * @param uniformPath The uniformPath of the test (see TEST_IMPACT_ANALYSIS_DOC.md for more information).
 */
class TestCoverageBuilder(val uniformPath: String) {
	/** Mapping from path names to all files on this path.  */
	private val pathCoverageList = mutableMapOf<String, PathCoverageBuilder>()

	/** Collection of [PathCoverageBuilder]s associated with the test.  */
	val paths: List<PathCoverage>
		get() = pathCoverageList.values
			.sortedBy { it.path }
			.map { it.build() }

	/** Adds the [FileCoverageBuilder] to into the map, but filters out file coverage that is null or empty.  */
	fun add(fileCoverage: FileCoverageBuilder) {
		fileCoverage.takeIf { !it.isEmpty }?.let { coverage ->
			pathCoverageList.computeIfAbsent(coverage.path) { PathCoverageBuilder(it) }
				.add(coverage)
		}
	}

	/** Adds the [FileCoverageBuilder]s into the map, but filters out empty ones.  */
	fun addAll(fileCoverageList: List<FileCoverageBuilder>) {
		fileCoverageList.forEach { add(it) }
	}

	/** Returns all [FileCoverageBuilder]s stored for the test.  */
	val files: List<FileCoverageBuilder>
		get() = pathCoverageList.values
			.flatMap { it.files }
			.toList()

	/** Returns true if there is no coverage for the test yet.  */
	val isEmpty: Boolean
		get() = pathCoverageList.isEmpty()
}