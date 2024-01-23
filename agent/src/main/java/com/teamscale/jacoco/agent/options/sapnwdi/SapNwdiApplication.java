package com.teamscale.jacoco.agent.options.sapnwdi;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An SAP application that is identified by a {@link #markerClass} and refers to a corresponding Teamscale project.
 */
public class SapNwdiApplication {

	/** Parses an application definition string e.g. "com.package.MyClass:projectId;com.company.Main:project". */
	public static List<SapNwdiApplication> parseApplications(String applications) throws AgentOptionParseException {
		List<SapNwdiApplication> nwdiConfiguration = new ArrayList<>();
		String[] markerClassAndProjectPairs = applications.split(";");
		if (markerClassAndProjectPairs.length == 0) {
			throw new AgentOptionParseException("Application definition is expected not to be empty.");
		}

		for (String markerClassAndProjectPair : markerClassAndProjectPairs) {
			if (markerClassAndProjectPair.trim().isEmpty()) {
				throw new AgentOptionParseException("Application definition is expected not to be empty.");
			}
			String[] markerClassAndProject = markerClassAndProjectPair.split(":");
			if (markerClassAndProject.length != 2) {
				throw new AgentOptionParseException(
						"Application definition " + markerClassAndProjectPair + " is expected to contain a marker class and project separated by a colon.");
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
			SapNwdiApplication nwdiApplication = new SapNwdiApplication(markerClass, teamscaleProject);
			nwdiConfiguration.add(nwdiApplication);
		}
		return nwdiConfiguration;
	}

	/** A fully qualified class name that is used to match a jar file to this application. */
	public final String markerClass;

	/** The teamscale project to which coverage should be uploaded. */
	public final String teamscaleProject;

	private SapNwdiApplication(String markerClass, String teamscaleProject) {
		this.markerClass = markerClass;
		this.teamscaleProject = teamscaleProject;
	}

	/** @see #markerClass */
	public String getMarkerClass() {
		return markerClass;
	}

	/** @see #teamscaleProject */
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
		SapNwdiApplication that = (SapNwdiApplication) o;
		return markerClass.equals(that.markerClass) && teamscaleProject.equals(that.teamscaleProject);
	}

	@Override
	public int hashCode() {
		return Objects.hash(markerClass, teamscaleProject);
	}
}
