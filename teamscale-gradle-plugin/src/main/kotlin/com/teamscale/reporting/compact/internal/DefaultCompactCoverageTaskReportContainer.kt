package com.teamscale.reporting.compact.internal

import com.teamscale.reporting.compact.CompactCoverageTaskReportContainer
import com.teamscale.utils.DefaultSingleFileReport
import com.teamscale.utils.Reports
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * Implementation of CompactCoverageTaskReportContainer.
 */
open class DefaultCompactCoverageTaskReportContainer @Inject constructor(objectFactory: ObjectFactory) :
	Reports<Report>(objectFactory, Report::class.java),
	CompactCoverageTaskReportContainer {

	init {
		addReport(objectFactory.newInstance(DefaultSingleFileReport::class.java, "compactCoverage"))
	}

	@get:Internal
	override val compactCoverage: SingleFileReport
		get() = getByName("compactCoverage") as SingleFileReport
}


