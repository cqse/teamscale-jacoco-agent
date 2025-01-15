package com.teamscale.maven.tia

import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import java.util.*
import java.util.function.Function

/** Accessor for different types of properties, e.g. project or user properties, in a [MavenSession].  */
class ArgLineProperty private constructor(
	/** The name of the property.  */
	val propertyName: String,
	private val propertiesAccess: (MavenSession) -> Properties
) {
	/** Returns the value of this property in the given Maven session  */
	fun getValue(session: MavenSession): String =
		propertiesAccess(session).getProperty(propertyName)

	/** Sets the value of this property in the given Maven session  */
	fun setValue(session: MavenSession, value: String?) {
		propertiesAccess(session).setProperty(propertyName, value)
	}

	companion object {
		/**
		 * Name of the property used in the maven-osgi-test-plugin.
		 */
		val TYCHO_ARG_LINE: ArgLineProperty = projectProperty("tycho.testArgLine")

		/**
		 * Name of the property used in the maven-surefire-plugin.
		 */
		val SUREFIRE_ARG_LINE: ArgLineProperty = projectProperty("argLine")

		/**
		 * Name of the property used in the spring-boot-maven-plugin start goal.
		 */
		val SPRING_BOOT_ARG_LINE: ArgLineProperty = userProperty("spring-boot.run.jvmArguments")

		/** The standard properties that this plugin might modify.  */
		val STANDARD_PROPERTIES: Array<ArgLineProperty> =
			arrayOf(TYCHO_ARG_LINE, SUREFIRE_ARG_LINE, SPRING_BOOT_ARG_LINE)

		private fun getProjectProperties(session: MavenSession) =
			session.currentProject.properties

		private fun getUserProperties(session: MavenSession) =
			session.userProperties

		/** Creates a project property ([MavenProject.getProperties]).  */
		fun projectProperty(name: String) =
			ArgLineProperty(name) { getProjectProperties(it) }

		/** Creates a user property ([MavenSession.getUserProperties]).  */
		fun userProperty(name: String) =
			ArgLineProperty(name) { getUserProperties(it) }
	}
}
