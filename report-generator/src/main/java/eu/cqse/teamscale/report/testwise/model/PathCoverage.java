package eu.cqse.teamscale.report.testwise.model;

import org.conqat.lib.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Container for {@link FileCoverage}s of the same path. */
public class PathCoverage {

	/** File system path. */
	private final String path;

	/** Mapping from file names to {@link FileCoverage}. */
	private final Map<String, FileCoverage> fileCoverageList = new HashMap<>();

	/** Constructor. */
	public PathCoverage(String path) {
		this.path = path;
	}

	/** @see #path */
	@XmlAttribute(name = "name")
	public String getPath() {
		return path;
	}

	/**
	 * Adds the given {@link FileCoverage} to the container.
	 * If coverage for the same file already exists it gets merged.
	 */
	public void add(FileCoverage fileCoverage) {
		if (fileCoverageList.containsKey(fileCoverage.getFileName())) {
			FileCoverage existingFile = fileCoverageList.get(fileCoverage.getFileName());
			existingFile.merge(fileCoverage);
		} else {
			fileCoverageList.put(fileCoverage.getFileName(), fileCoverage);
		}
	}

	/** Returns a collection of {@link FileCoverage}s associated with this path. */
	@XmlElement(name = "file")
	public Collection<FileCoverage> getFiles() {
		return CollectionUtils.sort(fileCoverageList.values(), Comparator.comparing(FileCoverage::getFileName));
	}
}
