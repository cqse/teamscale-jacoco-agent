package eu.cqse.teamscale.jacoco.agent.convert;

import java.io.IOException;
import java.util.List;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;

import eu.cqse.teamscale.jacoco.agent.dump.Dump;
import eu.cqse.teamscale.jacoco.agent.report.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.agent.util.AntPatternIncludeFilter;

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

		List<SessionInfo> sessioninfos = loader.getSessionInfoStore().getInfos();
		CCSMAssert.isFalse(sessioninfos.isEmpty(), "No sessions were recorded. Must implement handling for this.");
		SessionInfo sessionInfo = sessioninfos.get(0);

		AntPatternIncludeFilter locationIncludeFilter = new AntPatternIncludeFilter(
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters());
		XmlReportGenerator generator = new XmlReportGenerator(arguments.getClassDirectoriesOrZips(),
				locationIncludeFilter, arguments.shouldIgnoreDuplicateClassFiles());
		String xml = generator.convert(new Dump(loader.getExecutionDataStore(), sessionInfo));
		FileSystemUtils.writeFileUTF8(arguments.getOutputFile(), xml);
	}

}
