package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.PathCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestCoverage;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.jacoco.report.internal.xml.XMLDocument;
import org.jacoco.report.internal.xml.XMLElement;

import java.io.IOException;
import java.io.OutputStream;

/** Serializes coverage data as XML fragments. */
public final class TestwiseXmlReportWriter {

	/** The system dtd file. */
	private static final String SYSTEM = "testwise-coverage.dtd";

	/** The tab indentation character. */
	private static final String INDENT = "\t";

	/** The XML root element of the report. */
	private final XMLElement root;

	/** Constructor. */
	public TestwiseXmlReportWriter(OutputStream output) throws IOException {
		root = new XMLDocument("report", null, SYSTEM,
				"UTF-8", true, output);
	}

	/** Writes the given test coverage to the file. */
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

	/** Closes the report. */
	public void closeReport() throws IOException {
		root.text("\n");
		root.close();
	}

	/** Writes a "path" tag to the report. */
	private static void writePath(PathCoverage pathCoverage, XMLElement parent) throws IOException {
		parent.text("\n" + INDENT + INDENT);
		final XMLElement element = createChild(parent, "path", pathCoverage.path);
		for (String file: CollectionUtils.sort(pathCoverage.fileCoverageList.keySet())) {
			writeFile(pathCoverage.fileCoverageList.get(file), element);
		}
		element.text("\n" + INDENT + INDENT);
		element.close();
	}

	/** Writes a "file" tag to the report. */
	private static void writeFile(FileCoverage fileCoverage, XMLElement parent) throws IOException {
		parent.text("\n" + INDENT + INDENT + INDENT);
		final XMLElement element = createChild(parent, "file", fileCoverage.fileName);
		writeLines(fileCoverage, element);
		element.text("\n" + INDENT + INDENT + INDENT);
		element.close();
	}

	/** Writes a "lines" tag to the report. */
	private static void writeLines(FileCoverage fileCoverage, XMLElement parent) throws IOException {
		parent.text("\n" + INDENT + INDENT + INDENT + INDENT);
		final XMLElement element = parent.element("lines");
		element.attr("nr", fileCoverage.getCompactifiedRangesAsString());
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
