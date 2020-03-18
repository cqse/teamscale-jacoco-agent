package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.tia.client.RunningTest;
import com.teamscale.tia.client.TestRun;
import com.teamscale.tia.client.TiaAgent;
import okhttp3.HttpUrl;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Response;

import java.io.File;
import java.net.URISyntaxException;
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

public class TestwiseCoverageAgentTest {

	@Rule
	public MockitoRule mockRule = MockitoJUnit.rule();

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

	private AgentOptions mockOptions() throws URISyntaxException {
		AgentOptions options = mock(AgentOptions.class);

		TeamscaleServer server = new TeamscaleServer();
		server.commit = new CommitDescriptor("branch", "12345");
		server.url = HttpUrl.get("http://doesnt-exist.io");
		server.userName = "build";
		server.userAccessToken = "token";
		server.partition = "partition";
		when(options.getTeamscaleServerOptions()).thenReturn(server);
		when(options.getHttpServerPort()).thenReturn(54321);
		when(options.shouldUploadTestWiseCoverageToTeamscale()).thenReturn(true);

		when(options.getLocationIncludeFilter()).thenReturn(new ClasspathWildcardIncludeFilter("**", ""));
		// must have at least one class file or the report generator will throw an exception
		when(options.getClassDirectoriesOrZips()).thenReturn(Collections.singletonList(
				new File(getClass().getResource("Main.class").toURI())));

		when(options.createTeamscaleClient()).thenReturn(client);
		return options;
	}

}