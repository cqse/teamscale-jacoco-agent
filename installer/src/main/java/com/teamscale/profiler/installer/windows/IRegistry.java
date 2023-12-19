package com.teamscale.profiler.installer.windows;

import com.teamscale.profiler.installer.FatalInstallerError;

/**
 * Abstraction of the Windows registry to make registry usage testable.
 */
public interface IRegistry {

	/**
	 * Reads a registry value inside a registry key in HKLM.
	 * Returns null if the value does not exist.
	 */
	String getHklmValue(String key, String name) throws FatalInstallerError;

	/** Sets a registry value inside a registry key in HKLM. */
	void setHklmValue(String key, String name, String value) throws FatalInstallerError;

	/** Deletes a registry value inside a registry key in HKLM. */
	void deleteHklmValue(String key, String name) throws FatalInstallerError;

}
