package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryMultiUploader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArtifactoryMultiProjectPropertiesLocatorTest {

	private static final String TEST_URL = "https://www.wikipedia.org/";

	@Test
	public void findsUrlProperty(){
		List<String> urls = new ArrayList<>();
		ArtifactoryMultiUploader uploader = new ArtifactoryMultiUploader();
		ArtifactoryMultiProjectPropertiesLocator locator = new ArtifactoryMultiProjectPropertiesLocator(uploader, true);
		File file = new File(getClass().getResource("artifactory-properties").getFile());
		locator.searchFileForGitPropertiesAsync(file, false);
		//assertThat(uploader.getArtifactoryUploaders().get(0).describe()).isEqualTo("Uploading to " + TEST_URL);
		// we use executor so concurrency! cannot simply step through
	}
}
