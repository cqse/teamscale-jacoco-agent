package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.jacoco.agent.commit_resolution.git_properties.CommitInfo;
import com.teamscale.jacoco.agent.upload.DelayedMultiUploaderBase;
import com.teamscale.jacoco.agent.upload.IUploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/** Wrapper for {@link ArtifactoryUploader} that allows to upload the same coverage file to multiple Artifactory paths. */
public class ArtifactoryMultiUploader extends DelayedMultiUploaderBase implements IUploader  {

	private final List<ArtifactoryUploader> artifactoryUploaders = new ArrayList<>();

	public void addArtifactoryUploader(ArtifactoryUploader newUploader) {
		artifactoryUploaders.add(newUploader);
	}

	public List<ArtifactoryUploader> getArtifactoryUploaders() {
		return artifactoryUploaders;
	}

	@Override
	protected Collection<IUploader> getWrappedUploaders() {
		return new ArrayList<>(artifactoryUploaders);
	}
}