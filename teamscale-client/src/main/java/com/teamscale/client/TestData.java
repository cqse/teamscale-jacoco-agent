package com.teamscale.client;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Represents additional test data to attach to {@link ClusteredTestDetails}. Use the {@link Builder} to create new
 * {@link TestData} objects.
 * <p>
 * Internally, the data you pass to the builder is hashed and only the hash is transferred as {@link
 * ClusteredTestDetails#content} to Teamscale to save network bandwidth and RAM. Whenever a test case's hash changes,
 * Teamscale will select it for the next TIA test run.
 */
public class TestData {

	/** The hash of the test data which will be sent to Teamscale as the {@link ClusteredTestDetails#content}. */
	/*package*/ final String hash;

	private TestData(String hash) {
		this.hash = hash;
	}

	/**
	 * Builder for {@link TestData} objects. This class is thread-safe and ensures that reading the test data does not
	 * result in {@link OutOfMemoryError}s.
	 */
	public static class Builder {

		private static final byte[] DIGEST_SEPARATOR = "-!#!-".getBytes();

		private MessageDigest digest = DigestUtils.getSha1Digest();

		/** Adds the given bytes as additional test data. */
		public synchronized Builder addByteArray(byte[] content) {
			ensureHasNotBeenFinalized();
			DigestUtils.updateDigest(digest, content);
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR);
			return this;
		}

		private void ensureHasNotBeenFinalized() {
			if (digest == null) {
				throw new IllegalStateException(
						"You tried to use this TestData.Builder after calling #build() on it. Builders cannot be reused.");
			}
		}

		/** Adds the given String as additional test data. */
		public synchronized Builder addString(String content) {
			ensureHasNotBeenFinalized();
			DigestUtils.updateDigest(digest, content);
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR);
			return this;
		}

		/** Adds the contents of the given file path as additional test data. */
		public synchronized Builder addFileContent(Path fileWithContent) throws IOException {
			ensureHasNotBeenFinalized();
			DigestUtils.updateDigest(digest, fileWithContent);
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR);
			return this;
		}

		/**
		 * Builds the {@link TestData} object. After calling this method, you cannot use this builder anymore.
		 */
		public synchronized TestData build() {
			ensureHasNotBeenFinalized();
			String hash = Hex.encodeHexString(digest.digest());
			digest = null;
			return new TestData(hash);
		}
	}

}
