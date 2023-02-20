package com.teamscale

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** Generates unique ports for each test task so that they can be executed in parallel.  */
abstract class AgentPortGenerator : BuildService<BuildServiceParameters.None> {
    private var nextPort = 8123

    /** Generates a new unique port number to be used by a JaCoCo agent. */
    @Synchronized
    fun getNextPort(): Int {
        return nextPort++
    }
}