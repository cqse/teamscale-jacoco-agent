package eu.cqse.teamscale.jacoco.report;

import eu.cqse.teamscale.jacoco.cache.ProbesCache;
import eu.cqse.teamscale.jacoco.cache.AnalyzerCache;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataWriter;

import java.io.*;
import java.util.Collection;

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 * The class supports all major jacoco versions starting with 0.7.5 up to 0.8.x.
 * https://github.com/jacoco/jacoco/wiki/ExecFileVersions
 */
class CachingExecutionDataReader {

    private final Collection<File> classesDirectories;
    private ProbesCache probesCache;

    CachingExecutionDataReader(Collection<File> classesDirectories) {
        this.classesDirectories = classesDirectories;
    }

    private void analyzeClassDirs() {
        probesCache = new ProbesCache();
        AnalyzerCache newAnalyzer = new AnalyzerCache(probesCache);
        for (File classDir : classesDirectories) {
            try {
                if (classDir.exists()) {
                    newAnalyzer.analyzeAll(classDir);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (probesCache.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (File classesDirectory : classesDirectories) {
                builder.append(classesDirectory.getPath());
                builder.append(", ");
            }
            throw new RuntimeException("No class files found in the given directories! " + builder.toString());
        }
    }

    /**
     * Read JaCoCo report determining the format to be used.
     *
     * @param jacocoExecutionData  execution data file
     * @param executionDataVisitor visitor to store execution data.
     * @param sessionInfoStore     visitor to store info session.
     */
    void readJacocoReport(File jacocoExecutionData, IExecutionDataVisitor executionDataVisitor, ISessionInfoVisitor sessionInfoStore) {
        if (jacocoExecutionData == null) {
            return;
        }
        if (isCurrentReportFormat(jacocoExecutionData)) {
            throw new RuntimeException("Incompatible format! Only JaCoCo 0.7.5 and newer is supported!");
        }
        if (probesCache == null) {
            analyzeClassDirs();
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(jacocoExecutionData))) {
            ExecutionDataReader reader = new ExecutionDataReader(inputStream);
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.setExecutionDataVisitor(executionDataVisitor);
            while (true) {
                if (!(reader.read())) break;
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read %s", jacocoExecutionData.getAbsolutePath()), e);
        }
    }

    /**
     * @return true if the execution data file has been created with JaCoCo 0.7.5 - 0.8.x.
     * False indicates 0.5.x - 0.7.4.
     */
    private static boolean isCurrentReportFormat(File jacocoExecutionData) {
        if (jacocoExecutionData == null) {
            return true;
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(jacocoExecutionData))) {
            byte firstByte = dis.readByte();
            if (firstByte != ExecutionDataWriter.BLOCK_HEADER)
                throw new RuntimeException();
            if (dis.readChar() != ExecutionDataWriter.MAGIC_NUMBER)
                throw new RuntimeException();
            char version = dis.readChar();
            return version == ExecutionDataWriter.FORMAT_VERSION;
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException(String.format("Unable to read %s to determine JaCoCo binary format.", jacocoExecutionData.getAbsolutePath()), e);
        }
    }

    /**
     * Converts the given store to coverage data. The coverage will only contain line coverage information.
     */
    IBundleCoverage buildCoverage(ExecutionDataStore executionDataStore) {
        CoverageBuilder builder = new CoverageBuilder();
        for (ExecutionData executionData : executionDataStore.getContents()) {
            IClassCoverage coverage = probesCache.getCoverage(executionData);
            if (coverage != null) {
                builder.visitCoverage(coverage);
            }
        }
        return builder.getBundle("total");
    }
}