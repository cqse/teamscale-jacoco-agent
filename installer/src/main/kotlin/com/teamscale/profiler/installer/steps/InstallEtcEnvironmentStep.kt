package com.teamscale.profiler.installer.steps

import com.teamscale.profiler.installer.*
import com.teamscale.profiler.installer.steps.IStep.IUninstallErrorReporter
import org.apache.commons.lang3.SystemUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

/** On Linux, registers the agent globally via environment variables set in /etc/environment  */
class InstallEtcEnvironmentStep(
	private val etcDirectory: Path,
	private val environmentVariables: JvmEnvironmentMap
) : IStep {
	override fun shouldRun() = SystemUtils.IS_OS_LINUX

	@Throws(FatalInstallerError::class)
	override fun install(credentials: TeamscaleCredentials) {
		val environmentFile = environmentFile
		val etcEnvironmentAddition = java.lang.String.join("\n", environmentVariables.etcEnvironmentLinesList)

		if (!Files.exists(environmentFile)) {
			System.err.println(
				"""$environmentFile does not exist. Skipping system-wide registration of the profiler.
				You need to manually register the profiler for process that should be profiled by setting the following environment variables:
				
				$etcEnvironmentAddition
				""".trimIndent()
			)
			return
		}

		val content = """
			$etcEnvironmentAddition
			""".trimIndent()
		try {
			Files.writeString(
				environmentFile, content, StandardCharsets.US_ASCII,
				StandardOpenOption.APPEND
			)
		} catch (e: IOException) {
			throw PermissionError("Could not change contents of $environmentFile", e)
		}
	}

	private val environmentFile: Path
		get() = etcDirectory.resolve("environment")

	override fun uninstall(errorReporter: IUninstallErrorReporter) {
		val environmentFile = environmentFile
		if (!Files.exists(environmentFile)) {
			return
		}

		try {
			val lines = Files.readAllLines(environmentFile, StandardCharsets.US_ASCII)
			val newContent = removeProfilerVariables(lines)
			Files.writeString(environmentFile, newContent, StandardCharsets.US_ASCII)
		} catch (e: IOException) {
			errorReporter.report(
				PermissionError(
					"Failed to remove profiler from " + environmentFile + "." +
							" Please remove the relevant environment variables yourself." +
							" Otherwise, Java applications may crash.", e
				)
			)
		}
	}

	private fun removeProfilerVariables(linesWithoutNewline: List<String>) =
		linesWithoutNewline.filter { line ->
			!environmentVariables.etcEnvironmentLinesList.contains(line)
		}.joinToString("\n")
}
