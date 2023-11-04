package com.teamscale.profiler.installer;

import picocli.CommandLine;

import java.util.ResourceBundle;

/**
 * Calculates the version string shown with {@code -V}.
 */
public class VersionProvider implements CommandLine.IVersionProvider {

	private static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("com.teamscale.profiler.installer.app");
		VERSION = bundle.getString("version");
	}

	@Override
	public String[] getVersion() {
		return new String[]{"Teamscale profiler installer version " + VERSION};
	}
}