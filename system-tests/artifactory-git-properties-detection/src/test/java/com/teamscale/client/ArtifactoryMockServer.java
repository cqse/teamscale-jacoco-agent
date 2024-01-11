package com.teamscale.client;

import org.conqat.lib.commons.collections.PairList;
import spark.Request;
import spark.Response;
import spark.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Mocks a Artifactory server: stores all uploaded reports so tests can run assertions on them.
 */
public class ArtifactoryMockServer {

	/** All reports uploaded to this Teamscale instance. */
	public final PairList<String, String> uploadedReports = new PairList<>();
	private final Service service;

	public ArtifactoryMockServer(int port) {
		service = Service.ignite();
		service.port(port);
		service.put(":path", this::handleReport);
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.notFound((Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			return "Unexpected request: " + request.requestMethod() + " " + request.uri();
		});
		service.awaitInitialization();
	}

	private String handleReport(Request request, Response response) throws IOException {
		readZipInputStream(new ByteArrayInputStream(request.bodyAsBytes()), (entry, content) -> {
			if (!entry.isDirectory()) {
				uploadedReports.add(request.params("path") + " -> " + entry.getName(), content.toString());
			}
		});
		return "success";
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	public void shutdown() {
		service.stop();
		service.awaitStop();
	}

	private static void readZipInputStream(
			InputStream inputStream, BiConsumer<ZipEntry, ByteArrayOutputStream> consumerFunction) throws IOException {
		try (ZipInputStream zipInput = new ZipInputStream(inputStream)) {
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = zipInput.read(buffer)) != -1) {
					outStream.write(buffer, 0, length);
				}
				consumerFunction.accept(entry, outStream);
			}
		}
	}
}
