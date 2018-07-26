package eu.cqse

/**
 * Helper for getting the plugin version at runtime.
 */
object BuildVersion {

    /**
     * Extracts the plugin's version from the jar's manifest file.
     */
    val buildVersion: String
        get() = BuildVersion::class.java.getPackage().implementationVersion

}
