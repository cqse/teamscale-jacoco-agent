package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.EnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import com.teamscale.profiler.installer.windows.IRegistry;
import com.teamscale.profiler.installer.windows.WindowsRegistry;
import org.conqat.lib.commons.string.StringUtils;
import org.conqat.lib.commons.system.SystemUtils;

import java.util.Map;

/** On Windows, registers the agent globally via environment variables set for the entire machine. */
public class InstallWindowsSystemEnvironmentStep implements IStep {

	private final EnvironmentMap environmentVariables;
	private final IRegistry registry;

	public InstallWindowsSystemEnvironmentStep(EnvironmentMap environmentMap, IRegistry registry) {
		this.environmentVariables = environmentMap;
		this.registry = registry;
	}

	@Override
	public boolean shouldRun() {
		return SystemUtils.isWindows();
	}

	@Override
	public void install(TeamscaleCredentials credentials) throws FatalInstallerError {
		Map<String, String> map = environmentVariables.getMap();
		for (String variable : map.keySet()) {
			addProfiler(variable, map.get(variable), registry);
		}
	}

	@Override
	public void uninstall(IUninstallErrorReporter errorReporter) {
		Map<String, String> map = environmentVariables.getMap();
		for (String variable : map.keySet()) {
			try {
				String valueToRemove = map.get(variable);
				removeProfiler(variable, valueToRemove, registry);
			} catch (FatalInstallerError e) {
				errorReporter.report(e);
			}
		}
	}

	/**
	 * Adds the profiler to the given registry under the given variable, appending it in case the variable already has a
	 * value set.
	 */
	/*package*/
	static void addProfiler(String variable, String valueToAdd, IRegistry registry) throws FatalInstallerError {
		String currentValue = registry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable);

		String newValue = valueToAdd;
		if (!StringUtils.isEmpty(currentValue)) {
			newValue = valueToAdd + " " + currentValue;
		}
		registry.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable, newValue);
	}

	/**
	 * Removes the profiler from the given registry under the given variable, leaving any other parts of the variable in
	 * place.
	 */
	/*package*/
	static void removeProfiler(String variable, String valueToRemove,
							   IRegistry registry) throws FatalInstallerError {
		String currentValue = registry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable);
		if (StringUtils.isEmpty(currentValue)) {
			return;
		}

		if (currentValue.equals(valueToRemove)) {
			registry.deleteHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable);
			return;
		}

		if (!currentValue.contains(valueToRemove)) {
			return;
		}

		if (currentValue.contains(valueToRemove + " ")) {
			valueToRemove += " ";
		}
		String newValue = currentValue.replace(valueToRemove, "");
		registry.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable, newValue);
	}
}
