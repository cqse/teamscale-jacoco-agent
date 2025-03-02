package com.teamscale.utils

import groovy.lang.Closure
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.util.internal.ConfigureUtil
import java.io.File
import javax.inject.Inject

abstract class SimpleReport(
	private val name: String,
	private val outputType: Report.OutputType
) : ConfigurableReport {
	override fun getName(): String {
		return name
	}

	override fun getDisplayName(): String {
		return name
	}

	override fun toString(): String {
		return "Report $name"
	}

	abstract override fun getOutputLocation(): FileSystemLocationProperty<out FileSystemLocation?>

	override fun getOutputType(): Report.OutputType {
		return outputType
	}

	@Deprecated("", ReplaceWith("outputLocation.fileValue(file)"))
	override fun setDestination(file: File) {
		outputLocation.fileValue(file)
	}

	override fun configure(configure: Closure<*>): Report {
		return ConfigureUtil.configureSelf(configure, this)
	}
}

abstract class DefaultSingleFileReport @Inject constructor(name: String) :
	SimpleReport(name, Report.OutputType.FILE), SingleFileReport
