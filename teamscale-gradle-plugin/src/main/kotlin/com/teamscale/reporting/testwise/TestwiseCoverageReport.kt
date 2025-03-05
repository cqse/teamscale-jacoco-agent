package com.teamscale.reporting.testwise

import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.reporting.testwise.internal.DefaultTestwiseCoverageTaskReportContainer
import com.teamscale.reporting.testwise.internal.TestwiseCoverageReporting
import com.teamscale.utils.reporting
import com.teamscale.utils.teamscale
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.util.internal.ClosureBackedAction
import javax.inject.Inject


/** Task which runs the impacted tests. */
@Suppress("MemberVisibilityCanBePrivate")
abstract class TestwiseCoverageReport @Inject constructor(objectFactory: ObjectFactory) : DefaultTask(),
	Reporting<TestwiseCoverageTaskReportContainer> {

	@get:Input
	abstract val partial: Property<Boolean>

	@get:PathSensitive(PathSensitivity.NONE)
	@get:InputFiles
	abstract val executionData: ConfigurableFileCollection

	@get:Classpath
	abstract val classDirectories: ConfigurableFileCollection

	private val reports: TestwiseCoverageTaskReportContainer

	init {
		group = "Teamscale"
		description = "Generates a testwise coverage report"

		reports = objectFactory.newInstance(DefaultTestwiseCoverageTaskReportContainer::class.java)
		reports.testwiseCoverage.required.convention(true)
		reports.testwiseCoverage.outputLocation.convention(project.reporting.baseDirectory.file("testwise-coverage/${name}/testwise-coverage.json"))

		onlyIf("Any of the execution data files exists") { executionData.files.any { it.exists() } }
	}

	/**
	 * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
	 */
	@TaskAction
	fun generateTestwiseCoverageReports() {
		check(classDirectories.files.isNotEmpty()) { "The classDirectories does not contain any input! Did you forget to configure sourceSets on the task?" }
		logger.info("Generating coverage reports...")
		TestwiseCoverageReporting(
			logger,
			partial.get(),
			classDirectories.files,
			ClasspathWildcardIncludeFilter(null, null),
			executionData.files,
			reports.testwiseCoverage.outputLocation.asFile.get()
		).generateTestwiseCoverageReports()
	}

	/**
	 * Adds execution data files to be used during coverage analysis.
	 *
	 * @param files one or more files to add
	 */
	fun executionData(vararg files: Any) {
		executionData.from(*files)
	}

	/**
	 * Adds execution data generated by a task to the list of those used during coverage analysis. Only tasks with a [JacocoTaskExtension] will be included; all others will be ignored.
	 *
	 * @param tasks one or more tasks to add
	 */
	fun executionData(vararg tasks: Task) {
		for (task in tasks) {
			executionData(task)
		}
	}

	fun executionData(test: Task) {
		executionData(test.project.tasks.named(test.name))
	}

	fun executionData(test: TaskProvider<out Task>) {
		if (test.get() is Test) {
			partial.set(test.map { (it as Test).teamscale.partial.get() })
			executionData.from(test.map { (it as Test).teamscale.agent.destination })
			classDirectories.from(test.map { (it as Test).classpath })
			mustRunAfter(test)
		}
	}

	/**
	 * Adds a source set to the list to be reported on. The output of this source set will be used as classes to include in the report. The source for this source set will be used for any classes
	 * included in the report.
	 *
	 * @param sourceSets one or more source sets to report on
	 */
	fun sourceSets(vararg sourceSets: SourceSet) {
		for (sourceSet in sourceSets) {
			classDirectories.from(sourceSet.output)
		}
	}

	/**
	 * The reports that this task potentially produces.
	 *
	 * @return The reports that this task potentially produces
	 */
	@Nested
	override fun getReports(): TestwiseCoverageTaskReportContainer {
		return reports
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param closure The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(closure: Closure<*>): TestwiseCoverageTaskReportContainer {
		return reports(ClosureBackedAction(closure))
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param configureAction The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(configureAction: Action<in TestwiseCoverageTaskReportContainer>): TestwiseCoverageTaskReportContainer {
		configureAction.execute(reports)
		return reports
	}
}
