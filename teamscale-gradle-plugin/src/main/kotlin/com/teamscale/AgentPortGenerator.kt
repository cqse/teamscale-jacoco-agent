package com.teamscale

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** Generates unique ports for each test task so that they can be executed in parallel.  */
abstract class AgentPortGenerator : BuildService<BuildServiceParameters.None> {
    private var nextPort = 8123

    @Synchronized
    fun getNextPort(): Int {
        return nextPort++
    }
}