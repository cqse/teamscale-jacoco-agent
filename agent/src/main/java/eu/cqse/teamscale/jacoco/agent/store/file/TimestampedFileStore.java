package eu.cqse.teamscale.jacoco.agent.store.file;

import eu.cqse.teamscale.client.EReportFormat;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes XMLs to files in a folder. The files are timestamped with the time of
 * writing the trace to make each file reasonably unique so they don't overwrite
 * each other.
 */
public class TimestampedFileStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The directory to which to write the XML files. */
	private final Path outputDirectory;

	/** Constructor. */
	public TimestampedFileStore(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/** @see #outputDirectory */
	public Path getOutputDirectory() {
		return outputDirectory;
	}

	/** {@inheritDoc} */
	@Override
	public void store(String xml, EReportFormat format) {
		try (Benchmark benchmark = new Benchmark("Writing the " + format + " report to a file")) {
			long currentTime = System.currentTimeMillis();
			Path outputPath = outputDirectory.resolve(format.filePrefix + "-" + currentTime + "." + format.extension);
			try {
				FileSystemUtils.writeFile(outputPath.toFile(), xml);
			} catch (IOException e) {
				logger.error("Failed to write XML to {}", outputPath, e);
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public String describe() {
		return "Saving to local filesystem path " + outputDirectory;
	}

}
