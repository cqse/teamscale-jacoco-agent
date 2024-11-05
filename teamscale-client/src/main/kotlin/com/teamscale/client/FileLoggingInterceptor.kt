package com.teamscale.client

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

/**
 * OkHttpInterceptor which prints out the full request and server response of requests to a file.
 */
class FileLoggingInterceptor
/** Constructor.  */(private val logfile: File) : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()

		val requestStartTime = System.nanoTime()
		PrintWriter(FileWriter(logfile)).use { fileWriter ->
			fileWriter.write(
				String.format(
					"--> Sending request %s on %s %s%n%s%n", request.method, request.url,
					chain.connection(),
					request.headers
				)
			)
			val requestBuffer = Buffer()
			if (request.body != null) {
				request.body?.writeTo(requestBuffer)
			}
			fileWriter.write(requestBuffer.readUtf8())

			val response = getResponse(chain, request, fileWriter)

			val requestEndTime = System.nanoTime()
			fileWriter.write(
				String.format(
					"<-- Received response for %s %s in %.1fms%n%s%n%n", response.code,
					response.request.url, (requestEndTime - requestStartTime) / 1e6, response.headers
				)
			)

			var wrappedBody: ResponseBody? = null
			if (response.body != null) {
				val contentType = response.body!!.contentType()
				val content = response.body!!.string()
				fileWriter.write(content)

				wrappedBody = ResponseBody.create(contentType, content)
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
