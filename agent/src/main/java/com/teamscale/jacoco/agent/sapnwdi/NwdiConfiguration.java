package com.teamscale.jacoco.agent.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.string.StringUtils;

import java.util.List;
import java.util.stream.Stream;

public class NwdiConfiguration {
	private List<NwdiApplication> applications;
	private HttpUrl teamscaleUrl;
	private String teamscaleUser;
	private String teamscaleApiKey;
	private String teamscalePartition;
	private String teamscaleMessage = "Agent coverage upload";

	public List<NwdiApplication> getApplications() {
		return applications;
	}

	public HttpUrl getTeamscaleUrl() {
		return teamscaleUrl;
	}

	public String getTeamscaleUser() {
		return teamscaleUser;
	}

	public String getTeamscaleApiKey() {
		return teamscaleApiKey;
	}

	public String getTeamscalePartition() {
		return teamscalePartition;
	}

	public String getTeamscaleMessage() { return teamscaleMessage; }

	/** Checks if none of the required fields is null or empty. */
	public boolean hasAllRequiredFieldsSet() {
		return Stream.of(teamscaleUrl.toString(), teamscaleUser, teamscaleApiKey, teamscalePartition).noneMatch(StringUtils::isEmpty)
				&& applications != null && !applications.isEmpty()
				&& applications.stream().allMatch(NwdiApplication::hasAllRequiredFieldsSet);
	}

	public static class NwdiApplication {
		private String name;
		private String markerClass;
		private String teamscaleProject;

		private CommitDescriptor foundTimestamp;
		private String foundJarFile;

		public String getName() {
			return name;
		}

		public String getMarkerClass() {
			return markerClass;
		}

		public String getTeamscaleProject() {
			return teamscaleProject;
		}

		public CommitDescriptor getFoundTimestamp() {
			return foundTimestamp;
		}

		public String getFoundJarFile() {
			return foundJarFile;
		}

		public void setFoundTimestamp(CommitDescriptor foundTimestamp, String foundJarFile) {
			this.foundTimestamp = foundTimestamp;
			this.foundJarFile = foundJarFile;
		}

		/** Checks if none of the required fields is null or empty. */
		public boolean hasAllRequiredFieldsSet() {
			return Stream.of(markerClass, teamscaleProject).noneMatch(StringUtils::isEmpty);
		}
	}
}
