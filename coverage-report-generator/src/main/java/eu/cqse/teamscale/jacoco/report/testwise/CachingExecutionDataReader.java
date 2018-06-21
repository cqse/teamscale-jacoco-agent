package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.cache.AnalyzerCache;
import eu.cqse.teamscale.jacoco.cache.ProbesCache;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestCoverage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 * The class supports all major jacoco versions starting with 0.7.5 up to 0.8.x.
 * https://github.com/jacoco/jacoco/wiki/ExecFileVersions
 */
class CachingExecutionDataReader {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Cached probes. */
	private ProbesCache probesCache;

	/** Analyzes the given class files and creates a lookup of which probes belong to which method. */
	public void analyzeClassDirs(Collection<File> classesDirectories, Predicate<String> locationIncludeFilter) {
		if (probesCache != null) {
			return;
		}
		probesCache = new ProbesCache();
		AnalyzerCache newAnalyzer = new AnalyzerCache(probesCache, locationIncludeFilter);
		for (File classDir: classesDirectories) {
			try {
				if (classDir.exists()) {
					newAnalyzer.analyzeAll(classDir);
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
		if (probesCache.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (File classesDirectory: classesDirectories) {
				builder.append(classesDirectory.getPath());
				builder.append(", ");
			}
			throw new RuntimeException("No class files found in the given directories! " + builder.toString());
		}
	}

	/**
	 * Converts the given store to coverage data. The coverage will only contain line coverage information.
	 */
	public TestCoverage buildCoverage(String testId, ExecutionDataStore executionDataStore) {
		TestCoverage testCoverage = new TestCoverage(testId);
		for (ExecutionData executionData: executionDataStore.getContents()) {
			testCoverage.add(probesCache.getCoverage(executionData));
		}
		return testCoverage;
	}
}