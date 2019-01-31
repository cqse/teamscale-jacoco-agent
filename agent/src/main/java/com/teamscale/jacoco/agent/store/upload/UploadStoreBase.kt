package com.teamscale.jacoco.agent.store.upload

import com.teamscale.jacoco.agent.store.IXmlStore
import com.teamscale.jacoco.agent.store.UploadStoreException
import com.teamscale.jacoco.agent.store.file.TimestampedFileStore
import com.teamscale.jacoco.util.Benchmark
import com.teamscale.jacoco.util.LoggingUtils
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Base class for uploading the coverage zip to a provided url  */
abstract class UploadStoreBase<T>
/** Constructor.  */
    (
    /** The store to write failed uploads to.  */
    protected val failureStore: TimestampedFileStore,
    /** The URL to upload to.  */
    protected val uploadUrl: HttpUrl,
    /** Additional files to include in the uploaded zip.  */
    protected val additionalMetaDataFiles: List<Path>
) : IXmlStore {

    /** The logger.  */
    protected val logger = LoggingUtils.getLogger(this)

    /** The API which performs the upload  */
    protected val api: T

    init {

        val retrofit = Retrofit.Builder().baseUrl(uploadUrl).build()
        api = getApi(retrofit)
    }

    /** Returns the API for creating request to the http store  */
    protected abstract fun getApi(retrofit: Retrofit): T

    /** Uploads the coverage zip to the server  */
    @Throws(IOException::class, UploadStoreException::class)
    protected abstract fun uploadCoverageZip(zipFileBytes: ByteArray): Response<ResponseBody>

    override fun store(xml: String) {
        Benchmark("Uploading report via HTTP").use {
            if (!tryUpload(xml)) {
                logger.warn("Storing failed upload in {}", failureStore.outputDirectory)
                failureStore.store(xml)
            }
        }
    }

    /** Performs the upload and returns `true` if successful.  */
    protected fun tryUpload(xml: String): Boolean {
        logger.debug("Uploading coverage to {}", uploadUrl)

        val zipFileBytes: ByteArray
        try {
            zipFileBytes = createZipFile(xml)
        } catch (e: IOException) {
            logger.error("Failed to compile coverage zip file for upload to {}", uploadUrl, e)
            return false
        }

        try {
            val response = uploadCoverageZip(zipFileBytes)
            if (response.isSuccessful) {
                return true
            }

            var errorBody = ""
            if (response.errorBody() != null) {
                errorBody = response.errorBody()!!.string()
            }

            logger.error(
                "Failed to upload coverage to {}. Request failed with error code {}. Error:\n{}",
                uploadUrl, response.code(), errorBody
            )
            return false
        } catch (e: IOException) {
            logger.error("Failed to upload coverage to {}. Probably a network problem", uploadUrl, e)
            return false
        } catch (e: UploadStoreException) {
            logger.error("Failed to upload coverage to {}. The configuration is probably incorrect", uploadUrl, e)
            return false
        }

    }

    /**
     * Creates the zip file to upload which includes the given coverage XML and all
     * [.additionalMetaDataFiles].
     */
    @Throws(IOException::class)
    private fun createZipFile(xml: String): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            ZipOutputStream(byteArrayOutputStream).use { zipOutputStream ->
                OutputStreamWriter(zipOutputStream, "UTF-8").use { writer ->
                    fillZipFile(
                        zipOutputStream,
                        writer,
                        xml
                    )
                }
            }
            return byteArrayOutputStream.toByteArray()
        }
    }

    /**
     * Fills the upload zip file with the given coverage XML and all
     * [.additionalMetaDataFiles].
     */
    @Throws(IOException::class)
    private fun fillZipFile(zipOutputStream: ZipOutputStream, writer: OutputStreamWriter, xml: String) {
        zipOutputStream.putNextEntry(ZipEntry("coverage.xml"))
        writer.write(xml)

        // We flush the writer, but don't close it here, because closing the writer
        // would also close the zipOutputStream, making further writes impossible.
        writer.flush()

        for (additionalFile in additionalMetaDataFiles) {
            zipOutputStream.putNextEntry(ZipEntry(additionalFile.fileName.toString()))
            zipOutputStream.write(additionalFile.toFile().readBytes())
        }
    }
}
