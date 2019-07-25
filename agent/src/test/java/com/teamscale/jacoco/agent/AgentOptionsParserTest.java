package com.teamscale.jacoco.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarInputStream;

import org.junit.Test;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.report.util.CommandLineLogger;

public class AgentOptionsParserTest {
	
	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger());
	
	@Test
	public void testFromArchive() throws IOException, AgentOptionParseException {
		for (String archiveName : new String[]{"plain-git-properties.jar", "spring-boot-git-properties.jar", "spring-boot-git-properties.war"}) {
			JarInputStream jarInputStream = new JarInputStream(this.getClass().getResourceAsStream(archiveName));
			Optional<CommitDescriptor> commitDescriptor = parser.getCommitFromGitProperties(jarInputStream);
			assertThat(commitDescriptor).isPresent().map(CommitDescriptor::toString).hasValue("master:1564065275000");
		}
	}
	
	@Test
	public void testInvalidTiemstamp() {
		Properties gitProperties = new Properties();
		gitProperties.put("git.commit.time", "123ab");
		gitProperties.put("git.branch", "master");
		assertThatThrownBy(() -> parser.getGitPropertiesCommitDescriptor("test", gitProperties))
				.isInstanceOf(AgentOptionParseException.class)
				.hasMessageContaining("can't neither be parsed with the pattern");
	}
	
	@Test
	public void testLongTimestamp() throws AgentOptionParseException {
		Properties gitProperties = new Properties();
		gitProperties.put("git.commit.time", "123456789");
		gitProperties.put("git.branch", "master");
		CommitDescriptor commitDescriptor = parser.getGitPropertiesCommitDescriptor("test", gitProperties);
		assertThat(commitDescriptor.toString()).isEqualTo("master:123456789000");
	}
}
