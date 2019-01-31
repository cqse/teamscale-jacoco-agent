package com.teamscale.jacoco.agent.store.upload.azure

import okhttp3.HttpUrl

/** Config necessary to upload files to an azure file storage.  */
class AzureFileStorageConfig {
    /** The URL to the azure file storage  */
    var url: HttpUrl? = null

    /** The access key of the azure file storage  */
    var accessKey: String? = null

    /** Checks if none of the required fields is null.  */
    fun hasAllRequiredFieldsSet(): Boolean {
        return url != null && accessKey != null
    }

    /** Checks if all required fields are null.  */
    fun hasAllRequiredFieldsNull(): Boolean {
        return url == null && accessKey == null
    }
}
