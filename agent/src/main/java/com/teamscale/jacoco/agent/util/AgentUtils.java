package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.PreMain;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

/** General utilities for working with the agent. */
public class AgentUtils {

	/** Version of this program. */
	public static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("com.teamscale.jacoco.agent.app");
		VERSION = bundle.getString("version");
	}

	/** Returns the directory that contains the agent or null if it can't be resolved. */
	public static Path getAgentDirectory() {
		try {
			URI jarFileUri = PreMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the agent jar not moved
			return Paths.get(jarFileUri).getParent().getParent();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to obtain agent directory. This is a bug, please report it.", e);
		}
	}

}
