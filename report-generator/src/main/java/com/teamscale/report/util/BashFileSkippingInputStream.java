package com.teamscale.report.util;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles executable spring-boot jar files that prepend a bash file to the beginning of the ZIP file to make it
 * directly executable without "java -jar my.jar". We just skip the bash file until we find the zip file header.
 */
public class BashFileSkippingInputStream extends FilterInputStream {

	/**
	 * Wraps the given input stream in a BufferedInputStream and consumes all bytes until a zip file header is found.
	 */
	public BashFileSkippingInputStream(InputStream in) throws IOException {
		super(new BufferedInputStream(in));
		consumeUntilZipHeader();
	}

	/**
	 * Reads the stream until the zip file header "50 4B 03 04" is found or EOF is reached.
	 * After calling the method the read pointer points to the first byte of the zip file header.
	 */
	private void consumeUntilZipHeader() throws IOException {
		byte[] buffer = new byte[8192];
		in.mark(buffer.length);
		int count = in.read(buffer, 0, buffer.length);
		while (count > 0) {
			for (int i = 0; i < count - 3; i++) {
				if (buffer[i] == 0x50 && buffer[i + 1] == 0x4B && buffer[i + 2] == 0x03 && buffer[i + 3] == 0x04) {
					in.reset();
					in.skip(i);
					return;
				}
			}

			// Reset mark to 3 bytes before the end of the previously read buffer end to
			// also detect a zip header when it spans over two buffers
			in.reset();
			in.skip(buffer.length - 3);
			in.mark(buffer.length);
			count = in.read(buffer, 0, buffer.length);
		}
	}
}
