package eu.cqse.config

import org.gradle.api.Action
import org.gradle.api.Project
import java.io.Serializable

/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscalePluginExtension : Serializable {

    val server = Server()

    /** Configures the Teamscale server. */
    fun server(action: Action<in Server>) {
        action.execute(server)
    }

    val commit = Commit()

    /** Configures the code commit. */
    fun commit(action: Action<in Commit>) {
        action.execute(commit)
    }

    /** Creates Impacted tasks for all tests if enabled. */
    var testImpactMode: Boolean? = null

    val report = Reports()

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in Reports>) {
        action.execute(report)
    }

    val agent = AgentConfiguration()

    /** Configures the jacoco agent options. */
    fun agent(action: Action<in AgentConfiguration>) {
        action.execute(agent)
    }

    /**
     * @return True if all required fields have been set otherwise false.
     */
    fun validate(project: Project, testTaskName: String): Boolean {
        if (testImpactMode == true) {
            return commit.validate(project, testTaskName) && report.testwiseCoverage.validate(
                project,
                testTaskName
            )
        }
        return true
    }

    companion object {

        /**
         * Merges the configuration of the teamscale root extension with the more specific task extension.
         * Values set in the task extension will overwrite values set via the root extension.
         */
        fun merge(root: TeamscalePluginExtension, task: TeamscalePluginExtension): TeamscalePluginExtension {
            val merged = TeamscalePluginExtension()
            merged.server.copyWithDefault(task.server, root.server)
            merged.commit.copyWithDefault(task.commit, root.commit)
            merged.report.copyWithDefault(task.report, root.report)
            merged.agent.copyWithDefault(task.agent, root.agent)
            merged.testImpactMode = task.testImpactMode ?: root.testImpactMode
            return merged
        }
    }
}
