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
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
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
	val uploadedReports: MutableList<ExternalReport> = ArrayList()

	/** All user agents that were present in the received requests.  */
	@JvmField
	val collectedUserAgents: MutableSet<String> = HashSet()

	/** A list of all commits to which an upload happened. Can either be branch:timestamp or revision  */
	@JvmField
	val uploadCommits: MutableList<String> = ArrayList()

	/** A list of all commits for which impacted tests were requested. Can either be branch:timestamp or revision  */
	@JvmField
	val impactedTestCommits: MutableList<String> = ArrayList()

	/** A list of all commits used as baseline for retrieving impacted tests  */
	@JvmField
	val baselines: MutableList<String> = ArrayList()

	/** A list of all repositories to which an upload happened.  */
	@JvmField
	val uploadRepositories: MutableList<String> = ArrayList()

	/** A list of all repositories for which impacted tests were requested.  */
	@JvmField
	val impactedTestRepositories: MutableList<String> = ArrayList()

	/** All tests that the test engine has signaled to Teamscale as being available for execution.  */
	@JvmField
	val availableTests: MutableSet<TestWithClusterId> = HashSet()
	private val tempDir: Path = Files.createTempDirectory("TeamscaleMockServer")
	private val service: Service = Service.ignite()
	private var impactedTests: List<String>? = null
	private val profilerEvents: MutableList<String> = ArrayList()
	private var profilerConfiguration: ProfilerConfiguration? = null

	init {
		service.port(port)
		service.exception(Exception::class.java) { exception: Exception, request: Request?, response: Response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			response.body("Exception: " + exception.message)
		}
		service.notFound { request: Request, response: Response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			"Unexpected request: " + request.requestMethod() + " " + request.uri()
		}
		service.init()
		service.awaitInitialization()
	}

	/** Configures the server to accept report uploads and store them within the mock for later retrieval.  */
	fun acceptingReportUploads(): TeamscaleMockServer {
		service.post(
			"api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report"
		) { request: Request, response: Response -> this.handleReport(request, response) }
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withImpactedTests(vararg impactedTests: String): TeamscaleMockServer {
		this.impactedTests = Arrays.asList(*impactedTests)
		service.put(
			"api/v9.4.0/projects/:projectName/impacted-tests"
		) { request: Request, response: Response ->
			this.handleImpactedTests(
				request,
				response
			)
		}
		return this
	}

	/** Configures the server to answer all impacted test calls with the given tests.  */
	fun withProfilerConfiguration(profilerConfiguration: ProfilerConfiguration?): TeamscaleMockServer {
		this.profilerConfiguration = profilerConfiguration
		service.post(
			"api/v9.4.0/running-profilers"
		) { request: Request, response: Response ->
			this.handleProfilerRegistration(
				request,
				response
			)
		}
		service.put(
			"api/v9.4.0/running-profilers/:profilerId"
		) { request: Request, response: Response ->
			this.handleProfilerHeartbeat(
				request,
				response
			)
		}
		service.delete(
			"api/v9.4.0/running-profilers/:profilerId"
		) { request: Request, response: Response ->
			this.handleProfilerUnregister(
				request,
				response
			)
		}
		return this
	}

	/** Configures the server to answer all POST/PUT requests with an error.  */
	fun disallowingStateChanges(): TeamscaleMockServer {
		service.post("") { request: Request?, response: Response? ->
			throw IllegalStateException("No POST to the mock server expected!")
		}
		service.put("") { request: Request?, response: Response? ->
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
	fun parseUploadedTestwiseCoverageReport(index: Int): TestwiseCoverageReport {
		return deserialize(
			uploadedReports[index].reportString,
			TestwiseCoverageReport::class.java
		)
	}

	@Throws(IOException::class)
	private fun handleImpactedTests(request: Request, response: Response): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		impactedTestCommits.add(request.queryParams("end-revision") + ", " + request.queryParams("end"))
		impactedTestRepositories.add(request.queryParams("repository"))
		baselines.add(request.queryParams("baseline-revision") + ", " + request.queryParams("baseline"))
		availableTests.addAll(
			deserializeList(
				request.body(),
				TestWithClusterId::class.java
			)
		)
		val tests = impactedTests!!.stream().map<PrioritizableTest> { testName: String -> PrioritizableTest(testName) }
			.collect(
				Collectors.toList<PrioritizableTest>()
			)
		return listOf(PrioritizableTestCluster("cluster", tests)).serialize()
	}

	@Throws(JsonProcessingException::class)
	private fun handleProfilerRegistration(request: Request, response: Response): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add(
			"Profiler registered and requested configuration " + request.queryParams("configuration-id")
		)
		val registration = ProfilerRegistration()
		registration.profilerConfiguration = this.profilerConfiguration
		registration.profilerId = "123"
		return registration.serialize()
	}

	private fun handleProfilerHeartbeat(request: Request, response: Response): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add("Profiler " + request.params(":profilerId") + " sent heartbeat")
		return ""
	}

	private fun handleProfilerUnregister(request: Request, response: Response): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		profilerEvents.add("Profiler " + request.params(":profilerId") + " unregistered")
		return "foo"
	}

	fun getProfilerEvents(): List<String> {
		return profilerEvents
	}

	@Throws(IOException::class, ServletException::class)
	private fun handleReport(request: Request, response: Response): String {
		collectedUserAgents.add(request.headers("User-Agent"))
		uploadCommits.add(request.queryParams("revision") + ", " + request.queryParams("t"))
		uploadRepositories.add(request.queryParams("repository"))
		val multipartConfigElement = MultipartConfigElement(tempDir.toString())
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)

		val parts = request.raw().parts
		val partition = request.queryParams("partition")
		for (part in parts) {
			val reportString = IOUtils.toString(part.inputStream)
			uploadedReports.add(ExternalReport(reportString, partition))
			part.delete()
		}

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
