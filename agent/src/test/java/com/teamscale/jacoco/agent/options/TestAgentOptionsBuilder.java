package com.teamscale.jacoco.agent.options;


/**
 * Builds {@link AgentOptions} for test purposes
 */
public class TestAgentOptionsBuilder {

	private String teamscalePartition;
	private String teamscaleMessage;
	private Integer httpServerPort;

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with the given {@linkplain
	 * AgentOptions#httpServerPort HTTP server port}.
	 */
	public TestAgentOptionsBuilder withHttpServerPort(Integer httpServerPort) {
		this.httpServerPort = httpServerPort;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with the given {@linkplain
	 * com.teamscale.client.TeamscaleServer#partition Teamscale partition}.
	 */
	public TestAgentOptionsBuilder withTeamscalePartition(String teamscalePartition) {
		this.teamscalePartition = teamscalePartition;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with the given {@linkplain
	 * com.teamscale.client.TeamscaleServer#message Teamscale message}.
	 */
	public TestAgentOptionsBuilder withTeamscaleMessage(String teamscaleMessage) {
		this.teamscaleMessage = teamscaleMessage;
		return this;
	}

	/**
	 * Builds the {@link AgentOptions}.
	 **/
	public AgentOptions create() {
		AgentOptions agentOptions = new AgentOptions();
		agentOptions.teamscaleServer.partition = teamscalePartition;
		agentOptions.teamscaleServer.setMessage(teamscaleMessage);
		agentOptions.httpServerPort = httpServerPort;
		return agentOptions;
	}

}
