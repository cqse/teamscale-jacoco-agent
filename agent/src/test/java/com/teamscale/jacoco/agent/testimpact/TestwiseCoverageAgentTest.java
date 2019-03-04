/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/

package com.teamscale.jacoco.agent.testimpact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import org.jacoco.agent.rt.IAgent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.AgentOptionsParser;
import com.teamscale.report.util.CommandLineLogger;

/** Tests for {@link TestwiseCoverageAgent}. */
public class TestwiseCoverageAgentTest {

	/** JaCoCo agent mocked, since we only test the HTTP part here. */
	IAgent mock = mock(IAgent.class);

	/** An expected exception. Configured in test methods as needed. */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/** Tests whether the server listens on the given port. */
	@Test
	public void testSingleAgent() throws Exception {
		String optionsString = "out=,class-dir=,http-server-port=8080";
		AgentOptions options = AgentOptionsParser.parse(optionsString, new CommandLineLogger());
		TestwiseCoverageAgent agent = new TestwiseCoverageAgent(options, mock);
		assertEquals(8080, agent.getPort());
	}

	/**
	 * Tests whether something else running on the primary port makes
	 * registration fail.
	 */
	@Test
	public void testBlockedPort() throws Exception {
		String optionsString = "out=,class-dir=,http-server-port=8090";
		AgentOptions options = AgentOptionsParser.parse(optionsString, new CommandLineLogger());

		try (ServerSocket socket = new ServerSocket(8090)) {
			thrown.expect(SocketTimeoutException.class);
			new TestwiseCoverageAgent(options, mock);
		}
	}

	/** Tests whether high port numbers fail. */
	@Test
	public void testOutOfPorts() throws Exception {
		String optionsString = "out=,class-dir=,http-server-port=65535";
		AgentOptions options = AgentOptionsParser.parse(optionsString, new CommandLineLogger());
		thrown.expect(IOException.class);
		thrown.expectMessage("Unable to determine a free server port.");
		new TestwiseCoverageAgent(options, mock);
	}

	/** Tests whether multiple instances register with each other. */
	@Test
	public void testMultiAgents() throws Exception {
		String optionsString = "out=,class-dir=,http-server-port=8070";
		AgentOptions options = AgentOptionsParser.parse(optionsString, new CommandLineLogger());

		TestwiseCoverageAgent agent1 = new TestwiseCoverageAgent(options, mock);
		TestwiseCoverageAgent agent2 = new TestwiseCoverageAgent(options, mock);

		assertEquals(8070, agent1.getPort());
		assertEquals(8071, agent2.getPort());
	}
}
