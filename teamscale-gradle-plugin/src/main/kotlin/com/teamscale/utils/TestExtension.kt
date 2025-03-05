package com.teamscale.utils

import com.teamscale.extension.TeamscaleTaskExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

val Test.teamscale
	get() = this.extensions.getByType<TeamscaleTaskExtension>()

val Test.jacoco
	get() = this.extensions.getByType<JacocoTaskExtension>()
