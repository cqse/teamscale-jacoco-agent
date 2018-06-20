package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.cache.AnalyzerCache;
import eu.cqse.teamscale.jacoco.cache.ProbesCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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

    /**  */
    public void analyzeClassDirs(Collection<File> classesDirectories) {
        if (probesCache != null) {
            return;
        }
        probesCache = new ProbesCache();
        AnalyzerCache newAnalyzer = new AnalyzerCache(probesCache);
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
    public IBundleCoverage buildCoverage(ExecutionDataStore executionDataStore) {
        CoverageBuilder builder = new CoverageBuilder();
        for (ExecutionData executionData: executionDataStore.getContents()) {
            IClassCoverage coverage = probesCache.getCoverage(executionData);
            if (coverage != null) {
                builder.visitCoverage(coverage);
            }
        }
        return builder.getBundle("total");
    }
}