package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.jacoco.agent.options.ProjectAndCommit;
import com.teamscale.jacoco.agent.upload.DelayedMultiUploaderBase;
import com.teamscale.jacoco.agent.upload.IUploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/** Wrapper for {@link TeamscaleUploader} that allows to upload the same coverage file to multiple Teamscale projects. */
public class DelayedTeamscaleMultiProjectUploader extends DelayedMultiUploaderBase implements IUploader {

	private final BiFunction<String, String, IUploader> uploaderFactory;
	private final List<IUploader> teamscaleUploaders = new ArrayList<>();

	public DelayedTeamscaleMultiProjectUploader(BiFunction<String, String, IUploader> uploaderFactory) {
		this.uploaderFactory = uploaderFactory;
	}

	/** Sets the project and revision detected for the Teamscale project. */
	public void setTeamscaleProjectForRevision(ProjectAndCommit projectAndCommit) {
		IUploader uploader = uploaderFactory.apply(projectAndCommit.getProject(),
				projectAndCommit.getCommitInfo().revision); // TODO
		teamscaleUploaders.add(uploader);
	}

	@Override
	protected Collection<IUploader> getWrappedUploaders() {
		return teamscaleUploaders;
	}
}
