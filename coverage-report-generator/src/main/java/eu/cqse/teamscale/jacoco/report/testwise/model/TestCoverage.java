package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/** Generic holder of test coverage of a single test based on line-ranges. */
public class TestCoverage {

	/** The external ID of the test. */
	@XmlAttribute
	public String externalId;

	/** Mapping from path names to all files on this path. */
	private final Map<String, PathCoverage> pathCoverageList = new HashMap<>();

	/** Constructor. */
	public TestCoverage(String externalId) {
		this.externalId = externalId;
	}

	/** Adds the {@link FileCoverage} to into the map,  but filters out empty coverage. */
	public void add(FileCoverage fileCoverage) {
		if (fileCoverage == null || fileCoverage.isEmpty()) {
			return;
		}
		PathCoverage pathCoverage = pathCoverageList.computeIfAbsent(fileCoverage.path, PathCoverage::new);
		pathCoverage.add(fileCoverage);
	}

	/** Adds the {@link FileCoverage}s to into the map,  but filters out empty ones. */
	public void addAll(List<FileCoverage> fileCoverageList) {
		for (FileCoverage fileCoverage : fileCoverageList) {
			add(fileCoverage);
		}
	}

	/** Returns all {@link FileCoverage}s stored for the test. */
	public List<FileCoverage> getFiles() {
		return pathCoverageList.values().stream()
				.flatMap(path -> path.getFiles().stream())
				.collect(Collectors.toList());
	}

	/** Returns a collection of {@link PathCoverage}s associated with the test. */
	@XmlElement(name = "path")
	public Collection<PathCoverage> getPaths() {
		return pathCoverageList.values();
	}

	/** Returns true if there is no coverage for the test yet. */
	public boolean isEmpty() {
		return pathCoverageList.isEmpty();
	}
}
