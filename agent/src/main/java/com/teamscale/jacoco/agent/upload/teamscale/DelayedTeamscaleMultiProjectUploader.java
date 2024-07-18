package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.CommitInfo;
import com.teamscale.jacoco.agent.options.ProjectAndCommit;
import com.teamscale.jacoco.agent.upload.DelayedMultiUploaderBase;
import com.teamscale.jacoco.agent.upload.IUploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/** Wrapper for {@link TeamscaleUploader} that allows to upload the same coverage file to multiple Teamscale projects. */
public class DelayedTeamscaleMultiProjectUploader extends DelayedMultiUploaderBase implements IUploader {

	private final BiFunction<String, CommitInfo, IUploader> uploaderFactory;
	private final List<IUploader> teamscaleUploaders = new ArrayList<>();

	public DelayedTeamscaleMultiProjectUploader(BiFunction<String, CommitInfo, IUploader> uploaderFactory) {
		this.uploaderFactory = uploaderFactory;
	}

	/**
	 * Checks if there is already a uploader present with the given project and commit. This is relevant to prevent
	 * uploading coverage multiple times to the same location.
	 */
	public boolean projectAndCommitAlreadyRegistered(ProjectAndCommit projectAndCommit) {
		return teamscaleUploaders.stream().anyMatch(uploader -> {
			TeamscaleUploader teamscaleUploader = (TeamscaleUploader) uploader;
			if (uploader == null) {
				return false;
			}
			TeamscaleServer teamscaleServer = teamscaleUploader.getTeamscaleServer();
			if (!teamscaleServer.project.equals(projectAndCommit.getProject())) {
				return false;
			}

			CommitInfo newCommitInfo = projectAndCommit.getCommitInfo();
			if (newCommitInfo.preferCommitDescriptorOverRevision) {
				return newCommitInfo.commit.equals(teamscaleServer.commit);
			}
			if (newCommitInfo.revision != null) {
				return newCommitInfo.revision.equals(teamscaleServer.revision);
			}
			// Needs to be checked a second time, as it can be that just git.commit.time and git.branch is set,
			// which does not set CommitInfo#preferCommitDescriptorOverRevision.
			return newCommitInfo.commit.equals(teamscaleServer.commit);
		});
	}
	/** Sets the project and revision detected for the Teamscale project. */
	public void setTeamscaleProjectForRevision(ProjectAndCommit projectAndCommit) {
		IUploader uploader = uploaderFactory.apply(projectAndCommit.getProject(),
				projectAndCommit.getCommitInfo());
		teamscaleUploaders.add(uploader);
	}

	@Override
	protected Collection<IUploader> getWrappedUploaders() {
		return teamscaleUploaders;
	}
}
