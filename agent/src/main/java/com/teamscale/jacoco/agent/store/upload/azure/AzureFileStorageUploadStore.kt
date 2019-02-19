package com.teamscale.jacoco.agent.store.upload.azure

import com.teamscale.client.EReportFormat
import com.teamscale.jacoco.agent.store.UploadStoreException
import com.teamscale.jacoco.agent.store.file.TimestampedFileStore
import com.teamscale.jacoco.agent.store.upload.UploadStoreBase
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageHttpUtils.EHttpMethod.HEAD
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageHttpUtils.EHttpMethod.PUT
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.AUTHORIZATION
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_LENGTH
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_TYPE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_CONTENT_LENGTH
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_RANGE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_TYPE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_WRITE
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern

/** Uploads the coverage archive to a provided azure file storage.  */
class AzureFileStorageUploadStore
/** Constructor.  */
@Throws(UploadStoreException::class)
constructor(
    failureStore: TimestampedFileStore, config: AzureFileStorageConfig,
    additionalMetaDataFiles: List<Path>
) : UploadStoreBase<IAzureUploadApi>(failureStore, config.url!!, additionalMetaDataFiles) {

    /** The access key for the azure file storage  */
    private val accessKey: String? = config.accessKey

    /** The account for the azure file storage  */
    private val account: String

    init {
        this.account = getAccount()

        validateUploadUrl()
    }

    /** Extracts and returns the account of the provided azure file storage from the URL.  */
    @Throws(UploadStoreException::class)
    private fun getAccount(): String {
        val matcher = AZURE_FILE_STORAGE_HOST_PATTERN.matcher(this.uploadUrl.host())
        return if (matcher.matches()) {
            matcher.group(1)
        } else {
            throw UploadStoreException(
                String.format(
                    "URL is malformed. Must be in the format " + "\"https://<account>.file.core.windows.net/<share>/\", but was instead: %s",
                    uploadUrl
                )
            )
        }
    }

    override fun describe(): String {
        return String.format("Uploading coverage to the Azure File Storage at %s", this.uploadUrl)
    }

    override fun getApi(retrofit: Retrofit): IAzureUploadApi {
        return retrofit.create(IAzureUploadApi::class.java)
    }

    @Throws(IOException::class, UploadStoreException::class)
    override fun uploadCoverageZip(zipFileBytes: ByteArray): Response<ResponseBody> {
        val fileName = createFileName()
        if (checkFile(fileName).isSuccessful) {
            logger.warn(String.format("The file %s does already exists at %s", fileName, uploadUrl))
        }

        return createAndFillFile(zipFileBytes, fileName)
    }

    /**
     * Makes sure that the upload url is valid and that it exists on the file storage.
     * If some directories do not exists, they will be created.
     */
    @Throws(UploadStoreException::class)
    private fun validateUploadUrl() {
        val pathParts = this.uploadUrl.pathSegments()

        if (pathParts.size < 2) {
            throw UploadStoreException(
                String.format(
                    "%s is too short for a file path on the storage. " + "At least the share must be provided: https://<account>.file.core.windows.net/<share>/",
                    uploadUrl.url().path
                )
            )
        }

        try {
            checkAndCreatePath(pathParts)
        } catch (e: IOException) {
            throw UploadStoreException(
                String.format(
                    "Checking the validity of %s failed. " + "There is probably something wrong with the URL or a problem with the account/key: ",
                    this.uploadUrl.url().path
                ), e
            )
        }

    }

    /**
     * Checks the directory path in the store url. Creates any missing directories.
     */
    @Throws(IOException::class, UploadStoreException::class)
    private fun checkAndCreatePath(pathParts: List<String>) {
        for (i in 2 until pathParts.size) {
            val directoryPath = String.format("/%s/", pathParts.subList(0, i).joinToString("/"))
            if (!checkDirectory(directoryPath).isSuccessful) {
                val mkdirResponse = createDirectory(directoryPath)
                if (!mkdirResponse.isSuccessful) {
                    throw UploadStoreException.createForResponseBody(
                        String.format("Creation of path '/%s' was unsuccessful", directoryPath), mkdirResponse
                    )
                }
            }
        }
    }

    /** Creates a file name for the zip-archive containing the coverage.  */
    private fun createFileName(): String {
        return String.format("%s-%s.zip", EReportFormat.JACOCO.name.toLowerCase(), System.currentTimeMillis())
    }

    /** Checks if the file with the given name exists  */
    @Throws(IOException::class, UploadStoreException::class)
    private fun checkFile(fileName: String): Response<Void> {
        val filePath = uploadUrl.url().path + fileName

        val headers = AzureFileStorageHttpUtils.baseHeaders
        val queryParameters = HashMap<String, String>()

        val auth = AzureFileStorageHttpUtils
            .getAuthorizationString(HEAD, account, accessKey!!, filePath, headers, queryParameters)

        headers.put(AUTHORIZATION, auth)
        return api.head(filePath, headers, queryParameters).execute()
    }

    /** Checks if the directory given by the specified path does exist.  */
    @Throws(IOException::class, UploadStoreException::class)
    private fun checkDirectory(directoryPath: String): Response<Void> {
        val headers = AzureFileStorageHttpUtils.baseHeaders

        val queryParameters = HashMap<String, String>()
        queryParameters["restype"] = "directory"

        val auth = AzureFileStorageHttpUtils
            .getAuthorizationString(HEAD, account, accessKey!!, directoryPath, headers, queryParameters)

        headers.put(AUTHORIZATION, auth)
        return api.head(directoryPath, headers, queryParameters).execute()
    }

    /**
     * Creates the directory specified by the given path.
     * The path must contain the share where it should be created on.
     */
    @Throws(IOException::class, UploadStoreException::class)
    private fun createDirectory(directoryPath: String): Response<ResponseBody> {
        val headers = AzureFileStorageHttpUtils.baseHeaders

        val queryParameters = HashMap<String, String>()
        queryParameters["restype"] = "directory"

        val auth = AzureFileStorageHttpUtils
            .getAuthorizationString(PUT, account, accessKey!!, directoryPath, headers, queryParameters)

        headers.put(AUTHORIZATION, auth)
        return api.put(directoryPath, headers, queryParameters).execute()
    }

    /** Creates and fills a file with the given data and name.  */
    @Throws(UploadStoreException::class, IOException::class)
    private fun createAndFillFile(zipFilBytes: ByteArray, fileName: String): Response<ResponseBody> {
        val response = createFile(zipFilBytes, fileName)
        if (response.isSuccessful) {
            return fillFile(zipFilBytes, fileName)
        }
        logger.warn(String.format("Creation of file '%s' was unsuccessful.", fileName))
        return response
    }

    /**
     * Creates an empty file with the given name.
     * The size is defined by the length of the given byte array.
     */
    @Throws(IOException::class, UploadStoreException::class)
    private fun createFile(zipFileBytes: ByteArray, fileName: String): Response<ResponseBody> {
        val filePath = uploadUrl.url().path + fileName

        val headers = AzureFileStorageHttpUtils.baseHeaders
        headers.put(X_MS_CONTENT_LENGTH, zipFileBytes.size.toString() + "")
        headers.put(X_MS_TYPE, "file")

        val queryParameters = HashMap<String, String>()

        val auth = AzureFileStorageHttpUtils
            .getAuthorizationString(PUT, account, accessKey!!, filePath, headers, queryParameters)

        headers.put(AUTHORIZATION, auth)
        return api.put(filePath, headers, queryParameters).execute()
    }

    /**
     * Fills the file defined by the name with the given data.
     * Should be used with [.createFile], because the request only writes exactly the length of
     * the given data, so the file should be exactly as big as the data, otherwise it will be partially filled or is
     * not big enough.
     */
    @Throws(IOException::class, UploadStoreException::class)
    private fun fillFile(zipFileBytes: ByteArray, fileName: String): Response<ResponseBody> {
        val filePath = uploadUrl.url().path + fileName

        val range = "bytes=0-" + (zipFileBytes.size - 1)
        val contentType = "application/octet-stream"

        val headers = AzureFileStorageHttpUtils.baseHeaders
        headers.put(X_MS_WRITE, "update")
        headers.put(X_MS_RANGE, range)
        headers.put(CONTENT_LENGTH, "" + zipFileBytes.size)
        headers.put(CONTENT_TYPE, contentType)

        val queryParameters = HashMap<String, String>()
        queryParameters["comp"] = "range"

        val auth = AzureFileStorageHttpUtils
            .getAuthorizationString(PUT, account, accessKey!!, filePath, headers, queryParameters)

        headers.put(AUTHORIZATION, auth)
        val content = RequestBody.create(MediaType.parse(contentType), zipFileBytes)
        return api.putData(filePath, headers, queryParameters, content).execute()
    }

    companion object {

        /** Pattern matches the host of a azure file storage  */
        private val AZURE_FILE_STORAGE_HOST_PATTERN = Pattern
            .compile("^(\\w*)\\.file\\.core\\.windows\\.net$")
    }
}
