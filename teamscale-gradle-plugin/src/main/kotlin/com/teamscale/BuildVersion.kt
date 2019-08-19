package com.teamscale

import java.util.*

/** Helper for getting the plugin version at runtime. */
object BuildVersion {

    private val bundle = ResourceBundle.getBundle("com.teamscale.plugin")

    /** Extracts the plugin's version from the jar's manifest file. */
    val buildVersion: String = bundle.getString("pluginVersion")

    /** Extracts the agent's version from the jar's manifest file. */
    val agentVersion: String = bundle.getString("agentVersion")

}
