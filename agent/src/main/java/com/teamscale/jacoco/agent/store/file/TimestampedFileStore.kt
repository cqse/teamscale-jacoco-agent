package com.teamscale.jacoco.agent.store.file;

import com.teamscale.client.EReportFormat;
import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.util.Benchmark;
import com.teamscale.jacoco.util.LoggingUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes XMLs to files in a folder. The files are timestamped with the time of
 * writing the trace to make each file reasonably unique so they don't overwrite
 * each other.
 */
public class TimestampedFileStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LoggingUtils.getLogger(this);

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
	public void store(String xml) {
		try (Benchmark benchmark = new Benchmark("Writing the JaCoCo report to a file")) {
			long currentTime = System.currentTimeMillis();
			Path outputPath = outputDirectory.resolve("jacoco-" + currentTime + ".xml");
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
