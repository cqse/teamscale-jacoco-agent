package com.teamscale.jacoco.agent.git_properties;

import com.teamscale.client.CommitDescriptor;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GitPropertiesLocatorTest {

	private static final List<String> TEST_ARCHIVES = Arrays
			.asList("plain-git-properties.jar", "spring-boot-git-properties.jar", "spring-boot-git-properties.war");

	@Test
	public void testReadingGitPropertiesFromArchive() throws Exception {
		for (String archiveName : TEST_ARCHIVES) {
			JarInputStream jarInputStream = new JarInputStream(getClass().getResourceAsStream(archiveName));
			CommitDescriptor commitDescriptor = GitPropertiesLocator
					.getCommitFromGitProperties(jarInputStream, new File("test.jar"));
			assertThat(commitDescriptor).isNotNull();
			assertThat(commitDescriptor.toString()).isEqualTo("master:1564065275000");
		}
	}

	@Test
	public void testGitPropertiesWithInvalidTimestamp() {
		Properties gitProperties = new Properties();
		gitProperties.put("git.commit.time", "123ab");
		gitProperties.put("git.branch", "master");
		assertThatThrownBy(
				() -> GitPropertiesLocator.parseGitPropertiesJarEntry("test", gitProperties, new File("test.jar")))
				.isInstanceOf(InvalidGitPropertiesException.class);
	}
}
