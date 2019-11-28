package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.report.util.CommandLineLogger;
import org.junit.Test;

public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger());

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parser.parse("");
		parser.parse(null);
	}
}
