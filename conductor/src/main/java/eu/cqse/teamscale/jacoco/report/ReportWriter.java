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
    private UniversalExecutionDataReader universalExecutionDataReader;

    public ReportWriter(Collection<File> classesDirectory) {
        universalExecutionDataReader = new UniversalExecutionDataReader(classesDirectory);
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
                System.out.println("Session: " + info.getId());
                ReportWriter.this.processNextSession(info, executionDataStore);
            }
        };
        universalExecutionDataReader.readJacocoReport(execFile, executionDataVisitor, executionDataVisitor);
    }

    public void processNextSession(SessionInfo session, ExecutionDataStore executionDataStore) {
        // TODO add zero coverage
//        IBundleCoverage bundle = universalExecutionDataReader.analyzeFiles(executionDataVisitor.getMerged(), classFilesCache.values()).getBundle(title);
//        visitor.visitSession(null, bundle, null, ZERO);
        try {
            IBundleCoverage bundle = universalExecutionDataReader.buildCoverage(executionDataStore);
            visitor.visitSession(session, bundle, null, ECoverageMode.IGNORE_ZERO);
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
