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

    /** Creates CPT tasks for all tests if enabled. */
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

    val includes = mutableListOf<String>()

    /** Configures the reports to be uploaded. */
    fun include(pattern: String) {
        includes.add(pattern)
    }

    val excludes = mutableListOf<String>()

    /** Configures the reports to be uploaded. */
    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    /**  */
    fun validate(project: Project, testTaskName: String): Boolean {
        if(testImpactMode == true) {
            return report.testwiseCoverage.validate(project, testTaskName) && report.jUnit.validate(project, testTaskName)
        }
        return true
    }

    companion object {
        fun merge(root: TeamscalePluginExtension, task: TeamscalePluginExtension): TeamscalePluginExtension {
            val merged = TeamscalePluginExtension()
            merged.server.copyWithDefault(task.server, root.server)
            merged.commit.copyWithDefault(task.commit, root.commit)
            merged.report.copyWithDefault(task.report, root.report)
            merged.agent.copyWithDefault(task.agent, root.agent)
            merged.testImpactMode = task.testImpactMode ?: root.testImpactMode
            merged.includes.addAll(task.includes)
            merged.includes.addAll(root.includes)
            merged.excludes.addAll(task.excludes)
            merged.excludes.addAll(root.excludes)
            return merged
        }
    }
}
