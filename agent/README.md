# Teamscale JaCoCo Agent

This program provides a Java agent that can regularly dump coverage from a running application.
The JaCoCo coverage tool is used underneath.

When used as a Java agent for your Java application, coverage is dumped to the file system, via
HTTP file uploads or directly to Teamscale in regular intervals while the application is running.
Coverage is also dumped when the application is shut down.

## Installing

Unzip this zip file into any folder. __All files contained in the zip file are required for the agent
to work.__

Configure the agent on your application's JVM:

    -javaagent:AGENTJAR=OPTIONS

Where

- `AGENTJAR` is the path to the Jar file of the Teamscale JaCoCo agent (inside the `lib` folder of this zip)
- `OPTIONS` are one or more comma-separated options for the agent in the format `key1=value1,key2=value2` and so on.

The following options are available:

### General options

- `out` (required): the path to a writable directory where the generated coverage XML files will be stored. (For details 
  see path format section below)
- `includes` (recommended): include patterns for classes. Separate multiple patterns with a semicolon.
  This may speed up the profiled application and reduce the size of the output XML.
  These patterns are matched against
  the Java class names. E.g. to match all classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `*com.yourcompany.*;*com.yourotherpackage.*` (the initial star before each package name is a
  precaution in case your classes are nested inside e.g. a `src` folder, which might be interpreted as a part
  of the package name. We recommend always using this form).
  Make sure to include **all** relevant application code
  but no external libraries. For further details, please see the [JaCoCo documentation][jacoco-doc] in the "Agent" 
  section.
- `excludes` (optional): exclude patterns for classes. Same syntax as the `includes` parameter.
  For further details, please see the [JaCoCo documentation][jacoco-doc] in the "Agent" section.
- `config-file` (optional): a file which contains one or more of the previously named options as `key=value` entries 
  which are separated by line breaks. The file may also contain comments starting with `#`. (For details see path format 
  section below)
- `logging-config`: path to a [logback][logback] configuration XML file (other configuration formats are not supported 
  at the moment).
  Use this to change the logging behaviour of the agent. Some sample configurations are provided with the agent in the
  `logging` folder, e.g. to enable debug logging or log directly to the console. (For details see path format section 
  below)
- `mode`: which coverage collection mode to use. Can be either `normal` or `testwise` (Default is `normal`)
  
You can pass additional options directly to the original JaCoCo agent by prefixing them with `jacoco-`, e.g.
`jacoco-sessionid=session1` will set the session ID of the profiling session. See the "Agent" section of the JaCoCo 
documentation for a list of all available options.

__The `-javaagent` option MUST be specified BEFORE the `-jar` option!__

__Please check the produced log file for errors and warnings before using the agent in any productive setting.__

The log file is written to the agent's directory in the subdirectory `logs` by default. If there is no log file at that location, it means the agent didn't even start and you have not configured it correctly.

#### Path format

All paths supplied to the agent can be absolute or relative to the working directory. Furthermore paths may contain ant 
patterns with `*`, `**` and `?`.

### Options for normal mode

- `interval`: the interval in minutes between dumps of the current coverage to an XML file (Default is 60). If set to 
  0 coverage is only dumped at JVM shutdown.
- `dump-on-exit`: whether a coverage report should be written on JVM shutdown (Default is true).
- `ignore-duplicates`: forces JaCoCo to ignore duplicate class files. This is the default to make the initial
  setup of the tool as easy as possible. However, this should be disabled for productive use if possible.
  See the special section on `ignore-duplicates` below.
- `class-dir`: the path under which all class files of the profiled application are stored. Normally, this is inferred
  by the agent automatically. For some application, profiling performance may improve if you specify it explicitly. May be
  a directory or a Jar/War/Ear/... file. Separate multiple paths with a semicolon. (For details see path format section 
  above)
- `upload-url`: an HTTP(S) URL to which to upload generated XML files. The XML files will be zipped before the upload.
  Note that you still need to specify an `out` directory where failed uploads are stored.
- `upload-metadata`: paths to files that should also be included in uploaded zips. Separate multiple paths with a 
  semicolon.
  You can use this to include useful meta data about the deployed application with the coverage, e.g. its version number.
- `teamscale-server-url`: the HTTP(S) URL of the teamscale instance to which coverage should be uploaded.
- `teamscale-project`: the project ID within Teamscale to which the coverage belongs.
- `teamscale-user`: the username used to authenticate against Teamscale. The user account must have the 
  "Perform External Uploads" permission on the given project.
- `teamscale-access-token`: the access token of the user.
- `teamscale-partition`: the partition within Teamscale to upload coverage to. A partition can be an arbitrary string 
  which can be used to encode e.g. the test environment or the tester. These can be individually toggled on or off in 
  Teamscale's UI.
- `teamscale-commit`: the commit (Format: `branch:timestamp`) which has been used to build the system under test.
  Teamscale uses this to map the coverage to the corresponding source code. Thus, this must be the exact code commit 
  from the VCS that was deployed. For an alternative see `teamscale-commit-manifest-jar`.

  If **Git** is your VCS, you can get the commit info via
  
```bash
echo `git rev-parse --abbrev-ref HEAD`:`git --no-pager log -n1 --format="%ct000"`
```
  
  Note: Getting the branch does most likely not work when called in the build pipeline, because Jenkins, GitLab,
  Travis etc. checkout a specific commit by its SHA1, which leaves the repository in a detached head mode and thus 
  returns HEAD instead of the branch. In this case the environment variable provided by the build runner should be used 
  instead.
  
  If **Subversion** is your VCS and your reposiory follows the SVN convention with `trunk`, `branches`, and `tags` 
  directories, you can get the commit info via
  
  ```bash
 echo `svn info --show-item url | egrep -o '/(branches|tags)/[^/]+|trunk' | egrep -o '[^/]+$'`:`LANG=C svn info --show-item last-changed-date | date -f - +"%s%3N"`
```
  
- `teamscale-commit-manifest-jar` As an alternative to `teamscale-commit` the agent accepts values supplied via 
  `Branch` and  `Timestamp` entries in the given jar/war's `META-INF/MANIFEST.MF` file. (For details see path format 
  section above)
- `teamscale-git-properties-jar` As an alternative to `teamscale-commit` the agent accepts values supplied via 
  a `git.properties` file generated with [the corresponding Maven or Gradle plugin][git-properties-spring] and stored in a jar/war/ear/...
- `teamscale-message` (optional): the commit message shown within Teamscale for the coverage upload (Default is "Agent 
  coverage upload").
- `config-file` (optional): a file which contains one or more of the previously named options as `key=value` entries 
  which are separated by line breaks. The file may also contain comments starting with `#`. (For details see path format 
  section above)
- `validate-ssl` (optional): by default the agent will accept any SSL certificate. This enables a fast setup of the agent
  even in the face of broken or self-signed certificates. If you need to validate certificates, set this option to `true`.
  You might need to make your self-signed certificates available to the agent via a keystore. See
  [the Teamscale userguide's section on that topic][ts-userguide] for how to do that.
- `azure-url`: a HTTPS URL to an azure file storage. Must be in the following format: 
  https://\<account\>.file.core.windows.net/\<share\>/(\<path\>)</pre>. The \<path\> is optional; note, that in the case 
  that the given
  path does not yet exists at the given share, it will be created.
- `azure-key`: the access key to the azure file storage. This key is bound to the account, not the share.
- `http-server-port`: the port at which the agent should start an HTTP server that listens for commands.
    The agent's REST API has the following endpoints:
    - `[GET] /partition` Returns the name of the currently configured partition name.
    - `[POST] /partition/{partitionName}` Sets the name of the partition name that should be used for all followup 
      report dumps (see `teamscale-partition`). For reports that are not directly sent to Teamscale the generated report 
      will contain the partition name as session ID.
    - `[POST] /dump` Instructs the agent to dump the collected coverage.
    - `[POST] /reset` Instructs the agent to reset the collected coverage. This will discard all coverage collected in 
      the current JVM session.

## Options for testwise mode

The testwise coverage mode allows to record coverage per test, which is needed for test impact analysis. This means that
you can distinguish later, which test did produce which coverage. To enable this the `mode` option must be set to 
`testwise`.

Tests are identified by the `uniformPath`, which is a file system like path that is used to uniquely identify a test 
within Teamscale and should be chosen accordingly. It is furthermore used to make the set of tests hierarchically 
navigable within Teamscale.

In the testwise coverage mode the agent only produces an exec file that needs to be converted and augmented with more 
data from the test system. See TEST_IMPACT_ANALYSIS.md for more details.

There are two basic scenarios to distinguish between.

#### 1. The system under test is restarted for every test case

To record coverage in this setting an environment variable must be set to the test's uniform path before starting the 
system under test. New coverage is always appended to the existing coverage file, so the test system is responsible for 
cleaning the output directory before starting a new test run.

- `test-env` (required): the name of an environment variable that holds the name of the test's uniform path

#### 2. The system under test is started once

The test system (the application executing the test specification) can inform the agent of when a test started and 
finished via a REST API. The corresponding server listens at the specified port.

- `http-server-port` (required): the port at which the agent should start an HTTP server that listens for test events 
  (Recommended port is 8123)
  
The agent's REST API has the following endpoints:
- `[GET] /test` Returns the testPath of the current test. The result will be empty when the test already finished or was 
  not started yet.
- `[POST] /test/start/{testPath}` Signals to the agent that the test with the given testPath is about to start.
- `[POST] /test/end/{testPath}` Signals to the agent that the test with the given testPath has just finished.
  The body of the request may optionally contain the test execution result in json format:
```json
{
 "result": "ERROR",
 "message": "<stacktrace>|<ignore reason>"
}
```

`result` can be one of:
- `PASSED` Test execution was successful. 
- `IGNORED` The test is currently marked as "do not execute" (e.g. JUnit @Ignore or @Disabled).
- `SKIPPED` Caused by a failing assumption.
- `FAILURE` Caused by a failing assertion.
- `ERROR` Caused by an error during test execution (e.g. exception thrown).

(`uniformPath` and `durationMillis` is set automatically)
  
The `testPath` parameter is a hierarchically structured identifier of the test and must be url encoded.
E.g. `com/example/MyTest/testSomething` -> `http://localhost:8123/test/start/com%2Fexample%2FMyTest%2FtestSomething`.

## Additional steps for WebSphere

Register the agent in WebSphere's `startServer.bat` or `startServer.sh`.
Please also apply this additional JVM parameter:

    -Xshareclasses:none

This option disables a WebSphere internal class cache that causes problems with the profiler.

Please set the agent's `includes` parameter so that the WebSphere code is not being profiled.
This ensures that the performance of your application does not degrade.

Also consider to use the `config-file` option as WebSphere 17 and probably other versions silently strip any option 
parameters exceeding 500 characters without any error message, which can lead to very subtle bugs when running the 
profiler.

## Additional steps for JBoss

Register the agent in the `JAVA_OPTS` environment variable in the `run.conf` file inside the JBoss
installation directory.

Please set the agent's `includes` parameter so that the JBoss code is not being profiled.
This ensures that the performance of your application does not degrade.

## Additional steps for Wildfly

Register the agent in the `JAVA_OPTS` environment variable in the `standalone.conf` or `domain.conf`
file inside the Wildfly installation directory - depending on which "mode" is used; probably standalone.

Please set the agent's `includes` parameter so that the Wildfly code is not being profiled.
This ensures that the performance of your application does not degrade.

## Additional steps for Tomcat/TomEE

Register the agent in the `CATALINA_OPTS` environment variable inside the `bin/setenv.sh` or `bin/setenv.bat`
script in the Tomcat installation directory. Create this file if it does not yet exist.

Please set the agent's `includes` parameter so that the Tomcat code is not being profiled.
This ensures that the performance of your application does not degrade.

## Additional steps for Glassfish

You have two options to register the JVM option:

1. Register the agent [via the `asadmin` tool][glassfish-asadmin].
2. Register the agent by [manually editing `domain.xml`][glassfish-domainxml] and adding a `jvm-options` element.

When using the `asadmin` tool, some characters need to be escaped with a backslash, see [this StackOverflow answer][glassfish-escaping].

Afterwards, restart Glassfish. Please verify the setup with `asadmin`'s `list-jvm-options` command.

## Additional steps for Jetty

Register the agent by setting a `JAVA_OPTIONS` variable such that the Jetty process can see it.

## Additional steps for Java Web Start

Please ask CQSE for special tooling that is available to instrument Java Web Start processes.


## Store Commit in Jar file

If you are using Git, you can use either a Maven or Gradle plugin to store the commit
in any Jar/War/Ear/... file and tell the agent to read it via `teamscale-git-properties-jar`.

Alternatively, it is also convenient to use the MANIFEST entries via `teamscale-commit-manifest-jar` to link artifacts to commits, 
especially when tests are executed independently from the build. The following assumes that we are using a Git 
repository.

### Maven

To configure this for the maven build add the following to your top level `pom.xml`.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId> <!-- Works also for the maven-jar-plugin -->
    ...
    <configuration>
         ...
        <resourceEncoding>UTF-8</resourceEncoding>
        <archive>
            <manifest>
                <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
            </manifest>
            <manifestEntries>
                <Branch>${branch}</Branch>
                <Timestamp>${timestamp}</Timestamp>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

When executing maven pass in the branch and timestamp to Maven:
```sh
mvn ... -Dbranch=$BRANCH_NAME -Dtimestamp=$(git --no-pager log -n1 --format="%ct000")
```

### Gradle

```groovy
plugins {
	id 'org.ajoberstar.grgit' version '2.3.0'
}

jar {
	manifest {
		attributes 'Branch': System.getProperty("branch")
		attributes 'Timestamp': grgit.log {
			maxCommits = 1
		}.first().dateTime.toInstant().toEpochMilli()
	}
}
```

```sh
./gradlew jar -Dbranch=master
```

## `ignore-duplicates`

The underlying JaCoCo coverage instrumentation tooling relies on fully qualified class names
to uniquely identify classes. However, in practice, applications are often deployed with
multiple versions of the same class. This can happen, e.g. if you use the same library
in different versions in sub-projects of your code.

At runtime, it is not deterministic, which of these versions will be loaded by the class loader.
Thus, when trying to convert the recorded coverage back to covered source code lines,
JaCoCo cannot determine which of the two versions was actually profiled.

By default, the agent is configured to only log a warning in these cases and simply pick one
of the versions at random. Thus, the reported coverage for such files may not be accurate
and may even be totally unusable.
It is thus desirable to fix these warnings by ensuring that only one version of each class
is deployed with your application. This has to be fixed in your build process.

Please refer to [this StackOverflow post][so-duplicates] and the [JaCoCo FAQ][jacoco-faq] for more
information.

# Docker

The agent is available as a Docker image if you would like to profile an application that is running inside a
Docker image.

You'll need this image in addition to your application's image:

- Teamscale JaCoCo agent: [cqse/teamscale-jacoco-agent](https://hub.docker.com/r/cqse/teamscale-jacoco-agent/)

The agent image has the same versioning scheme as the agent itself: `AGENTVERSION-jacoco-JACOCOVERSION`.
There is no `latest` tag.

## Prepare your application

Make sure that your Java process is the root process in the Docker image (PID 1). Otherwise, it will not receive
the SIGTERM signal when the Docker image is stopped and JaCoCo will not dump its coverage (i.e. coverage is lost).
You can do this by either using `ENTRYPOINT ["java", ...]`, `CMD exec java ...` or `CMD ["java", ...]` to start
your application. For more information see [this StackOverflow answer][so-java-exec-answer].

Next, make sure your Java process can somehow pick up the Java agent VM parameters, e.g.
via `JAVA_TOOL_OPTIONS`. If your docker images starts the Java process directly, this should
work out of the box. If you are using an application container (e.g. Tomcat), you'll have
to check how to pass these options to your application's VM.

## Compose the images

Here's an example Docker Compose file that instruments an application:

	services:
	  app:
		build: ./app
		environment:
		  JAVA_TOOL_OPTIONS: -javaagent:/agent/agent.jar=AGENTOPTIONS
		expose:
		  - '9876'
		volumes_from:
		  - service:agent:ro
	  agent:
		image: cqse/teamscale-jacoco-agent:5.0.0-jacoco-0.7.9
	version: '2.0'

This configures your application and the agent image:

- your application mounts the volumes from the agent image, which contains the profiler binaries
- your application enables the profiler via `JAVA_TOOL_OPTIONS`. `AGENTOPTIONS` are the agent options as
  discussed in this guide's section on the agent above

# One-time conversion of .exec files to .xml

This tool is also useful in case you are using the plain JaCoCo agent.

Converting `.exec` files produced by raw JaCoCo to XML can be cumbersome. You can run the `bin/convert`
tool to perform this conversion for you. Run `bin/convert --help` to
see all available command line options.

This is especially useful since this conversion does allow for duplicate class files by default, which
the raw JaCoCo conversion will not allow.

__The caveats listed in the above `ignore-duplicates` section still apply!__

# Troubleshooting

## My application fails to start after registering the agent

Most likely, you provided invalid parameters to the agent. Please check the agent's directory for a log file.
If that does not exist, please check stdout of your application. If the agent can't write its log file, it
will report the errors on stdout.

## Produced coverage files are huge

You're probably profiling and analyzing more code than necessary (e.g. third-party libraries etc). Make sure to
set restrictive include/exclude patterns via the agent's options (see above).

Enable debug logging to see what is being filtered out and fine-tune these patterns.

## I do not have access to the class files

In that case simply _don't_ specify a `class-dir` option.

## Error: "Can't add different class with same name"

This is a restriction of JaCoCo. See the above section about `ignore-duplicates`.
To fix this error, it is best to resolve the underlying problem (two classes with the same fully qualified name
but different code). If this is not possible or not desirable, you can set `ignore-duplicates=true` in the
agent options to turn this error into a warning. Be advised that coverage for all classes that produce this
error/warning in the log may have inaccurate coverage values reported.

Please refer to [this StackOverflow post][so-duplicates] and the [JaCoCo FAQ][jacoco-faq] for more
information.

## How to change the log level

Set an appropriate logback logging configuration XML. See the agent options description above for how to do this.

## How to see which files/folders are filtered due to the `includes` and `excludes` parameters

Enable debug logging in the logging config. Warning: this may create a lot of log entries!


[so-java-exec-answer]: https://stackoverflow.com/questions/31836498/sigterm-not-received-by-java-process-using-docker-stop-and-the-official-java-i#31840306
[so-duplicates]: https://stackoverflow.com/questions/11673356/jacoco-cant-add-different-class-with-same-name-org-hamcrest-basedescription
[jacoco-faq]: https://www.jacoco.org/jacoco/trunk/doc/faq.html
[jacoco-doc]: https://www.jacoco.org/jacoco/trunk/doc
[logback]: https://logback.qos.ch/manual/index.html
[glassfish-asadmin]: https://docs.oracle.com/cd/E19798-01/821-1751/gepzd/index.html
[glassfish-domainxml]: https://docs.oracle.com/cd/E19798-01/821-1753/abhar/index.html
[glassfish-escaping]: https://stackoverflow.com/questions/24699202/how-to-add-a-jvm-option-to-glassfish-4-0
[git-properties-spring]: https://docs.spring.io/spring-boot/docs/current/reference/html/howto-build.html#howto-git-info
[ts-userguide]: https://www.cqse.eu/download/teamscale/userguide.pdf
