package eu.cqse.teamscale.jacoco.agent.store.upload.azure;

import eu.cqse.teamscale.jacoco.agent.store.UploadStoreException;
import org.conqat.lib.commons.assertion.CCSMAssert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_ENCODING;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_LANGUAGE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_LENGTH;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_MD_5;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.CONTENT_TYPE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.DATE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_MATCH;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_MODIFIED_SINCE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_NONE_MATCH;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.IF_UNMODIFIED_SINCE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.azure.AzureHttpHeader.RANGE;

/** Utils class for communicating with an azure file storage. */
public class AzureFileStorageHttpUtils {

	/** Version of the azure file storage. Must be in every request */
	public static final String VERSION = "2018-03-28";

	/** Formatting pattern for every date in a request */
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("E, d MMM y HH:mm:ss z").withZone(
			ZoneId.of("GMT"));


	/** Creates the string that must be signed as the authorization for the request. */
	private static String createSignString(EHttpMethod httpMethod, Map<String, String> headers, String account,
										   String path, Map<String, String> queryParameters) {
		// TODO (SA) should this assert the presence of the required headers?
		CCSMAssert.isTrue(headers.size() > 0,
				"Headers for the azure request cannot be empty! At least 'x-ms-version' and 'x-ms-date' must be set");

		Map<String, String> xmsHeader = headers.entrySet().stream().filter(x -> x.getKey().startsWith("x-ms"))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// TODO (SA) Use String.join("\n", varargs) to avoid mistakes with missing/additional line breaks?
		// TODO (SA) I would remove the comments, because they add no additional information over the constants.
		return httpMethod + "\n"
				+ getStringOrEmpty(headers, CONTENT_ENCODING) + "\n" // content encoding
				+ getStringOrEmpty(headers, CONTENT_LANGUAGE) + "\n" // content language
				+ getStringOrEmpty(headers, CONTENT_LENGTH) + "\n" // content length
				+ getStringOrEmpty(headers, CONTENT_MD_5) + "\n" // content md5
				+ getStringOrEmpty(headers, CONTENT_TYPE) + "\n" // content type
				+ getStringOrEmpty(headers, DATE) + "\n" // date
				+ getStringOrEmpty(headers, IF_MODIFIED_SINCE) + "\n" // if modified since
				+ getStringOrEmpty(headers, IF_MATCH) + "\n" // if match
				+ getStringOrEmpty(headers, IF_NONE_MATCH) + "\n" // if none match
				+ getStringOrEmpty(headers, IF_UNMODIFIED_SINCE) + "\n" // if unmodified since
				+ getStringOrEmpty(headers, RANGE) + "\n" // range
				+ createCanonicalizedString(xmsHeader) + "\n"
				+ createCanonicalizedResources(account, path, queryParameters);
	}

	/** Returns the value from the map for the given key or an empty string if the key does not exist. */
	private static String getStringOrEmpty(Map<String, String> map, String key) {
		return Objects.toString(map.get(key), "");
	}

	/** Creates the string for the canonicalized resources. */
	private static String createCanonicalizedResources(String account, String path, Map<String, String> options) {
		String canonicalizedResources = String.format("/%s%s", account, path);

		if (options.size() > 0) {
			canonicalizedResources += "\n" + createCanonicalizedString(options);
		}

		return canonicalizedResources;
	}

	/** Creates a string with a map where each key-value pair is in a newline separated by a colon. */
	private static String createCanonicalizedString(Map<String, String> options) {
		List<String> sortedKeys = new ArrayList<>(options.keySet());
		sortedKeys.sort(String::compareTo);

		List<String> values = sortedKeys.stream()
				.map(key -> String.format("%s:%s", key, options.get(key))).collect(Collectors.toList());
		return String.join("\n", values);
	}

	/** Creates the string which is needed for the authorization of an azure file storage request. */
	// TODO (SA) make method private?
	public static String getAuthorizationString(EHttpMethod method, String account, String key, String path,
												Map<String, String> headers, Map<String, String> queryParameters)
			throws UploadStoreException {
		String stringToSign = createSignString(method, headers, account, path, queryParameters);

		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA256"));
			String authKey = new String(Base64.getEncoder().encode(mac.doFinal(stringToSign.getBytes("UTF-8"))));
			return "SharedKey " + account + ":" + authKey;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new UploadStoreException("Something is really wrong...", e);
		} catch (InvalidKeyException | IllegalArgumentException e) {
			throw new UploadStoreException(String.format("The given access key is malformed: %s", key), e);
		}
	}

	/** Returns the current date-time string, correctly formatted for an azue file storage request */
	// TODO (SA) make method private?
	public static String getCurrentDateTimeString() {
		return FORMAT.format(LocalDateTime.now());
	}

	/** Simple enum for all available HTTP methods. */
	public enum EHttpMethod {
		PUT,
		HEAD
	}
}

