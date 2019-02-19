package com.teamscale.jacoco.agent.store

import okhttp3.ResponseBody
import retrofit2.Response

import java.io.IOException

/**
 * Exception thrown from an upload store. Either during the upload or in the validation process.
 */
class UploadStoreException(message: String, e: Throwable? = null) : Exception(message, e) {

    companion object {
        fun createForResponseBody(message: String, response: Response<ResponseBody>): Exception {
            return try {
                val errorBodyMessage = response.errorBody()?.string()
                UploadStoreException(String.format("%s (%s): \n%s", message, response.code(), errorBodyMessage))
            } catch (e: IOException) {
                UploadStoreException(message, e)
            }
        }
    }
}
