package com.teamscale.config.extension

import com.teamscale.TestImpacted
import com.teamscale.config.AgentConfiguration
import com.teamscale.config.TestwiseCoverageConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable

/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscaleTestImpactedTaskExtension(
    val project: Project,
    val parent: TeamscalePluginExtension,
    jacocoExtension: JacocoTaskExtension,
    testImpacted: TestImpacted
) : Serializable {

    val testwiseCoverageConfiguration = TestwiseCoverageConfiguration(project, testImpacted)

    /** Configures the reports to be uploaded. */
    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration>) {
        action.execute(testwiseCoverageConfiguration)
    }

    /** Settings regarding the teamscale jacoco agent. */
    val agent = AgentConfiguration(project, jacocoExtension)

    /** Configures the jacoco agent options. */
    fun agent(action: Action<in AgentConfiguration>) {
        action.execute(agent)
    }
}



