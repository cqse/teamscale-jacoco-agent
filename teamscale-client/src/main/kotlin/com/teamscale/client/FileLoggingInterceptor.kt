package com.teamscale.client

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

/**
 * [okhttp3.Interceptor] which prints out the full request and server response of requests to a file.
 */
class FileLoggingInterceptor(
	private val logfile: File
) : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		val requestStartTime = System.nanoTime()
		PrintWriter(FileWriter(logfile)).use { fileWriter ->
			fileWriter.write(
				"--> Sending request ${request.method} on ${request.url} ${chain.connection()}\n${request.headers}\n"
			)
			val requestBuffer = Buffer()
			request.body?.writeTo(requestBuffer)
			fileWriter.write(requestBuffer.readUtf8())

			val response = getResponse(chain, request, fileWriter)
			val requestEndTime = System.nanoTime()
			fileWriter.write(
				"<-- Received response for ${response.code} ${response.request.url} in ${(requestEndTime - requestStartTime) / 1e6}ms\n${response.headers}\n\n"
			)

			var wrappedBody: ResponseBody? = null
			response.body?.let {
				val contentType = it.contentType()
				val content = it.string()
				fileWriter.write(content)

				wrappedBody = content.toResponseBody(contentType)
			}
			return response.newBuilder().body(wrappedBody).build()
		}
	}

	@Throws(IOException::class)
	private fun getResponse(chain: Interceptor.Chain, request: Request, fileWriter: PrintWriter): Response {
		try {
			return chain.proceed(request)
		} catch (e: Exception) {
			fileWriter.write("\n\nRequest failed!\n")
			e.printStackTrace(fileWriter)
			throw e
		}
	}
}
