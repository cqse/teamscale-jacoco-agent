package com.teamscale

import com.teamscale.client.CommitDescriptor
import com.teamscale.client.EReportFormat
import com.teamscale.client.TeamscaleClient
import com.teamscale.config.ServerConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import java.io.IOException

/** Handles report uploads to Teamscale. */
@Suppress("MemberVisibilityCanBePrivate")
abstract class TeamscaleUpload : DefaultTask() {

	/** The Teamscale server configuration. */
	@get:Input
	abstract val serverConfiguration: Property<ServerConfiguration>

	/** The commit/revision for which the reports should be uploaded. */
	@get:Input
	@get:Optional
	abstract val commitDescriptorOrRevision: Property<Pair<CommitDescriptor?, String?>>

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@get:Input
	@get:Optional
	abstract val repository: Property<String>

	/** The list of reports to be uploaded. */
	@get:Input
	abstract val reports: MapProperty<String, ConfigurableFileCollection>

	/** The partition to upload the report to. */
	@get:Input
	abstract val partition: Property<String>

	/** The commit message shown in Teamscale for the upload. */
	@get:Input
	abstract val message: Property<String>

	@get:Input
	abstract val ignoreFailures: Property<Boolean>

	init {
		group = "Teamscale"
		description = "Uploads reports to Teamscale"
	}

	fun from(task: TaskProvider<*>) {
		from(task.get())
	}

	fun from(task: Task) {
		when (task) {
			is TestImpacted -> {
				if (task.reports.testwiseCoverage.required.get()) {
					addReport(EReportFormat.TESTWISE_COVERAGE.name, task.reports.testwiseCoverage.outputLocation)
				} else if (task.reports.junitXml.required.get()) {
					addReport(EReportFormat.JUNIT.name, task.reports.junitXml.outputLocation.asFileTree.matching {
						include("**/*.xml")
					})
				} else {
					error("Testwise coverage collection and JUnit report collection are not enabled for task ${task.path}! Enable it by setting reports.testwiseCoverage.required = true or reports.junitXml.required = true for the task, to be able to upload it.")
				}
			}

			is Test -> {
				check(task.reports.junitXml.required.get()) { "XML report generation is not enabled for task ${task.path}! Enable it by setting reports.junitXml.required = true for the task, to be able to upload it." }
				addReport(EReportFormat.JUNIT.name, task.reports.junitXml.outputLocation.asFileTree.matching {
					include("**/*.xml")
				})
			}

			is JacocoReport -> {
				check(task.reports.xml.required.get()) { "XML report generation is not enabled for task ${task.path}! Enable it by setting reports.xml.required = true for the task, to be able to upload it." }
				addReport(EReportFormat.JACOCO.name, task.reports.xml.outputLocation)
			}

			else -> throw GradleException("Unsupported task type ${task.javaClass.name}! Use addReport(format, reportFiles) instead to upload reports produced by other tasks.")
		}
	}

	fun addReport(format: String, reportFiles: Any) {
		val files = reports.getting(format).orNull
		if (files == null) {
			reports.put(format, project.files(reportFiles))
		} else {
			files.from(reportFiles)
		}
	}

	/** Executes the report upload. */
	@TaskAction
	fun action() {
		val reports = reports.get()

		if (reports.isEmpty()) {
			logger.warn("Skipping upload. No reports specified for uploading.")
			return
		}

		val server = serverConfiguration.get()
		server.validate()

		try {
			logger.info("Uploading to $server at ${commitDescriptorOrRevision.get()}...")
			server.toClient().uploadReports(reports)
		} catch (e: Exception) {
			if (ignoreFailures.get()) {
				logger.warn("Ignoring failure during upload:")
				logger.warn(e.message, e)
			} else {
				throw e
			}
		}
	}


	private fun TeamscaleClient.uploadReports(reports: MutableMap<String, ConfigurableFileCollection>) {
		val formatAndReports = reports.mapValues { getExistingReportFiles(it.value) }.filter {
			if (it.value.isEmpty()) {
				logger.info("Skipped empty upload for ${it.key} reports to partition $partition.")
				false
			} else {
				true
			}
		}
		logger.info("Uploading ${formatAndReports.values.flatten().size} report(s) to partition $partition...")

		formatAndReports.forEach { (format, files) ->
			files.forEach {
				logger.debug("- {}: {}", format, it.absolutePath)
			}
		}

		uploadReportFiles(formatAndReports, partition.get(), message.get())
	}

	private fun TeamscaleClient.uploadReportFiles(
		reports: Map<String, Collection<File>>,
		partition: String,
		message: String
	) {
		try {
			retry(3) {
				uploadReports(reports, commitDescriptorOrRevision.get().first, commitDescriptorOrRevision.get().second, repository.orNull, partition, message)
			}
		} catch (e: IOException) {
			throw GradleException("Upload failed (${e.message})", e)
		}
	}

	private fun getExistingReportFiles(reports: FileCollection) =
		reports.files.filter { it.exists() }.flatMap { fileOrDir -> fileOrDir.walkTopDown().filter { it.isFile } }.distinct()
}

/**
 * Retries the given block numOfRetries-times catching any thrown exceptions.
 * If none of the retries succeeded, the latest caught exception is rethrown.
 */
fun <T> retry(numOfRetries: Int, block: () -> T): T {
	var throwable: Throwable? = null
	(1..numOfRetries).forEach { attempt ->
		try {
			return block()
		} catch (e: Throwable) {
			throwable = e
			println("Failed attempt $attempt / $numOfRetries")
		}
	}
	throw throwable!!
}
