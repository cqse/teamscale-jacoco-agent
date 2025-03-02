package com.teamscale

import com.teamscale.utils.DefaultSingleFileReport
import com.teamscale.utils.Reports
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.JUnitXmlReport
import org.gradle.api.tasks.testing.TestTaskReports

open class DefaultTestImpactedTaskReports(reports: TestTaskReports, objectFactory: ObjectFactory) :
	Reports<Report>(objectFactory, Report::class.java),
	TestImpactedTaskReports {

	init {
		addReport(reports.junitXml)
		addReport(reports.html)
		addReport(objectFactory.newInstance(DefaultSingleFileReport::class.java, "testwiseCoverage"))
	}

	@Internal
	override fun getHtml(): DirectoryReport {
		return getByName("html") as DirectoryReport
	}

	@Internal
	override fun getJunitXml(): JUnitXmlReport {
		return getByName("junitXml") as JUnitXmlReport
	}

	@get:Internal
	override val testwiseCoverage: SingleFileReport
		get() = getByName("testwiseCoverage") as SingleFileReport
}
