package eu.cqse.teamscale.report.testwise.jacoco;

import eu.cqse.teamscale.report.testwise.jacoco.cache.AnalyzerCache;
import eu.cqse.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.report.testwise.jacoco.cache.ProbesCache;
import eu.cqse.teamscale.report.jacoco.dump.Dump;
import eu.cqse.teamscale.report.testwise.model.TestCoverage;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage;
import eu.cqse.teamscale.report.util.ILogger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 */
class CachingExecutionDataReader {

	/** The logger. */
	private final ILogger logger;

	/** Constructor. */
	public CachingExecutionDataReader(ILogger logger) {
		this.logger = logger;
	}

	/** Cached probes. */
	private ProbesCache probesCache;

	/**
	 * Analyzes the given class/jar/war/... files and creates a lookup of which probes belong to which method.
	 */
	public void analyzeClassDirs(Collection<File> classesDirectories, Predicate<String> locationIncludeFilter) throws CoverageGenerationException {
		if (probesCache != null) {
			return;
		}
		probesCache = new ProbesCache();
		AnalyzerCache analyzer = new AnalyzerCache(probesCache, locationIncludeFilter, logger);
		for (File classDir : classesDirectories) {
			if (classDir.exists()) {
				try {
					analyzer.analyzeAll(classDir);
				} catch (IOException e) {
					logger.error("Failed to analyze class files in " + classDir + "! " +
							"Maybe the folder contains incompatible class files. " +
							"Coverage for class files in this folder will be ignored.", e);
				}
			}
		}
		if (probesCache.isEmpty()) {
			String directoryList = classesDirectories.stream().map(File::getPath).collect(Collectors.joining(","));
			throw new CoverageGenerationException("No class files found in the given directories! " + directoryList);
		}
	}

	/**
	 * Converts the given store to coverage data. The coverage will only contain line range coverage information.
	 */
	public TestwiseCoverage buildCoverage(List<Dump> dumps) {
		TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
		for (Dump dump : dumps) {
			String testId = dump.info.getId();
			if (testId.isEmpty()) {
				// Ignore intermediate coverage that does not belong to any specific test
				logger.warn("Found a session with empty name! This could indicate that coverage is dumped also for " +
						"coverage in between tests or that the given test name was empty");
				continue;
			}
			try {
				TestCoverage testCoverage = buildCoverage(testId, dump.store);
				testwiseCoverage.add(testCoverage);
			} catch (CoverageGenerationException e) {
				logger.error("Failed to generate coverage for test " + testId + "! Skipping to the next test.", e);
			}
		}
		return testwiseCoverage;
	}

	/**
	 * Converts the given store to coverage data. The coverage will only contain line range coverage information.
	 */
	public TestCoverage buildCoverage(String testId, ExecutionDataStore executionDataStore) throws CoverageGenerationException {
		TestCoverage testCoverage = new TestCoverage(testId);
		for (ExecutionData executionData : executionDataStore.getContents()) {
			testCoverage.add(probesCache.getCoverage(executionData));
		}
		return testCoverage;
	}
}