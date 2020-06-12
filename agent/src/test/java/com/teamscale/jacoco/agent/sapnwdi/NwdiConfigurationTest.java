package com.teamscale.jacoco.agent.sapnwdi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for Reading NWDI Configurations. */
public class NwdiConfigurationTest {

	@Test
	public void testValidNwdiConfiguration() throws Exception {
		NwdiConfiguration configuration = readConfig("nwdi-config.json");

		assertThat(configuration).isNotNull();
		assertThat(configuration.hasAllRequiredFieldsSet()).isTrue();
		assertThat(configuration.getTeamscaleUrl()).isEqualTo(HttpUrl.parse("http://foo/"));
		assertThat(configuration.getApplications()).hasSize(2);
		assertThat(configuration.getApplications().get(1).getMarkerClass()).isEqualTo("com.teamscale.test2.Bar");
	}

	@Test
	public void testMissingValues() throws Exception {
		NwdiConfiguration configuration = readConfig("nwdi-missing-api.json");
		assertThat(configuration.hasAllRequiredFieldsSet()).isFalse();
	}

	@Test
	public void testMissingApps() throws Exception {
		NwdiConfiguration configuration = readConfig("nwdi-missing-apps.json");
		assertThat(configuration.hasAllRequiredFieldsSet()).isFalse();
	}

	@Test
	public void testMissingAppData() throws Exception {
		NwdiConfiguration configuration = readConfig("nwdi-missing-app-data.json");
		assertThat(configuration.hasAllRequiredFieldsSet()).isFalse();
	}

	/** Returns the contents of the given config file. */
	private NwdiConfiguration readConfig(String name) throws Exception {
		JsonAdapter<NwdiConfiguration> jsonAdapter = new Moshi.Builder().add(new HttpUrlAdapter()).build().adapter(NwdiConfiguration.class);
		return jsonAdapter.fromJson(FileSystemUtils.readFile(new File(getClass().getResource(name).toURI())));
	}
}
