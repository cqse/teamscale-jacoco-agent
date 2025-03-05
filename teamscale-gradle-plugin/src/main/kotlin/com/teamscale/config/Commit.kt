package com.teamscale.config

import com.teamscale.client.CommitDescriptor
import com.teamscale.config.internal.BranchAndTimestamp
import com.teamscale.config.internal.CommitInfo
import com.teamscale.config.internal.Revision
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import java.io.Serializable
import javax.inject.Inject

/** The commit object which holds the end commit for which we do Test Impact Analysis. */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Commit @Inject constructor(
	objects: ObjectFactory,
	private val providers: ProviderFactory,
	layout: ProjectLayout
) : Serializable {

	/**
	 * The branch to which the artifacts belong to.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combined] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	@Deprecated("Use the revision instead")
	abstract val branchName: Property<String>

	/**
	 * The timestamp of the commit that has been used to generate the artifacts.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combined] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	@Deprecated("Use the revision instead")
	abstract val timestamp: Property<Any>

	/**
	 * The revision of the commit that the artifacts should be uploaded to.
	 * This is e.g. the SHA1 hash of the commit in Git or the revision of the commit in SVN.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combined] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	val revision: Property<String> =
		objects.property<String>().convention(providers.of(GitRevisionValueSource::class.java) {
			parameters {
				projectDirectory.set(layout.projectDirectory)
			}
		})

	/**
	 * Checks that a branch name and timestamp are set or can be retrieved from the projects git and
	 * stores them for later use.
	 */
	internal val combined: Provider<CommitInfo> by lazy {
		val commitProvider: Provider<CommitInfo> =
			providers.zip(branchName, timestamp) { branch, timestamp ->
				BranchAndTimestamp(CommitDescriptor(branch, timestamp.toString()))
			}
		val revisionProvider = revision.map { Revision(it) }
		// If timestamp and branch are set manually, prefer to use them
		// otherwise use revision as 2nd option
		commitProvider.orElse(revisionProvider)
	}
}
