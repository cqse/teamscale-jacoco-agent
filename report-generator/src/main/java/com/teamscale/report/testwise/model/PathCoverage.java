package com.teamscale.report.testwise.model;

import java.util.List;

/** Container for {@link FileCoverage}s of the same path. */
public class PathCoverage {

	/** File system path. */
	private final String path;

	/** Files with coverage. */
	private final List<FileCoverage> files;

	/** Constructor. */
	public PathCoverage(String path, List<FileCoverage> files) {
		this.path = path;
		this.files = files;
	}

	public String getPath() {
		return path;
	}

	public List<FileCoverage> getFiles() {
		return files;
	}
}
