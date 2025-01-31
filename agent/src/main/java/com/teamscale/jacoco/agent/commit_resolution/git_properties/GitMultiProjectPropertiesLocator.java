package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.logging.LoggingUtils;
import com.teamscale.jacoco.agent.options.ProjectAndCommit;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedTeamscaleMultiProjectUploader}. Specifically, this searches for the
 * 'teamscale.project' property specified in each of the discovered 'git.properties' files.
 */
public class GitMultiProjectPropertiesLocator implements IGitPropertiesLocator {

	private final Logger logger = LoggingUtils.getLogger(this);

	private final Executor executor;
	private final DelayedTeamscaleMultiProjectUploader uploader;

	private final boolean recursiveSearch;

	private final @Nullable DateTimeFormatter gitPropertiesCommitTimeFormat;

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader, boolean recursiveSearch, @Nullable DateTimeFormatter gitPropertiesCommitTimeFormat) {
		// using a single threaded executor allows this class to be lock-free
		this(uploader, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitMultiProjectPropertiesLocator.class,
								"git.properties Jar scanner thread")), recursiveSearch, gitPropertiesCommitTimeFormat);
	}

	public GitMultiProjectPropertiesLocator(DelayedTeamscaleMultiProjectUploader uploader, Executor executor,
			boolean recursiveSearch, @Nullable DateTimeFormatter gitPropertiesCommitTimeFormat) {
		this.uploader = uploader;
		this.executor = executor;
		this.recursiveSearch = recursiveSearch;
		this.gitPropertiesCommitTimeFormat = gitPropertiesCommitTimeFormat;
	}

	/**
	 * Asynchronously searches the given jar file for git.properties files and adds a corresponding uploader to the
	 * multi-project uploader.
	 */
	@Override
	public void searchFileForGitPropertiesAsync(File file, boolean isJarFile) {
		executor.execute(() -> searchFile(file, isJarFile));
	}

	/**
	 * Synchronously searches the given jar file for git.properties files and adds a corresponding uploader to the
	 * multi-project uploader.
	 */
	@VisibleForTesting
	void searchFile(File file, boolean isJarFile) {
		logger.debug("Searching file {} for multiple git.properties", file.toString());
		try {
			List<ProjectAndCommit> projectAndCommits = GitPropertiesLocatorUtils.getProjectRevisionsFromGitProperties(
					file,
					isJarFile,
					recursiveSearch, gitPropertiesCommitTimeFormat);
			if (projectAndCommits.isEmpty()) {
				logger.debug("No git.properties file found in {}", file);
				return;
			}

			for (ProjectAndCommit projectAndCommit : projectAndCommits) {
				// this code only runs when 'teamscale-project' is not given via the agent properties,
				// i.e., a multi-project upload is being attempted.
				// Therefore, we expect to find both the project (teamscale.project) and the revision
				// (git.commit.id) in the git.properties file.
				if (projectAndCommit.getProject() == null || projectAndCommit.getCommitInfo() == null) {
					logger.debug(
							"Found inconsistent git.properties file: the git.properties file in {} either does not specify the" +
									" Teamscale project ({}) property, or does not specify the commit " +
									"({}, {} + {}, or {} + {})." +
									" Will skip this git.properties file and try to continue with the other ones that were found during discovery.",
							file, GitPropertiesLocatorUtils.GIT_PROPERTIES_TEAMSCALE_PROJECT,
							GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_ID,
							GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_BRANCH,
							GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_TIME,
							GitPropertiesLocatorUtils.GIT_PROPERTIES_TEAMSCALE_COMMIT_BRANCH,
							GitPropertiesLocatorUtils.GIT_PROPERTIES_TEAMSCALE_COMMIT_TIME);
					continue;
				}
				logger.debug("Found git.properties file in {} and found Teamscale project {} and revision {}", file,
						projectAndCommit.getProject(), projectAndCommit.getCommitInfo());
				uploader.addTeamscaleProjectAndCommit(file, projectAndCommit);
			}
		} catch (IOException | InvalidGitPropertiesException e) {
			logger.error("Error during asynchronous search for git.properties in {}", file, e);
		}
	}

}
