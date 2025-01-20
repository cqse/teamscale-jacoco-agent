package com.teamscale.profiler.installer

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import picocli.CommandLine

/** Helps Picocli to convert from String to HttpUrl.  */
class HttpUrlTypeConverter : CommandLine.ITypeConverter<HttpUrl> {
	override fun convert(value: String) =
		value.toHttpUrlOrNull() ?: throw CommandLine.TypeConversionException("This is not a valid URL: $value")
}
