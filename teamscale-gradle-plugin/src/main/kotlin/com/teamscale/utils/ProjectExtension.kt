package com.teamscale.utils

import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.reporting.ReportingExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.testing.base.TestingExtension

/** This is provided by the [ReportingBasePlugin]. */
val Project.reporting
	get() = this.extensions.getByType<ReportingExtension>()

/** This is provided by the [TestSuiteBasePlugin]. */
val Project.testing
	get() = this.extensions.getByType<TestingExtension>()
