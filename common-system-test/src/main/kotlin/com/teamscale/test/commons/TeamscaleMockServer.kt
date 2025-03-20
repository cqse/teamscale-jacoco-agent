package com.teamscale.test.commons

import com.fasterxml.jackson.core.JsonProcessingException
import com.teamscale.client.*
import com.teamscale.client.JsonUtils.deserialize
import com.teamscale.client.JsonUtils.deserializeList
import com.teamscale.client.JsonUtils.serialize
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import spark.Request
import spark.Response
import spark.Service
import spark.utils.IOUtils
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.servlet.MultipartConfigElement
import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse

/**
 * Mocks a Teamscale server: returns predetermined impacted tests and stores all uploaded reports so tests can run
 * assertions on them.
 */
class TeamscaleMockServer(port: Int) {
	/** All reports uploaded to this Teamscale instance.  */
	@JvmField
	val uploadedReports = mutableListOf<ExternalReport>()

	/** All user agents that were present in the received requests.  */
	@JvmField
	val collectedUserAgents = mutableSetOf<String>()

	/** A list of all commits to which an upload happened. Can either be branch:timestamp or revision  */
	@JvmField
	val uploadCommits = mutableListOf<String>()

	/** A list of all commits for which impacted tests were requested. Can either be branch:timestamp or revision  */
	@JvmField
	val impactedTestCommits = mutableListOf<String>()

	/** A list of all commits used as baseline for retrieving impacted tests  */
	@JvmField
	val baselines = mutableListOf<String>()

	/** A list of all repositories to which an upload happened.  */
	@JvmField
	val uploadRepositories = mutableListOf<String?>()

	/** A list of all repositories for which impacted tests were requested.  */
	@JvmField
	val impactedTestRepositories = mutableListOf<String?>()

	/** All tests that the test engine has signaled to Teamscale as being available for execution.  */
	@JvmField
	val availableTests = mutableSetOf<TestWithClusterId>()

	private val tempDir = Files.createTempDirectory("TeamscaleMockServer")
	private val service = Service.ignite()
	private var impactedTests = listOf<String>()
	val profilerEvents = mutableListOf<String>()
	private var profilerConfiguration: ProfilerConfiguration? = null
	private var username: String? = null
	private var accessToken: String? = null

	init {
		service.port(port)
		service.exception(Exception::class.java) { exception, _, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			response.body("Exception: ${exception.message}")
		}
		service.notFound { request, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			"Unexpected request: ${request.requestMethod()} ${request.uri()}"
		}
		service.init()
		service.awaitInitialization()
	}

	/** Enables authentication for the mock server by setting the provided username and access token.  */
	fun withAuthentication(username: String, accessToken: String): TeamscaleMockServer {
		this.username = username
		this.accessToken = accessToken
		return this
	}

	/** Configures the server to accept report uploads and store them within the mock for later retrieval.  */
	fun acceptingReportUploads(): TeamscaleMockServer {
		service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report", ::handleReport)
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withImpactedTests(vararg impactedTests: String): TeamscaleMockServer {
		this.impactedTests = listOf(*impactedTests)
		service.put("api/v9.4.0/projects/:projectName/impacted-tests", ::handleImpactedTests)
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withProfilerConfiguration(profilerConfiguration: ProfilerConfiguration?): TeamscaleMockServer {
		this.profilerConfiguration = profilerConfiguration
		service.post("api/v2024.7.0/profilers", ::handleProfilerRegistration)
		service.put("api/v2024.7.0/profilers/:profilerId", ::handleProfilerHeartbeat)
		service.delete("api/v2024.7.0/profilers/:profilerId", ::handleProfilerUnregister)
		service.post("api/v2024.7.0/profilers/:profilerId/logs", ::handleProfilerLogs);
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
	 * Returns the report at the given index in [uploadedReports], parsed as a [TestwiseCoverageReport].
	 *
	 * @throws IOException when parsing the report fails.
	 */
	@Throws(IOException::class)
	fun parseUploadedTestwiseCoverageReport(index: Int) =
		deserialize<TestwiseCoverageReport>(uploadedReports[index].reportString)

	@Throws(IOException::class)
	private fun handleImpactedTests(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		impactedTestCommits.add("${request.queryParams("end-revision")}, ${request.queryParams("end")}")
		impactedTestRepositories.add(request.queryParams("repository"))
		baselines.add("${request.queryParams("baseline-revision")}, ${request.queryParams("baseline")}")
		availableTests.addAll(deserializeList<TestWithClusterId>(request.body()))
		val tests = impactedTests.map { testName -> PrioritizableTest(testName) }
		return listOf(PrioritizableTestCluster("cluster", tests)).serialize()
	}

	@Throws(JsonProcessingException::class)
	private fun handleProfilerRegistration(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		profilerEvents.add(
			"Profiler registered and requested configuration ${request.queryParams("configuration-id")}"
		)
		return ProfilerRegistration().apply {
			profilerConfiguration = this@TeamscaleMockServer.profilerConfiguration
			profilerId = "123"
		}.serialize()
	}

	private fun handleProfilerHeartbeat(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		profilerEvents.add("Profiler ${request.params(":profilerId")} sent heartbeat")
		return ""
	}

	private fun handleProfilerLogs(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		profilerEvents.add("Profiler ${request.params(":profilerId")} sent logs")
		return ""
	}

	private fun handleProfilerUnregister(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		profilerEvents.add("Profiler ${request.params(":profilerId")} unregistered")
		return "foo"
	}

	@Throws(IOException::class, ServletException::class)
	private fun handleReport(request: Request, response: Response): String {
		requireAuthentication(request, response)

		request.collectUserAgent()
		uploadCommits.add("${request.queryParams("revision")}, ${request.queryParams("t")}")
		uploadRepositories.add(request.queryParams("repository"))
		val multipartConfigElement = MultipartConfigElement(tempDir.toString())
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)

		val parts = request.raw().parts
		val partition = request.queryParams("partition")
		parts.forEach { part ->
			val reportString = IOUtils.toString(part.inputStream)
			uploadedReports.add(ExternalReport(reportString, partition))
			part.delete()
		}

		return "success"
	}

	private fun Request.collectUserAgent() {
		collectedUserAgents.add(headers("User-Agent"))
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	fun shutdown() {
		service.stop()
		service.awaitStop()
	}

	private fun requireAuthentication(request: Request, response: Response) {
		username?.let { user -> accessToken?.let { token ->
			val authHeader = request.headers("Authorization")
			if (authHeader == null || authHeader != buildBasicAuthHeader(user, token)) {
				response.status(401)
				throw IllegalArgumentException("Unauthorized")
			}
		}}
	}

	private fun buildBasicAuthHeader(username: String, accessToken: String): String {
		val credentials = "$username:$accessToken"
		return "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray())
	}
}
