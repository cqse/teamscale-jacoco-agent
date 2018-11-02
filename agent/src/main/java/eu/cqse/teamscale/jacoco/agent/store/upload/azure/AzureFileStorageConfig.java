package eu.cqse.teamscale.jacoco.agent.store.upload.azure;

import okhttp3.HttpUrl;

/** Config necessary to upload files to an azure file storage. */
public class AzureFileStorageConfig {
	/** The URL to the azure file storage */
	public HttpUrl url;

	/** The access key of the azure file storage */
	public String accessKey;

	/** Checks if none of the required fields is null. */
	public boolean hasAllRequiredFieldsSet() {
		return url != null && accessKey != null;
	}

	/** Checks if all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null && accessKey == null;
	}
}
