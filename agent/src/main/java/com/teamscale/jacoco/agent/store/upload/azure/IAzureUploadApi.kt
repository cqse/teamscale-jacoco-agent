package com.teamscale.jacoco.agent.store.upload.azure

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.HEAD
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

/** [Retrofit] API specification for the [AzureFileStorageUploadStore].  */
interface IAzureUploadApi {

    /** PUT call to the azure file storage without any data in the body  */
    @PUT("{path}")
    fun put(
        @Path(value = "path", encoded = true) path: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap query: Map<String, String>
    ): Call<ResponseBody>

    /** PUT call to the azure file storage with data in the body  */
    @PUT("{path}")
    fun putData(
        @Path(value = "path", encoded = true) path: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap query: Map<String, String>,
        @Body content: RequestBody
    ): Call<ResponseBody>

    /** HEAD call to the azure file storage  */
    @HEAD("{path}")
    fun head(
        @Path(value = "path", encoded = true) path: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap query: Map<String, String>
    ): Call<Void>
}
