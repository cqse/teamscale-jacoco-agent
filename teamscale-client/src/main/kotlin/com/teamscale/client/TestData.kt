package com.teamscale.client

import com.teamscale.client.TestData.Builder
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Represents additional test data to attach to [ClusteredTestDetails]. Use the [Builder] to create new
 * [TestData] objects.
 *
 *
 * Internally, the data you pass to the builder is hashed and only the hash is transferred as [ ][ClusteredTestDetails.content] to Teamscale to save network bandwidth and RAM. Whenever a test case's hash changes,
 * Teamscale will select it for the next TIA test run.
 */
class TestData private constructor(
	/** The hash of the test data which will be sent to Teamscale as the [ClusteredTestDetails.content].  */ /*package*/
	val hash: String
) {
	/**
	 * Builder for [TestData] objects. This class is thread-safe and ensures that reading the test data does not
	 * result in [OutOfMemoryError]s.
	 */
	class Builder {
		private var digest: MessageDigest? = DigestUtils.getSha1Digest()

		/** Adds the given bytes as additional test data.  */
		@Synchronized
		fun addByteArray(content: ByteArray?): Builder {
			ensureHasNotBeenFinalized()
			DigestUtils.updateDigest(digest, content)
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR)
			return this
		}

		private fun ensureHasNotBeenFinalized() {
			checkNotNull(digest) { "You tried to use this TestData.Builder after calling #build() on it. Builders cannot be reused." }
		}

		/** Adds the given String as additional test data.  */
		@Synchronized
		fun addString(content: String?): Builder {
			ensureHasNotBeenFinalized()
			DigestUtils.updateDigest(digest, content)
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR)
			return this
		}

		/** Adds the contents of the given file path as additional test data.  */
		@Synchronized
		@Throws(IOException::class)
		fun addFileContent(fileWithContent: Path?): Builder {
			ensureHasNotBeenFinalized()
			DigestUtils.updateDigest(digest, fileWithContent)
			DigestUtils.updateDigest(digest, DIGEST_SEPARATOR)
			return this
		}

		/**
		 * Builds the [TestData] object. After calling this method, you cannot use this builder anymore.
		 */
		@Synchronized
		fun build(): TestData {
			ensureHasNotBeenFinalized()
			val hash = Hex.encodeHexString(digest!!.digest())
			digest = null
			return TestData(hash)
		}

		companion object {
			private val DIGEST_SEPARATOR = "-!#!-".toByteArray()
		}
	}
}
