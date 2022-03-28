package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import org.conqat.lib.commons.collections.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GitPropertiesLocatorTest {

	private static final List<String> TEST_ARCHIVES = Arrays
			.asList("plain-git-properties.jar", "spring-boot-git-properties.jar", "spring-boot-git-properties.war");

	private static String nestedTestArchive = "nested-jar.war";

	@Test
	public void testReadingGitPropertiesFromArchive() throws Exception {
		for (String archiveName : TEST_ARCHIVES) {
			JarInputStream jarInputStream = new JarInputStream(getClass().getResourceAsStream(archiveName));
			Pair<String, Properties> commit = GitPropertiesLocatorUtils
					.findGitPropertiesInArchive(jarInputStream);
			assertThat(commit).isNotNull();
			String rev = GitPropertiesLocatorUtils
					.getGitPropertiesValue(commit.getSecond(), GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_ID, "test",
							new File("test.jar"));
			assertThat(rev).isEqualTo("72c7b3f7e6c4802414283cdf7622e6127f3f8976");
		}
	}

	/**
	 * Checks if extraction of git.properties works for nested jar files.
	 */
	@Test
	public void testReadingGitPropertiesFromNestedArchive() throws Exception {
		URL nestedArchiveURL = getClass().getResource(nestedTestArchive);
		String nestedPath = nestedArchiveURL.getFile() + "WEB-INF/lib/demoLib-1.0-SNAPSHOT.jar";
		File nestedArchiveFile = new File(nestedPath);
		Pair<String, Properties> commit = GitPropertiesLocatorUtils.findGitPropertiesInNestedArchiveFile(
				nestedArchiveFile);
		String rev = GitPropertiesLocatorUtils
				.getGitPropertiesValue(commit.getSecond(), GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_ID, "test",
						new File("test.jar"));
		assertThat(rev).isEqualTo("5b3b2d44987be38f930fe57128274e317316423d");
	}


	@Test
	public void testGitPropertiesWithInvalidTimestamp() {
		Properties gitProperties = new Properties();
		gitProperties.put("git.commit.time", "123ab");
		gitProperties.put("git.branch", "master");
		assertThatThrownBy(
				() -> GitPropertiesLocatorUtils.getGitPropertiesValue(gitProperties, GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_ID, "test", new File("test.jar")))
				.isInstanceOf(InvalidGitPropertiesException.class);
	}

}
