package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.PathCoverage
import java.util.function.Function
import java.util.stream.Collectors

/** Container for [FileCoverageBuilder]s of the same path.  */
class PathCoverageBuilder
/** Constructor.  */(
	/** File system path.  */
	val path: String
) {
	/** @see .path
	 */

	/** Mapping from file names to [FileCoverageBuilder].  */
	private val fileCoverageList: MutableMap<String?, FileCoverageBuilder?> = HashMap()

	/**
	 * Adds the given [FileCoverageBuilder] to the container.
	 * If coverage for the same file already exists it gets merged.
	 */
	fun add(fileCoverage: FileCoverageBuilder) {
		if (fileCoverageList.containsKey(fileCoverage.fileName)) {
			val existingFile = fileCoverageList[fileCoverage.fileName]
			existingFile!!.merge(fileCoverage)
		} else {
			fileCoverageList[fileCoverage.fileName] = fileCoverage
		}
	}

	val files: Collection<FileCoverageBuilder?>
		/** Returns a collection of [FileCoverageBuilder]s associated with this path.  */
		get() = fileCoverageList.values

	/** Builds a [PathCoverage] object.  */
	fun build(): PathCoverage {
		val files = fileCoverageList.values.stream()
			.sorted(
				Comparator.comparing(
					Function { obj: FileCoverageBuilder? -> obj!!.fileName })
			)
			.map { obj: FileCoverageBuilder? -> obj!!.build() }.collect(Collectors.toList())
		return PathCoverage(path, files)
	}
}
