package com.teamscale.client

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/** Helper class for generating a teamscale compatible service.  */
object TeamscaleServiceGenerator {

    /**
     * Generates a [Retrofit] instance for the given
     * service, which uses basic auth to authenticate against the server and which sets the accept header to json.
     */
    fun <S> createService(serviceClass: Class<S>, baseUrl: HttpUrl, username: String, password: String): S {
        val httpClient = OkHttpClient.Builder()
        httpClient.connectTimeout(60, TimeUnit.SECONDS)
        httpClient.readTimeout(60, TimeUnit.SECONDS)

        httpClient.addInterceptor(TeamscaleServiceGenerator.getBasicAuthInterceptor(username, password))
        httpClient.addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept", "application/json").build()
            )
        }

        val client = httpClient.build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(serviceClass)
    }

    /**
     * Returns an interceptor, which adds a basic auth header to a request.
     */
    private fun getBasicAuthInterceptor(username: String, password: String): Interceptor {
        val credentials = "$username:$password"
        val basic = "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray())

        return Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", basic).build()
            chain.proceed(request)
        }
    }
}
