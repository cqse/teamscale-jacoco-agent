package com.teamscale.profiler.installer

import picocli.CommandLine
import java.util.*

/**
 * Calculates the version string shown with `-V`.
 */
class VersionProvider : CommandLine.IVersionProvider {
	override fun getVersion() = arrayOf("Teamscale profiler installer version $VERSION")

	companion object {
		private val VERSION: String

		init {
			val bundle = ResourceBundle.getBundle("com.teamscale.profiler.installer.app")
			VERSION = bundle.getString("version")
		}
	}
}