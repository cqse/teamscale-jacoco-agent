package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import okhttp3.HttpUrl;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CoverageToTeamscaleStrategyTest {

	@Mock
	private TeamscaleClient client;

	@Mock
	private JaCoCoTestwiseReportGenerator reportGenerator;

	@Mock
	private JacocoRuntimeController controller;

	@Test
	public void shouldRecordCoverageForTestsEvenIfNotProvidedAsAvailableTest() throws Exception {
		when(controller.dumpAndReset()).thenReturn(new Dump(new SessionInfo("mytest", 0, 0), new ExecutionDataStore()));

		AgentOptions options = mockOptions();
		CoverageToTeamscaleStrategy strategy = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);

		TestCoverageBuilder testCoverageBuilder = new TestCoverageBuilder("mytest");
		FileCoverageBuilder fileCoverageBuilder = new FileCoverageBuilder("src/main/java", "Main.java");
		fileCoverageBuilder.addLineRange(1, 4);
		testCoverageBuilder.add(fileCoverageBuilder);
		when(reportGenerator.convert(any(Dump.class))).thenReturn(testCoverageBuilder);

		// we skip testRunStart and don't provide any available tests
		strategy.testStart("mytest");
		strategy.testEnd("mytest", new TestExecution("mytest", 0L, ETestExecutionResult.PASSED));
		strategy.testRunEnd();

		verify(client).uploadReport(eq(EReportFormat.TESTWISE_COVERAGE),
				matches("\\Q{\"tests\":[{\"duration\":\\E[^,]*\\Q,\"paths\":[{\"files\":[{\"coveredLines\":\"1-4\",\"fileName\":\"Main.java\"}],\"path\":\"src/main/java\"}],\"result\":\"PASSED\",\"sourcePath\":\"mytest\",\"uniformPath\":\"mytest\"}]}\\E"),
				any(), any(), any());
	}

	@Test
	public void shouldRetryFailedRequestsOnce() throws Exception {
		List<PrioritizableTestCluster> clusters = Collections
				.singletonList(new PrioritizableTestCluster("cluster",
						Collections.singletonList(new PrioritizableTest("mytest"))));
		when(client.getImpactedTests(any(), any(), any(), any(), anyBoolean())).thenReturn(Response.success(clusters));

		AgentOptions options = mockOptions();
		CoverageToTeamscaleStrategy strategy = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);

		// should retry and thus not throw an exception
		strategy.testRunStart(
				Collections.singletonList(new ClusteredTestDetails("mytest", "mytest", "content", "cluster")), false,
				null);
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

		when(options.createTeamscaleClient()).thenReturn(client);
		return options;
	}

}