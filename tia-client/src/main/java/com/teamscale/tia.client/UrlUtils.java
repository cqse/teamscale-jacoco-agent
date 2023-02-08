package com.teamscale.tia.client;

/** Utils for URL encoding as the Java internal URLEncoder does not handle all escape-worthy symbols for path segments. */
public class UrlUtils {

	/**
	 * Percent-decodes a string, such as used in a URL Path (not a query string / form encode, which uses + for spaces,
	 * etc).
	 * Source: https://stackoverflow.com/a/44076794
	 */
	public static String percentEncode(String encodeMe) {
		if (encodeMe == null) {
			return "";
		}
		String encoded = encodeMe.replace("%", "%25");
		encoded = encoded.replace(" ", "%20");
		encoded = encoded.replace("!", "%21");
		encoded = encoded.replace("#", "%23");
		encoded = encoded.replace("$", "%24");
		encoded = encoded.replace("&", "%26");
		encoded = encoded.replace("'", "%27");
		encoded = encoded.replace("(", "%28");
		encoded = encoded.replace(")", "%29");
		encoded = encoded.replace("*", "%2A");
		encoded = encoded.replace("+", "%2B");
		encoded = encoded.replace(",", "%2C");
		encoded = encoded.replace("/", "%2F");
		encoded = encoded.replace(":", "%3A");
		encoded = encoded.replace(";", "%3B");
		encoded = encoded.replace("=", "%3D");
		encoded = encoded.replace("?", "%3F");
		encoded = encoded.replace("@", "%40");
		encoded = encoded.replace("[", "%5B");
		encoded = encoded.replace("]", "%5D");
		return encoded;
	}
}
