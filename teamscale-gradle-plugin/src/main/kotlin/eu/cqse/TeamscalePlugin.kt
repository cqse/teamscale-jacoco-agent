package eu.cqse

import eu.cqse.config.TeamscalePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.util.GradleVersion
import java.io.IOException

open class TeamscalePlugin : Plugin<Project> {

    /** The version of the teamscale gradle plugin and coverage conductor.  */
    private var pluginVersion = BuildVersion.buildVersion

    /** The name of the extension used to configure the plugin. */
    private val teamscaleExtensionName = "teamscale"

    /** Applies the teamscale plugin against the given project.  */
    override fun apply(project: Project) {
        project.logger.info("Applying teamscale plugin $pluginVersion to ${project.name}")
        project.plugins.apply(JavaPlugin::class.java)

        project.extensions.add(teamscaleExtensionName, TeamscalePluginExtension::class.java)

        if (GradleVersion.current() < GradleVersion.version("4.6")) {
            throw GradleException("The teamscale plugin requires Gradle version 4.6 or higher")
        }

        project.repositories.maven {
            it.setUrl("https://share.cqse.eu/public/maven/")
        }

        // Add coverage conductor to a custom configuration that will later be used to
        // create the classpath for the custom task created by this plugin.
        project.configurations.maybeCreate("coverageConductor")
                .defaultDependencies { dependencies ->
                    dependencies.add(project.dependencies.create("eu.cqse:coverage-conductor:$pluginVersion"))
                }

        // Add the teamscale extension also to all test tasks
        project.tasks.withType(Test::class.java) { gradleTestTask ->
            gradleTestTask.extensions.create(teamscaleExtensionName, TeamscalePluginExtension::class.java)

            // Create the CPT task when the Test task is registered to allow client-side modifications of the classpath
            project.tasks.create("${gradleTestTask.name}CPT", ImpactedTestsExecutor::class.java)
        }

        project.afterEvaluate {
            project.tasks.withType(Test::class.java) { gradleTestTask ->
                if (gradleTestTask.testFramework !is JUnitPlatformTestFramework) {
                    return@withType
                }
                val root = project.extensions.getByType(TeamscalePluginExtension::class.java)
                val task = gradleTestTask.extensions.getByType(TeamscalePluginExtension::class.java)
                val config = TeamscalePluginExtension.merge(root, task)
                val cptRunTestTask = project.tasks.getByName("${gradleTestTask.name}CPT") as ImpactedTestsExecutor
                cptRunTestTask.onlyIf { config.testImpactMode ?: false }
                if (!config.report.validate()) {
                    return@withType
                }
                configureTestwiseCoverageCollectingTestWrapperTask(project, gradleTestTask, config, cptRunTestTask)
            }
        }
    }

    /**
     * Configures the given impacted test executor.
     */
    private fun configureTestwiseCoverageCollectingTestWrapperTask(project: Project, gradleTestTask: Test, config: TeamscalePluginExtension, cptRunTestTask: ImpactedTestsExecutor) {
        project.logger.info("Configuring CPT task for ${project.name}:${gradleTestTask.name}")
        val commit = try {
            config.commit.getCommit(project.rootDir)
        } catch (e: IOException) {
            project.logger.error("Could not determine teamscale upload commit for ${project.name} $gradleTestTask", e)
            return
        }

        // CPT test task
        cptRunTestTask.apply {
            testTask = gradleTestTask
            configuration = config
            commitDescriptor = commit
            configure(project)
        }

        // Report upload task
        val teamscaleUploadTask = project.rootProject.tasks
                .maybeCreate("${gradleTestTask.name}ReportUpload", TeamscaleUploadTask::class.java)
        cptRunTestTask.finalizedBy(teamscaleUploadTask)

        // Copy dependencies from gradle test task
        cptRunTestTask.dependsOn(gradleTestTask.dependsOn)

        teamscaleUploadTask.apply {
            server = config.server
            commitDescriptor = commit

            if (config.report.testwiseCoverage.upload == true) {
                addReport(config.report.testwiseCoverage.getReport(project, gradleTestTask))
            }
            if (config.report.jUnit.upload == true) {
                addReport(config.report.jUnit.getReport(project, gradleTestTask))
            }
        }
    }
}
