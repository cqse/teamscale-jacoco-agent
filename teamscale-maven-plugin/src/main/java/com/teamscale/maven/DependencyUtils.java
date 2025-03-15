package com.teamscale.maven;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper for dealing with maven project dependencies.
 */
public class DependencyUtils {

	/** Determines all direct and transitive project dependencies in the given scopes including the project itself. */
	public static List<MavenProject> findDependencies(List<MavenProject> reactorProjects, MavenProject currentProject,
			String... scopes) {
		List<MavenProject> result = new ArrayList<>();
		result.add(currentProject);
		Set<String> scopeList = new HashSet<>(Arrays.asList(scopes));
		for (Dependency dependency : currentProject.getDependencies()) {
			if (scopeList.contains(dependency.getScope())) {
				MavenProject project = findProjectFromReactor(reactorProjects, dependency);
				if (project != null) {
					result.add(project);
				}
			}
		}
		return result;
	}

	/**
	 * Note that if the dependency specified using version range and reactor contains multiple modules with the same
	 * artifactId and groupId but of different versions, then the first dependency which matches range will be selected.
	 * For example in case of range <code>[0,2]</code> if version 1 is before version 2 in reactor, then version 1 will
	 * be selected.
	 */
	private static MavenProject findProjectFromReactor(List<MavenProject> reactorProjects, Dependency d) {
		VersionRange depVersionAsRange;
		try {
			depVersionAsRange = VersionRange
					.createFromVersionSpec(d.getVersion());
		} catch (InvalidVersionSpecificationException e) {
			throw new AssertionError(e);
		}

		for (final MavenProject p : reactorProjects) {
			DefaultArtifactVersion pv = new DefaultArtifactVersion(
					p.getVersion());
			if (p.getGroupId().equals(d.getGroupId())
					&& p.getArtifactId().equals(d.getArtifactId())
					&& depVersionAsRange.containsVersion(pv)) {
				return p;
			}
		}
		return null;
	}
}
