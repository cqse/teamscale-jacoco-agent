package com.teamscale.jacoco.agent.sapnwdi;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Container for SAP application definitions. */
public class SapNwdiApplications {

	private final List<NwdiApplication> applications = new ArrayList<>();

	public List<NwdiApplication> getApplications() {
		return applications;
	}

	/** Checks if any applications are defined. */
	public boolean hasAllRequiredFieldsSet() {
		return !applications.isEmpty();
	}

	public static SapNwdiApplications parseApplications(String applications) throws AgentOptionParseException {
		SapNwdiApplications nwdiConfiguration = new SapNwdiApplications();
		String[] markerClassAndProjectPairs = applications.split(",");
		for (String markerClassAndProjectPair : markerClassAndProjectPairs) {
			String[] markerClassAndProject = markerClassAndProjectPair.split(":");
			if (markerClassAndProject.length != 2) {
				throw new AgentOptionParseException(
						"Application definition " + markerClassAndProjectPair + " is expected to contain exactly one colon.");
			}
			String markerClass = markerClassAndProject[0].trim();
			if (markerClass.isEmpty()) {
				throw new AgentOptionParseException("Marker class is not given for " + markerClassAndProjectPair + "!");
			}
			String teamscaleProject = markerClassAndProject[1].trim();
			if (teamscaleProject.isEmpty()) {
				throw new AgentOptionParseException(
						"Teamscale project is not given for " + markerClassAndProjectPair + "!");
			}
			NwdiApplication nwdiApplication = new NwdiApplication(markerClass, teamscaleProject);
			nwdiConfiguration.applications.add(nwdiApplication);
		}
		return nwdiConfiguration;
	}

	public static class NwdiApplication {

		private final String markerClass;
		private final String teamscaleProject;

		public NwdiApplication(String markerClass, String teamscaleProject) {
			this.markerClass = markerClass;
			this.teamscaleProject = teamscaleProject;
		}

		public String getMarkerClass() {
			return markerClass;
		}

		public String getTeamscaleProject() {
			return teamscaleProject;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NwdiApplication that = (NwdiApplication) o;
			return markerClass.equals(that.markerClass) && teamscaleProject.equals(that.teamscaleProject);
		}

		@Override
		public int hashCode() {
			return Objects.hash(markerClass, teamscaleProject);
		}
	}
}
