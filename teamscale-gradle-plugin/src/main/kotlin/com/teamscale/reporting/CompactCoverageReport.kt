package com.teamscale.reporting

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.gradle.util.internal.ClosureBackedAction
import javax.inject.Inject


/** Base class for tasks which generate a report based on JaCoCo based binary data. */
@CacheableTask
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class JaCoCoBasedReportTaskBase<T: ReportContainer<*>> : DefaultTask(), Reporting<T> {

	/** The binary execution data. */
	@get:PathSensitive(PathSensitivity.NONE)
	@get:InputFiles
	abstract val executionData: ConfigurableFileCollection

	/** The class directories used to resolve the binary files against. */
	@get:Classpath
	abstract val classDirectories: ConfigurableFileCollection

	/** Object factory. */
	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** The report container. Is exposed to the up-to-date checks via [getReports]. */
	@get:Internal
	protected abstract val reportContainer: T

	init {
		group = "Teamscale"
		onlyIf("Any of the execution data files exists") { executionData.files.any { it.exists() } }
	}

	/** Generates the report */
	@TaskAction
	abstract fun generateReport()

	/**
	 * Adds execution data files to be used during coverage analysis.
	 *
	 * @param files one or more files to add
	 */
	fun executionData(vararg files: Any) {
		executionData.from(*files)
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
	override fun getReports(): T {
		return reportContainer
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param closure The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(closure: Closure<*>): T {
		return reports(ClosureBackedAction(closure))
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param configureAction The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(configureAction: Action<in T>): T {
		configureAction.execute(reportContainer)
		return reportContainer
	}
}


