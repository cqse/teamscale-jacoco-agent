package com.teamscale.jacoco.agent.store.file;

import com.teamscale.jacoco.agent.store.upload.delay.ICachingXmlStore;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Writes XMLs to files in a folder. The files are timestamped with the time of writing the trace to make each file
 * reasonably unique so they don't overwrite each other.
 */
public class TimestampedFileStore implements ICachingXmlStore {

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

	@Override
	public Stream<String> streamCachedXmls() {
		return streamXmlPaths().map(path -> {
			try {
				return FileSystemUtils.readFileUTF8(path.toFile());
			} catch (IOException e) {
				logger.error("Unable to read cached coverage XML file {}. Ignoring this file.", path.toString(), e);
				return null;
			}
		}).filter(Objects::nonNull);
	}

	@Override
	public void clear() {
		streamXmlPaths().forEach(path -> {
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				logger.error("Failed to delete cached coverage XML file {}.", path.toString(), e);
			}
		});
	}

	private Stream<Path> streamXmlPaths() {
		try {
			return Files.list(outputDirectory).filter(path -> {
				String fileName = path.getFileName().toString();
				return fileName.startsWith("jacoco-") && fileName.endsWith(".xml");
			});
		} catch (IOException e) {
			logger.error("Failed to list cached coverage XML files in {}", outputDirectory.toString(), e);
			return Stream.empty();
		}
	}
}
