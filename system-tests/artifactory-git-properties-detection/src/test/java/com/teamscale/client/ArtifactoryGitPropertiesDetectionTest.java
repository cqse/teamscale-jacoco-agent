package com.teamscale.client;

import com.teamscale.test.commons.SystemTestUtils;
import org.conqat.lib.commons.collections.PairList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryGitPropertiesDetectionTest {

	/**
	 * This port must match what is configured for the -javaagent line in the build.gradle file.
	 */
	private static final int FAKE_ARTIFACTORY_PORT = 63100;
	private static final int AGENT_PORT = 63101;
	private ArtifactoryMockServer artifactoryMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() {
			artifactoryMockServer = new ArtifactoryMockServer(FAKE_ARTIFACTORY_PORT);
	}

	@AfterEach
	public void shutdownFakeTeamscaleServer() {
		artifactoryMockServer.shutdown();
	}

	@Test
	public void systemTest() throws Exception {
		SystemUnderTest.foo();

		SystemTestUtils.dumpCoverage(AGENT_PORT);

		assertThat(artifactoryMockServer.uploadedReports).hasSize(1);
		PairList<String, String> reports = artifactoryMockServer.uploadedReports;
		assertThat(reports).hasSize(1);
		// Ensure infos from src/main/resources/git.properties are picked up
		assertThat(reports.getFirst(0)).startsWith("uploads/master/1645713803000-86f9d655bf8a204d98bc3542e0d15cea38cc7c74/");
	}
}
