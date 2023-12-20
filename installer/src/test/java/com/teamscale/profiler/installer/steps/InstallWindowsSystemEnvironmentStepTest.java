package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.EnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.Installer;
import com.teamscale.profiler.installer.MockRegistry;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the installation step that sets Windows environment variables. This test mocks the registry so we can test this
 * step on any OS.
 */
class InstallWindowsSystemEnvironmentStepTest {

	private static final TeamscaleCredentials CREDENTIALS = new TeamscaleCredentials(
			HttpUrl.get("http://localhost:" + 8058 + "/"), "user", "accesskey");

	private static final EnvironmentMap ENVIRONMENT = new EnvironmentMap("_JAVA_OPTIONS",
			"\"-javaagent:C:\\Program Files\\foo.jar\"");

	@Test
	void successfulInstall() throws FatalInstallerError {
		MockRegistry registry = new MockRegistry();
		new InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).install(CREDENTIALS);

		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo(ENVIRONMENT.getMap().get("_JAVA_OPTIONS"));
	}

	@Test
	void successfulUninstall() throws FatalInstallerError {
		MockRegistry registry = new MockRegistry();
		Installer.UninstallerErrorReporter errorReporter = new Installer.UninstallerErrorReporter();

		new InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).install(CREDENTIALS);
		new InstallWindowsSystemEnvironmentStep(ENVIRONMENT, registry).uninstall(errorReporter);

		assertThat(errorReporter.wereErrorsReported()).isFalse();
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull();
	}

	@Test
	void addAndRemoveProfiler() throws FatalInstallerError {
		MockRegistry registry = new MockRegistry();

		InstallWindowsSystemEnvironmentStep.addProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:foo.jar");

		InstallWindowsSystemEnvironmentStep.removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNullOrEmpty();
	}

	@Test
	void addAndRemoveProfilerWithPreviousValue() throws FatalInstallerError {
		MockRegistry registry = new MockRegistry();

		registry.setVariable("_JAVA_OPTIONS", "-javaagent:other.jar");
		InstallWindowsSystemEnvironmentStep.addProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:foo.jar -javaagent:other.jar");

		InstallWindowsSystemEnvironmentStep.removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:other.jar");

		// removing it again should do nothing
		InstallWindowsSystemEnvironmentStep.removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:other.jar");
	}

}