package eu.cqse.teamscale.report.testwise.model;

import org.conqat.lib.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Generic holder of test coverage of a single test based on line-ranges. */
public class TestCoverage {

	/** The uniformPath of the test (see TEST_IMPACT_ANALYSIS_DOC.md for more information). */
	private final String uniformPath;

	/** Mapping from path names to all files on this path. */
	private final Map<String, PathCoverage> pathCoverageList = new HashMap<>();

	/** Constructor. */
	public TestCoverage(String uniformPath) {
		this.uniformPath = uniformPath;
	}

	/** @see #uniformPath */
	public String getUniformPath() {
		return uniformPath;
	}

	/** Returns a collection of {@link PathCoverage}s associated with the test. */
	public Collection<PathCoverage> getPaths() {
		return CollectionUtils.sort(pathCoverageList.values(), Comparator.comparing(PathCoverage::getPath));
	}

	/** Adds the {@link FileCoverage} to into the map, but filters out file coverage that is null or empty. */
	public void add(FileCoverage fileCoverage) {
		if (fileCoverage == null || fileCoverage.isEmpty()
				|| fileCoverage.getFileName() == null || fileCoverage.getPath() == null) {
			return;
		}
		PathCoverage pathCoverage = pathCoverageList.computeIfAbsent(fileCoverage.getPath(), PathCoverage::new);
		pathCoverage.add(fileCoverage);
	}

	/** Adds the {@link FileCoverage}s into the map, but filters out empty ones. */
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

	/** Returns true if there is no coverage for the test yet. */
	public boolean isEmpty() {
		return pathCoverageList.isEmpty();
	}

}
