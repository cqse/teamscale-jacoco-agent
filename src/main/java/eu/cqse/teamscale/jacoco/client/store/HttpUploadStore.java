package eu.cqse.teamscale.jacoco.client.store;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.cqse.teamscale.jacoco.client.util.Benchmark;

/**
 * Uploads XMLs and metadata via HTTP multi-part form data requests.
 */
public class HttpUploadStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The store to write failed uploads to. */
	private final TimestampedFileStore failureStore;

	private final URL uploadUrl;
	private final List<Path> additionalMetaDataFiles;

	/** Constructor. */
	public HttpUploadStore(TimestampedFileStore failureStore, URL uploadUrl, List<Path> additionalMetaDataFiles) {
		this.failureStore = failureStore;
		this.uploadUrl = uploadUrl;
		this.additionalMetaDataFiles = additionalMetaDataFiles;
	}

	/** {@inheritDoc} */
	@Override
	public void store(String xml) {
		try (Benchmark benchmark = new Benchmark("Uploading report to Teamscale")) {
			// TODO (FS) try uploading
			failureStore.store(xml);
		}
	}

}
