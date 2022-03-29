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
		setTiaProperty("reportDirectory", "./target/tia");
		setTiaProperty("url", teamscaleUrl);
		setTiaProperty("project", project);
		setTiaProperty("userName", userName);
		// TODO (FS) access key?
		// TODO (FS) agent url
	}

	/**
	 * Sets a user property in the TIA namespace. User properties are respected both during the build and during tests
	 * (as e.g. failsafe tests are often run in a separate JVM spawned by Maven).
	 */
	private void setTiaProperty(String name, String value) {
		String fullyQualifiedName = "teamscale.test.impacted." + name;
		if (session.getUserProperties().get(fullyQualifiedName) == null) {
			session.getUserProperties().setProperty(fullyQualifiedName, value);
		}
	}
}
