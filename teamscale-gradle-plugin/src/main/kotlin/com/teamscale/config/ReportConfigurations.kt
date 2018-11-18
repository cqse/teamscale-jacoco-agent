package com.teamscale.config

import com.teamscale.Report
import com.teamscale.client.EReportFormat
import com.teamscale.report.util.AntPatternIncludeFilter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.io.Serializable

/** Configuration for the testwise coverage report. */
open class TestwiseCoverageConfiguration : Serializable {

    /** The partition for which artifacts are uploaded. */
    var partition: String? = null

    /** The message that shows up for the upload in Teamscale. */
    var message: String? = null

    /** The destination where the report should be written to. */
    var destination: File? = null

    /**
     * Whether the report should be uploaded or not.
     * If it is null the report should not even be created.
     */
    var upload: Boolean? = null

    /** Transformer that  */
    var partitionTransformer: Transformer<String, Project>? = null

    /** Convenience method for setting the transformer to append the project name to the partition. */
    fun uploadPerModule() {
        partitionTransformer = Transformer { project -> "$partition/${project.name}" }
    }

    /** @see #destination */
    fun setDestination(destination: String) {
        this.destination = File(destination)
    }

    /** Returns the destination set for the report or the default destination if not set. */
    open fun getDestinationOrDefault(project: Project, gradleTestTask: Task): File {
        return destination ?: project.file(
            "${project.buildDir}/reports/${EReportFormat.TESTWISE_COVERAGE.name.toLowerCase()}/" +
                    "${EReportFormat.TESTWISE_COVERAGE.name.toLowerCase()}-${project.name}-${gradleTestTask.name}.json"
        )
    }

    /** Returns the destination set for the report or the default destination if not set. */
    open fun getTempDestination(project: Project, gradleTestTask: Task): File {
        return project.file(
            "${project.buildDir}/tmp/${EReportFormat.TESTWISE_COVERAGE.name.toLowerCase()}/" +
                    "${EReportFormat.TESTWISE_COVERAGE.name.toLowerCase()}-${project.name}-${gradleTestTask.name}-${EReportFormat.TESTWISE_COVERAGE.name}"
        )
    }

    /** Creates a copy of the report configuration, setting all non-set values to their default value. */
    open fun copyWithDefault(toCopy: TestwiseCoverageConfiguration, default: TestwiseCoverageConfiguration) {
        destination = toCopy.destination ?: default.destination
        message = toCopy.message ?: default.message
        partition = toCopy.partition ?: default.partition
        upload = toCopy.upload ?: default.upload
        partitionTransformer = toCopy.partitionTransformer ?: default.partitionTransformer ?:
                Transformer { "$partition" }
    }

    /** Takes the partition base name and a report format and merges it into a partition name. */
    open fun getTransformedPartition(project: Project): String {
        return partitionTransformer?.transform(project) ?: partition
        ?: throw IllegalArgumentException("No partition set for ${project.name}")
    }

    /** Returns a report specification used in the TeamscaleUploadTask. */
    fun getReport(project: Project, gradleTestTask: Test): Report {
        return Report(
            format = EReportFormat.TESTWISE_COVERAGE,
            report = getDestinationOrDefault(project, gradleTestTask),
            message = message ?: "${EReportFormat.TESTWISE_COVERAGE.readableName} gradle upload",
            partition = getTransformedPartition(project)
        )
    }

    /** Returns true if all required fields are set. */
    fun validate(project: Project, testTaskName: String): Boolean {
        if (upload == true && partition == null) {
            project.logger.info("No partition set for ${EReportFormat.TESTWISE_COVERAGE.readableName} upload of ${project.name}:$testTaskName!")
            return false
        }
        return true
    }
}

/** Configuration for the google closure coverage. */
class GoogleClosureConfiguration : Serializable {

    /**
     * The directories in which the reports can be found after the tests.
     * This plugin does not generate them, but only collects them afterwards.
     */
    var destination: Set<File>? = null

    /** Ant include patterns for js files that should be contained in the testwise coverage report. */
    var includes: List<String>? = null

    /** Ant exclude patterns for js files that should not be contained in the testwise coverage report. */
    var excludes: List<String>? = null

    /** @see #destination */
    fun setDestination(files: FileCollection) {
        destination = files.files
    }

    /** Returns a predicate with the include/exclude patterns. */
    fun getFilter() = FileNameFilter(
        includes ?: emptyList(),
        excludes ?: emptyList()
    )

    /** Creates a copy of the report configuration, setting all non-set values to their default value. */
    fun copyWithDefault(toCopy: GoogleClosureConfiguration, default: GoogleClosureConfiguration) {
        destination = toCopy.destination ?: default.destination
        excludes = toCopy.excludes ?: default.excludes
        includes = toCopy.includes ?: default.includes
    }

    class FileNameFilter(private val includes: List<String>, private val excludes: List<String>) : Serializable {

        /** Returns a predicate with the include/exclude patterns. */
        fun getPredicate() = AntPatternIncludeFilter(includes, excludes)
    }
}
