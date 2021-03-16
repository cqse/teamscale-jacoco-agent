package com.teamscale.jacoco.agent.options;

/** Class encapsulating the Teamscale project and git revision an upload should be performed to. */
public class ProjectRevision {

	private String project;
	private String revision;

	public ProjectRevision(String project, String revision) {
		this.project = project;
		this.revision = revision;
	}

	/** @see #project */
	public String getProject() {
		return project;
	}

	/** @see #revision */
	public String getRevision() {
		return revision;
	}

}
