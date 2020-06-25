package com.teamscale.jacoco.agent.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.git_properties.AysncInfoFileLocator;
import com.teamscale.jacoco.agent.git_properties.InvalidGitPropertiesException;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.upload.delay.DelayedCommitDescriptorUploader;
import com.teamscale.jacoco.agent.upload.delay.DelayedNwdiUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedNwdiUploader}. This will only start uploading if a marker class for each
 * configured application has been loaded.
 */
public class NwdiManifestLocator extends AysncInfoFileLocator {

	/** Name of the Manifest file. */
	private static final String GIT_PROPERTIES_FILE_NAME = "MANIFEST.MF";

	private final Logger logger = LoggingUtils.getLogger(getClass());

	private final NwdiConfiguration config;

	public NwdiManifestLocator(DelayedNwdiUploader store, NwdiConfiguration config) {
		super(store);
		this.config = config;
	}

	@Override
	protected void searchJarFile(File jarFile) {
		try (JarInputStream jarStream = new JarInputStream(new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			CommitDescriptor commit = getCommitFromManifest(jarStream, jarFile);
			if (commit == null) {
				logger.debug("No MANIFEST.MF file found in {}", jarFile.toString());
				return;
			}

			for (NwdiConfiguration.NwdiApplication application : config.getApplications()) {
				if(hasMarkerClass(jarStream, jarFile, application.getMarkerClass())) {
					if (application.getFoundTimestamp() != null) {
						if (!application.getFoundTimestamp().equals(commit)) {
							logger.error(
									"Found inconsistent git.properties files: {} contained SHA1 {} while {} contained {}." +
											" Please ensure that all git.properties files of your application are consistent." +
											" Otherwise, you may" +
											" be uploading to the wrong commit which will result in incorrect coverage data" +
											" displayed in Teamscale. If you cannot fix the inconsistency, you can manually" +
											" specify a Jar/War/Ear/... file from which to read the correct git.properties" +
											" file with the agent's teamscale-git-properties-jar parameter.",
									application.getFoundJarFile(), application.getFoundTimestamp(), jarFile, commit);
						}
						return;
					}
				}
				logger.debug("Found git.properties file in {} and found commit descriptor {}", jarFile.toString(),
						commit);
				application.setFoundTimestamp(commit, jarFile.getAbsolutePath());
			}

			store.setCommitAndTriggerAsynchronousUpload(revision);
		} catch (IOException | NwdiManifestException e) {
			logger.error("Error during asynchronous search for git.properties in {}", jarFile.toString(), e);
		}
	}

	private boolean hasMarkerClass(JarInputStream jarStream, File jarFile, String markerClass) throws IOException {
		JarEntry entry = jarStream.getNextJarEntry();
		while (entry != null) {
			if (Paths.get(entry.getName()).getFileName().toString().toLowerCase().equals(GIT_PROPERTIES_FILE_NAME)) {
				Properties gitProperties = new Properties();
				gitProperties.load(jarStream);
				return parseGitPropertiesJarEntry(entry.getName(), gitProperties, jarFile);
			}
			entry = jarStream.getNextJarEntry();
		}

		return false;
	}

	/**
	 * Reads the `Implementation-Version` entry from the given EAR file's manifest and builds a commit descriptor out
	 * of it.
	 */
	static CommitDescriptor getCommitFromManifest(JarInputStream jarStream, File jarFile) throws NwdiManifestException {
			Manifest manifest = jarStream.getManifest();
			if (manifest == null) {
				throw new NwdiManifestException(
						"Unable to read manifest from " + jarFile + ". Maybe the manifest is corrupt?");
			}
			String timestamp = manifest.getMainAttributes().getValue("Implementation-Version");
			if (StringUtils.isEmpty(timestamp)) {
				throw new NwdiManifestException("No entry 'Implementation-Version' in MANIFEST");
			}
			// TODO Transform Timestamp from yyyyMMddHHmmss to Teamscale format
			return new CommitDescriptor("master", timestamp);

	}
}
