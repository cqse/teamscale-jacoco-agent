package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.AgentOptions.AgentOptionParseException;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleServer;
import org.junit.Test;

import java.util.Set;
import java.util.function.Predicate;

import static eu.cqse.teamscale.jacoco.agent.AgentOptions.getClassName;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JUNIT;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TESTWISE_COVERAGE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TEST_LIST;
import static org.assertj.core.api.Assertions.assertThat;

/** Tests the {@link AgentOptions}. */
public class AgentOptionsTest {

	/** Tests path to class name conversion. */
	@Test
	public void testPathToClassNameConversion() {
		assertThat(getClassName("file.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName("file.jar@com/foo/Bar$Goo.class")).isEqualTo("com.foo.Bar.Goo");
		assertThat(getClassName("file1.jar@goo/file2.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName("com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName(
				"C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class"))
				.isEqualTo("com.customer.something.SomeClass");
	}

	/** Tests include pattern matching. */
	@Test
	public void testIncludePatternMatching() throws AgentOptionParseException {
		assertThat(includeFilter("com.*")).accepts("file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar$Goo.class",
				"file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "com.foo/Bar.class");
		assertThat(includeFilter("com.*")).rejects("foo/com/Bar.class", "com.class", "file.jar@com.class",
				"A$com$Bar.class");
		assertThat(includeFilter("*com.*")).accepts("file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar$Goo.class",
				"file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "foo/com/goo/Bar.class",
				"A$com$Bar.class", "src/com/foo/Bar.class");
		assertThat(includeFilter("*com.*;*de.*"))
				.accepts("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar$Goo.class");
		assertThat(excludeFilter("*com.*;*de.*"))
				.rejects("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar$Goo.class");
		assertThat(includeFilter("*com.customer.*")).accepts(
				"C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class");
	}

	/** Tests the options for uploading coverage to teamscale. */
	@Test
	public void testTeamscaleUploadOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = new AgentOptions("out=.,class-dir=.," +
				"teamscale-server-url=127.0.0.1," +
				"teamscale-project=test," +
				"teamscale-user=build," +
				"teamscale-access-token=token," +
				"teamscale-partition=\"Unit Tests\"," +
				"teamscale-commit=default:HEAD," +
				"teamscale-message=\"This is my message\"");

		TeamscaleServer teamscaleServer = agentOptions.getTeamscaleServerOptions();
		assertThat(teamscaleServer.url.toString()).isEqualTo("http://127.0.0.1/");
		assertThat(teamscaleServer.project).isEqualTo("test");
		assertThat(teamscaleServer.userName).isEqualTo("build");
		assertThat(teamscaleServer.userAccessToken).isEqualTo("token");
		assertThat(teamscaleServer.partition).isEqualTo("Unit Tests");
		assertThat(teamscaleServer.commit.toString()).isEqualTo("default:HEAD");
		assertThat(teamscaleServer.message).isEqualTo("This is my message");
	}

	/** Tests the options for . */
	@Test
	public void testHttpServerOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = new AgentOptions("out=.,class-dir=.," +
				"http-server-port=8081," +
				"http-server-formats=TESTWISE_COVERAGE;TEST_LIST;JUNIT");
		assertThat(agentOptions.getHttpServerReportFormats()).containsExactlyInAnyOrder(TESTWISE_COVERAGE, TEST_LIST, JUNIT);
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> includeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = new AgentOptions("out=.,class-dir=.,includes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().test(string);
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> excludeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = new AgentOptions("out=.,class-dir=.,excludes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().test(string);
	}

}
