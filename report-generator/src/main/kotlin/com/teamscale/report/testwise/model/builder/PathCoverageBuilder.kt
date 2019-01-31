package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.PathCoverage
import java.util.*

/** Container for [FileCoverageBuilder]s of the same path.  */
class PathCoverageBuilder
/** Constructor.  */
    (
    /** File system path.  */
    /** @see .path
     */
    val path: String
) {

    /** Mapping from file names to [FileCoverageBuilder].  */
    private val fileCoverageList = HashMap<String, FileCoverageBuilder>()

    /** Returns a collection of [FileCoverageBuilder]s associated with this path.  */
    val files: Collection<FileCoverageBuilder>
        get() = fileCoverageList.values

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

    /** Builds a [PathCoverage] object.  */
    fun build(): PathCoverage {
        val files = fileCoverageList.values
            .sortedBy { it.fileName }
            .map { it.build() }
        return PathCoverage(path, files)
    }
}
