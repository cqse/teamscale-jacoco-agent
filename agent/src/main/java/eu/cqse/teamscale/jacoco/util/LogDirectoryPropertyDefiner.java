package eu.cqse.teamscale.jacoco.util;

import ch.qos.logback.core.PropertyDefinerBase;
import eu.cqse.teamscale.jacoco.agent.PreMain;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class LogDirectoryPropertyDefiner extends PropertyDefinerBase {
	@Override
	public String getPropertyValue() {
		try {
			URI jarFileUri = PreMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the agent jar not moved
			// Then the log dir should be next to the bin/ dir
			return Paths.get(jarFileUri).getParent().getParent().resolve("logs").toAbsolutePath().toString();
		} catch (URISyntaxException e) {
			// we can't log the exception yet since logging is not yet initialized
			// fall back to the working directory
			return Paths.get(".").toAbsolutePath().toString();
		}
	}
}
