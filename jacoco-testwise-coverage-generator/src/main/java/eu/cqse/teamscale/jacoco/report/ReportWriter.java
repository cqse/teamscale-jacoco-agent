package eu.cqse.teamscale.jacoco.report;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class ReportWriter {

    private SessionAwareVisitor visitor;
    private OutputStream outputStream;
    private CachingExecutionDataReader cachingExecutionDataReader;
    private final XMLReportGenerator.SessionCallback sessionCallback;

    public ReportWriter(Collection<File> classesDirectories) {
        this(classesDirectories, null);
    }

    public ReportWriter(Collection<File> classesDirectories, XMLReportGenerator.SessionCallback sessionCallback) {
        cachingExecutionDataReader = new CachingExecutionDataReader(classesDirectories);
        this.sessionCallback = sessionCallback;
    }

    public void startReport(File report) throws IOException {
        startReport(new FileOutputStream(report));
    }

    public void startReport(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
        this.visitor = new SessionAwareXMLFormatter().createVisitor(outputStream);
    }

    public void readExecutionData(File execFile) {
        ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor() {
            @Override
            public void processNextSession(SessionInfo info, ExecutionDataStore executionDataStore) {
                ReportWriter.this.processNextSession(info, executionDataStore);
            }
        };
        cachingExecutionDataReader.readJacocoReport(execFile, executionDataVisitor, executionDataVisitor);
    }

    public void processNextSession(SessionInfo session, ExecutionDataStore executionDataStore) {
        // TODO add zero coverage
//        IBundleCoverage bundle = cachingExecutionDataReader.analyzeFiles(executionDataVisitor.getMerged(), classFilesCache.values()).getBundle(title);
//        visitor.visitSession(null, bundle, null, ZERO);
        if (sessionCallback != null) {
            sessionCallback.onProcessSession(session.getId());
        }
        try {
            IBundleCoverage bundle = cachingExecutionDataReader.buildCoverage(executionDataStore);
            visitor.visitSession(session, bundle, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void finishReport() throws IOException {
        visitor.visitEnd();
        outputStream.flush();
        outputStream.close();
    }
}
