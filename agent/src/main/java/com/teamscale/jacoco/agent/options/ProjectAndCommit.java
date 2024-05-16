package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.commit_resolution.git_properties.CommitInfo;

import java.util.Objects;

/** Class encapsulating the Teamscale project and git commitInfo an upload should be performed to. */
public class ProjectAndCommit {

	private final String project;
	private final CommitInfo commitInfo;

	public ProjectAndCommit(String project, CommitInfo commitInfo) {
		this.project = project;
		this.commitInfo = commitInfo;
	}

	/** @see #project */
	public String getProject() {
		return project;
	}

	/** @see #commitInfo */
	public CommitInfo getCommitInfo() {
		return commitInfo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectAndCommit that = (ProjectAndCommit) o;
		return Objects.equals(project, that.project) &&
				Objects.equals(commitInfo, that.commitInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(project, commitInfo);
	}

	@Override
	public String toString() {
		return "ProjectRevision{" +
				"project='" + project + '\'' +
				", commitInfo='" + commitInfo + '\'' +
				'}';
	}
}
