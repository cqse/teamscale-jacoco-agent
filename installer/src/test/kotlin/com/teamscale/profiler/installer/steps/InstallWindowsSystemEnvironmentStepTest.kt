package com.teamscale.profiler.installer.steps

import com.teamscale.profiler.installer.FatalInstallerError
import com.teamscale.profiler.installer.Installer.UninstallerErrorReporter
import com.teamscale.profiler.installer.JvmEnvironmentMap
import com.teamscale.profiler.installer.TeamscaleCredentials
import com.teamscale.profiler.installer.steps.InstallWindowsSystemEnvironmentStep.Companion.addProfiler
import com.teamscale.profiler.installer.steps.InstallWindowsSystemEnvironmentStep.Companion.removeProfiler
import com.teamscale.profiler.installer.utils.MockRegistry
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests the installation step that sets Windows environment variables. This test mocks the registry so we can test this
 * step on any OS.
 */
internal class InstallWindowsSystemEnvironmentStepTest {
	@Test
	@Throws(FatalInstallerError::class)
	fun successfulInstall() {
		val registry = MockRegistry()
		InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).install(CREDENTIALS)

		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS"))
			.isEqualTo("\"-javaagent:C:\\Program Files\\foo.jar\"")
		Assertions.assertThat(registry.getVariable("JAVA_TOOL_OPTIONS")).isEqualTo("-javaagent:C:\\Programs\\foo.jar")
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun successfulUninstall() {
		val registry = MockRegistry()
		val errorReporter = UninstallerErrorReporter()

		InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).install(CREDENTIALS)
		InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).uninstall(errorReporter)

		Assertions.assertThat(errorReporter.wereErrorsReported()).isFalse()
		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull()
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun addAndRemoveProfiler() {
		val registry = MockRegistry()

		registry.addProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar")
		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:foo.jar")

		registry.removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar")
		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS")).isNullOrEmpty()
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun addAndRemoveProfilerWithPreviousValue() {
		MockRegistry().apply {
			setVariable("_JAVA_OPTIONS", "-javaagent:other.jar")
			addProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar")
			Assertions.assertThat(getVariable("_JAVA_OPTIONS"))
				.isEqualTo("-javaagent:foo.jar -javaagent:other.jar")

			removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar")
			Assertions.assertThat(getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:other.jar")

			// removing it again should do nothing
			removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar")
			Assertions.assertThat(getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:other.jar")
		}
	}

	companion object {
		private val CREDENTIALS = TeamscaleCredentials(
			"http://localhost:8058/".toHttpUrl(), "user", "accesskey"
		)

		private val ENVIRONMENT = JvmEnvironmentMap(
			"_JAVA_OPTIONS", "-javaagent:C:\\Program Files\\foo.jar",
			"JAVA_TOOL_OPTIONS", "-javaagent:C:\\Programs\\foo.jar"
		)
	}
}