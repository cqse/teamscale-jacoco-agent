package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedTeamscaleMultiProjectUploader}. Specifically, this searches for the
 * 'teamscale.project' property specified in each of the discovered 'git.properties' files.
 */
public class GitMultiProjectPropertiesLocator implements IGitPropertiesLocator {

	private final Logger logger = LoggingUtils.getLogger(GitSingleProjectPropertiesLocator.class);

	private final Executor executor;
	private final DelayedTeamscaleMultiProjectUploader uploader;

	private final boolean recursiveSearch;

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader, boolean recursiveSearch) {
		// using a single threaded executor allows this class to be lock-free
		this(uploader, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitMultiProjectPropertiesLocator.class,
								"git.properties Jar scanner thread")), recursiveSearch);
	}

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader, Executor executor,
											boolean recursiveSearch) {
		this.uploader = uploader;
		this.executor = executor;
		this.recursiveSearch = recursiveSearch;
	}

	/**
	 * Asynchronously searches the given jar file for a git.properties file.
	 */
	@Override
	public void searchFileForGitPropertiesAsync(File file, boolean isJarFile) {
		executor.execute(() -> searchFile(file, isJarFile));
	}

	private void searchFile(File file, boolean isJarFile) {
		logger.debug("Searching file {} for multiple git.properties", file.toString());
		try {
			List<ProjectRevision> projectRevisions = GitPropertiesLocatorUtils.getProjectRevisionsFromGitProperties(
					file,
					isJarFile,
					recursiveSearch);
			if (projectRevisions.isEmpty()) {
				logger.debug("No git.properties file found in {}", file);
				return;
			}
			// this code only runs when 'teamscale-project' is not given via the agent properties,
			// i.e., a multi-project upload is being attempted.
			// Therefore, we expect to find both the project (teamscale.project) and the revision
			// (git.commit.id) in the git.properties file.

			for (ProjectRevision projectRevision : projectRevisions) {
				if (projectRevision.getProject() == null || projectRevision.getRevision() == null) {
					logger.error(
							"Found inconsistent git.properties file: the git.properties file in {} either does not specify the" +
									" Teamscale project (teamscale.project) property, or does not specify the commit SHA (git.commit.id)." +
									" Please note that both of these properties are required in order to allow multi-project upload to Teamscale.",
							file);
					return;
				}
				uploader.setTeamscaleProjectForRevision(projectRevision);
				logger.debug("Found git.properties file in {} and found Teamscale project {} and revision {}", file,
						projectRevision.getProject(), projectRevision.getRevision());
			}
		} catch (IOException | InvalidGitPropertiesException e) {
			logger.error("Error during asynchronous search for git.properties in {}", file, e);
		}
	}

}
