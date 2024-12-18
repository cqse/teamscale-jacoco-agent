package com.teamscale.profiler.installer.steps

import com.teamscale.profiler.installer.*
import com.teamscale.profiler.installer.steps.IStep.IUninstallErrorReporter
import com.teamscale.profiler.installer.windows.IRegistry
import com.teamscale.profiler.installer.windows.WindowsRegistry
import org.apache.commons.lang3.SystemUtils

/** On Windows, registers the agent globally via environment variables set for the entire machine.  */
class InstallWindowsSystemEnvironmentStep(
	private val environmentVariables: JvmEnvironmentMap,
	private val registry: IRegistry
) : IStep {
	override fun shouldRun() = SystemUtils.IS_OS_WINDOWS

	@Throws(FatalInstallerError::class)
	override fun install(credentials: TeamscaleCredentials) {
		environmentVariables.environmentVariableMap.forEach { (key, value) ->
			registry.addProfiler(key, value)
		}
	}

	override fun uninstall(errorReporter: IUninstallErrorReporter) {
		environmentVariables.environmentVariableMap.forEach { (key, value) ->
			try {
				registry.removeProfiler(key, value)
			} catch (e: FatalInstallerError) {
				errorReporter.report(e)
			}
		}
	}

	companion object {
		/**
		 * Adds the profiler to the given registry under the given variable, appending it in case the variable already has a
		 * value set.
		 */
		@Throws(FatalInstallerError::class)
		fun IRegistry.addProfiler(variable: String, valueToAdd: String?) {
			if (valueToAdd.isNullOrBlank()) return
			val currentValue = getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable)

			var newValue = valueToAdd
			if (!currentValue.isNullOrBlank()) {
				newValue = "$valueToAdd $currentValue"
			}
			setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable, newValue)
		}

		/**
		 * Removes the profiler from the given registry under the given variable, leaving any other parts of the variable in
		 * place.
		 */
		@Throws(FatalInstallerError::class)
		fun IRegistry.removeProfiler(
			variable: String,
			removal: String?
		) {
			if (removal.isNullOrBlank()) return

			var valueToRemove = removal
			val currentValue = getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable)
			if (currentValue.isNullOrBlank()) return

			if (currentValue == valueToRemove) {
				deleteHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable)
				return
			}

			if (!currentValue.contains(valueToRemove)) {
				return
			}

			if (currentValue.contains("$valueToRemove ")) {
				valueToRemove += " "
			}
			val newValue = currentValue.replace(valueToRemove, "")
			setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, variable, newValue)
		}
	}
}
