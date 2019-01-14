package com.teamscale.report.testwise.closure;

import com.google.gson.Gson;
import com.teamscale.report.testwise.closure.model.ClosureCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import org.conqat.lib.commons.collections.Pair;
import org.conqat.lib.commons.collections.PairList;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Creates {@link TestwiseCoverage} from Google closure coverage files. The given {@link ClosureCoverage} files must be
 * augmented with the {@link ClosureCoverage#uniformPath} field, which is not part of the Google closure coverage
 * specification.
 */
public class ClosureTestwiseCoverageGenerator {

	/** Directories and zip files that contain closure coverage files. */
	private Collection<File> closureCoverageDirectories;

	/** Include filter to apply to all js files contained in the original Closure coverage report. */
	private Predicate<String> locationIncludeFilter;

	/**
	 * Create a new generator with a collection of report files.
	 *
	 * @param closureCoverageDirectories Root directory that contains the Google closure coverage reports.
	 * @param locationIncludeFilter      Filter for js files
	 */
	public ClosureTestwiseCoverageGenerator(Collection<File> closureCoverageDirectories, Predicate<String> locationIncludeFilter) {
		this.closureCoverageDirectories = closureCoverageDirectories;
		this.locationIncludeFilter = locationIncludeFilter;
	}

	/**
	 * Converts all JSON files in {@link #closureCoverageDirectories} to {@link TestCoverageBuilder}
	 * and takes care of merging coverage distributed over multiple files.
	 */
	public TestwiseCoverage readTestCoverage() {
		TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
		for (File closureCoverageDirectory : closureCoverageDirectories) {
			if (closureCoverageDirectory.isFile()) {
				testwiseCoverage.add(readTestCoverage(closureCoverageDirectory));
				continue;
			}
			List<File> coverageFiles = FileSystemUtils.listFilesRecursively(closureCoverageDirectory,
					file -> "json".equals(FileSystemUtils.getFileExtension(file)));
			for (File coverageReportFile : coverageFiles) {
				testwiseCoverage.add(readTestCoverage(coverageReportFile));
			}
		}
		return testwiseCoverage;
	}

	/**
	 * Reads the given JSON file and converts its content to {@link TestCoverageBuilder}.
	 * If this fails for some reason the method returns null.
	 */
	private TestCoverageBuilder readTestCoverage(File file) {
		try {
			FileReader fileReader = new FileReader(file);
			ClosureCoverage coverage = new Gson().fromJson(fileReader, ClosureCoverage.class);
			return convertToTestCoverage(coverage);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Converts the given {@link ClosureCoverage} to {@link TestCoverageBuilder}. */
	private TestCoverageBuilder convertToTestCoverage(ClosureCoverage coverage) {
		if (StringUtils.isEmpty(coverage.uniformPath)) {
			return null;
		}
		TestCoverageBuilder testCoverage = new TestCoverageBuilder(coverage.uniformPath);
		PairList<String, List<Boolean>> executedLines = PairList.zip(coverage.fileNames, coverage.executedLines);
		for (Pair<String, List<Boolean>> fileNameAndExecutedLines : executedLines) {
			if (!locationIncludeFilter.test(fileNameAndExecutedLines.getFirst())) {
				continue;
			}

			File coveredFile = new File(fileNameAndExecutedLines.getFirst());
			List<Boolean> coveredLines = fileNameAndExecutedLines.getSecond();
			String path = Optional.ofNullable(coveredFile.getParent()).orElse("");
			FileCoverageBuilder fileCoverage = new FileCoverageBuilder(path, coveredFile.getName());
			for (int i = 0; i < coveredLines.size(); i++) {
				if (coveredLines.get(i) != null && coveredLines.get(i)) {
					fileCoverage.addLine(i + 1);
				}
			}
			testCoverage.add(fileCoverage);
		}
		return testCoverage;
	}
}