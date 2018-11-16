package eu.cqse.teamscale.report.testwise.model.builder;

import eu.cqse.teamscale.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.report.testwise.model.PathCoverage;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/** Container for {@link FileCoverageBuilder}s of the same path. */
public class PathCoverageBuilder {

	/** File system path. */
	private final String path;

	/** Mapping from file names to {@link FileCoverageBuilder}. */
	private final Map<String, FileCoverageBuilder> fileCoverageList = new HashMap<>();

	/** Constructor. */
	public PathCoverageBuilder(String path) {
		this.path = path;
	}

	/** @see #path */
	public String getPath() {
		return path;
	}

	/**
	 * Adds the given {@link FileCoverageBuilder} to the container.
	 * If coverage for the same file already exists it gets merged.
	 */
	public void add(FileCoverageBuilder fileCoverage) {
		if (fileCoverageList.containsKey(fileCoverage.getFileName())) {
			FileCoverageBuilder existingFile = fileCoverageList.get(fileCoverage.getFileName());
			existingFile.merge(fileCoverage);
		} else {
			fileCoverageList.put(fileCoverage.getFileName(), fileCoverage);
		}
	}

	/** Returns a collection of {@link FileCoverageBuilder}s associated with this path. */
	public Collection<FileCoverageBuilder> getFiles() {
		return fileCoverageList.values();
	}

	/** Builds a {@link PathCoverage} object. */
	public PathCoverage build() {
		List<FileCoverage> files = fileCoverageList.values().stream()
				.sorted(Comparator.comparing(FileCoverageBuilder::getFileName))
				.map(FileCoverageBuilder::build).collect(toList());
		return new PathCoverage(path, files);
	}
}
