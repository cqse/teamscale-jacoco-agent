package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitPropertiesLocatorUtilsTest {

	/**
	 * Registers a protocol handler so the test can construct "nested:" URLs that are not supported by plain Java
	 * but Spring boot.
	 */
	@BeforeAll
	public static void registerCatchAllUrlProtocol() {
		URL.setURLStreamHandlerFactory(protocol -> {
			if (!"nested".equals(protocol)) {
				return null;
			}
			return new URLStreamHandler() {
				/** Returns null, since opening the connection is never done in the test.: */
				protected URLConnection openConnection(URL url) {
					return null;
				}
			};
		});
	}

	@Test
	public void parseSpringBootCodeLocations() throws Exception {
		assertThat(GitPropertiesLocatorUtils
				.extractGitPropertiesSearchRoot(new URL("jar:file:/home/k/demo.jar!/BOOT-INF/classes!/")).getFirst())
				.isEqualTo(new File("/home/k/demo.jar"));

		URL springBoot3Url = new URL(
				"jar:nested:/home/k/proj/spring-boot/demo/build/libs/demo-0.0.1-SNAPSHOT.jar/!BOOT-INF/classes/!/.");
		assertThat(GitPropertiesLocatorUtils.extractGitPropertiesSearchRoot(springBoot3Url).getFirst())
				.isEqualTo(new File("/home/k/proj/spring-boot/demo/build/libs/demo-0.0.1-SNAPSHOT.jar"));
	}

	@Test
	public void parseFileCodeLocations() throws Exception {
		assertThat(GitPropertiesLocatorUtils
				.extractGitPropertiesSearchRoot(new URL("file:/home/k/demo.jar")).getFirst())
				.isEqualTo(new File("/home/k/demo.jar"));
	}

	@Test
	public void getArtifactoryUrls() throws IOException {
		List<String> testUrls = Arrays.asList("https://www.wikipedia.org/", "https://stackoverflow.com/");
		File file = new File(getClass().getResource("artifactory-properties").getFile());
		List<String> urls = GitPropertiesLocatorUtils.getAllArtifactoryUrlsFromGitProperties(file, false, true);
		assertThat(urls).hasSize(testUrls.size());
		assertThat(urls).containsAll(testUrls);
	}
}