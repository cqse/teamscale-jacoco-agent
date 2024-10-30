package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.PathCoverage
import java.util.function.Function
import java.util.stream.Collectors

/** Generic holder of test coverage of a single test based on line-ranges.  */
class TestCoverageBuilder
/** Constructor.  */(
	/** The uniformPath of the test (see TEST_IMPACT_ANALYSIS_DOC.md for more information).  */
	@JvmField val uniformPath: String
) {
	/** @see .uniformPath
	 */

	/** Mapping from path names to all files on this path.  */
	private val pathCoverageList: MutableMap<String, PathCoverageBuilder> = HashMap()

	val paths: List<PathCoverage>
		/** Returns a collection of [PathCoverageBuilder]s associated with the test.  */
		get() = pathCoverageList.values.stream().sorted(
			Comparator.comparing { obj -> obj.path }
		).map { obj: PathCoverageBuilder -> obj.build() }.collect(Collectors.toList())

	/** Adds the [FileCoverageBuilder] to into the map, but filters out file coverage that is null or empty.  */
	fun add(fileCoverage: FileCoverageBuilder?) {
		if (fileCoverage == null || fileCoverage.isEmpty) {
			return
		}
		val pathCoverage = pathCoverageList.computeIfAbsent(fileCoverage.path) { path -> PathCoverageBuilder(path) }
		pathCoverage.add(fileCoverage)
	}

	/** Adds the [FileCoverageBuilder]s into the map, but filters out empty ones.  */
	fun addAll(fileCoverageList: List<FileCoverageBuilder?>) {
		for (fileCoverage: FileCoverageBuilder? in fileCoverageList) {
			add(fileCoverage)
		}
	}

	val files: List<FileCoverageBuilder?>
		/** Returns all [FileCoverageBuilder]s stored for the test.  */
		get() {
			return pathCoverageList.values.stream()
				.flatMap { path: PathCoverageBuilder -> path.files.stream() }
				.collect(Collectors.toList())
		}

	val isEmpty: Boolean
		/** Returns true if there is no coverage for the test yet.  */
		get() {
			return pathCoverageList.isEmpty()
		}
}
