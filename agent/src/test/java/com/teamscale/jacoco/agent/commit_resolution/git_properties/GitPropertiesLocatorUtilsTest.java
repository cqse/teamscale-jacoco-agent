package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

class GitPropertiesLocatorUtilsTest {

	@Test
	public void parseSpringBootCodeLocations() throws Exception {
		Assertions.assertThat(GitPropertiesLocatorUtils
				.extractGitPropertiesSearchRoot(new URL("jar:file:/home/k/demo.jar!/BOOT-INF/classes!/")).getFirst())
				.isEqualTo(new File("/home/k/demo.jar"));
	}

	@Test
	public void parseFileCodeLocations() throws Exception {
		Assertions.assertThat(GitPropertiesLocatorUtils
				.extractGitPropertiesSearchRoot(new URL("file:/home/k/demo.jar")).getFirst())
				.isEqualTo(new File("/home/k/demo.jar"));
	}

}