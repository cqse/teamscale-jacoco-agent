package com.teamscale.jacoco.agent.upload.azure;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
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

	/**
	 * Handles all command-line options prefixed with 'azure-'
	 *
	 * @return true if it has successfully process the given option.
	 */
	public static boolean handleAzureFileStorageOptions(AzureFileStorageConfig azureFileStorageConfig, String key,
														String value)
			throws AgentOptionParseException {
		switch (key) {
			case "azure-url":
				azureFileStorageConfig.url = AgentOptionsParser.parseUrl(value);
				if (azureFileStorageConfig.url == null) {
					throw new AgentOptionParseException("Invalid URL given for option 'upload-azure-url'");
				}
				return true;
			case "azure-key":
				azureFileStorageConfig.accessKey = value;
				return true;
			default:
				return false;
		}
	}
}
