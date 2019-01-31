package com.teamscale.report.testwise.model.builder;

import com.teamscale.report.testwise.model.PathCoverage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/** Generic holder of test coverage of a single test based on line-ranges. */
public class TestCoverageBuilder {

	/** The uniformPath of the test (see TEST_IMPACT_ANALYSIS_DOC.md for more information). */
	private final String uniformPath;

	/** Mapping from path names to all files on this path. */
	private final Map<String, PathCoverageBuilder> pathCoverageList = new HashMap<>();

	/** Constructor. */
	public TestCoverageBuilder(String uniformPath) {
		this.uniformPath = uniformPath;
	}

	/** @see #uniformPath */
	public String getUniformPath() {
		return uniformPath;
	}

	/** Returns a collection of {@link PathCoverageBuilder}s associated with the test. */
	public List<PathCoverage> getPaths() {
		return pathCoverageList.values().stream().sorted(Comparator.comparing(PathCoverageBuilder::getPath))
				.map(PathCoverageBuilder::build).collect(toList());
	}

	/** Adds the {@link FileCoverageBuilder} to into the map, but filters out file coverage that is null or empty. */
	public void add(FileCoverageBuilder fileCoverage) {
		if (fileCoverage == null || fileCoverage.isEmpty()
				|| fileCoverage.getFileName() == null || fileCoverage.getPath() == null) {
			return;
		}
		PathCoverageBuilder pathCoverage = pathCoverageList
				.computeIfAbsent(fileCoverage.getPath(), PathCoverageBuilder::new);
		pathCoverage.add(fileCoverage);
	}

	/** Adds the {@link FileCoverageBuilder}s into the map, but filters out empty ones. */
	public void addAll(List<FileCoverageBuilder> fileCoverageList) {
		for (FileCoverageBuilder fileCoverage : fileCoverageList) {
			add(fileCoverage);
		}
	}

	/** Returns all {@link FileCoverageBuilder}s stored for the test. */
	public List<FileCoverageBuilder> getFiles() {
		return pathCoverageList.values().stream()
				.flatMap(path -> path.getFiles().stream())
				.collect(toList());
	}

	/** Returns true if there is no coverage for the test yet. */
	public boolean isEmpty() {
		return pathCoverageList.isEmpty();
	}

}
