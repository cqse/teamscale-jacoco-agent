package com.teamscale.config

import com.teamscale.config.internal.BaselineInfo
import com.teamscale.config.internal.Revision
import com.teamscale.config.internal.Timestamp
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.Serializable

/** The object which holds the baseline commit for which we do Test Impact Analysis. */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Baseline : Serializable {

	/**
	 * The timestamp of the commit that should be used as a baseline for the test impact analysis.
	 */
	abstract val timestamp: Property<Any>

	/**
	 * The revision of the commit that should be used as a baseline for the test impact analysis.
	 * This is, e.g., the SHA1 hash of the commit in Git or the revision of the commit in SVN.
	 */
	abstract val revision: Property<String>

	/**
	 * Provides a combined provider that resolves to the branch and timestamp if given or to the revision otherwise.
	 */
	internal val combined: Provider<BaselineInfo> by lazy {
		val timestampProvider: Provider<BaselineInfo> = timestamp.map { Timestamp(it.toString()) }
		val revisionProvider = revision.map { Revision(it) }
		timestampProvider.orElse(revisionProvider)
	}
}
