/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/

package com.teamscale.jacoco.agent.testimpact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import org.jacoco.agent.rt.IAgent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.teamscale.jacoco.agent.AgentOptionParseException;
import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.AgentOptionsParser;
import com.teamscale.report.util.CommandLineLogger;

/** Tests for {@link TestwiseCoverageAgent}. */
public class TestwiseCoverageAgentTest {

	/** Minimal options string without port number */
	private static final String OPTIONS_PREFIX = "out=,class-dir=,http-server-port=";

	/** JaCoCo agent mocked, since we only test the HTTP part here. */
	private final IAgent mock = mock(IAgent.class);

	/** Exception excepted to be thrown. Configured in tests as needed. */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/** Tests whether the server listens on the given port. */
	@Test
	public void testSingleAgent() throws Exception {
		int port = 8080;
		TestwiseCoverageAgent agent = new TestwiseCoverageAgent(options(port), mock, null);
		assertEquals(port, agent.getPort());
	}

	/**
	 * Tests whether something else running on the primary port makes
	 * registration fail.
	 */
	@Test
	public void testOccupiedPort() throws Exception {
		int port = 8090;
		try (ServerSocket socket = new ServerSocket(port)) {
			thrown.expect(SocketTimeoutException.class);
			new TestwiseCoverageAgent(options(port), mock, null);
		}
	}

	/** Tests whether high port numbers fail. */
	@Test
	public void testOutOfPorts() throws Exception {
		int port = 65535;
		thrown.expect(IOException.class);
		thrown.expectMessage("Unable to determine a free server port.");
		new TestwiseCoverageAgent(options(port), mock, null);
	}

	/** Tests whether multiple instances register with each other. */
	@Test
	public void testMultiAgents() throws Exception {
		int port = 8070;
		AgentOptions options = options(port);

		TestwiseCoverageAgent agent1 = new TestwiseCoverageAgent(options, mock, null);
		agent1.awaitServiceInitialization();
		TestwiseCoverageAgent agent2 = new TestwiseCoverageAgent(options, mock, null);
		agent2.awaitServiceInitialization();

		assertEquals(port, agent1.getPort());
		assertEquals(port + 1, agent2.getPort());
	}

	/** Tests whether events are forwarded. */
	@Test
	public void testEventForwarding() throws Exception {
		int primaryAgentPort = 8100;
		AgentOptions options = options(primaryAgentPort);

		IAgent secondaryAgent = mock(IAgent.class);
		ArgumentCaptor<String> stringArg = ArgumentCaptor.forClass(String.class);

		new TestwiseCoverageAgent(options, mock, null).awaitServiceInitialization();
		new TestwiseCoverageAgent(options, secondaryAgent, null).awaitServiceInitialization();

		String testId = "FIRST_TEST";
		IAgentService.create(primaryAgentPort).signalTestStart(testId).execute();

		verify(mock).setSessionId(stringArg.capture());
		assertEquals(testId, stringArg.getValue());

		verify(secondaryAgent).setSessionId(stringArg.capture());
		assertEquals(testId, stringArg.getValue());
	}

	/** Returns a valid options string for the given port number. */
	private static AgentOptions options(int port) throws AgentOptionParseException {
		return AgentOptionsParser.parse(OPTIONS_PREFIX + port, new CommandLineLogger());
	}
}
