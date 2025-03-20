package com.teamscale.utils

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/** Marker interface. */
interface PartialData : Named {
	companion object {
		/** Typed attribute that signals whether binary testwise coverage data of a project only contains partial data. */
		val PARTIAL_DATA_ATTRIBUTE = Attribute.of("com.teamscale.partial", Boolean::class.javaObjectType)
	}
}
