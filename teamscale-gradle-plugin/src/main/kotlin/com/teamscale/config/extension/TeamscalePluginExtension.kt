package com.teamscale.config.extension

import com.teamscale.config.Commit
import com.teamscale.config.ServerConfiguration
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property


/**
 * Holds all user configuration for the teamscale plugin.
 */
@Suppress("unused")
abstract class TeamscalePluginExtension(objects: ObjectFactory) {

	val server = objects.newInstance(ServerConfiguration::class.java)

	/** Configures the Teamscale server. */
	fun server(action: Action<in ServerConfiguration>) {
		action.execute(server)
	}

	val commit = objects.newInstance(Commit::class.java)

	/** Configures the code commit. */
	fun commit(action: Action<in Commit>) {
		action.execute(commit)
	}

	/**
	 * Impacted tests are calculated from baseline to endCommit. This sets the baseline.
	 */
	abstract val baseline: Property<Long>

	/**
	 * Impacted tests are calculated from baseline to endCommit.
	 * The baselineRevision sets the baseline with the help of a VCS revision (e.g. git SHA1) instead of a branch and timestamp
	 */
	abstract val baselineRevision: Property<String>

	/** Configures the repository in which the baseline should be resolved in Teamscale (esp. if there's more than one repo in the Teamscale project). */
	abstract val repository: Property<String>

}

