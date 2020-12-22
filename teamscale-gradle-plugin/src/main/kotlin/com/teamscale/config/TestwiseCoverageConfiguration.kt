package com.teamscale.config

import com.teamscale.TestImpacted
import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import java.io.File

/** Configuration for the testwise coverage report. */
open class TestwiseCoverageConfiguration(project: Project, task: TestImpacted) :
    ReportConfigurationBase(EReportFormat.TESTWISE_COVERAGE, project, task) {
    init {
        destination.set(project.providers.provider {
            File(
                project.rootProject.buildDir, "reports/testwise-coverage/${task.name}/${
                    partition.get().replace(
                        "[ /\\\\]".toRegex(),
                        "-"
                    )
                }.json"
            )
        })
    }
}