/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Marc R. Hoffmann - initial API and implementation
 *******************************************************************************/
package eu.cqse.teamscale.jacoco.report;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.internal.analysis.CounterImpl;
import org.jacoco.report.internal.xml.XMLElement;

import java.io.IOException;

import static org.jacoco.core.analysis.ICounter.EMPTY;
import static org.jacoco.core.analysis.ICounter.NOT_COVERED;

/**
 * Serializes coverage data as XML fragments.
 */
public final class XMLCoverageWriter {

    public static final String SESSION_ID_SEPARATOR = "!#!";

    /**
     * Creates a child element with a name attribute.
     *
     * @param parent  parent element
     * @param tagname name of the child tag
     * @param name    value of the name attribute
     * @return child element
     * @throws IOException if XML can't be written to the underlying output
     */
    private static XMLElement createChild(final XMLElement parent, final String tagname, final String name) throws IOException {
        final XMLElement child = parent.element(tagname);
        child.attr("name", name);
        return child;
    }

    /**
     * Writes the structure of a given bundle.
     *
     * @param bundle  bundle coverage data
     * @param element container element for the bundle data
     * @throws IOException if XML can't be written to the underlying output
     */
    static void writeBundle(final IBundleCoverage bundle, final XMLElement element, ECoverageMode mode) throws IOException {
        if (!containsCoverage(bundle))
            return;
        for (final IPackageCoverage p : bundle.getPackages()) {
            writePackage(p, element, mode);
        }
    }

    static void writeSession(final SessionInfo s, IBundleCoverage bundleCoverage, final XMLElement parent, ECoverageMode coverageMode) throws IOException {
        final XMLElement element = parent.element("session");
        String[] split = s.getId().split(SESSION_ID_SEPARATOR);
        element.attr("class", split[0]);
        element.attr("name", split[1]);
        element.attr("duration", split[2]);
        writeBundle(bundleCoverage, element, coverageMode);
    }

    private static void writePackage(final IPackageCoverage p, final XMLElement parent, ECoverageMode mode) throws IOException {
        if (!containsCoverage(p))
            return;
        final XMLElement element = createChild(parent, "package", p.getName());
        for (final ISourceFileCoverage s : p.getSourceFiles()) {
            writeSourceFile(s, element, mode);
        }
    }

    private static boolean containsCoverage(final ICoverageNode node) throws IOException {
        final ICounter counter = node.getCounter(ICoverageNode.CounterEntity.LINE);
        return counter.getTotalCount() > 0 && counter.getCoveredCount() > 0;
    }

    private static void writeSourceFile(final ISourceFileCoverage s,
                                        final XMLElement parent, ECoverageMode mode) throws IOException {
        if (!containsCoverage(s))
            return;
        final XMLElement element = createChild(parent, "sourcefile", s.getName());
        writeLines(s, element, mode);
    }

    private static void writeLines(final ISourceNode source, final XMLElement parent, ECoverageMode mode) throws IOException {
        final int last = source.getLastLine();
        for (int nr = source.getFirstLine(); nr <= last; nr++) {
            final ILine line = source.getLine(nr);
            if (line.getStatus() != EMPTY || (mode == ECoverageMode.IGNORE_ZERO && line.getStatus() == NOT_COVERED)) {
                final XMLElement element = parent.element("line");
                element.attr("nr", nr);
                ICounter lic = line.getInstructionCounter();
                ICounter bc = line.getBranchCounter();
                if(mode == ECoverageMode.ZERO) {
                    writeCounter(element, "mi", "ci", CounterImpl.getInstance(lic.getTotalCount(), 0));
                    writeCounter(element, "mb", "cb", CounterImpl.getInstance(bc.getTotalCount(), 0));
                } else {
                    writeCounter(element, "mi", "ci", lic);
                    writeCounter(element, "mb", "cb", bc);
                }
                parent.text("\n");
            }
        }
    }

    private static void writeCounter(final XMLElement element,
                                     final String missedattr, final String coveredattr,
                                     final ICounter counter) throws IOException {
        element.attr(missedattr, counter.getMissedCount());
        element.attr(coveredattr, counter.getCoveredCount());
    }

    private XMLCoverageWriter() {
    }

}
