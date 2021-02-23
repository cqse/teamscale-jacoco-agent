package com.teamscale.config.extension

import com.teamscale.TestImpacted
import com.teamscale.config.AgentConfiguration
import com.teamscale.config.TestwiseCoverageConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable

/**
 * Holds all user configuration regarding testwise coverage report uploads.
 */
open class TeamscaleTestImpactedTaskExtension(
    val project: Project,
    jacocoExtension: JacocoTaskExtension,
    testImpactedTask: TestImpacted
) : Serializable {

    val report = TestwiseCoverageConfiguration(project, testImpactedTask)

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in TestwiseCoverageConfiguration>) {
        action.execute(report)
    }

    /** Settings regarding the teamscale jacoco agent. */
    val agent = AgentConfiguration(project, jacocoExtension)

    /** Configures the jacoco agent options. */
    fun agent(action: Action<in AgentConfiguration>) {
        action.execute(agent)
    }
}



