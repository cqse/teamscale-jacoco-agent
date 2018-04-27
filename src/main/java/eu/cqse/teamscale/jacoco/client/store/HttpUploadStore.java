package eu.cqse.teamscale.jacoco.client.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import eu.cqse.teamscale.jacoco.client.util.Benchmark;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Uploads XMLs and metadata via HTTP multi-part form data requests.
 */
public class HttpUploadStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The store to write failed uploads to. */
	private final TimestampedFileStore failureStore;

	/** The URL to upload to. */
	private final HttpUrl uploadUrl;

	/** Additional files to include in the uploaded zip. */
	private final List<Path> additionalMetaDataFiles;

	/** The API which performs the upload */
	private final IHttpUploadApi api;

	/** Constructor. */
	public HttpUploadStore(TimestampedFileStore failureStore, HttpUrl uploadUrl, List<Path> additionalMetaDataFiles) {
		this.failureStore = failureStore;
		this.uploadUrl = uploadUrl;
		this.additionalMetaDataFiles = additionalMetaDataFiles;

		Retrofit retrofit = new Retrofit.Builder().baseUrl(uploadUrl).build();
		api = retrofit.create(IHttpUploadApi.class);
	}

	/** {@inheritDoc} */
	@Override
	public void store(String xml) {
		try (Benchmark benchmark = new Benchmark("Uploading report via HTTP")) {
			if (!tryUploading(xml)) {
				logger.warn("Storing failed upload in {}", failureStore.getOutputDirectory());
				failureStore.store(xml);
			}
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	private boolean tryUploading(String xml) {
		logger.debug("Uploading coverage to {}", uploadUrl);

		byte[] zipFileBytes;
		try {
			zipFileBytes = createZipFile(xml);
		} catch (IOException e) {
			logger.error("Failed to compile coverage zip file for upload to {}", uploadUrl, e);
			return false;
		}

		try {
			Response<ResponseBody> response = api.uploadCoverageZip(zipFileBytes);
			if (response.isSuccessful()) {
				return true;
			}

			logger.error("Failed to upload coverage to {}. Request failed with error code {}. Response body:\n{}",
					uploadUrl, response.code(), String.valueOf(response.body()));
			return false;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}. Probably a network problem", uploadUrl, e);
			return false;
		}
	}

	/**
	 * Creates the zip file to upload which includes the given coverage XML and all
	 * {@link #additionalMetaDataFiles}.
	 */
	private byte[] createZipFile(String xml) throws IOException {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
					OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream)) {
				fillZipFile(zipOutputStream, writer, xml);
			}
			return byteArrayOutputStream.toByteArray();
		}
	}

	/**
	 * Fills the upload zip file with the given coverage XML and all
	 * {@link #additionalMetaDataFiles}.
	 */
	private void fillZipFile(ZipOutputStream zipOutputStream, OutputStreamWriter writer, String xml)
			throws IOException {
			zipOutputStream.putNextEntry(new ZipEntry("coverage.xml"));
			writer.write(xml);
			writer.flush();

			for (Path additionalFile : additionalMetaDataFiles) {
				zipOutputStream.putNextEntry(new ZipEntry(additionalFile.getFileName().toString()));
				zipOutputStream.write(FileSystemUtils.readFileBinary(additionalFile.toFile()));
			}
	}

	/** {@inheritDoc} */
	@Override
	public String describe() {
		return "Uploading to " + uploadUrl + " (fallback in case of network errors to: " + failureStore.describe()
				+ ")";
	}

}
