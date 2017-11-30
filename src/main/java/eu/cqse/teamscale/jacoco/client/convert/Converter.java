package eu.cqse.teamscale.jacoco.client.convert;

import java.io.IOException;
import java.util.List;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;

import eu.cqse.teamscale.jacoco.client.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.client.watch.IJacocoController.Dump;

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
		XmlReportGenerator generator = new XmlReportGenerator(arguments.getClassDirectoriesOrZips(),
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters(),
				arguments.shouldIgnoreDuplicateClassFiles());
		List<SessionInfo> sessioninfos = loader.getSessionInfoStore().getInfos();
		CCSMAssert.isFalse(sessioninfos.isEmpty(), "No sessions were recorded. Must implement handling for this.");
		SessionInfo sessionInfo = sessioninfos.get(0);
		String xml = generator.convert(new Dump(loader.getExecutionDataStore(), sessionInfo));
		FileSystemUtils.writeFileUTF8(arguments.getOutputFile(), xml);
	}

}
