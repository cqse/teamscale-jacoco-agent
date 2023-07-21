package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;
import picocli.CommandLine;

public class HttpUrlTypeConverter implements CommandLine.ITypeConverter<HttpUrl> {
	@Override
	public HttpUrl convert(String value) {
		HttpUrl url = HttpUrl.parse(value);
		if (url == null) {
			throw new CommandLine.TypeConversionException("This is not a valid URL: " + value);
		}
		return url;
	}
}
