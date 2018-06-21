package eu.cqse.teamscale.jacoco.agent.convert;

import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.linebased.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.util.AntPatternIncludeFilter;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.IOException;

/** Converts one .exec binary coverage file to XML. */
public class Converter {

	/** The command line arguments. */
	private ConvertCommand arguments;

	/** Constructor. */
	public Converter(ConvertCommand arguments) {
		this.arguments = arguments;
	}

	/** Converts one .exec binary coverage file to XML. */
	public void run() throws IOException {
		ExecFileLoader loader = new ExecFileLoader();
		loader.load(arguments.getInputFile());

		SessionInfo sessionInfo = loader.getSessionInfoStore().getMerged("dummy");
		ExecutionDataStore executionDataStore = loader.getExecutionDataStore();

		AntPatternIncludeFilter locationIncludeFilter = new AntPatternIncludeFilter(
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters());
		XmlReportGenerator generator = new XmlReportGenerator(arguments.getClassDirectoriesOrZips(),
				locationIncludeFilter, arguments.shouldIgnoreDuplicateClassFiles());

		String xml = generator.convert(new Dump(sessionInfo, executionDataStore));
		FileSystemUtils.writeFileUTF8(arguments.getOutputFile(), xml);
	}
}
