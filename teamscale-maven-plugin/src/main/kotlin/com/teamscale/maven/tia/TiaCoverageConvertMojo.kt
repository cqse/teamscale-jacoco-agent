package com.teamscale.maven.tia

import com.teamscale.jacoco.agent.options.AgentOptionParseException
import com.teamscale.jacoco.agent.options.ClasspathUtils
import com.teamscale.jacoco.agent.options.FilePatternResolver
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import shadow.com.teamscale.client.TestDetails
import shadow.com.teamscale.report.EDuplicateClassFileBehavior
import shadow.com.teamscale.report.ReportUtils
import shadow.com.teamscale.report.testwise.ETestArtifactFormat
import shadow.com.teamscale.report.testwise.TestwiseCoverageReportWriter
import shadow.com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import shadow.com.teamscale.report.testwise.model.TestExecution
import shadow.com.teamscale.report.testwise.model.factory.TestInfoFactory
import shadow.com.teamscale.report.util.ClasspathWildcardIncludeFilter
import shadow.com.teamscale.report.util.CommandLineLogger
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Batch converts all created .exec file reports into a testwise coverage
 * report.
 */
@Mojo(
	name = "testwise-coverage-converter",
	defaultPhase = LifecyclePhase.VERIFY,
	requiresDependencyResolution = ResolutionScope.RUNTIME
)
class TiaCoverageConvertMojo : AbstractMojo() {
	/**
	 * Wildcard include patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(defaultValue = "**")
	lateinit var includes: Array<String>

	/**
	 * Wildcard exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter
	lateinit var excludes: Array<String>

	/**
	 * After how many tests the testwise coverage should be split into multiple
	 * reports (Default is 5000).
	 */
	@Parameter(defaultValue = "5000")
	var splitAfter: Int = 5000

	/**
	 * The project build directory (usually: `./target`). Provided
	 * automatically by Maven.
	 */
	@Parameter(defaultValue = "\${project.build.directory}")
	lateinit var projectBuildDir: String

	/**
	 * The output directory of the testwise coverage reports.
	 */
	@Parameter
	lateinit var outputFolder: String

	/**
	 * The running Maven session. Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "\${session}")
	lateinit var session: MavenSession
	private val logger = CommandLineLogger()

	@Throws(MojoFailureException::class)
	override fun execute() {
		val reportFileDirectories = mutableListOf<File>()
		reportFileDirectories.add(Paths.get(projectBuildDir, "tia").toAbsolutePath().resolve("reports").toFile())
		if (outputFolder.isBlank()) {
			outputFolder = Paths.get(projectBuildDir, "tia", "reports").toString()
		}
		val classFileDirectories: MutableList<File>
		try {
			Files.createDirectories(Paths.get(outputFolder))
			classFileDirectories = getClassDirectoriesOrZips(projectBuildDir)
			findSubprojectReportAndClassDirectories(reportFileDirectories, classFileDirectories)
		} catch (e: IOException) {
			logger.error("Could not create testwise report generator. Aborting.", e)
			throw MojoFailureException(e)
		} catch (e: AgentOptionParseException) {
			logger.error("Could not create testwise report generator. Aborting.", e)
			throw MojoFailureException(e)
		}
		logger.info("Generating the testwise coverage report")
		val generator = createJaCoCoTestwiseReportGenerator(classFileDirectories)
		val testInfoFactory = createTestInfoFactory(reportFileDirectories)
		val jacocoExecutionDataList = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, reportFileDirectories)
		val reportFilePath = Paths.get(outputFolder, "testwise-coverage.json").toString()

		try {
			TestwiseCoverageReportWriter(
				testInfoFactory, File(reportFilePath), splitAfter
			).use { coverageWriter ->
				jacocoExecutionDataList.forEach { executionDataFile ->
					logger.info("Writing execution data for file: ${executionDataFile.name}")
					generator.convertAndConsume(executionDataFile, coverageWriter)
				}
			}
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}

	@Throws(AgentOptionParseException::class)
	private fun findSubprojectReportAndClassDirectories(
		reportFiles: MutableList<File>,
		classFiles: MutableList<File>
	) {
		session.topLevelProject.collectedProjects.forEach { subProject ->
			val subprojectBuildDirectory = subProject.build.directory
			reportFiles.add(
				Paths.get(subprojectBuildDirectory, "tia").toAbsolutePath().resolve("reports").toFile()
			)
			classFiles.addAll(getClassDirectoriesOrZips(subprojectBuildDirectory))
		}
	}

	@Throws(MojoFailureException::class)
	private fun createTestInfoFactory(reportFiles: List<File>): TestInfoFactory {
		try {
			val testDetails = ReportUtils.readObjects(
				ETestArtifactFormat.TEST_LIST,
				Array<TestDetails>::class.java,
				reportFiles
			)
			val testExecutions = ReportUtils.readObjects(
				ETestArtifactFormat.TEST_EXECUTION,
				Array<TestExecution>::class.java,
				reportFiles
			)
			logger.info("Writing report with ${testDetails.size} Details/${testExecutions.size} Results")
			return TestInfoFactory(testDetails, testExecutions)
		} catch (e: IOException) {
			logger.error("Could not read test details from reports. Aborting.", e)
			throw MojoFailureException(e)
		}
	}

	private fun createJaCoCoTestwiseReportGenerator(classFiles: List<File>) =
		JaCoCoTestwiseReportGenerator(
			classFiles,
			ClasspathWildcardIncludeFilter(
				includes.joinToString(":"),
				excludes.joinToString(":")
			), EDuplicateClassFileBehavior.WARN, logger
		)

	@Throws(AgentOptionParseException::class)
	private fun getClassDirectoriesOrZips(projectBuildDir: String) =
		ClasspathUtils.resolveClasspathTextFiles(
			"classes",
			FilePatternResolver(CommandLineLogger()),
			listOf(projectBuildDir)
		)
}
