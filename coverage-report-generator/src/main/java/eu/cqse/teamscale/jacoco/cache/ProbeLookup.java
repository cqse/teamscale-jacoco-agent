package eu.cqse.teamscale.jacoco.cache;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.LineRange;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.data.ExecutionData;

import java.util.ArrayList;
import java.util.List;

/** Holds information about a class' probes and to which line ranges they refer. */
public class ProbeLookup {

	/** Fully qualified name of the class. */
	private String className;

	/** Name of the java source file. */
	private String sourceFileName;

	/**
	 * List of method's line ranges. The index in this list corresponds to the probe ID.
	 * So the same {@link LineRange}s can appear multiple times in the list if a method contains more than one probe.
	 */
	private List<LineRange> probes = new ArrayList<>();

	/**
	 * Holds the line range of the currently analyzed method.
	 * <p>
	 * By convention all calls to {@link #addProbe(int)} and {@link #visitLine(int)}
	 * belong to one method until {@link #finishMethod()} is called.
	 */
	private LineRange currentMethod = new LineRange();

	/** Constructor. */
	ProbeLookup(String className) {
		this.className = className;
	}

	/** Sets the file name of the currently analyzed class. */
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	/** Adds the probe with the given id to the method. */
	public void addProbe(int probeId) {
		ensureArraySize(probeId);
		probes.set(probeId, currentMethod);
	}

	/** Adds the given line to the method. */
	public void visitLine(int line) {
		currentMethod.adjustToContain(line);
	}

	/**
	 * Ensures that the probes list is big enough to allow access to the given index.
	 * Intermediate list entries are filled with null.
	 */
	private void ensureArraySize(int index) {
		while (index >= probes.size()) {
			probes.add(null);
		}
	}

	/** Indicates that the method analysis is finished. */
	public void finishMethod() {
		currentMethod = new LineRange();
	}

	/**
	 * Generates {@link FileCoverage} from an {@link ExecutionData}.
	 */
	public FileCoverage getFileCoverage(ExecutionData executionData) {
		boolean[] executedProbes = executionData.getProbes();

		// Check probe invariant
		if (probes.size() > executedProbes.length) {
			throw new RuntimeException("Probe lookup does not match with actual probe size for " +
					sourceFileName + " " + className + " (" + probes.size() + " vs " + executedProbes.length + ")!");
		}

		final FileCoverage fileCoverage = new FileCoverage(StringUtils.removeLastPart(className, '/'), sourceFileName);
		for (int i = 0; i < probes.size(); i++) {
			LineRange lineRange = probes.get(i);
			if (executedProbes[i] && lineRange != null) {
				fileCoverage.addLineRange(lineRange);
			}
		}

		return fileCoverage;
	}

}
