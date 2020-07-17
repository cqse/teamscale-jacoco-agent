package com.teamscale.jacoco.agent.options.sapnwdi;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Tests for parsing the SAP NWDI application definition. */
public class SapNwdiApplicationsTest {

	@Test
	public void testEmptyConfig() throws AgentOptionParseException {
		assertThatCode(() -> SapNwdiApplications.parseApplications(""))
				.hasMessage("Application definition is expected not to be empty.");
		SapNwdiApplications configuration = SapNwdiApplications.parseApplications(",");
		assertThat(configuration.hasAllRequiredFieldsSet()).isFalse();
	}

	@Test
	public void testIncompleteMarkerClassConfig() {
		assertThatCode(() -> SapNwdiApplications.parseApplications(":alias"))
				.hasMessage("Marker class is not given for :alias!");
	}

	@Test
	public void testIncompleteProjectConfig() {
		assertThatCode(() -> SapNwdiApplications.parseApplications("class:"))
				.hasMessage("Application definition class: is expected to contain a marker class and project separated by a colon.");
	}

	@Test
	public void testSingleApplication() throws Exception {
		SapNwdiApplications configuration = SapNwdiApplications.parseApplications("com.teamscale.test2.Bar:alias");
		assertThat(configuration.getApplications()).element(0).satisfies(application -> {
			assertThat(application.getMarkerClass()).isEqualTo("com.teamscale.test2.Bar");
			assertThat(application.getTeamscaleProject()).isEqualTo("alias");
		});
	}

	@Test
	public void testMultipleApplications() throws Exception {
		SapNwdiApplications configuration = SapNwdiApplications
				.parseApplications("com.teamscale.test1.Bar:alias, com.teamscale.test2.Bar:id");
		assertThat(configuration.getApplications()).element(0).satisfies(application -> {
			assertThat(application.getMarkerClass()).isEqualTo("com.teamscale.test1.Bar");
			assertThat(application.getTeamscaleProject()).isEqualTo("alias");
		});
		assertThat(configuration.getApplications()).element(1).satisfies(application -> {
			assertThat(application.getMarkerClass()).isEqualTo("com.teamscale.test2.Bar");
			assertThat(application.getTeamscaleProject()).isEqualTo("id");
		});
	}
}
