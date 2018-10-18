package eu.cqse.teamscale.jacoco.agent.store.upload.azure;

/** Constants for the names of HTTP header used in a request to an Azure file storage. */
public class AzureHttpHeader {
	public static final String X_MS_CONTENT_LENGTH = "x-ms-content-length";
	public static final String X_MS_DATE = "x-ms-date";
	public static final String X_MS_RANGE = "x-ms-range";
	public static final String X_MS_TYPE = "x-ms-type";
	public static final String X_MS_VERSION = "x-ms-version";
	public static final String X_MS_WRITE = "x-ms-write";
	public static final String AUTHORIZATION = "Authorization";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_MD_5 = "Content-MD5";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String RANGE = "Range";
}