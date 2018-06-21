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
package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.LineRange;
import eu.cqse.teamscale.jacoco.report.testwise.model.PathCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestCoverage;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.jacoco.report.internal.xml.XMLDocument;
import org.jacoco.report.internal.xml.XMLElement;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Serializes coverage data as XML fragments.
 */
public final class TestwiseXmlReportWriter {

    private static final String SYSTEM = "testwise-coverage.dtd";

    private static final String INDENT = "\t";

    private final XMLElement root;

    public TestwiseXmlReportWriter(OutputStream output) throws IOException {
        root = new XMLDocument("report", null, SYSTEM,
                "UTF-8", true, output);
    }

    public void writeTestCoverage(TestCoverage testCoverage) throws IOException {
        root.text("\n" + INDENT);
        final XMLElement element = root.element("test");
        element.attr("externalId", testCoverage.externalId);
        for (String path: CollectionUtils.sort(testCoverage.pathCoverageList.keySet())) {
            writePath(testCoverage.pathCoverageList.get(path), element);
        }
        element.text("\n" + INDENT);
        element.close();
    }

    public void closeReport() throws IOException {
        root.text("\n");
        root.close();
    }

    private static void writePath(PathCoverage pathCoverage, XMLElement parent) throws IOException {
        parent.text("\n" + INDENT + INDENT);
        final XMLElement element = createChild(parent, "path", pathCoverage.path);
        for (String file: CollectionUtils.sort(pathCoverage.fileCoverageList.keySet())) {
            writeFile(pathCoverage.fileCoverageList.get(file), element);
        }
        element.text("\n" + INDENT + INDENT);
        element.close();
    }

    private static void writeFile(FileCoverage fileCoverage, XMLElement parent) throws IOException {
        parent.text("\n" + INDENT + INDENT + INDENT);
        final XMLElement element = createChild(parent, "file", fileCoverage.fileName);
        writeLines(fileCoverage, element);
        element.text("\n" + INDENT + INDENT + INDENT);
        element.close();
    }

    private static void writeLines(FileCoverage fileCoverage, XMLElement parent) throws IOException {
        parent.text("\n" + INDENT + INDENT + INDENT + INDENT);
        final XMLElement element = parent.element("lines");
        element.attr("nr", fileCoverage.getRangesAsString());
        element.close();
    }

    /**
     * Creates a child element with a name attribute.
     *
     * @param parent  parent element
     * @param tagName name of the child tag
     * @param name    value of the name attribute
     * @return child element
     * @throws IOException if XML can't be written to the underlying output
     */
    private static XMLElement createChild(XMLElement parent, String tagName, String name) throws IOException {
        final XMLElement child = parent.element(tagName);
        child.attr("name", name);
        return child;
    }

}
