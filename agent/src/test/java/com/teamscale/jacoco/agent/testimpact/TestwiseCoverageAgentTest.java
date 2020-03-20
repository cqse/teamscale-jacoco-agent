package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.ETestWiseCoverageMode;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.tia.client.RunningTest;
import com.teamscale.tia.client.TestRun;
import com.teamscale.tia.client.TiaAgent;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestwiseCoverageAgentTest {

	@Mock
	private TeamscaleClient client;

	@Mock
	private JaCoCoTestwiseReportGenerator reportGenerator;

	@Test
	public void testAccessViaTiaClientAndReportUploadToTeamscale() throws Exception {
		List<ClusteredTestDetails> availableTests = Arrays
				.asList(new ClusteredTestDetails("test1", "test1", "content", "cluster"),
						new ClusteredTestDetails("test2", "test2", "content", "cluster"));
		List<PrioritizableTestCluster> impactedClusters = Collections
				.singletonList(new PrioritizableTestCluster("cluster",
						Collections.singletonList(new PrioritizableTest("test2"))));
		when(client.getImpactedTests(any(), any(), any(), any(), anyBoolean()))
				.thenReturn(Response.success(impactedClusters));

		TestCoverageBuilder testCoverageBuilder = new TestCoverageBuilder("test2");
		FileCoverageBuilder fileCoverageBuilder = new FileCoverageBuilder("src/main/java", "Main.java");
		fileCoverageBuilder.addLineRange(1, 4);
		testCoverageBuilder.add(fileCoverageBuilder);
		when(reportGenerator.convert(any(Dump.class))).thenReturn(testCoverageBuilder);

		new TestwiseCoverageAgent(mockOptions(), null, reportGenerator);

		TiaAgent agent = new TiaAgent(false, HttpUrl.get("http://localhost:54321"));

		TestRun testRun = agent.startTestRun(availableTests);
		assertThat(testRun.getPrioritizedTests()).hasSize(1);
		assertThat(testRun.getPrioritizedTests().get(0).tests).hasSize(1);
		PrioritizableTest test = testRun.getPrioritizedTests().get(0).tests.get(0);
		assertThat(test.uniformPath).isEqualTo("test2");

		RunningTest runningTest = testRun.startTest(test);
		runningTest.endTestNormally(new TestRun.TestResultWithMessage(ETestExecutionResult.PASSED, "message"));

		testRun.endTestRun();
		verify(client).uploadReport(eq(EReportFormat.TESTWISE_COVERAGE),
				matches("\\Q{\"tests\":[{\"content\":\"content\",\"paths\":[],\"sourcePath\":\"test1\",\"uniformPath\":\"test1\"},{\"content\":\"content\",\"duration\":\\E[^,]*\\Q,\"message\":\"message\",\"paths\":[{\"files\":[{\"coveredLines\":\"1-4\",\"fileName\":\"Main.java\"}],\"path\":\"src/main/java\"}],\"result\":\"PASSED\",\"sourcePath\":\"test2\",\"uniformPath\":\"test2\"}]}\\E"),
				any(), any(), any());
	}

	private AgentOptions mockOptions() {
		AgentOptions options = mock(AgentOptions.class);

		TeamscaleServer server = new TeamscaleServer();
		server.commit = new CommitDescriptor("branch", "12345");
		server.url = HttpUrl.get("http://doesnt-exist.io");
		server.userName = "build";
		server.userAccessToken = "token";
		server.partition = "partition";
		when(options.getTeamscaleServerOptions()).thenReturn(server);
		when(options.getHttpServerPort()).thenReturn(54321);
		when(options.getTestWiseCoverageMode()).thenReturn(ETestWiseCoverageMode.TEAMSCALE_REPORT);

		when(options.createTeamscaleClient()).thenReturn(client);
		return options;
	}

}