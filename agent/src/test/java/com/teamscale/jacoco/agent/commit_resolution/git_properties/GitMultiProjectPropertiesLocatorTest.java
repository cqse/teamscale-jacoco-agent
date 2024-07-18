package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.ProjectAndCommit;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitMultiProjectPropertiesLocatorTest {
	@Test
	void testNoErrorIsThrownWhenGitPropertiesFileDoesNotHaveAProject() {

		List<ProjectAndCommit> projectAndCommits = new ArrayList<>();
		GitMultiProjectPropertiesLocator locator = new GitMultiProjectPropertiesLocator(
				new DelayedTeamscaleMultiProjectUploader((project, revision) -> {
					projectAndCommits.add(new ProjectAndCommit(project, revision));
					return null;
				}), true);
		File jarFile = new File(getClass().getResource("emptyTeamscaleProjectGitProperties").getFile());
		locator.searchFile(jarFile, false);
		assertThat(projectAndCommits.size()).isEqualTo(1);
		assertThat(projectAndCommits.get(0).getProject()).isEqualTo("my-teamscale-project");
	}

	@Test
	void testNoMultipleUploadsToSameProjectAndRevision() {
		List<ProjectAndCommit> projectAndCommits = new ArrayList<>();
		GitMultiProjectPropertiesLocator locator = new GitMultiProjectPropertiesLocator(
				new DelayedTeamscaleMultiProjectUploader((project, revision) -> {
					projectAndCommits.add(new ProjectAndCommit(project, revision));
					TeamscaleServer server = new TeamscaleServer();
					server.project = project;
					server.revision = revision.revision;
					server.commit = revision.commit;
					return new TeamscaleUploader(server);
				}), true);
		File jarFile = new File(getClass().getResource("multiple-same-target-git-properties-folder").getFile());
		locator.searchFile(jarFile, false);
		assertThat(projectAndCommits.size()).isEqualTo(2);
		assertThat(projectAndCommits.get(0).getProject()).isEqualTo("demo2");
		assertThat(projectAndCommits.get(0).getCommitInfo().commit).isEqualTo(
				new CommitDescriptor("master", "1645713803000"));
		assertThat(projectAndCommits.get(1).getProject()).isEqualTo("demolib");
		assertThat(projectAndCommits.get(1).getCommitInfo().revision).isEqualTo(
				"05b9d066a0c0762be622987de403b5752fa01cc0");
	}

}