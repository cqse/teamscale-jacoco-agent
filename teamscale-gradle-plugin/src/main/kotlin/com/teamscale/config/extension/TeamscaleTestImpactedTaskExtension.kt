package com.teamscale.config.extension

import com.teamscale.config.AgentConfiguration
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable
import javax.inject.Inject

/**
 * Holds all user configuration regarding testwise coverage report uploads.
 */
@Suppress("unused")
open class TeamscaleTestImpactedTaskExtension @Inject constructor(
    objects: ObjectFactory,
    teamscaleJaCoCoAgentConfiguration: FileCollection,
    jacocoExtension: JacocoTaskExtension
) : Serializable {

    /** Settings regarding the teamscale jacoco agent. */
    val agent = objects.newInstance<AgentConfiguration>(teamscaleJaCoCoAgentConfiguration, jacocoExtension)

    /** Configures the jacoco agent options. */
    fun agent(action: Action<in AgentConfiguration>) {
        action.execute(agent)
    }
}



