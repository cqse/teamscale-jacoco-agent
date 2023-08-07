package com.teamscale

import com.teamscale.config.extension.TeamscaleJacocoReportTaskExtension
import com.teamscale.config.extension.TeamscalePluginExtension
import com.teamscale.config.extension.TeamscaleTestTaskExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.maybeCreate
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.util.GradleVersion


/**
 * Root entry point for the Teamscale plugin.
 *
 * The plugin applies the Java plugin and a root extension named teamscale.
 * Each Test task configured in the project the plugin creates a new task suffixed with {@value #impactedTestsSuffix}
 * that executes the same set of tests, but additionally collects testwise coverage and executes only impacted tests.
 * Furthermore all reports configured are uploaded to Teamscale after the tests have been executed.
 *
 * The plugin needs a gradle version of 6.5 or higher. */
open class TeamscalePlugin : Plugin<Project> {

    companion object {

        /** The name of the extension used to configure the plugin. */
        const val teamscaleExtensionName = "teamscale"

        /** The name of the configuration that holds the impacted test executor and its dependencies. */
        const val impactedTestEngineConfiguration = "impactedTestsEngine"

        /** The name of the configuration that holds the teamscale jacoco agent and its dependencies. */
        const val teamscaleJaCoCoAgentConfiguration = "teamscaleJaCoCoAgent"
    }

    /** The version of the teamscale gradle plugin and impacted-tests-executor.  */
    private val pluginVersion = BuildVersion.pluginVersion

    /** Reference to the teamscale upload task */
    private lateinit var teamscaleUploadTask: TeamscaleUploadTask

    /** Applies the teamscale plugin against the given project.  */
    override fun apply(project: Project) {
        project.logger.info("Applying teamscale plugin $pluginVersion to ${project.name}")
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(JacocoPlugin::class.java)

        val pluginExtension =
            project.extensions.create(teamscaleExtensionName, TeamscalePluginExtension::class.java, project)

        if (GradleVersion.current() < GradleVersion.version("6.5")) {
            throw GradleException("The teamscale plugin requires Gradle version 6.5 or higher")
        }

        // Add impacted tests executor to a custom configuration that will later be used to
        // create the classpath for the TestImpacted created by this plugin.
        project.configurations.maybeCreate(impactedTestEngineConfiguration)
            .defaultDependencies {
                add(project.dependencies.create("com.teamscale:impacted-test-engine:$pluginVersion"))
            }

        // Add teamscale jacoco agent to a custom configuration that will later be used to
        // to generate testwise coverage if enabled.
        project.configurations.maybeCreate(teamscaleJaCoCoAgentConfiguration)
            .defaultDependencies {
                add(project.dependencies.create("com.teamscale:teamscale-jacoco-agent:$pluginVersion"))
            }

        teamscaleUploadTask =
            project.rootProject.tasks.maybeCreate("teamscaleReportUpload", TeamscaleUploadTask::class.java)
        teamscaleUploadTask.apply {
            extension = pluginExtension
        }

        // Add the teamscale extension also to all TestImpacted tasks
        extendTestImpactedTasks(project, pluginExtension)
        extendTestTasks(project)
        extendJacocoReportTasks(project)
    }

    private fun extendTestImpactedTasks(
        project: Project,
        pluginExtension: TeamscalePluginExtension
    ) {
        val agentPortGenerator: Provider<AgentPortGenerator> = project.gradle.sharedServices.registerIfAbsent(
            "agent-port-generator",
            AgentPortGenerator::class.java
        ) {}
        project.tasks.withType<TestImpacted> {
            val testImpactedTask = this
            /** Configures the given impacted test executor. */
            project.logger.info(
                "Configuring impacted tests executor task for ${project.name}:${
                    testImpactedTask
                        .name
                }"
            )
            val extension = pluginExtension.applyTo(testImpactedTask)
            val port = agentPortGenerator.get().getNextPort()
            extension.agent.useLocalAgent("http://127.0.0.1:${port}/")
            testImpactedTask.apply {
                taskExtension = extension
                this.pluginExtension = pluginExtension
                dependsOn.add(project.configurations.getByName(impactedTestEngineConfiguration))
            }
            val teamscaleReportTask = project.rootProject.tasks
                .maybeCreate<TestwiseCoverageReportTask>("${testImpactedTask.name}Report")
            testImpactedTask.finalizedBy(teamscaleReportTask)
            testImpactedTask.reportTask = teamscaleReportTask
            teamscaleReportTask.apply {
                testTaskName = testImpactedTask.name
                configuration = extension
            }
            teamscaleReportTask.uploadTask = teamscaleUploadTask
            teamscaleUploadTask.reports.add(extension.report.getReport())
        }
    }

    /**
     * Adds an extension to the JacocoReport tasks and adds the report
     * to the upload task when the extension has been configured.
     */
    private fun extendJacocoReportTasks(project: Project) {
        project.tasks.withType<JacocoReport> {
            val reportTask = this
            teamscaleUploadTask.mustRunAfter(reportTask)

            val extension =
                reportTask.extensions.create(
                    teamscaleExtensionName,
                    TeamscaleJacocoReportTaskExtension::class.java,
                    project,
                    reportTask
                )
            teamscaleUploadTask.reports.add(extension.report.getReport())
        }
    }

    /**
     * Adds an extension to the standard test tasks and adds the report
     * to the upload task when the extension has been configured.
     */
    private fun extendTestTasks(project: Project) {
        project.tasks.withType<Test> {
            if (this is TestImpacted) {
                return@withType
            }
            val extension =
                extensions.create(
                    teamscaleExtensionName,
                    TeamscaleTestTaskExtension::class.java,
                    project,
                    this
                )
            teamscaleUploadTask.reports.add(extension.report.getReport())
        }
    }

}
