package com.teamscale.profiler.installer.windows;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.teamscale.profiler.installer.FatalInstallerError;

/**
 * Accesses the Windows registry.
 */
public class WindowsRegistry implements IRegistry {

	@Override
	public String getHklmValue(String key, String name) throws FatalInstallerError {
		try {
			if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, name)) {
				return null;
			}
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, name);
		} catch (Win32Exception e) {
			throw new FatalInstallerError(
					"Failed to read registry key HKLM\\" + key + ". Try running this installer as Administrator.", e);
		}
	}

	@Override
	public void setHklmValue(String key, String name, String value) throws FatalInstallerError {
		try {
			Advapi32Util.registrySetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, name, value);
		} catch (Win32Exception e) {
			throw new FatalInstallerError(
					"Failed to write registry key HKLM\\" + key + ". Try running this installer as Administrator.", e);
		}
	}

	@Override
	public void deleteHklmValue(String key, String name) throws FatalInstallerError {
		try {
			if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, name)) {
				return;
			}
			Advapi32Util.registryDeleteValue(WinReg.HKEY_LOCAL_MACHINE, key, name);
		} catch (Win32Exception e) {
			throw new FatalInstallerError(
					"Failed to delete registry key HKLM\\" + key + ". Try running this installer as Administrator.", e);
		}
	}
}
