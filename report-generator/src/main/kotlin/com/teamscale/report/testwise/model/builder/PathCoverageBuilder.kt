package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.PathCoverage

/** Container for [FileCoverageBuilder]s of the same path.
 *
 * @param path File system path.
 */
class PathCoverageBuilder(
	val path: String
) {
	/** Mapping from file names to [FileCoverageBuilder].  */
	private val fileCoverageList = mutableMapOf<String, FileCoverageBuilder>()

	/**
	 * Adds the given [FileCoverageBuilder] to the container.
	 * If coverage for the same file already exists, it gets merged.
	 */
	fun add(fileCoverage: FileCoverageBuilder) {
		fileCoverageList.merge(fileCoverage.fileName, fileCoverage) { existing, new ->
			existing.apply { merge(new) }
		}
	}

	/** Returns a collection of [FileCoverageBuilder]s associated with this path.  */
	val files: Collection<FileCoverageBuilder>
		get() = fileCoverageList.values

	/** Builds a [PathCoverage] object.  */
	fun build() =
		PathCoverage(
			path,
			fileCoverageList.values
				.sortedBy { it.fileName }
				.map { it.build() }
		)
}