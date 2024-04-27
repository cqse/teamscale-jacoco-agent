package com.teamscale.client

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.io.File
import java.io.IOException

/**
 * OkHttpInterceptor which prints out the full request and server response of requests to a file.
 */
class FileLoggingInterceptor(
	private val logfile: File
) : Interceptor {

	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		val requestStartTime = System.nanoTime()
		logfile.printWriter().use { fileWriter ->
			fileWriter.apply {
				write("--> Sending request ${request.method} on ${request.url} ${chain.connection()}\n${request.headers}\n")
				val requestBuffer = Buffer()
				request.body?.writeTo(requestBuffer)
				write(requestBuffer.readUtf8())

				val response = getResponse(chain, request)
				val requestEndTime = System.nanoTime()
				write(
					"<-- Received response for %s %s in %.1fms%n%s%n%n".format(
						response.code,
						response.request.url, (requestEndTime - requestStartTime) / 1e6, response.headers
					)
				)

				var wrappedBody: ResponseBody? = null
				response.body?.let { body ->
					val contentType = body.contentType()
					val content = body.string()
					write(content)

					wrappedBody = ResponseBody.create(contentType, content)
				}
				return response.newBuilder().body(wrappedBody).build()
			}
		}
	}

	@Throws(IOException::class)
	private fun getResponse(chain: Interceptor.Chain, request: Request): Response {
		return try {
			chain.proceed(request)
		} catch (e: Exception) {
			logfile.printWriter().use { it.write("\n\nRequest failed!\n"); e.printStackTrace(it) }
			throw e
		}
	}
}