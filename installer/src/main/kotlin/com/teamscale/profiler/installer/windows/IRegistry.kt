package com.teamscale.profiler.installer.windows

import com.teamscale.profiler.installer.FatalInstallerError

/**
 * Abstraction of the Windows registry to make registry usage testable.
 */
interface IRegistry {
	/**
	 * Reads a registry value inside a registry key in HKLM.
	 * Returns null if the value does not exist.
	 */
	@Throws(FatalInstallerError::class)
	fun getHklmValue(key: String, name: String): String?

	/** Sets a registry value inside a registry key in HKLM.  */
	@Throws(FatalInstallerError::class)
	fun setHklmValue(key: String, name: String, value: String)

	/** Deletes a registry value inside a registry key in HKLM.  */
	@Throws(FatalInstallerError::class)
	fun deleteHklmValue(key: String, name: String)
}
