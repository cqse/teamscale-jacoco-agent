package eu.cqse.teamscale.jacoco.converter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.AntPatternUtils;
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

	/**
	 * Ant-style include filters to apply to all locations during class file
	 * traversal.
	 */
	private final List<Pattern> locationIncludeFilters;

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/**
	 * Constructor.
	 */
	public XmlReportGenerator(List<File> codeDirectoriesOrArchives, List<String> locationIncludeFilters) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
		this.locationIncludeFilters = CollectionUtils.map(locationIncludeFilters,
				filter -> AntPatternUtils.convertPattern(filter, false));
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
		Analyzer analyzer = new Analyzer(store, coverageBuilder) {
			@Override
			public int analyzeAll(java.io.InputStream input, String location) throws IOException {
				if (isFiltered(location)) {
					logger.debug("Filtering location {}", location);
					return 0;
				}
				return super.analyzeAll(input, location);
			};
		};

		for (File file : codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file);
		}

		return coverageBuilder.getBundle("dummybundle");
	}

	private boolean isFiltered(String location) {
		if (locationIncludeFilters.isEmpty()) {
			return false;
		}

		for (Pattern pattern : locationIncludeFilters) {
			if (pattern.matcher(location).matches()) {
				return false;
			}
		}

		return true;
	}

}
