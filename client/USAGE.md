# Teamscale JaCoCo Agent

This program provides a Java agent that can regularly dump coverage from a running application.
The JaCoCo coverage tool is used underneath.

## Installing

Unzip this zip file into any folder.
When used as an agent, coverage is dumped to the file system or via HTTP in regular intervals while the application
is running.

Configure the agent on your application's JVM:

    -javaagent:CLIENTJAR=OPTIONS

Where

- `CLIENTJAR` is the path to the Jar file of the Teamscale JaCoCo client (inside the `lib` folder of this zip)
- `OPTIONS` are one or more comma-separated options in the format `key=value` for the agent

The following options are available:

- `out` (required): the path to a writable directory where the generated coverage XML files will be stored.
- `class-dir` (required): the path under which all class files of the profiled application are stored. May be
  a directory or a Jar/War/Ear/... file. Separate multiple paths with a semicolon.
- `interval`: the interval in minutes between dumps of the current coverage to an XML file
- `includes` (recommended): include patterns for classes. Separate multiple patterns with a semicolon.
  This may speed up the profiled application and reduce the size of the output XML.
  These patterns are matched against
  the Java class names. E.g. to match all classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `*com.yourcompany.*:*com.yourotherpackage.*` (the initial star before each package name is a
  precaution in case your classes are nested inside e.g. a `src` folder, which might be interpreted as a part
  of the package name. We recommend always using this form).
  Make sure to include **all** relevant application code
  but no external libraries. For further details, please see the JaCoCo documentation in the "Agent" section.
- `excludes`: exclude patterns for classes. Same syntax as the `includes` parameter.
  For further details, please see the JaCoCo documentation in the "Agent" section.
- `ignore-duplicates`: forces JaCoCo to ignore duplicate class files. This is the default to make the initial
  setup of the tool as easy as possible. However, this should be disabled for productive use if possible, see the comments below.
- `upload-url`: an HTTP(S) URL to which to upload generated XML files. The XML files will be zipped before the upload.
  Note that you still need to specify an `out` directory where failed uploads are stored.
- `upload-metadata`: paths to files that should also be included in uploaded zips. Separate multiple paths with a semicolon.
  You can use this to include useful meta data about the deployed application with the coverage, e.g. its version number.
- `logging-config`: path to a Log4J configuration XML file (other configuration formats are not supported at the moment).
  Use this to change the logging behaviour of the agent. Some sample configurations are provided with the agent.

You can pass additional options directly to the original JaCoCo agent by prefixing them with `jacoco-`, e.g.
`jacoco-sessionid=session1` will set the session ID of the profiling session. See the "Agent" section of the JaCoCo documentation
for a list of all available options.

__Please check the produced log file for errors and warnings before using the agent in any productive setting.__

The log file is written to the current directory of the profiled Java process by default.

### Additional steps for WebSphere

For web applications running in WebSphere, please also apply this additional JVM parameter:

    -Xshareclasses:none

This option disables a WebSphere internal class cache that causes problems with the profiler.

# Docker

The agent is available as a Docker image if you would like to profile an application that is running inside a
Docker image.

You'll need this image in addition to your application's image:

- Teamscale JaCoCo client: [cqse/teamscale-jacoco-client](https://hub.docker.com/r/cqse/teamscale-jacoco-client/)

The client image has the same versioning scheme as the client itself: `CLIENTVERSION-jacoco-JACOCOVERSION`.
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
		image: cqse/teamscale-jacoco-client:5.0.0-jacoco-0.7.9
	version: '2.0'

This configures your application and the agent image:

- your application mounts the volumes from the agent image, which contains the profiler binaries
- your application enables the profiler via `JAVA_TOOL_OPTIONS`. `AGENTOPTIONS` are the agent options as
  discussed in this guide's section on the agent above

# One-time conversion of .exec files to .xml

Converting `.exec` files produced by raw JaCoCo to XML can be cumbersome. You can run the `bin/convert`
tool to perform this conversion for you. Run `bin/convert --help` to
see all available command line options.

This is especially useful since this conversion does allow for duplicate class files by default, which
the raw JaCoCo conversion will not allow.

# Troubleshooting

## Produced coverage files are huge

You're probably profiling and analyzing more code than necessary (e.g. third-party libraries etc). Make sure to
set restrictive include/exclude patterns via the agent's options (see above).

Enable debug logging to see what is being filtered out and fine-tune these patterns.

## I do not have access to the class files

In case the class files of the application are not locally available (e.g. loaded with custom class loader, ...)
you can set the `jacoco-classdumpdir` option to dump the classes to any temporary directory and instruct the
agent to read them from there with the `class-dir` option.

## Error: "Can't add different class with same name"

This is a restriction of JaCoCo. You specified a class file location with the `-c` parameter that contains two versions of the same class that are not identical. This may happen e.g. when you
have multiple application versions under the `-c` path. It may also happen if your application simply contains such conflicting classes (which is not good, you should fix this!).
You have three options to fix this problem:

1. Make the `-c` parameter more concrete so it only includes the correct version of your application
2. Use the `-f` and `-e` to exclude one of the duplicates. Make sure to exclude the right one or you might not get accurate coverage for those files!
3. (Discouraged!) You can use the `-d true` parameter to simply suppress these errors. Note, however, that coverage reported for these duplicated classes may be inaccurate!

## How to change the log level

Set an appropriate Log4J logging configuration XML. See the agent options description above for how to do this.

## How to see which files/folders are filtered due to the `includes` and `excludes` parameters

Enable debug logging in the logging config. Warning: this may create a lot of log entries!


[so-java-exec-answer]: https://stackoverflow.com/questions/31836498/sigterm-not-received-by-java-process-using-docker-stop-and-the-official-java-i#31840306

