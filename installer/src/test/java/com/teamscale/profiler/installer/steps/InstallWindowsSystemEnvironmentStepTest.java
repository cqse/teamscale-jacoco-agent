package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.EnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.Installer;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import com.teamscale.profiler.installer.windows.IRegistry;
import com.teamscale.profiler.installer.windows.WindowsRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the installation step that sets Windows environment variables. This test mocks the registry so we can test this
 * step on any OS.
 */
class InstallWindowsSystemEnvironmentStepTest {

	private static final TeamscaleCredentials CREDENTIALS = new TeamscaleCredentials(
			HttpUrl.get("http://localhost:" + 8058 + "/"), "user", "accesskey");

	private static final EnvironmentMap environment = new EnvironmentMap("_JAVA_OPTIONS",
			"\"-javaagent:C:\\Program Files\\foo.jar\"");

	private static class FakeRegistry implements IRegistry {

		private final Map<String, String> values = new HashMap<>();

		@Override
		public String getHklmValue(String key, String name) {
			return values.get(key + "\\" + name);
		}

		@Override
		public void setHklmValue(String key, String name, String value) {
			values.put(key + "\\" + name, value);
		}

		@Override
		public void deleteHklmValue(String key, String name) {
			values.remove(key + "\\" + name);
		}

		public String getVariable(String name) {
			return getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name);
		}

		public void setVariable(String name, String value) {
			setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name, value);
		}
	}

	@Test
	void successfulInstall() throws FatalInstallerError {
		FakeRegistry registry = new FakeRegistry();
		new InstallWindowsSystemEnvironmentStep(environment, registry).install(CREDENTIALS);

		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo(environment.getMap().get("_JAVA_OPTIONS"));
	}

	@Test
	void successfulUninstall() throws FatalInstallerError {
		FakeRegistry registry = new FakeRegistry();
		Installer.UninstallerErrorReporter errorReporter = new Installer.UninstallerErrorReporter();

		new InstallWindowsSystemEnvironmentStep(environment, registry).install(CREDENTIALS);
		new InstallWindowsSystemEnvironmentStep(environment, registry).uninstall(errorReporter);

		assertThat(errorReporter.wereErrorsReported()).isFalse();
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull();
	}

	@Test
	void addAndRemoveProfiler() throws FatalInstallerError {
		FakeRegistry registry = new FakeRegistry();

		InstallWindowsSystemEnvironmentStep.addProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:foo.jar");

		InstallWindowsSystemEnvironmentStep.removeProfiler("_JAVA_OPTIONS", "-javaagent:foo.jar", registry);
		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNullOrEmpty();
	}

	@Test
	void addAndRemoveProfilerWithPreviousValue() throws FatalInstallerError {
		FakeRegistry registry = new FakeRegistry();

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