package com.teamscale.jacoco.agent.options;

import com.teamscale.report.util.CommandLineLogger;
import org.junit.jupiter.api.Test;

/** Tests parsing of the agent's command line options. */
public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger());

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parser.parse("");
		parser.parse(null);
	}
}
