package eu.cqse.config

import eu.cqse.Report
import eu.cqse.teamscale.client.EReportFormat
import eu.cqse.teamscale.client.EReportFormat.*
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

    /** The partition */
    var partition: String? = null
    var message: String? = null
    var destination: File? = null

    /**
     * Whether the report should be uploaded or not.
     * If it is null the report should not even be created.
     */
    var upload: Boolean? = null

    fun setDestination(destination: String) {
        this.destination = File(destination)
    }

    open fun getDestinationOrDefault(project: Project, gradleTestTask: Task): File {
        return destination ?: project.file(
            "${project.buildDir}/reports/${format.name.toLowerCase()}/" +
                    (format.name.toLowerCase() + "-" + project.name + "-" + gradleTestTask.name + "." + format.extension)
        )
    }

    open fun copyWithDefault(toCopy: ReportConfigurationBase, default: ReportConfigurationBase) {
        destination = toCopy.destination ?: default.destination
        message = toCopy.message ?: default.message
        partition = toCopy.partition ?: default.partition
        upload = toCopy.upload ?: default.upload
    }

    /** Takes the partition base name and a report format and merges it into a partition name. */
    fun getTransformedPartition(project: Project): String {
        return "$partition/${project.name}${format.partitionSuffix}"
    }

    fun getReport(project: Project, gradleTestTask: Test): Report {
        return Report(
            format = format,
            report = getDestinationOrDefault(project, gradleTestTask),
            message = message ?: "${format.readableName} gradle upload",
            partition = getTransformedPartition(project)
        )
    }
}

class TestwiseCoverageConfiguration : ReportConfigurationBase(TESTWISE_COVERAGE)

class JacocoReportConfiguration : ReportConfigurationBase(JACOCO)

class JUnitReportConfiguration : ReportConfigurationBase(JUNIT) {

    override fun getDestinationOrDefault(project: Project, gradleTestTask: Task): File {
        return destination ?: project.file("${project.buildDir}/reports/junit/${gradleTestTask.name}")
    }

}

class ClosureConfiguration : Serializable {

    var destination: Set<File>? = null
    var excludes: List<String>? = null
    var includes: List<String>? = null

    fun setDestination(files: FileCollection) {
        destination = files.files
    }

    fun getFilter() = AntPatternIncludeFilter(
        includes ?: emptyList(),
        excludes ?: emptyList()
    )

    fun copyWithDefault(toCopy: ClosureConfiguration, default: ClosureConfiguration) {
        destination = toCopy.destination ?: default.destination
        excludes = toCopy.excludes ?: default.excludes
        includes = toCopy.includes ?: default.includes
    }
}
