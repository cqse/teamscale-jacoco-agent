package com.teamscale.utils

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

interface PartialData : Named {
	companion object {
		val PARTIAL_DATA_ATTRIBUTE = Attribute.of("com.teamscale.partial", Boolean::class.javaObjectType)
	}
}
