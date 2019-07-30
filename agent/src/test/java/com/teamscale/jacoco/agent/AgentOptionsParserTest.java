package com.teamscale.jacoco.agent;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.report.util.CommandLineLogger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger());

	private static final List<String> TEST_ARCHIVES = Arrays
			.asList("plain-git-properties.jar", "spring-boot-git-properties.jar", "spring-boot-git-properties.war");

	@Test
	public void testReadingGitPropertiesFromArchive() throws IOException, AgentOptionParseException {
		for (String archiveName : TEST_ARCHIVES) {
			JarInputStream jarInputStream = new JarInputStream(this.getClass().getResourceAsStream(archiveName));
			CommitDescriptor commitDescriptor = parser.getCommitFromGitProperties(jarInputStream, new File("test.jar"));
			assertThat(commitDescriptor.toString()).isEqualTo("master:1564065275000");
		}
	}

	@Test
	public void testGitPropertiesWithInvalidTimestamp() {
		Properties gitProperties = new Properties();
		gitProperties.put("git.commit.time", "123ab");
		gitProperties.put("git.branch", "master");
		assertThatThrownBy(() -> parser.parseGitPropertiesJarEntry("test", gitProperties, new File("test.jar")))
				.isInstanceOf(AgentOptionParseException.class);
	}
}
