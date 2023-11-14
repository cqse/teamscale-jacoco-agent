package com.teamscale.jacoco.agent.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;

import okhttp3.HttpUrl;

/**
 * Builds {@link AgentOptions} for test purposes
 */
public class TestAgentOptionsBuilder {

	private Integer httpServerPort;
	private final ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
	private final TeamscaleServer teamscaleServer = new TeamscaleServer();

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain AgentOptions#httpServerPort HTTP server port}.
	 */
	public TestAgentOptionsBuilder withHttpServerPort(Integer httpServerPort) {
		this.httpServerPort = httpServerPort;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#partition
	 * Teamscale partition}.
	 */
	public TestAgentOptionsBuilder withTeamscalePartition(String teamscalePartition) {
		this.teamscaleServer.partition = teamscalePartition;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#message Teamscale
	 * message}.
	 */
	public TestAgentOptionsBuilder withTeamscaleMessage(String teamscaleMessage) {
		this.teamscaleServer.setMessage(teamscaleMessage);
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#project Teamscale
	 * project}.
	 */
	public TestAgentOptionsBuilder withTeamscaleProject(String project) {
		this.teamscaleServer.project = project;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#url Teamscale
	 * url}.
	 */
	public TestAgentOptionsBuilder withTeamscaleUrl(String url) {
		this.teamscaleServer.url = HttpUrl.parse(url);
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#userName Teamscale
	 * username}.
	 */
	public TestAgentOptionsBuilder withTeamscaleUser(String user) {
		this.teamscaleServer.userName = user;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#userAccessToken
	 * Teamscale access token}.
	 */
	public TestAgentOptionsBuilder withTeamscaleAccessToken(String accessToken) {
		this.teamscaleServer.userAccessToken = accessToken;
		return this;
	}

	/**
	 * Ensures that the {@link AgentOptions} are {@linkplain #create() built} with
	 * the given {@linkplain com.teamscale.client.TeamscaleServer#revision Teamscale
	 * revision}.
	 */
	public TestAgentOptionsBuilder withTeamscaleRevision(String revision) {
		this.teamscaleServer.revision = revision;
		return this;
	}

	/**
	 * Adds minimal artifactory configs so that a ArtifactoryUploader can be built.
	 */
	public TestAgentOptionsBuilder withMinimalArtifactoryConfig(String apiKey, String partition, String url) {
		artifactoryConfig.apiKey = apiKey;
		artifactoryConfig.partition = partition;
		artifactoryConfig.url = HttpUrl.parse(url);
		artifactoryConfig.commitInfo = new ArtifactoryConfig.CommitInfo("fake_revision",
				CommitDescriptor.parse("somebranch:0"));
		return this;
	}

	/**
	 * Builds the {@link AgentOptions}.
	 **/
	public AgentOptions create() {
		AgentOptions agentOptions = new AgentOptions();
		agentOptions.teamscaleServer = teamscaleServer;
		agentOptions.httpServerPort = httpServerPort;
		agentOptions.artifactoryConfig = artifactoryConfig;
		return agentOptions;
	}

}
