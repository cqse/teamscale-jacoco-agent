plugins {
	`java-library`
	com.teamscale.`kotlin-convention`
	com.teamscale.coverage
	com.teamscale.publish
}

publishAs {
	readableName.set("Teamscale Upload Client")
	description.set(
		"A tiny service client that only supports Teamscale's the external upload interface and impacted-tests service."
	)
}

dependencies {
	api(libs.retrofit.core)
	implementation(libs.okhttp.core)
	implementation(libs.commonsCodec)
	implementation(libs.slf4j.api)
	implementation(libs.retrofit.converter.jackson)
	testImplementation(libs.okhttp.mockwebserver)
}
