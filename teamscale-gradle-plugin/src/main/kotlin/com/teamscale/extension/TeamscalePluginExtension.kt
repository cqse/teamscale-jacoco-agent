package com.teamscale.extension

import com.teamscale.config.Baseline
import com.teamscale.config.Commit
import com.teamscale.config.ServerConfiguration
import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property


/**
 * Holds all user configuration for the teamscale plugin.
 */
@Suppress("unused")
abstract class TeamscalePluginExtension(objects: ObjectFactory, layout: ProjectLayout) {

	val server = objects.newInstance(ServerConfiguration::class.java)

	/** Configures the Teamscale server. */
	fun server(action: Action<in ServerConfiguration>) {
		action.execute(server)
	}

	/** Configures the code commit. */
	val commit = objects.newInstance(Commit::class.java, layout)

	/** @see #commit */
	fun commit(action: Action<in Commit>) {
		action.execute(commit)
	}

	/**
	 * Impacted tests are calculated from baseline to endCommit. This sets the baseline.
	 */
	val baseline = objects.newInstance(Baseline::class.java)

	/** @see #baseline */
	fun baseline(action: Action<in Baseline>) {
		action.execute(baseline)
	}

	/**
	 * Configures the repository in which the baseline#revision and commit#revision should be resolved in Teamscale
	 * (esp. if there's more than one repo in the Teamscale project).
	 */
	abstract val repository: Property<String>

}

