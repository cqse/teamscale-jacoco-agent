package com.teamscale.jacoco.agent.commit_resolution.sapnwdi;

import com.teamscale.jacoco.agent.options.sapnwdi.SapNwdiApplications;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import org.conqat.lib.commons.string.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link ClassFileTransformer} that doesn't change the loaded classes but searches their corresponding Jar/War/Ear/...
 * files for a git.properties file.
 */
public class NwdiManifestLocatingTransformer implements ClassFileTransformer {

	private final Logger logger = LoggingUtils.getLogger(this);
	private final NwdiManifestLocator locator;
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;
	private final Map<String, SapNwdiApplications.SapNwdiApplication> markerClassesToApplications;

	public NwdiManifestLocatingTransformer(NwdiManifestLocator locator,
										   ClasspathWildcardIncludeFilter locationIncludeFilter,
										   Collection<SapNwdiApplications.SapNwdiApplication> apps) {
		this.locator = locator;
		this.locationIncludeFilter = locationIncludeFilter;
		this.markerClassesToApplications = apps.stream().collect(
				Collectors.toMap(SapNwdiApplications.SapNwdiApplication::getMarkerClass, application -> application));
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

		if (!this.markerClassesToApplications.containsKey(className)) {
			// only kick off search if the marker class was found.
			return null;
		}

		try {
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource == null) {
				// unknown when this can happen, we suspect when code is generated at runtime
				// but there's nothing else we can do here in either case
				return null;
			}

			URL jarOrClassFolderUrl = codeSource.getLocation();

			//TODO in which file should we search for the manifest?
			if (jarOrClassFolderUrl.getProtocol().toLowerCase().equals("file") &&
					StringUtils.endsWithOneOf(
							jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
				locator.searchJarFile(new File(jarOrClassFolderUrl.toURI()), markerClassesToApplications.get(className));
			}
		} catch (Throwable e) {
			// we catch Throwable to be sure that we log all errors as anything thrown from this method is
			// silently discarded by the JVM
			logger.error("Failed to process class {} in search of git.properties", className, e);
		}
		return null;
	}
}