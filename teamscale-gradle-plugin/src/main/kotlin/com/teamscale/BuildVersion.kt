package com.teamscale

import java.util.*

/** Helper for getting the plugin version at runtime. */
object BuildVersion {

    private val bundle = ResourceBundle.getBundle("com.teamscale.plugin")

    /** Extracts the plugin's version from the jar's plugin.properties file. */
    val pluginVersion: String
        get() = bundle.getString("pluginVersion")

}
