package eu.cqse.teamscale.report.testwise.jacoco.cache;

import eu.cqse.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import eu.cqse.teamscale.report.testwise.model.LineRange;
import eu.cqse.teamscale.report.util.ILogger;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.data.ExecutionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a class' probes and to which line ranges they refer.
 * <p>
 * - Create an instance of this class for every analyzed java class.
 * - Set the file name of the java source file from which the class has been created.
 * - Then call for every method in the class {@link #addLine(int)} and {@link #addProbe(int)} for all probes and lines
 * that belong to the method and call {@link #finishMethod()} to signal that the next calls belong to another method.
 * - Afterwards call {@link #getFileCoverage(ExecutionData, ILogger)} to transform probes ({@link ExecutionData}) for
 * this class into covered lines ({@link FileCoverageBuilder}).
 */
public class ClassCoverageLookup {

	/** Fully qualified name of the class (with / as separators). */
	private String className;

	/** Name of the java source file. */
	private String sourceFileName;

	/**
	 * List of method's line ranges. The index in this list corresponds to the probe ID.
	 * So the same {@link LineRange}s can appear multiple times in the list if a method contains more than one probe.
	 */
	private final List<LineRange> probes = new ArrayList<>();

	/**
	 * Holds the line range of the currently analyzed method.
	 * <p>
	 * By convention all calls to {@link #addProbe(int)} and {@link #addLine(int)}
	 * belong to one method until {@link #finishMethod()} is called.
	 */
	private LineRange currentMethod = new LineRange();

	/**
	 * Constructor.
	 *
	 * @param className Classname as stored in the bytecode e.g. com/company/Example
	 */
	ClassCoverageLookup(String className) {
		this.className = className;
	}

	/** Sets the file name of the currently analyzed class (without path). */
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	/** Adjusts the size of the probes list to the total probes count. */
	public void setTotalProbeCount(int count) {
		ensureArraySize(count - 1);
	}

	/** Adds the probe with the given id to the method. */
	public void addProbe(int probeId) {
		ensureArraySize(probeId);
		probes.set(probeId, currentMethod);
	}

	/** Adds the given line to the method. */
	public void addLine(int line) {
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
	 * Generates {@link FileCoverageBuilder} from an {@link ExecutionData}.
	 * {@link ExecutionData} holds coverage of exactly one class (whereby inner classes are a separate class).
	 * This method returns a {@link FileCoverageBuilder} object which is later merged with the {@link FileCoverageBuilder} of other
	 * classes that reside in the same file.
	 */
	public FileCoverageBuilder getFileCoverage(ExecutionData executionData, ILogger logger) throws CoverageGenerationException {
		boolean[] executedProbes = executionData.getProbes();

		if (checkProbeInvariant(executedProbes)) {
			throw new CoverageGenerationException("Probe lookup does not match with actual probe size for " +
					sourceFileName + " " + className + " (" + probes.size() + " vs " + executedProbes.length + ")! " +
					"This is a bug in the profiler tooling. Please report it back to CQSE.");
		}
		if (sourceFileName == null) {
			logger.warn(
					"No source file name found for class " + className + "! This class was probably not compiled with " +
							"debug information enabled!");
			return null;
		}

		String packageName = StringUtils.removeLastPart(className, '/');
		final FileCoverageBuilder fileCoverage = new FileCoverageBuilder(packageName, sourceFileName);
		for (int i = 0; i < probes.size(); i++) {
			LineRange lineRange = probes.get(i);
			// lineRange is null if the probe is outside of a method
			if (executedProbes[i])
				if (lineRange != null) {
					fileCoverage.addLineRange(lineRange);
				} else {
					logger.info(sourceFileName + " " + className + " did contain a covered probe " + i + "(of " +
							executedProbes.length + ") that could not be " +
							"matched to any method. This could be a bug in the profiler tooling. Please report it back " +
							"to CQSE.");
				}
		}

		return fileCoverage;
	}

	/** Checks that the executed probes is not smaller than the cached probes. */
	private boolean checkProbeInvariant(boolean[] executedProbes) {
		return probes.size() > executedProbes.length;
	}
}
