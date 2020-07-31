package com.teamscale.jacoco.agent.commit_resolution.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.options.sapnwdi.DelayedSapNwdiMultiUploader;
import com.teamscale.jacoco.agent.options.sapnwdi.SapNwdiApplications;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import org.conqat.lib.commons.string.StringUtils;
import org.slf4j.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
	private final DelayedSapNwdiMultiUploader store;
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;
	private final Map<String, SapNwdiApplications.SapNwdiApplication> markerClassesToApplications;

	public NwdiManifestLocatingTransformer(
			DelayedSapNwdiMultiUploader store,
			ClasspathWildcardIncludeFilter locationIncludeFilter,
			Collection<SapNwdiApplications.SapNwdiApplication> apps) {
		this.store = store;
		this.locationIncludeFilter = locationIncludeFilter;
		this.markerClassesToApplications = apps.stream().collect(
				Collectors.toMap(sapNwdiApplication -> sapNwdiApplication.getMarkerClass().replace('.', '/'),
						application -> application));
	}

	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> aClass,
							ProtectionDomain protectionDomain, byte[] classFileContent) {
		if (className == null || !className.startsWith("eu")) {
			return null;
		}
		logger.info(
				"Found " + className + " protection avail " + (protectionDomain != null) + " " + classFileContent.length);

		if (protectionDomain == null) {
			// happens for e.g. java.lang. We can ignore these classes
			return null;
		}

		if (StringUtils.isEmpty(className) || !locationIncludeFilter.isIncluded(className)) {
			// only search in jar files of included classes
			logger.info("class not matched!");
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
			logger.debug("Found " + className + " in " + jarOrClassFolderUrl);

			if (jarOrClassFolderUrl.getProtocol().toLowerCase().equals("file")) {
				Path file = Paths.get(jarOrClassFolderUrl.toURI());
				BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
				SapNwdiApplications.SapNwdiApplication application = markerClassesToApplications.get(className);
				CommitDescriptor commitDescriptor = new CommitDescriptor("master", attr.lastModifiedTime().toMillis());
				store.setCommitForApplication(commitDescriptor, application);
			}
		} catch (Throwable e) {
			// we catch Throwable to be sure that we log all errors as anything thrown from this method is
			// silently discarded by the JVM
			logger.error("Failed to process class {} in search of git.properties", className, e);
		}
		return null;
	}
}
