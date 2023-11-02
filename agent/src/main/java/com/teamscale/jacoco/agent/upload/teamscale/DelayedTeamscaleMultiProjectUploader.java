package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.upload.DelayedMultiUploaderBase;

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
	public void setTeamscaleProjectForRevision(ProjectRevision projectRevision) {
		IUploader uploader = uploaderFactory.apply(projectRevision.getProject(), projectRevision.getRevision());
		teamscaleUploaders.add(uploader);
	}

	@Override
	protected Collection<IUploader> getWrappedUploaders() {
		return teamscaleUploaders;
	}
}
