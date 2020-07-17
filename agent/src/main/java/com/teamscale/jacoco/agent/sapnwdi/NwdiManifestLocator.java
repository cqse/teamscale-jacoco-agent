package com.teamscale.jacoco.agent.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.delay.DelayedSapNwdiMultiUploader;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedSapNwdiMultiUploader}. This will only start uploading if a marker class for each
 * configured application has been loaded.
 */
public class NwdiManifestLocator {

	/** Name of the Manifest file. */
	private static final DateTimeFormatter IMPLEMENTATION_VERSION_DATE_FORMAT = DateTimeFormatter
			.ofPattern("uuuuMMddHHmmss");

	private final Logger logger = LoggingUtils.getLogger(getClass());

	private final DelayedSapNwdiMultiUploader store;

	public NwdiManifestLocator(DelayedSapNwdiMultiUploader store) {
		this.store = store;
	}

	/** Searches the jar file of the given application for a commit timestamp in the jar's manifest. */
	protected void searchJarFile(File jarFile, SapNwdiApplications.SapNwdiApplication application) {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			CommitDescriptor commit = getCommitFromManifest(jarStream, jarFile);
			logger.debug("Found MANIFEST.MF file in {} and found commit descriptor {}", jarFile.toString(),
					commit);
			store.setCommitForApplication(commit, application);
		} catch (IOException | NwdiManifestException e) {
			logger.error("Error during asynchronous search for git.properties in {}", jarFile.toString(), e);
		}
	}

	/**
	 * Reads the `Implementation-Version` entry from the given EAR file's manifest and builds a commit descriptor out of
	 * it.
	 */
	static CommitDescriptor getCommitFromManifest(JarInputStream jarStream, File jarFile) throws NwdiManifestException {
		Manifest manifest = jarStream.getManifest();
		if (manifest == null) {
			throw new NwdiManifestException(
					"Unable to read manifest from " + jarFile + ". Maybe the manifest is corrupt?");
		}
		String timestampString = manifest.getMainAttributes().getValue("Implementation-Version");
		if (StringUtils.isEmpty(timestampString)) {
			throw new NwdiManifestException("No entry 'Implementation-Version' in MANIFEST");
		}
		long timestamp = LocalDateTime.parse(timestampString, IMPLEMENTATION_VERSION_DATE_FORMAT)
				.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
		return new CommitDescriptor("master", timestamp);
	}
}
