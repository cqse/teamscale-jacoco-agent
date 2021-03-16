package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedTeamscaleMultiProjectUploader}.
 * Specifically, this searches for the teamscale-project property specified in each of the discovered git.properties
 * files.
 */
public class GitMultiProjectPropertiesLocator implements IGitPropertiesLocator {

	private final Logger logger = LoggingUtils.getLogger(GitPropertiesLocator.class);

	private final Executor executor;
	private final DelayedTeamscaleMultiProjectUploader uploader;

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader) {
		// using a single threaded executor allows this class to be lock-free
		this(uploader, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitMultiProjectPropertiesLocator.class,
								"git.properties Jar scanner thread")));
	}

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader, Executor executor) {
		this.uploader = uploader;
		this.executor = executor;
	}

	/**
	 * Asynchronously searches the given jar file for a git.properties file.
	 */
	@Override
	public void searchJarFileForGitPropertiesAsync(File jarFile) {
		executor.execute(() -> searchJarFile(jarFile));
	}

	private void searchJarFile(File jarFile) {
		try {
			ProjectRevision data = GitPropertiesLocatorUtils.getProjectRevisionFromGitProperties(jarFile);
			if (data == null) {
				logger.debug("No git.properties file found in {}", jarFile.toString());
				return;
			}
			if (data.getProject() == null || data.getRevision() == null) {
				logger.error(
						"Found inconsistent git.properties file: the git.properties file in {} either does not specify the" +
								" Teamscale project (teamscale-project) property, or does not specify the commit SHA (git.commit.id)." +
								" Please note that both of these properties are required in order to allow multi-project upload to Teamscale.",
						jarFile);
				return;
			}
			uploader.setTeamscaleProjectForRevision(data);
			logger.debug("Found git.properties file in {} and found Teamscale project {} and revision {}", jarFile.toString(),
					data.getProject(), data.getRevision());
		} catch (IOException | InvalidGitPropertiesException e) {
			logger.error("Error during asynchronous search for git.properties in {}", jarFile.toString(), e);
		}
	}

}
