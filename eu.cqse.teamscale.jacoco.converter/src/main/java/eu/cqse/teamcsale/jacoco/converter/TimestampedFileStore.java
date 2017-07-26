package eu.cqse.teamcsale.jacoco.converter;

import java.nio.file.Path;

public class TimestampedFileStore implements IXmlStore {

	private final Path outputDirectory;

	/**
	 * Constructor.
	 */
	public TimestampedFileStore(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@Override
	public void store(String xml) {
		long currentTime = System.currentTimeMillis();
		Path outputPath = outputDirectory.resolve("jacoco-coverage-" + currentTime + ".xml");
		// TODO (FS) write to path FileSYstemUtils
	}

}
