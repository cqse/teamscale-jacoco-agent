package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import org.conqat.lib.commons.string.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@link ClassFileTransformer} that doesn't change the loaded classes but searches their corresponding Jar/War/Ear/...
 * files for a git.properties file.
 */
public class GitPropertiesLocatingTransformer implements ClassFileTransformer {

	private final Logger logger = LoggingUtils.getLogger(this);
	private final Set<String> seenJars = new ConcurrentSkipListSet<>();
	private final IGitPropertiesLocator locator;
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;

	public GitPropertiesLocatingTransformer(IGitPropertiesLocator locator,
											ClasspathWildcardIncludeFilter locationIncludeFilter) {
		this.locator = locator;
		this.locationIncludeFilter = locationIncludeFilter;
	}

	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> aClass,
							ProtectionDomain protectionDomain, byte[] classFileContent) {

		if (protectionDomain == null) {
			// happens for e.g. java.lang. We can ignore these classes
			return null;
		}

		if (StringUtils.isEmpty(className) || !locationIncludeFilter.isIncluded(className)) {
			// only search in jar files of included classes
			return null;
		}

		try {
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource == null || codeSource.getLocation() == null) {
				// unknown when this can happen, we suspect when code is generated at runtime
				// but there's nothing else we can do here in either case.
				// codeSource.getLocation() is null e.g. when executing Pixelitor with Java14 for class sun/reflect/misc/Trampoline
				logger.debug("Could not locate code source for class {}. Skipping git.properties search for this class", className);
				return null;
			}

			URL jarOrClassFolderUrl = codeSource.getLocation();
			if (hasJarAlreadyBeenSearched(jarOrClassFolderUrl)) {
				return null;
			}

			if (jarOrClassFolderUrl.getProtocol().toLowerCase().equals("file") &&
					StringUtils.endsWithOneOf(
							jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
				logger.debug("Scheduled asynchronous search of jar file {} for git.properties", jarOrClassFolderUrl);
				// we do this asynchronously so that we don't unnecessarily block JVM startup
				locator.searchJarFileForGitPropertiesAsync(new File(jarOrClassFolderUrl.toURI()));
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
