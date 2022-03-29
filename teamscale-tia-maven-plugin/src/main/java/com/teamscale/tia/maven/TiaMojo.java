package com.teamscale.tia.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "tia", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class TiaMojo extends AbstractMojo {

	@Parameter(defaultValue = "${session}")
	private MavenSession session;

	@Parameter(required = true)
	private String teamscaleUrl;

	@Parameter(required = true)
	private String project;

	@Parameter(required = true)
	private String userName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		System.err.println("----->>>" + teamscaleUrl);
		session.getUserProperties().setProperty("teamscale.test.impacted.reportDirectory", "./target/tia");
		session.getUserProperties().setProperty("teamscale.test.impacted.server.url", teamscaleUrl);
		session.getUserProperties().setProperty("teamscale.test.impacted.server.project", project);
		session.getUserProperties().setProperty("teamscale.test.impacted.server.userName", userName);
		// TODO (FS) access key?
		// TODO (FS) agent url
	}
}
