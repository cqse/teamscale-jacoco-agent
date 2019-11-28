package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.slf4j.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class GitPropertiesLocatingTransformer implements ClassFileTransformer {

	private final Logger logger = LoggingUtils.getLogger(this);
	private final Set<String> seenJars = new ConcurrentSkipListSet<>();

	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> aClass,
							ProtectionDomain protectionDomain, byte[] classFileContent) {

		if (protectionDomain == null) {
			// happens for e.g. java.lang. We can ignore these classes
			return null;
		}

		try {
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource == null) {
				// unknown when this can happen, but we need to be safe and there's
				// nothing else we can do here
				return null;
			}

			URL jarOrClassFolderUrl = codeSource.getLocation();
			if (hasJarAlreadyBeenSearched(jarOrClassFolderUrl)) {
				return null;
			}

			if (jarOrClassFolderUrl.getProtocol().toLowerCase().equals("file") &&
					StringUtils.endsWithOneOf(
							jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
				// TODO (FS) search
			}
		} catch (Throwable e) {
			// we catch Throwable to be sure that we log all errors as anything thrown from this method is
			// silently discarded by the JVM
			logger.error("Failed to process class {} in search of git.properties", className, e);
		}
		return null;
	}

	private boolean hasJarAlreadyBeenSearched(URL jarOrClassFolderUrl) {
		return !seenJars.add(jarOrClassFolderUrl.toString());
	}

}
