package com.teamscale.jacoco.agent.store.upload.http

import com.teamscale.jacoco.agent.store.file.TimestampedFileStore
import com.teamscale.jacoco.agent.store.upload.UploadStoreBase
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit

import java.io.IOException
import java.nio.file.Path

/**
 * Uploads XMLs and metadata via HTTP multi-part form data requests.
 */
class HttpUploadStore
/** Constructor.  */
    (failureStore: TimestampedFileStore, uploadUrl: HttpUrl, additionalMetaDataFiles: List<Path>) :
    UploadStoreBase<IHttpUploadApi>(failureStore, uploadUrl, additionalMetaDataFiles) {

    override fun getApi(retrofit: Retrofit): IHttpUploadApi {
        return retrofit.create(IHttpUploadApi::class.java)
    }

    @Throws(IOException::class)
    override fun uploadCoverageZip(zipFileBytes: ByteArray): Response<ResponseBody> {
        return api.uploadCoverageZip(zipFileBytes)
    }

    /** {@inheritDoc}  */
    override fun describe(): String {
        return ("Uploading to " + uploadUrl + " (fallback in case of network errors to: " + failureStore.describe()
                + ")")
    }
}
