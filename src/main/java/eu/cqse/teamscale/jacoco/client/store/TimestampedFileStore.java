package eu.cqse.teamscale.jacoco.client.store;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import eu.cqse.teamscale.jacoco.client.util.Benchmark;

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

	/**
	 * Constructor.
	 */
	public TimestampedFileStore(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/** {@inheritDoc} */
	@Override
	public void store(String xml) {
		try (Benchmark benchmark = new Benchmark("Writing the report to a file")) {
			long currentTime = System.currentTimeMillis();
			Path outputPath = outputDirectory.resolve("jacoco-coverage-" + currentTime + ".xml");
			try {
				FileSystemUtils.writeFile(outputPath.toFile(), xml);
			} catch (IOException e) {
				logger.error("Failed to write XML to {}", outputPath, e);
			}
		}
	}

}
