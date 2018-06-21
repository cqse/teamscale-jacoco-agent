package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.HashMap;
import java.util.Map;

/** Container for {@link FileCoverage}s of the same path. */
public class PathCoverage {

	/** File system path. */
	public String path;

	/** Mapping from file names to {@link FileCoverage}. */
	public Map<String, FileCoverage> fileCoverageList = new HashMap<>();

	/** Constructor. */
	public PathCoverage(String path) {
		this.path = path;
	}

	/**
	 * Adds the given {@link FileCoverage} to the container.
	 * If coverage for the same file already exists it gets merged.
	 */
	public void add(FileCoverage fileCoverage) {
		if (fileCoverageList.containsKey(fileCoverage.fileName)) {
			FileCoverage existingFile = fileCoverageList.get(fileCoverage.fileName);
			existingFile.merge(fileCoverage.coveredRanges);
		} else {
			fileCoverageList.put(fileCoverage.fileName, fileCoverage);
		}
	}
}
