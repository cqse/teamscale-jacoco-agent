package eu.cqse.teamscale.jacoco.agent.store.upload.azure;

/** Constants for the names of HTTP header used in a request to an Azure file storage. */
// TODO (SA) can we provide more semantic information in the comments, instead of repeating the field names? While content length is clear to me, range and type, for example, are not.
public class AzureHttpHeader {
	/** x-ms content length */
	public static final String X_MS_CONTENT_LENGTH = "x-ms-content-length";

	/** x-ms date */
	public static final String X_MS_DATE = "x-ms-date";

	/** x-ms range */
	public static final String X_MS_RANGE = "x-ms-range";

	/** x-ms type */
	public static final String X_MS_TYPE = "x-ms-type";

	/** x-ms version */
	public static final String X_MS_VERSION = "x-ms-version";

	/** x-ms write */
	public static final String X_MS_WRITE = "x-ms-write";

	/** Authorization */
	public static final String AUTHORIZATION = "Authorization";

	/** Content-Encoding */
	public static final String CONTENT_ENCODING = "Content-Encoding";

	/** Content-Language */
	public static final String CONTENT_LANGUAGE = "Content-Language";

	/** Content-Length */
	public static final String CONTENT_LENGTH = "Content-Length";

	/** Content-MD5 */
	public static final String CONTENT_MD_5 = "Content-MD5";

	/** Content-Type */
	public static final String CONTENT_TYPE = "Content-Type";

	/** Date */
	public static final String DATE = "Date";

	/** If-Unmodified-Since */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

	/** If-Modified-Since */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	/** If-Match */
	public static final String IF_MATCH = "If-Match";

	/** If-None-Match */
	public static final String IF_NONE_MATCH = "If-None-Match";

	/** Range */
	public static final String RANGE = "Range";
}
