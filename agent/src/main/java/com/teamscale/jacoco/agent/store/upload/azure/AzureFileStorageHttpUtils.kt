package com.teamscale.jacoco.agent.store.upload.azure

import com.teamscale.jacoco.agent.store.UploadStoreException
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_ENCODING
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_LANGUAGE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_LENGTH
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_MD_5
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_TYPE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.DATE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_MATCH
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_MODIFIED_SINCE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_NONE_MATCH
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_UNMODIFIED_SINCE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.RANGE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_DATE
import com.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.X_MS_VERSION
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Arrays
import java.util.Base64
import java.util.HashMap
import java.util.Objects
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.Comparator

/** Utils class for communicating with an azure file storage.  */
/* package */ internal object AzureFileStorageHttpUtils {

    /** Version of the azure file storage. Must be in every request  */
    private val VERSION = "2018-03-28"

    /** Formatting pattern for every date in a request  */
    private val FORMAT = DateTimeFormatter.ofPattern("E, dd MMM y HH:mm:ss z").withZone(
        ZoneId.of("GMT")
    )

    /** Returns the list of headers which must be present at every request  */
    /* package */  val baseHeaders: MutableMap<String, String>
        get() {
            val headers = HashMap<String, String>()
            headers[X_MS_VERSION] = AzureFileStorageHttpUtils.VERSION
            headers[X_MS_DATE] = FORMAT.format(LocalDateTime.now())
            return headers
        }


    /** Creates the string that must be signed as the authorization for the request.  */
    private fun createSignString(
        httpMethod: EHttpMethod, headers: Map<String, String>, account: String,
        path: String, queryParameters: Map<String, String>
    ): String {
        require(
            headers.keys.containsAll(
                Arrays.asList(
                    X_MS_DATE,
                    X_MS_VERSION
                )
            )
        ) { "Headers for the azure request cannot be empty! At least 'x-ms-version' and 'x-ms-date' must be set" }

        val xmsHeader = headers.entries.filter { x -> x.key.startsWith("x-ms") }.map { Pair(it.key, it.value) }.toMap()

        return arrayOf(
            httpMethod.toString(),
            getStringOrEmpty(headers, CONTENT_ENCODING),
            getStringOrEmpty(headers, CONTENT_LANGUAGE),
            getStringOrEmpty(headers, CONTENT_LENGTH),
            getStringOrEmpty(headers, CONTENT_MD_5),
            getStringOrEmpty(headers, CONTENT_TYPE),
            getStringOrEmpty(headers, DATE),
            getStringOrEmpty(headers, IF_MODIFIED_SINCE),
            getStringOrEmpty(headers, IF_MATCH),
            getStringOrEmpty(headers, IF_NONE_MATCH),
            getStringOrEmpty(headers, IF_UNMODIFIED_SINCE),
            getStringOrEmpty(headers, RANGE),
            createCanonicalizedString(xmsHeader),
            createCanonicalizedResources(account, path, queryParameters)
        ).joinToString("\n")
    }

    /** Returns the value from the map for the given key or an empty string if the key does not exist.  */
    private fun getStringOrEmpty(map: Map<String, String>, key: String): String {
        return Objects.toString(map[key], "")
    }

    /** Creates the string for the canonicalized resources.  */
    private fun createCanonicalizedResources(account: String, path: String, options: Map<String, String>): String {
        var canonicalizedResources = String.format("/%s%s", account, path)

        if (options.size > 0) {
            canonicalizedResources += "\n" + createCanonicalizedString(options)
        }

        return canonicalizedResources
    }

    /** Creates a string with a map where each key-value pair is in a newline separated by a colon.  */
    private fun createCanonicalizedString(options: Map<String, String>): String {
        val sortedKeys = ArrayList(options.keys)
        sortedKeys.sortWith(Comparator { obj, anotherString -> obj.compareTo(anotherString) })

        val values = sortedKeys
            .map { key -> String.format("%s:%s", key, options[key]) }
        return values.joinToString("\n")
    }

    /** Creates the string which is needed for the authorization of an azure file storage request.  */
    /* package */ @Throws(UploadStoreException::class)
    fun getAuthorizationString(
        method: EHttpMethod, account: String, key: String, path: String,
        headers: Map<String, String>, queryParameters: Map<String, String>
    ): String {
        val stringToSign = createSignString(method, headers, account, path, queryParameters)

        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA256"))
            val authKey = String(Base64.getEncoder().encode(mac.doFinal(stringToSign.toByteArray(charset("UTF-8")))))
            return "SharedKey $account:$authKey"
        } catch (e: NoSuchAlgorithmException) {
            throw UploadStoreException("Something is really wrong...", e)
        } catch (e: UnsupportedEncodingException) {
            throw UploadStoreException("Something is really wrong...", e)
        } catch (e: InvalidKeyException) {
            throw UploadStoreException(String.format("The given access key is malformed: %s", key), e)
        } catch (e: IllegalArgumentException) {
            throw UploadStoreException(String.format("The given access key is malformed: %s", key), e)
        }

    }

    /** Simple enum for all available HTTP methods.  */
    enum class EHttpMethod {
        PUT,
        HEAD
    }
}

