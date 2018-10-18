package eu.cqse.teamscale.jacoco.agent.store.upload;

import eu.cqse.teamscale.client.EReportFormat;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.UploadStoreException;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import eu.cqse.teamscale.jacoco.util.LoggingUtils;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class HttpUploadStoreBase<T> implements IXmlStore {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** The store to write failed uploads to. */
	protected final TimestampedFileStore failureStore;

	/** The URL to upload to. */
	protected final HttpUrl uploadUrl;

	/** Additional files to include in the uploaded zip. */
	protected final List<Path> additionalMetaDataFiles;

	/** The API which performs the upload */
	protected final T api;

	/** Constructor. */
	public HttpUploadStoreBase(TimestampedFileStore failureStore, HttpUrl uploadUrl, List<Path> additionalMetaDataFiles) {
		this.failureStore = failureStore;
		this.uploadUrl = uploadUrl;
		this.additionalMetaDataFiles = additionalMetaDataFiles;

		Retrofit retrofit = new Retrofit.Builder().baseUrl(uploadUrl).build();
		api = getApi(retrofit);
	}

	/** TODO */
	protected abstract T getApi(Retrofit retrofit);

	/** TODO */
	protected abstract Set<EReportFormat> getSupportedFormats();

	/** TODO */
	protected abstract Response<ResponseBody> uploadCoverageZip(byte[] zipFileBytes) throws IOException, UploadStoreException;

	@Override
	public void store(String xml, EReportFormat format) {
		CCSMAssert.isTrue(getSupportedFormats().contains(format), "Upload does only support JaCoCo " +
				"coverage and cannot be used with Test Impact mode.");
		try (Benchmark benchmark = new Benchmark("Uploading report via HTTP")) {
			if (!tryUploading(xml)) {
				logger.warn("Storing failed upload in {}", failureStore.getOutputDirectory());
				failureStore.store(xml, format);
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
			Response<ResponseBody> response = uploadCoverageZip(zipFileBytes);
			if (response.isSuccessful()) {
				return true;
			}

			logger.error("Failed to upload coverage to {}. Request failed with error code {}. Response body:\n{}",
					uploadUrl, response.code(), String.valueOf(response.body()));
			return false;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}. Probably a network problem", uploadUrl, e);
			return false;
		} catch (UploadStoreException e) {
			logger.error("Failed to upload coverage to {}. The configuration is probably incorrect", uploadUrl, e);
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

		// We flush the writer, but don't close it here, because closing the writer
		// would also close the zipOutputStream, making further writes impossible.
		writer.flush();

		for (Path additionalFile : additionalMetaDataFiles) {
			zipOutputStream.putNextEntry(new ZipEntry(additionalFile.getFileName().toString()));
			zipOutputStream.write(FileSystemUtils.readFileBinary(additionalFile.toFile()));
		}
	}
}
