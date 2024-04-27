package com.teamscale.test.commons

import com.fasterxml.jackson.core.JsonProcessingException
import com.teamscale.client.*
import com.teamscale.client.JsonUtils.deserialize
import com.teamscale.client.JsonUtils.deserializeAsList
import com.teamscale.client.JsonUtils.serialize
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import spark.Request
import spark.Service
import spark.utils.IOUtils
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.servlet.MultipartConfigElement
import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part

/**
 * Mocks a Teamscale server: returns predetermined impacted tests and stores all uploaded reports so tests can run
 * assertions on them.
 */
class TeamscaleMockServer(port: Int) {
	/** All reports uploaded to this Teamscale instance.  */
	val uploadedReports: MutableList<ExternalReport> = ArrayList()

	/** All user agents that were present in the received requests.  */
	val collectedUserAgents: MutableSet<String> = HashSet()

	/** All tests that the test engine has signaled to Teamscale as being available for execution.  */
	val availableTests: MutableSet<TestWithClusterId> = HashSet()
	private val tempDir = Files.createTempDirectory("TeamscaleMockServer")
	private val service = Service.ignite()
	private var impactedTests: List<String>? = null
	private val profilerEvents: MutableList<String> = ArrayList()
	private var profilerConfiguration: ProfilerConfiguration? = null

	init {
		service.port(port)
		service.exception(
			Exception::class.java
		) { exception, _, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			response.body("Exception: " + exception.message)
		}
		service.notFound { request, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			"Unexpected request: " + request.requestMethod() + " " + request.uri()
		}
		service.init()
		service.awaitInitialization()
	}

	/** Configures the server to accept report uploads and store them within the mock for later retrieval.  */
	fun acceptingReportUploads(): TeamscaleMockServer {
		service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report") { request, _ ->
			handleReport(request)
		}
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withImpactedTests(vararg impactedTests: String): TeamscaleMockServer {
		this.impactedTests = impactedTests.toList()
		service.put("api/v8.0.0/projects/:projectName/impacted-tests") { request, _ ->
			handleImpactedTests(request)
		}
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withProfilerConfiguration(profilerConfiguration: ProfilerConfiguration?): TeamscaleMockServer {
		this.profilerConfiguration = profilerConfiguration
		service.post("api/v9.4.0/running-profilers") { request, _ ->
			handleProfilerRegistration(request)
		}
		service.put("api/v9.4.0/running-profilers/:profilerId") { request, _ ->
			handleProfilerHeartbeat(request)
		}
		service.delete("api/v9.4.0/running-profilers/:profilerId") { request, _ ->
			handleProfilerUnregister(request)
		}
		return this
	}

	/** Configures the server to answer all POST/PUT requests with an error.  */
	fun disallowingStateChanges(): TeamscaleMockServer {
		service.post("") { _, _ ->
			throw IllegalStateException("No POST to the mock server expected!")
		}
		service.put("") { _, _ ->
			throw IllegalStateException("No PUT to the mock server expected!")
		}
		return this
	}

	/**
	 * Returns the report at the given index in [.uploadedReports], parsed as a [TestwiseCoverageReport].
	 *
	 * @throws IOException when parsing the report fails.
	 */
	@Throws(IOException::class)
	fun parseUploadedTestwiseCoverageReport(index: Int) =
		uploadedReports[index].reportString.deserialize<TestwiseCoverageReport>()

	@Throws(IOException::class)
	private fun handleImpactedTests(request: Request): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		availableTests.addAll(request.body().deserializeAsList<TestWithClusterId>())
		impactedTests?.let { tests ->
			val prioTests = tests.map { testName ->
				PrioritizableTest(testName)
			}
			return listOf(PrioritizableTestCluster("cluster", prioTests)).serialize()
		} ?: return "[]"
	}

	@Throws(JsonProcessingException::class)
	private fun handleProfilerRegistration(request: Request): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add(
			"Profiler registered and requested configuration " + request.queryParams("configuration-id")
		)
		return ProfilerRegistration().apply {
			profilerConfiguration = this@TeamscaleMockServer.profilerConfiguration
			profilerId = "123"
		}.serialize()
	}

	private fun handleProfilerHeartbeat(request: Request): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add("Profiler " + request.params(":profilerId") + " sent heartbeat")
		return ""
	}

	private fun handleProfilerUnregister(request: Request): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add("Profiler " + request.params(":profilerId") + " unregistered")
		return "foo"
	}

	fun getProfilerEvents(): List<String> {
		return profilerEvents
	}

	@Throws(IOException::class, ServletException::class)
	private fun handleReport(request: Request): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		val multipartConfigElement = MultipartConfigElement(tempDir.toString())
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)

		val file: Part = request.raw().getPart("report")
		val partition: String = request.queryParams("partition")
		val reportString: String = IOUtils.toString(file.inputStream)
		uploadedReports.add(ExternalReport(reportString, partition))
		file.delete()

		return "success"
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	fun shutdown() {
		service.stop()
		service.awaitStop()
	}
}