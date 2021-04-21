package com.teamscale.jacoco.agent.options;

import java.util.Objects;

/** Class encapsulating the Teamscale project and git revision an upload should be performed to. */
public class ProjectRevision {

	private final String project;
	private final String revision;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectRevision that = (ProjectRevision) o;
		return Objects.equals(project, that.project) &&
				Objects.equals(revision, that.revision);
	}

	@Override
	public int hashCode() {
		return Objects.hash(project, revision);
	}

	@Override
	public String toString() {
		return "ProjectRevision{" +
				"project='" + project + '\'' +
				", revision='" + revision + '\'' +
				'}';
	}
}
