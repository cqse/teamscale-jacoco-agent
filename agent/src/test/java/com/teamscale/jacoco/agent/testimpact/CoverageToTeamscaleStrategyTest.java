package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import okhttp3.HttpUrl;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoverageToTeamscaleStrategyTest {

	@Test
	public void mustRunTestStartBeforeTestEnd() throws Exception {
		AgentOptions options = mockOptions();
		JacocoRuntimeController controller = mockController();
		CoverageToTeamscaleStrategy strategy = new CoverageToTeamscaleStrategy(controller, options);

		strategy.testStart("mytest");
		strategy.testEnd("mytest", new TestExecution("mytest", 0L, ETestExecutionResult.PASSED));
		assertThrows(UnsupportedOperationException.class, strategy::testRunEnd);
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
		// must have valid class files or the report generator will throw an exception
		when(options.getClassDirectoriesOrZips()).thenReturn(Collections.singletonList(
				new File(getClass().getResource("Main.class").toURI())));

		TeamscaleClient client = mock(TeamscaleClient.class);
		when(options.createTeamscaleClient()).thenReturn(client);
		return options;
	}

}