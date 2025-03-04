package com.teamscale.reporting.testwise.internal

import com.teamscale.reporting.testwise.TestwiseCoverageTaskReportContainer
import com.teamscale.utils.DefaultSingleFileReport
import com.teamscale.utils.Reports
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal
import javax.inject.Inject

open class DefaultTestwiseCoverageTaskReportContainer @Inject constructor(objectFactory: ObjectFactory) : Reports<Report>(objectFactory, Report::class.java),
	TestwiseCoverageTaskReportContainer {

	init {
		addReport(objectFactory.newInstance(DefaultSingleFileReport::class.java, "testwiseCoverage"))
	}

	@get:Internal
	override val testwiseCoverage: SingleFileReport
		get() = getByName("testwiseCoverage") as SingleFileReport
}
