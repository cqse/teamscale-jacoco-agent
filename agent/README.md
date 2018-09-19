# Teamscale JaCoCo Agent

This program provides a Java agent that can regularly dump coverage from a running application.
The JaCoCo coverage tool is used underneath.

## Installing

Unzip this zip file into any folder.
When used as a Java agent for your Java application, coverage is dumped to the file system, via
HTTP file uploads or directly to Teamscale in regular intervals while the application is running.
Coverage is also transferred when the application is shut down.

Configure the agent on your application's JVM:

    -javaagent:AGENTJAR=OPTIONS

Where

- `AGENTJAR` is the path to the Jar file of the Teamscale JaCoCo agent (inside the `lib` folder of this zip)
- `OPTIONS` are one or more comma-separated options for the agent in the format `key1=value1,key2=value2` and so on.

The following options are available:

- `out` (required): the path to a writable directory where the generated coverage XML files will be stored.
- `class-dir` (required): the path under which all class files of the profiled application are stored. May be
  a directory or a Jar/War/Ear/... file. Separate multiple paths with a semicolon.
- `interval`: the interval in minutes between dumps of the current coverage to an XML file.
- `includes` (recommended): include patterns for classes. Separate multiple patterns with a semicolon.
  This may speed up the profiled application and reduce the size of the output XML.
  These patterns are matched against
  the Java class names. E.g. to match all classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `*com.yourcompany.*;*com.yourotherpackage.*` (the initial star before each package name is a
  precaution in case your classes are nested inside e.g. a `src` folder, which might be interpreted as a part
  of the package name. We recommend always using this form).
  Make sure to include **all** relevant application code
  but no external libraries. For further details, please see the [JaCoCo documentation][jacoco-doc] in the "Agent" section.
- `excludes`: exclude patterns for classes. Same syntax as the `includes` parameter.
  For further details, please see the [JaCoCo documentation][jacoco-doc] in the "Agent" section.
- `ignore-duplicates`: forces JaCoCo to ignore duplicate class files. This is the default to make the initial
  setup of the tool as easy as possible. However, this should be disabled for productive use if possible.
  See the special section on `ignore-duplicates` below.
- `upload-url`: an HTTP(S) URL to which to upload generated XML files. The XML files will be zipped before the upload.
  Note that you still need to specify an `out` directory where failed uploads are stored.
- `upload-metadata`: paths to files that should also be included in uploaded zips. Separate multiple paths with a semicolon.
  You can use this to include useful meta data about the deployed application with the coverage, e.g. its version number.
- `logging-config`: path to a Log4J configuration XML file (other configuration formats are not supported at the moment).
  Use this to change the logging behaviour of the agent. Some sample configurations are provided with the agent in the
  `logging` folder, e.g. to enable debug logging or log directly to the console.
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
  
  You can get the commit info from your VCS e.g. for Git via 
  
```bash
echo `git rev-parse --abbrev-ref HEAD`:`git --no-pager log -n1 --format="%ct000"`
```

  Note: Getting the branch does most likely not work when called in the build pipeline, because Jenkins, GitLab,
  Travis etc. checkout a specific commit by its SHA1, which leaves the repository in a detached head mode and thus 
  returns HEAD instead of the branch. In this case the environment variable provided by the build runner should be used 
  instead.
  
- `teamscale-commit-manifest-jar` As an alternative to `teamscale-commit` the agent accepts values supplied via 
  `Branch` and  `Timestamp` entries in the given jar/war's `META-INF/MANIFEST.MF` file.
  
- `teamscale-message` (optional): the commit message shown within Teamscale for the coverage upload (Default is "Agent coverage upload").
- `http-server-port` (optional): the port at which the agent should start an HTTP server that listens for test events 
  (See `Test impact mode` below for details).
- `http-server-formats` (optional): a semicolon-separated list of report formats that should be generated. Can be one or more 
  of `TESTWISE_COVERAGE`, `TEST_LIST`, `JACOCO` and `JUNIT`. Default is `TESTWISE_COVERAGE`. Depending on the formats 
  more data might be required by the REST endpoints see `Test impact mode` below for details.
- `config-file` (optional): a file which contains one or more of the previously named options as `key=value` entries 
  which are separated by line breaks. The file may also contain comments starting with `#`.

You can pass additional options directly to the original JaCoCo agent by prefixing them with `jacoco-`, e.g.
`jacoco-sessionid=session1` will set the session ID of the profiling session. See the "Agent" section of the JaCoCo documentation
for a list of all available options.

__The `-javaagent` option MUST be specified BEFORE the `-jar` option!__

__Please check the produced log file for errors and warnings before using the agent in any productive setting.__

The log file is written to the working directory of the profiled Java process by default.

## Test impact mode

The agent can be used in a Test Impact scenario to collect testwise coverage. The test system (the application executing 
the test specification) can inform the agent of when a test starts and finished via a REST API. 
The agent then generates reports that contain method-based testwise coverage (if not disabled via `http-server-formats`).
The HTTP server is started when `http-server-port` is set (Recommended port is 8000).

The `interval` commandline argument behaves slightly different in Test Impact mode. It does not dump any coverage 
during a test, but in between tests when the given interval has exceeded, when the `/dump` endpoint has been called or 
when the program shuts down.

Tests are identified by the `externalId`. The ID can be an arbitrary string that the test system uses to identify the test.
When uploading test details before the coverage the `externalId` must be the same as this ID.

The agent's REST API has the following endpoints:
- `[POST] /test/start/{externalId}` Signals to the agent that the test with the given externalId is about to start. If the 
  `http-server-formats` flag contains `TEST_LIST` and/or `JUNIT` the request body must also contain test details in JSON 
  format like the following:
  ```json
  {
     "internalId": "some/logical/path/to/the/test",
     "externalId": "437334-7484-1",
     "displayName": "This is my test",
     "sourcePath": "some/logical/path/to/the/test",
     "content": "revision3"
  }
  ```
  More information on the test details can be found in TEST_IMPACT_ANALYSIS_DOC.

- `[POST] /test/end/{externalId}` Signals to the agent that the test with the given externalId has just finished. 
  Optionally the query can have a `result` query parameter attached to indicate the test execution result. It can be 
  set to one of the following values: `PASSED`, `IGNORED`, `SKIPPED`, `FAILURE`, `ERROR`. Additionally the last three 
  values can have additional information attached to the body of the request with a stacktrace for example.

- `[POST] /dump` Makes the agent dump all collected artifacts to the configured output location (file system or Teamscale).

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

## Additional steps for Tomcat

Register the agent in the `CATALINA_OPTS` environment variable inside the `bin/setenv.sh` or `bin/setenv.bat`
script in the Tomcat installation directory. Create this file if it does not yet exist.

Please set the agent's `includes` parameter so that the Tomcat code is not being profiled.
This ensures that the performance of your application does not degrade.

## Additional steps for Java Web Start

Please ask CQSE for special tooling that is available to instrument Java Web Start processes.

## Store Commit in Manifest

As it is very convenient to use the MANIFEST entries via `teamscale-commit-manifest-jar` to link artifacts to commits, 
especially when tests are executed independently from the build. The following assumes that we are using a Git repository.

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

In case the class files of the application are not locally available (e.g. loaded with custom class loader, ...)
you can set the `jacoco-classdumpdir` option to dump the classes to any temporary directory and instruct the
agent to read them from there with the `class-dir` option.

## Error: "Can't add different class with same name"

This is a restriction of JaCoCo. See the above section about `ignore-duplicates`.
To fix this error, it is best to resolve the underlying problem (two classes with the same fully qualified name
but different code). If this is not possible or not desirable, you can set `ignore-duplicates=true` in the
agent options to turn this error into a warning. Be advised that coverage for all classes that produce this
error/warning in the log may have inaccurate coverage values reported.

Please refer to [this StackOverflow post][so-duplicates] and the [JaCoCo FAQ][jacoco-faq] for more
information.

## How to change the log level

Set an appropriate Log4J logging configuration XML. See the agent options description above for how to do this.

## How to see which files/folders are filtered due to the `includes` and `excludes` parameters

Enable debug logging in the logging config. Warning: this may create a lot of log entries!


[so-java-exec-answer]: https://stackoverflow.com/questions/31836498/sigterm-not-received-by-java-process-using-docker-stop-and-the-official-java-i#31840306
[so-duplicates]: https://stackoverflow.com/questions/11673356/jacoco-cant-add-different-class-with-same-name-org-hamcrest-basedescription
[jacoco-faq]: https://www.jacoco.org/jacoco/trunk/doc/faq.html
[jacoco-doc]: https://www.jacoco.org/jacoco/trunk/doc

