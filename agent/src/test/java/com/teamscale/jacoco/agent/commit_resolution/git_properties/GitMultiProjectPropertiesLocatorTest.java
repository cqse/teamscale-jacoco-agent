package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.options.ProjectAndCommit;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
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

}