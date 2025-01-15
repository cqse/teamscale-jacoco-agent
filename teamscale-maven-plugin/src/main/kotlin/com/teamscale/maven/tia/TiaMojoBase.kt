package com.teamscale.maven.tia

import com.teamscale.maven.TeamscaleMojoBase
import org.apache.commons.lang3.StringUtils
import org.apache.maven.artifact.Artifact
import org.apache.maven.model.PluginExecution
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.conqat.lib.commons.filesystem.FileSystemUtils
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.createDirectories

/**
 * Base class for TIA Mojos. Provides all necessary functionality but can be subclassed to change the partition.
 *
 * For this plugin to work, you must either:
 *  - Make Surefire and Failsafe use our JUnit 5 test engine
 *  - Send test start and end events to the Java agent themselves
 *
 * To use our JUnit 5 impacted-test-engine, you must declare it as a test dependency. Example:
 *
 * ```
 * <dependencies>
 *   <dependency>
 *	   <groupId>com.teamscale</groupId>
 *	   <artifactId>impacted-test-engine</artifactId>
 *	   <version>30.0.0</version>
 *	   <scope>test</scope>
 *   </dependency>
 * </dependencies>
 * ```
 *
 * To send test events yourself, you can use our TIA client library (Maven coordinates: com.teamscale:tia-client).
 * The log file of the agent is written to `${project.build.directory}/tia/agent.log`.
 */
abstract class TiaMojoBase : TeamscaleMojoBase() {
	/**
	 * Impacted tests are calculated from [baselineCommit] to [commit]. This sets the baseline.
	 */
	@Parameter
	lateinit var baselineCommit: String

	/**
	 * Impacted tests are calculated from [baselineCommit] to [commit].
	 * The [baselineRevision] sets the [baselineCommit] with the help of a VCS revision (e.g. git SHA1) instead of a branch and timestamp
	 */
	@Parameter
	lateinit var baselineRevision: String

	/**
	 * You can optionally specify which code should be included in the coverage instrumentation. Each pattern is applied
	 * to the fully qualified class names of the profiled system. Use `*` to match any number characters and
	 * `?` to match any single character.
	 *
	 * Classes that match any of the include patterns are included, unless any exclude pattern excludes them.
	 */
	@Parameter
	lateinit var includes: Array<String>

	/**
	 * You can optionally specify which code should be excluded from the coverage instrumentation. Each pattern is
	 * applied to the fully qualified class names of the profiled system. Use `*` to match any number characters
	 * and `?` to match any single character.
	 *
	 * Classes that match any of the exclude patterns are excluded, even if they are included by an include pattern.
	 */
	@Parameter
	lateinit var excludes: Array<String>

	/**
	 * To instrument the system under test, a Java agent must be attached to the JVM of the system. The JVM
	 * command line arguments to achieve this are by default written to the property `argLine`, which is
	 * automatically picked up by Surefire and Failsafe and applied to the JVMs these plugins start. You can override
	 * the name of this property if you wish to manually apply the command line arguments yourself, e.g. if your system
	 * under test is started by some other plugin like the Spring boot starter.
	 */
	@Parameter
	lateinit var propertyName: String

	/**
	 * Port on which the Java agent listens for commands from this plugin. The default value 0 will tell the agent to
	 * automatically search for an open port.
	 */
	@Parameter(defaultValue = "0")
	lateinit var agentPort: String

	/**
	 * Optional additional arguments to send to the agent. Each argument must be of the form `KEY=VALUE`.
	 */
	@Parameter
	lateinit var additionalAgentOptions: Array<String>

	/**
	 * Changes the log level of the agent to DEBUG.
	 */
	@Parameter(defaultValue = "false")
	var debugLogging: Boolean = false

	/**
	 * Executes all tests, not only impacted ones if set. Defaults to false.
	 */
	@Parameter(defaultValue = "false")
	var runAllTests: Boolean = false

	/**
	 * Executes only impacted tests, not all ones if set. Defaults to true.
	 */
	@Parameter(defaultValue = "true")
	var runImpacted: Boolean = true

	/**
	 * Mode of producing testwise coverage.
	 */
	@Parameter(defaultValue = "teamscale-upload")
	lateinit var tiaMode: String

	/**
	 * Map of resolved Maven artifacts. Provided automatically by Maven.
	 */
	@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
	lateinit var pluginArtifactMap: Map<String, Artifact>

	/**
	 * The project build directory (usually: `./target`). Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "\${project.build.directory}")
	lateinit var projectBuildDir: String

	private lateinit var targetDirectory: Path

	@Throws(MojoFailureException::class, MojoExecutionException::class)
	override fun execute() {
		super.execute()

		if (baselineCommit.isNotBlank() && baselineRevision.isNotBlank()) {
			log.warn(
				"Both baselineRevision and baselineCommit are set but only one of them is needed. " +
						"The revision will be preferred in this case. If that's not intended, please do not set the baselineRevision manually."
			)
		}

		if (skip) return

		getTestPlugin(testPluginArtifact)?.let { testPlugin ->
			configureTestPlugin()
			testPlugin.executions.forEach { execution ->
				validateTestPluginConfiguration(execution)
			}
		}

		targetDirectory = Paths.get(projectBuildDir, "tia").toAbsolutePath()
		createTargetDirectory()

		resolveCommitOrRevision()
		setTiaProperties()

		val agentConfigFile = createAgentConfigFiles(agentPort)
		setArgLine(agentConfigFile, targetDirectory.resolve("agent.log"))
	}

	private fun setTiaProperties() {
		setTiaProperty("reportDirectory", targetDirectory.toString())
		setTiaProperty("server.url", teamscaleUrl)
		setTiaProperty("server.project", projectId)
		setTiaProperty("server.userName", username)
		setTiaProperty("server.userAccessToken", accessToken)

		if (StringUtils.isNotEmpty(resolvedRevision)) {
			setTiaProperty("endRevision", resolvedRevision)
		} else {
			setTiaProperty("endCommit", resolvedCommit)
		}

		if (StringUtils.isNotEmpty(baselineRevision)) {
			setTiaProperty("baselineRevision", baselineRevision)
		} else {
			setTiaProperty("baseline", baselineCommit)
		}

		setTiaProperty("repository", repository)
		setTiaProperty("partition", partition)
		if (agentPort == "0") {
			agentPort = findAvailablePort()
		}

		setTiaProperty("agentsUrls", "http://localhost:$agentPort")
		setTiaProperty("runImpacted", runImpacted.toString())
		setTiaProperty("runAllTests", runAllTests.toString())
	}

	/**
	 * Automatically find an available port.
	 */
	private fun findAvailablePort(): String {
		try {
			ServerSocket(0).use { socket ->
				val port = socket.localPort
				log.info("Automatically set server port to $port")
				return port.toString()
			}
		} catch (e: IOException) {
			log.error("Port blocked, trying again.", e)
			return findAvailablePort()
		}
	}

	/**
	 * Sets the teamscale-test-impacted engine as only includedEngine and passes all previous engine configuration to
	 * the impacted test engine instead.
	 */
	private fun configureTestPlugin() {
		enforcePropertyValue(INCLUDE_JUNIT5_ENGINES_OPTION, "includedEngines", "teamscale-test-impacted")
		enforcePropertyValue(EXCLUDE_JUNIT5_ENGINES_OPTION, "excludedEngines", "")
	}

	private fun enforcePropertyValue(
		engineOption: String,
		impactedEngineSuffix: String,
		newValue: String
	) {
		overrideProperty(engineOption, impactedEngineSuffix, newValue, session.currentProject.properties)
		overrideProperty(engineOption, impactedEngineSuffix, newValue, session.userProperties)
	}

	private fun overrideProperty(
		engineOption: String,
		impactedEngineSuffix: String,
		newValue: String,
		properties: Properties
	) {
		(properties.put(getPropertyName(engineOption), newValue) as? String)?.let { originalValue ->
			if (originalValue.isNotBlank() && (newValue != originalValue)) {
				setTiaProperty(impactedEngineSuffix, originalValue)
			}
		}
	}

	@Throws(MojoFailureException::class)
	private fun validateTestPluginConfiguration(execution: PluginExecution) {
		val configurationDom = execution.configuration as Xpp3Dom

		validateEngineNotConfigured(configurationDom, INCLUDE_JUNIT5_ENGINES_OPTION)
		validateEngineNotConfigured(configurationDom, EXCLUDE_JUNIT5_ENGINES_OPTION)

		validateParallelizationParameter(configurationDom, "threadCount")
		validateParallelizationParameter(configurationDom, "forkCount")

		val parameterDom = configurationDom.getChild("reuseForks") ?: return
		val value = parameterDom.value
		if (value != null && value != "true") {
			log.warn(
				"You configured surefire to not reuse forks." +
						" This has been shown to lead to performance decreases in combination with the Teamscale Maven Plugin." +
						" If you notice performance problems, please have a look at our troubleshooting section for possible solutions: https://docs.teamscale.com/howto/providing-testwise-coverage/#troubleshooting."
			)
		}
	}

	@Throws(MojoFailureException::class)
	private fun validateEngineNotConfigured(
		configurationDom: Xpp3Dom,
		xmlConfigurationName: String
	) {
		val engines = configurationDom.getChild(xmlConfigurationName)
		if (engines != null) {
			throw MojoFailureException(
				"You configured JUnit 5 engines in the $testPluginArtifact plugin via the $xmlConfigurationName configuration parameter. This is currently not supported when performing Test Impact analysis. Please add the $xmlConfigurationName via the ${
					getPropertyName(
						xmlConfigurationName
					)
				} property."
			)
		}
	}

	private fun getPropertyName(xmlConfigurationName: String) =
		"$testPluginPropertyPrefix.$xmlConfigurationName"

	private fun getTestPlugin(testPluginArtifact: String) =
		session.currentProject.model.build.pluginsAsMap[testPluginArtifact]

	@Throws(MojoFailureException::class)
	private fun validateParallelizationParameter(
		configurationDom: Xpp3Dom,
		parallelizationParameter: String
	) {
		configurationDom.getChild(parallelizationParameter)?.value?.let { value ->
			if (value == "1") return@let
			throw MojoFailureException(
				"You configured parallel tests in the " + testPluginArtifact + " plugin via the " + parallelizationParameter + " configuration parameter." +
						" Parallel tests are not supported when performing Test Impact analysis as they prevent recording testwise coverage." +
						" Please disable parallel tests when running Test Impact analysis."
			)
		}
	}

	/**
	 * @return the partition to upload testwise coverage to.
	 */
	protected abstract val partition: String

	/**
	 * @return the artifact name of the test plugin (e.g. Surefire, Failsafe).
	 */
	protected abstract val testPluginArtifact: String

	/** @return The prefix of the properties that are used to pass parameters to the plugin.
	 */
	protected abstract val testPluginPropertyPrefix: String

	/**
	 * @return whether this Mojo applies to integration tests.
	 *
	 *
	 * Depending on this, different properties are used to set the argLine.
	 */
	protected abstract val isIntegrationTest: Boolean

	@Throws(MojoFailureException::class)
	private fun createTargetDirectory() {
		try {
			targetDirectory.createDirectories()
		} catch (e: IOException) {
			throw MojoFailureException("Could not create target directory $targetDirectory", e)
		}
	}

	private fun setArgLine(agentConfigFile: Path, logFilePath: Path) {
		var agentLogLevel = "INFO"
		if (debugLogging) {
			agentLogLevel = "DEBUG"
		}

		ArgLine.cleanOldArgLines(session, log)
		findAgentJarFile()?.let { agentJarFile ->
			ArgLine.applyToMavenProject(
				ArgLine(additionalAgentOptions, agentLogLevel, agentJarFile, agentConfigFile, logFilePath),
				session, log, propertyName, isIntegrationTest
			)
		}
	}

	@Throws(MojoFailureException::class)
	private fun createAgentConfigFiles(agentPort: String): Path {
		val loggingConfigPath = targetDirectory.resolve("logback.xml")
		try {
			Files.newOutputStream(loggingConfigPath).use { loggingConfigOutputStream ->
				FileSystemUtils.copy(readAgentLogbackConfig(), loggingConfigOutputStream)
			}
		} catch (e: IOException) {
			throw MojoFailureException(
				"Writing the logging configuration file for the TIA agent failed. Make sure the path $loggingConfigPath is writeable.", e
			)
		}

		val configFilePath = targetDirectory.resolve("agent-at-port-$agentPort.properties")
		val agentConfig = createAgentConfig(loggingConfigPath, targetDirectory.resolve("reports"))
		try {
			Files.write(configFilePath, setOf(agentConfig))
		} catch (e: IOException) {
			throw MojoFailureException(
				"Writing the configuration file for the TIA agent failed. Make sure the path $configFilePath is writeable.", e
			)
		}

		log.info("Agent config file created at $configFilePath")
		return configFilePath
	}

	private fun readAgentLogbackConfig() =
		TiaMojoBase::class.java.getResourceAsStream("logback-agent.xml")

	private fun createAgentConfig(loggingConfigPath: Path, agentOutputDirectory: Path): String {
		var config = """
			mode=testwise
			tia-mode=$tiaMode
			teamscale-server-url=$teamscaleUrl
			teamscale-project=$projectId
			teamscale-user=$username
			teamscale-access-token=$accessToken
			teamscale-partition=${partition}
			http-server-port=$agentPort
			logging-config=$loggingConfigPath
			out=${agentOutputDirectory.toAbsolutePath()}
			""".trimIndent()
		if (includes.isNotEmpty()) {
			config += """
				
				includes=${includes.joinToString(";")}
				""".trimIndent()
		}
		if (excludes.isNotEmpty()) {
			config += """
				
				excludes=${excludes.joinToString(";")}
				""".trimIndent()
		}
		if (repository.isNotBlank()) {
			config += "\nteamscale-repository=$repository"
		}

		config += if (!resolvedRevision.isNullOrBlank()) {
			"\nteamscale-revision=$resolvedRevision"
		} else {
			"\nteamscale-commit=$resolvedCommit"
		}
		return config
	}

	private fun findAgentJarFile() =
		pluginArtifactMap["com.teamscale:teamscale-jacoco-agent"]?.file?.toPath()

	/**
	 * Sets a property in the TIA namespace. It seems that, depending on Maven version and which other plugins are used,
	 * different types of properties are respected both during the build and during tests (as e.g. failsafe tests are
	 * often run in a separate JVM spawned by Maven). So we set our properties in every possible way to make sure the
	 * plugin works out of the box in most situations.
	 */
	private fun setTiaProperty(name: String, value: String?) {
		if (value == null) return
		val fullyQualifiedName = "teamscale.test.impacted.$name"
		log.debug("Setting property $name=$value")
		session.userProperties.setProperty(fullyQualifiedName, value)
		session.systemProperties.setProperty(fullyQualifiedName, value)
		System.setProperty(fullyQualifiedName, value)
	}

	companion object {
		/**
		 * Name of the surefire/failsafe option to pass in
		 * [included engines](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#includeJUnit5Engines)
		 */
		private const val INCLUDE_JUNIT5_ENGINES_OPTION = "includeJUnit5Engines"

		/**
		 * Name of the surefire/failsafe option to pass in
		 * [excluded engines](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#excludejunit5engines)
		 */
		private const val EXCLUDE_JUNIT5_ENGINES_OPTION = "excludeJUnit5Engines"
	}
}
