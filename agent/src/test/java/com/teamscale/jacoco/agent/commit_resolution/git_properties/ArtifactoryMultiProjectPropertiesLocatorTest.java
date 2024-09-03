package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryMultiUploader;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArtifactoryMultiProjectPropertiesLocatorTest {

	@Test
	public void findsUrlProperty(){
		ArtifactoryMultiUploader uploader = new ArtifactoryMultiUploader();
		ArtifactoryMultiProjectPropertiesLocator locator = new ArtifactoryMultiProjectPropertiesLocator(uploader, true);
		File file = new File(getClass().getResource("artifactory-properties").getFile());
		locator.searchFileForGitPropertiesAsync(file, false);
		assertThat(uploader.getArtifactoryUploaders().size()).isEqualTo(2);
	}
}
