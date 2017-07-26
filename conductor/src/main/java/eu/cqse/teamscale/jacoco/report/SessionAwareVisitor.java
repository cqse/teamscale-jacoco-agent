package eu.cqse.teamscale.jacoco.report;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.internal.AbstractGroupVisitor;
import org.jacoco.report.internal.xml.XMLElement;
import org.jacoco.report.internal.xml.XMLGroupVisitor;

import java.io.IOException;

class SessionAwareVisitor extends XMLGroupVisitor {

    SessionAwareVisitor(final XMLElement element) throws IOException {
        super(element, null);
    }

    private SessionInfo sessionInfo;
    private ECoverageMode coverageMode;

    public final void visitSession(SessionInfo session, final IBundleCoverage bundle, final ISourceFileLocator locator, ECoverageMode mode) throws IOException {
        sessionInfo = session;
        coverageMode = mode;
        visitBundle(bundle, locator);
    }

    @Override
    protected void handleBundle(IBundleCoverage bundle, ISourceFileLocator locator) throws IOException {
        if(sessionInfo == null) {
            XMLCoverageWriter.writeBundle(bundle, element, coverageMode);
        } else {
            XMLCoverageWriter.writeSession(sessionInfo, bundle, element, coverageMode);
        }
    }

    @Override
    protected AbstractGroupVisitor handleGroup(String name) throws IOException {
        writeHeader(name);
        return new XMLGroupVisitor(element, name);
    }

    private void writeHeader(final String name) throws IOException {
        element.attr("name", name);
    }

    @Override
    protected void handleEnd() throws IOException {
        element.close();
    }
}
