package com.teamscale.config

import com.teamscale.client.CommitDescriptor
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import java.io.Serializable
import javax.inject.Inject

/** The commit object which holds the end commit for which we do Test Impact Analysis. */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Commit @Inject constructor(objects: ObjectFactory, val providers: ProviderFactory) : Serializable {

	/**
	 * The branch to which the artifacts belong to.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combine] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	@Deprecated("Use the revision instead")
	abstract val branchName: Property<String>

	/**
	 * The timestamp of the commit that has been used to generate the artifacts.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combine] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	@Deprecated("Use the revision instead")
	abstract val timestamp: Property<Any>

	/**
	 * The revision of the commit that the artifacts should be uploaded to.
	 * This is e.g. the SHA1 hash of the commit in Git or the revision of the commit in SVN.
	 * This field encapsulates the value set in the gradle config.
	 * Use [combine] to get a revision or branch and timestamp.
	 * It falls back to retrieving the values from the git repository, if not given manually.
	 */
	val revision: Property<String> =
		objects.property<String>().convention(providers.of(GitRevisionValueSource::class.java) {})

	/**
	 * Checks that a branch name and timestamp are set or can be retrieved from the projects git and
	 * stores them for later use.
	 */
	fun combine(): Provider<Pair<CommitDescriptor?, String?>> {
		val commitProvider: Provider<Pair<CommitDescriptor?, String?>> =
			providers.zip(branchName, timestamp) { branch, timestamp ->
				CommitDescriptor(branch, timestamp.toString()) to null as String?
			}
		val revisionProvider: Provider<Pair<CommitDescriptor?, String?>> = revision.map { null to it }
		// If timestamp and branch are set manually, prefer to use them
		// otherwise use revision as 2nd option
		return commitProvider.orElse(revisionProvider)
	}
}
