package eu.cqse.teamcsale.jacoco.converter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;

/** Creates an XML report from binary execution data. */
public class XmlReportGenerator {

	/** Directories and zip files that contain class files. */
	private final List<File> codeDirectoriesOrArchives;

	/** Constructor. */
	public XmlReportGenerator(List<File> codeDirectoriesOrArchives) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
	}

	/**
	 * Creates the report.
	 */
	public String convert(ExecutionData data) throws IOException {
		IBundleCoverage bundleCoverage = analyzeStructureAndAnnotateCoverage(data);
		return createReport(bundleCoverage, data);
	}

	/** Creates an XML report based on the given coverage data. */
	private String createReport(IBundleCoverage bundleCoverage, ExecutionData data) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		XMLFormatter xmlFormatter = new XMLFormatter();
		IReportVisitor visitor = xmlFormatter.createVisitor(output);

		SessionInfo sessionInfo = new SessionInfo("dummyid", 123l, 456l);
		visitor.visitInfo(Collections.singletonList(sessionInfo), Collections.singletonList(data));
		visitor.visitBundle(bundleCoverage, null);
		visitor.visitEnd();

		return output.toString(FileSystemUtils.UTF8_ENCODING);
	}

	/**
	 * Analyzes the structure of the class files in
	 * {@link #codeDirectoriesOrArchives} and builds an in-memory coverage report.
	 */
	private IBundleCoverage analyzeStructureAndAnnotateCoverage(ExecutionData data) throws IOException {
		CoverageBuilder coverageBuilder = new CoverageBuilder();
		ExecutionDataStore store = new ExecutionDataStore();
		store.put(data);
		Analyzer analyzer = new Analyzer(store, coverageBuilder);
		// TODO (FS) filter analyzed class files
		/*
		 * {
		 * 
		 * @Override public int analyzeAll(InputStream input, String location) throws
		 * IOException { if (!location.endsWith(".class")) { return 0; } File file = new
		 * File(location);
		 * 
		 * if (location.contains("test-data") ||
		 * includedClassFiles.contains(file.getName())) { //
		 * System.out.println("Ignoring: " + location); return 0; }
		 * includedClassFiles.add(file.getName());
		 * 
		 * return super.analyzeAll(input, location); } };
		 */

		for (File file : codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file);
		}

		return coverageBuilder.getBundle("dummybundle");
	}

}
