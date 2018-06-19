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

import eu.cqse.teamscale.jacoco.cache.ProbeLookup;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.internal.xml.XMLElement;

import java.io.IOException;

/**
 * Serializes coverage data as XML fragments.
 */
public final class XMLCoverageWriter {

    public static final String TEST_ID_SEPARATOR = "!#!";
    private static final String INDENT = "\t";

    private XMLCoverageWriter() {
    }

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

    static void writeTest(final SessionInfo s, IBundleCoverage bundleCoverage, final XMLElement parent) throws IOException {
        parent.text("\n");
        final XMLElement element = parent.element("test");
        element.attr("externalId", stripPrefix(s.getId(), ExecutionDataVisitor.CPT_SESSION_ID_PREFIX));
        writeBundle(bundleCoverage, element);
        element.text("\n");
    }

    private static void writeBundle(final IBundleCoverage bundle, final XMLElement parent) throws IOException {
        if (isEmpty(bundle))
            return;
        for (final IPackageCoverage p : bundle.getPackages()) {
            writePackage(p, parent);
        }
    }

    private static String stripPrefix(String text, String prefix) {
        if (text.startsWith(prefix)) {
            return text.substring(prefix.length());
        }
        return text;
    }

    private static void writePackage(final IPackageCoverage p, final XMLElement parent) throws IOException {
        if (isEmpty(p))
            return;
        parent.text("\n" + INDENT);
        final XMLElement element = createChild(parent, "path", p.getName());
        for (final IClassCoverage s : p.getClasses()) {
            writeSourceFile(s, element);
        }
        element.text("\n" + INDENT);
    }

    private static boolean isEmpty(final ICoverageNode node) {
        final ICounter counter = node.getCounter(ICoverageNode.CounterEntity.LINE);
        return counter.getTotalCount() <= 0 || counter.getCoveredCount() <= 0;
    }

    private static void writeSourceFile(final IClassCoverage s,
                                        final XMLElement parent) throws IOException {
        if (isEmpty(s))
            return;
        parent.text("\n" + INDENT + INDENT);
        final XMLElement element = createChild(parent, "file", s.getSourceFileName());
        writeLines(s, element);
        element.text("\n" + INDENT + INDENT);
    }

    private static void writeLines(final IClassCoverage source, final XMLElement parent) throws IOException {
        StringBuilder nrString = new StringBuilder();
        for (IMethodCoverage methodCoverage : source.getMethods()) {
            ProbeLookup.AggregatedMethodCoverage method = (ProbeLookup.AggregatedMethodCoverage) methodCoverage;
            if (nrString.length() > 0) {
                nrString.append(",");
            }
            nrString.append(method.range.toString());
        }
        parent.text("\n" + INDENT + INDENT + INDENT);
        final XMLElement element = parent.element("lines");
        element.attr("nr", nrString.toString());
    }

}
