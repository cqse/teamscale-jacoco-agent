package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.windows.IRegistry;
import com.teamscale.profiler.installer.windows.WindowsRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock of {@link IRegistry} to allow tests without actually changing the Windows registry.
 */
public class MockRegistry implements IRegistry {

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

	/** Reads the given environment variable from the registry. */
	public String getVariable(String name) {
		return getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name);
	}

	/** Sets the given environment variable in the registry. */
	public void setVariable(String name, String value) {
		setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name, value);
	}
}
