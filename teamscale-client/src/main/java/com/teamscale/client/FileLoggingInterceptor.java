package com.teamscale.client;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * OkHttpInterceptor which prints out the full request and server response of requests to a file.
 */
public class FileLoggingInterceptor implements Interceptor {

	private final File logfile;

	/** Constructor. */
	public FileLoggingInterceptor(File logfile) {
		this.logfile = logfile;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();

		long requestStartTime = System.nanoTime();
		try (PrintWriter fileWriter = new PrintWriter(new FileWriter(logfile))) {
			fileWriter.write(String.format("--> Sending request %s on %s %s%n%s%n", request.method(), request.url(),
					chain.connection(),
					request.headers()));

			Buffer requestBuffer = new Buffer();
			if (request.body() != null) {
				request.body().writeTo(requestBuffer);
			}
			fileWriter.write(requestBuffer.readUtf8());

			Response response = getResponse(chain, request, fileWriter);

			long requestEndTime = System.nanoTime();
			fileWriter.write(String
					.format("<-- Received response for %s %s in %.1fms%n%s%n%n", response.code(),
							response.request().url(), (requestEndTime - requestStartTime) / 1e6d, response.headers()));

			ResponseBody wrappedBody = null;
			if (response.body() != null) {
				MediaType contentType = response.body().contentType();
				String content = response.body().string();
				fileWriter.write(content);

				wrappedBody = ResponseBody.create(contentType, content);
			}
			return response.newBuilder().body(wrappedBody).build();
		}
	}

	private Response getResponse(Chain chain, Request request, PrintWriter fileWriter) throws IOException {
		try {
			return chain.proceed(request);
		} catch (Exception e) {
			fileWriter.write("\n\nRequest failed!\n");
			e.printStackTrace(fileWriter);
			throw e;
		}
	}
}
