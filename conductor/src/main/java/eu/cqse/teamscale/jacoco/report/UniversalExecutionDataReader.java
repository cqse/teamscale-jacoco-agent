package eu.cqse.teamscale.jacoco.report;

import eu.cqse.teamscale.jacoco.common.cache.ProbesCache;
import eu.cqse.teamscale.jacoco.common.cache.AnalyzerCache;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataWriter;

import java.io.*;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 * The class is universal, because it supports all major jacoco versions from 0.5.x up to 0.7.x.
 * https://github.com/jacoco/jacoco/wiki/ExecFileVersions
 */
class UniversalExecutionDataReader {

    private final Collection<File> classesDirectories;
    private ProbesCache probesCache;
    private Boolean useCurrentBinaryFormat;

    UniversalExecutionDataReader(Collection<File> classesDirectories) {
        this.classesDirectories = classesDirectories;
    }

    private void analyzeClassDirs() {
        probesCache = new ProbesCache();
        AnalyzerCache newAnalyzer = new AnalyzerCache(probesCache, useCurrentBinaryFormat);
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
        boolean useCurrentBinaryFormat = isCurrentReportFormat(jacocoExecutionData);
        if (this.useCurrentBinaryFormat == Boolean.valueOf(!useCurrentBinaryFormat)) {
            throw new RuntimeException("Incompatible format! Cannot merge execution data from two different jacoco versions!");
        }
        this.useCurrentBinaryFormat = useCurrentBinaryFormat;
        if (probesCache == null) {
            analyzeClassDirs();
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(jacocoExecutionData))) {
            if (useCurrentBinaryFormat) {
                ExecutionDataReader reader = new ExecutionDataReader(inputStream);
                reader.setSessionInfoVisitor(sessionInfoStore);
                reader.setExecutionDataVisitor(executionDataVisitor);
                while (true) {
                    if (!(reader.read())) break;
                }
            } else {
                eu.cqse.teamscale.jacoco.previous.data.ExecutionDataReader reader = new eu.cqse.teamscale.jacoco.previous.data.ExecutionDataReader(inputStream);
                reader.setSessionInfoVisitor(sessionInfoStore);
                reader.setExecutionDataVisitor(executionDataVisitor);
                while (true) {
                    if (!(reader.read())) break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read %s", jacocoExecutionData.getAbsolutePath()), e);
        }
    }

    /**
     * @return true if the execution data file has been created with JaCoCo 0.7.5 - 0.7.x.
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
            boolean isCurrentFormat = version == ExecutionDataWriter.FORMAT_VERSION;
            if (!isCurrentFormat) {
                System.out.println("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
            }
            return isCurrentFormat;
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