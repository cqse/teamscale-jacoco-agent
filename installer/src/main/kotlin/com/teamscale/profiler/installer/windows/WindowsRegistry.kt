package com.teamscale.profiler.installer.windows

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinReg
import com.teamscale.profiler.installer.FatalInstallerError

/**
 * Accesses the Windows registry.
 */
object WindowsRegistry : IRegistry {
	/** The key under which machine-global environment variables are stored.  */
	const val ENVIRONMENT_REGISTRY_KEY = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment"

	@Throws(FatalInstallerError::class)
	override fun getHklmValue(key: String, name: String): String? {
		try {
			if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, name)) {
				return null
			}
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, name)
		} catch (e: Win32Exception) {
			throw FatalInstallerError(
				"Failed to read registry key HKLM\\$key. Try running this installer as Administrator.", e
			)
		}
	}

	@Throws(FatalInstallerError::class)
	override fun setHklmValue(key: String, name: String, value: String) {
		try {
			Advapi32Util.registrySetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, name, value)
		} catch (e: Win32Exception) {
			throw FatalInstallerError(
				"Failed to write registry key HKLM\\$key. Try running this installer as Administrator.", e
			)
		}
	}

	@Throws(FatalInstallerError::class)
	override fun deleteHklmValue(key: String, name: String) {
		try {
			if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, name)) {
				return
			}
			Advapi32Util.registryDeleteValue(WinReg.HKEY_LOCAL_MACHINE, key, name)
		} catch (e: Win32Exception) {
			throw FatalInstallerError(
				"Failed to delete registry key HKLM\\$key. Try running this installer as Administrator.", e
			)
		}
	}
}
