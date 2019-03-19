package com.teamscale

/** Helper for getting the plugin version at runtime. */
object BuildVersion {

    /** Extracts the plugin's version from the jar's manifest file. */
    val buildVersion: String
        get() = BuildVersion::class.java.getPackage().implementationVersion ?: "0.4.1"

    /** Extracts the agent's version from the jar's manifest file. */
    val agentVersion: String
        get() = BuildVersion::class.java.getPackage().specificationVersion ?: "11.3.1"

}
