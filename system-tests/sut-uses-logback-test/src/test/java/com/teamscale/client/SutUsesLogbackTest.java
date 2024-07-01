package com.teamscale.client;

import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class SutUsesLogbackTest {

	private static final String AGENT_JAR = System.getProperty("agentJar");

	@Test
	public void systemTest() throws Exception {
		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new String[]{"java", "-javaagent:" + AGENT_JAR + "=debug=true", "-jar", "build/libs/app.jar"});

		assertThat(result.getStdout()).contains("This warning is to test logging in the SUT");
		assertThat(result.getStdout()).doesNotContainIgnoringCase("error");
		assertThat(result.getStderr()).isEmpty();

		Path appLogFile = Paths.get("logTest/app.log");
		assertThat(appLogFile).exists();
		assertThat(appLogFile).content().contains("This warning is to test logging in the SUT");
	}

}
