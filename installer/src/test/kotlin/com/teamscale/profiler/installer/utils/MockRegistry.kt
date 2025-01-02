package com.teamscale.profiler.installer.utils

import com.teamscale.profiler.installer.windows.IRegistry
import com.teamscale.profiler.installer.windows.WindowsRegistry

/**
 * Mock of [IRegistry] to allow tests without actually changing the Windows registry.
 */
class MockRegistry : IRegistry {
	private val values = mutableMapOf<String, String>()

	override fun getHklmValue(key: String, name: String) = values["$key\\$name"]

	override fun setHklmValue(key: String, name: String, value: String) {
		values["$key\\$name"] = value
	}

	override fun deleteHklmValue(key: String, name: String) {
		values.remove("$key\\$name")
	}

	/** Reads the given environment variable from the registry.  */
	fun getVariable(name: String) =
		getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name)

	/** Sets the given environment variable in the registry.  */
	fun setVariable(name: String, value: String) {
		setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, name, value)
	}
}
