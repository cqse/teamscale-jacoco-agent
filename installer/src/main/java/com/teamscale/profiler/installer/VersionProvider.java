package com.teamscale.profiler.installer;

import picocli.CommandLine;

import java.util.ResourceBundle;

public class VersionProvider implements CommandLine.IVersionProvider {

	private static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("com.teamscale.profiler.installer.app");
		VERSION = bundle.getString("version");
	}

	public String[] getVersion() {
		return new String[]{"Teamscale profiler installer version " + VERSION};
	}
}