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
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import okhttp3.HttpUrl;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Response;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoverageToTeamscaleStrategyTest {

	@Rule
	public MockitoRule mockRule = MockitoJUnit.rule();

	@Mock
	private TeamscaleClient client;

	@Mock
	private JaCoCoTestwiseReportGenerator reportGenerator;

	@Test
	public void mustRunTestStartBeforeTestEnd() throws Exception {
		AgentOptions options = mockOptions();
		JacocoRuntimeController controller = mockController();
		CoverageToTeamscaleStrategy strategy = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);

		strategy.testStart("mytest");
		strategy.testEnd("mytest", new TestExecution("mytest", 0L, ETestExecutionResult.PASSED));
		assertThrows(UnsupportedOperationException.class, strategy::testRunEnd);
	}

	@Test
	public void testValidCallSequence() throws Exception {
		List<PrioritizableTestCluster> clusters = Collections
				.singletonList(new PrioritizableTestCluster("cluster",
						Collections.singletonList(new PrioritizableTest("mytest"))));
		when(client.getImpactedTests(any(), any(), any(), any(), anyBoolean())).thenReturn(Response.success(clusters));

		TestCoverageBuilder testCoverageBuilder = new TestCoverageBuilder("mytest");
		FileCoverageBuilder fileCoverageBuilder = new FileCoverageBuilder("src/main/java", "Main.java");
		fileCoverageBuilder.addLineRange(1, 4);
		testCoverageBuilder.add(fileCoverageBuilder);
		when(reportGenerator.convert(any(Dump.class))).thenReturn(testCoverageBuilder);

		AgentOptions options = mockOptions();
		JacocoRuntimeController controller = mockController();
		CoverageToTeamscaleStrategy strategy = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);

		strategy.testRunStart(
				Collections.singletonList(new ClusteredTestDetails("mytest", "mytest", "content", "cluster")), false,
				null);
		strategy.testStart("mytest");
		strategy.testEnd("mytest", new TestExecution("mytest", 0L, ETestExecutionResult.PASSED));
		strategy.testRunEnd();

		verify(client).uploadReport(eq(EReportFormat.TESTWISE_COVERAGE),
				eq("{\"tests\":[{\"content\":\"content\",\"duration\":0.0,\"paths\":[{\"files\":[{\"coveredLines\":\"1-4\",\"fileName\":\"Main.java\"}],\"path\":\"src/main/java\"}],\"result\":\"PASSED\",\"sourcePath\":\"mytest\",\"uniformPath\":\"mytest\"}]}"),
				any(), any(), any());
	}

	private JacocoRuntimeController mockController() throws JacocoRuntimeController.DumpException {
		JacocoRuntimeController controller = mock(JacocoRuntimeController.class);
		when(controller.dumpAndReset()).thenReturn(new Dump(new SessionInfo("mytest", 0, 0), new ExecutionDataStore()));
		return controller;
	}

	private AgentOptions mockOptions() throws URISyntaxException {
		AgentOptions options = mock(AgentOptions.class);

		TeamscaleServer server = new TeamscaleServer();
		server.commit = new CommitDescriptor("branch", "12345");
		server.url = HttpUrl.get("http://doesnt-exist.io");
		server.userName = "build";
		server.userAccessToken = "token";
		server.partition = "partition";
		when(options.getTeamscaleServerOptions()).thenReturn(server);

		when(options.getLocationIncludeFilter()).thenReturn(new ClasspathWildcardIncludeFilter("**", ""));
		// must have at least one class file or the report generator will throw an exception
		when(options.getClassDirectoriesOrZips()).thenReturn(Collections.singletonList(
				new File(getClass().getResource("Main.class").toURI())));

		when(options.createTeamscaleClient()).thenReturn(client);
		return options;
	}

}