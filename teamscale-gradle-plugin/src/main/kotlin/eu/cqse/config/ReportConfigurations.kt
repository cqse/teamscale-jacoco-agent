package eu.cqse.config

import eu.cqse.Report
import eu.cqse.teamscale.client.EReportFormat
import eu.cqse.teamscale.client.EReportFormat.JUNIT
import eu.cqse.teamscale.client.EReportFormat.TESTWISE_COVERAGE
import eu.cqse.teamscale.report.util.AntPatternIncludeFilter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.io.Serializable

/** Base class for report configurations that should be generated and uploaded. */
open class ReportConfigurationBase(

    /** The report format. */
    var format: EReportFormat

) : Serializable {

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

    /** @see #destination */
    fun setDestination(destination: String) {
        this.destination = File(destination)
    }

    /** Returns the destination set for the report or the default destination if not set. */
    open fun getDestinationOrDefault(project: Project, gradleTestTask: Task): File {
        return destination ?: project.file(
            "${project.buildDir}/reports/${format.name.toLowerCase()}/" +
                    "${format.name.toLowerCase()}-${project.name}-${gradleTestTask.name}.${format.extension}"
        )
    }

    /** Creates a copy of the report configuration, setting all non-set values to their default value. */
    open fun copyWithDefault(toCopy: ReportConfigurationBase, default: ReportConfigurationBase) {
        destination = toCopy.destination ?: default.destination
        message = toCopy.message ?: default.message
        partition = toCopy.partition ?: default.partition
        upload = toCopy.upload ?: default.upload
    }

    /** Takes the partition base name and a report format and merges it into a partition name. */
    open fun getTransformedPartition(project: Project): String {
        return "$partition/${project.name}${format.partitionSuffix}"
    }

    /** Returns a report specification used in the TeamscaleUploadTask. */
    fun getReport(project: Project, gradleTestTask: Test): Report {
        return Report(
            format = format,
            report = getDestinationOrDefault(project, gradleTestTask),
            message = message ?: "${format.readableName} gradle upload",
            partition = getTransformedPartition(project)
        )
    }

    /** Returns true if all required fields are set. */
    fun validate(project: Project, testTaskName: String): Boolean {
        if (upload == true && partition == null) {
            project.logger.debug("No partition set for ${format.readableName} upload of ${project.name}:$testTaskName!")
            return false
        }
        return true
    }
}

/** Configuration for the testwise coverage report. */
class TestwiseCoverageConfiguration : ReportConfigurationBase(TESTWISE_COVERAGE)

/** Configuration for the jUnit report. */
class JUnitReportConfiguration : ReportConfigurationBase(JUNIT) {

    /** @inheritDoc */
    override fun getDestinationOrDefault(project: Project, gradleTestTask: Task): File {
        return destination ?: project.file("${project.buildDir}/reports/junit/${gradleTestTask.name}")
    }

    /** Takes the partition base name and a report format and merges it into a partition name. */
    override fun getTransformedPartition(project: Project): String {
        return "$partition"
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
    fun getFilter() = AntPatternIncludeFilter(
        includes ?: emptyList(),
        excludes ?: emptyList()
    )

    /** Creates a copy of the report configuration, setting all non-set values to their default value. */
    fun copyWithDefault(toCopy: GoogleClosureConfiguration, default: GoogleClosureConfiguration) {
        destination = toCopy.destination ?: default.destination
        excludes = toCopy.excludes ?: default.excludes
        includes = toCopy.includes ?: default.includes
    }
}
