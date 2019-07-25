package com.teamscale.report.testwise.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.cache.AnalyzerCache;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.jacoco.cache.ProbesCache;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;
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
	public void analyzeClassDirs(Collection<File> classesDirectories, Predicate<String> locationIncludeFilter,
								 EDuplicateClassFileBehavior duplicateClassFileBehavior) throws CoverageGenerationException {
		if (probesCache != null) {
			return;
		}
		probesCache = new ProbesCache(logger, duplicateClassFileBehavior);
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
	public DumpConsumer buildCoverageConsumer(Predicate<String> locationIncludeFilter,
											  Consumer<TestCoverageBuilder> nextConsumer) {
		return new DumpConsumer(logger, locationIncludeFilter, nextConsumer);
	}

	/**
	 * Consumer of {@link Dump} objects. Converts them to {@link TestCoverageBuilder} and passes them to the
	 * nextConsumer.
	 */
	public class DumpConsumer implements Consumer<Dump> {

		/** The logger. */
		private final ILogger logger;

		/** The location include filter to be applied on the profiled classes. */
		private final Predicate<String> locationIncludeFilter;

		/** Consumer that should be called with the newly built TestCoverageBuilder. */
		private final Consumer<TestCoverageBuilder> nextConsumer;

		private DumpConsumer(ILogger logger, Predicate<String> locationIncludeFilter,
							 Consumer<TestCoverageBuilder> nextConsumer) {
			this.logger = logger;
			this.locationIncludeFilter = locationIncludeFilter;
			this.nextConsumer = nextConsumer;
		}

		@Override
		public void accept(Dump dump) {
			String testId = dump.info.getId();
			if (testId.isEmpty()) {
				// Ignore intermediate coverage that does not belong to any specific test
				logger.debug("Found a session with empty name! This could indicate that coverage is dumped also for " +
						"coverage in between tests or that the given test name was empty");
				return;
			}
			try {
				TestCoverageBuilder testCoverage = buildCoverage(testId, dump.store, locationIncludeFilter);
				nextConsumer.accept(testCoverage);
			} catch (CoverageGenerationException e) {
				logger.error("Failed to generate coverage for test " + testId + "! Skipping to the next test.", e);
			}
		}

		/**
		 * Converts the given store to coverage data. The coverage will only contain line range coverage information.
		 */
		private TestCoverageBuilder buildCoverage(String testId, ExecutionDataStore executionDataStore,
												  Predicate<String> locationIncludeFilter) throws CoverageGenerationException {
			TestCoverageBuilder testCoverage = new TestCoverageBuilder(testId);
			for (ExecutionData executionData : executionDataStore.getContents()) {
				testCoverage.add(probesCache.getCoverage(executionData, locationIncludeFilter));
			}
			probesCache.flushLogger();
			return testCoverage;
		}
	}
}