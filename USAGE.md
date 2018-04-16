# Teamscale JaCoCo Client

## Using it as an agent

If used as an agent, coverage is dumped to the file system in regular intervals while the application
is running.

Configure the agent on your application's JVM:

    -javaagent:CLIENTJAR=OPTIONS

Where

* `CLIENTJAR` is the path to the Jar file of the Teamscale JaCoCo client
* `OPTIONS` are one or more comma-separated options in the format `key=value` for the agent

The following options are available:

- `out` (required): the path to a writable directory where the generated coverage XML files will be stored
- `class-dir` (required): the path under which all class files of the profiled application are stored. May be
  a directory or a Jar/War/Ear/... file. Separate multiple paths with a colon
- `interval`: the interval in minutes between dumps of the current coverage to an XML file
- `include` (recommended): include filters for class files during the conversion to XML. Separate multiple patterns with a colon.
  You should provide some include patters so that external libraries are not profiled - only your own code.
  Patterns are ANT-style patterns matched against the Unix-style path of every found class file, e.g.
  `./teamscale-jacoco-client-3.1.0-jacoco-0.7.9.jar@org/reactivestreams/Processor.class`. So to only include
  classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `**com/yourcompany/**:**com/yourotherpackage/**`
- `exclude`: exclude filters for class files during the conversion to XML. Separate multiple patterns with a colon
- `jacoco-include` (recommended): include patterns for the instrumentation to pass on to JaCoCo. See JaCoCo's `includes` parameter.
  You should provide some include patterns so that conversion will not have to parse all your external libraries.
  This speeds up conversion to XML.
  These patterns that are matched against
  the Java package names. E.g. to match all classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `com.yourcompany.*:com.yourotherpackage.*`
  By specifying this, the performance of the profiler is increased. Make sure to include **all** relevant application code
  but no external libraries. For further details, please see the JaCoCo documentation in the "Agent" section.
- `jacoco-exclude`: exclude patterns for the instrumentation to pass on to JaCoCo. See JaCoCo's `excludes` parameter
- `ignore-duplicates`: forces JaCoCo to ignore duplicate class files. Should be used with care, see below

Please note that the `include`/`exclude` parameters use a different pattern syntax than the
`jacoco-include`/`jacoco-exclude` patterns! See below for details.

### Logging

By default, the agent logs to the console. You can change this by providing a Log4j2 logging configuration:

    java -Dlog4j.configurationFile=LOG4J2XML ...

Where `LOG4J2XML` is the absolute path to the logging configuration. Example configurations are included with
the client.

## Using it as a separate process

This section explains how to instrument your application so coverage is recorded and run the
teamscale-jacoco-client as a separate process which writes the recorded coverage to disk at regular intervals.

This document assumes a Unix environment but the tool works just as well under Windows. Just
replace Unix paths with Windows paths etc.

### 1. Instrument the application with the profiler

Store the jacocoagent.jar in any directory of your choosing. The directory must be readable by the
application's process. Then make sure that the application is started with the following JVM parameters:

    -javaagent:JACOCOAGENTJAR=dumponexit=true,output=tcpserver,
    port=PORT,includes=INCLUDES

*(there must be no space between `tcpserver,` and `port`!)*

Where

* `JACOCOAGENTJAR` is the absolute path to the jacocoagent.jar file
* `PORT` refers to any free port number. It is used to communicate between the application's profiler and the client.
* `INCLUDES` refers to include patterns for classes that should be profiled. These are patterns that are matched against
  the Java package names. E.g. to match all classes in package `com.yourcompany` and `com.yourotherpackage` and all their
  subpackages you can use `com.yourcompany.*:com.yourotherpackage.*`
  By specifying this, the performance of the profiler is increased. Make sure to include **all** relevant application code
  but no external libraries. For further details, please see the JaCoCo documentation in the "Agent" section.

After starting the application, verify that the it has opened the specified port, e.g. by executing `netstat -lntu`.

#### Additional steps for WebSphere

For web applications running in WebSphere, please also apply this additional JVM parameter:

    -Xshareclasses:none

This option disables a WebSphere internal class cache that causes problems with the profiler.

### 2. Testing the connection

To test the setup, start the instrumented application, then start the client manually:

    java -jar teamscale-jacoco-client.jar watch
    -c CLASSES -i 1 -o OUTPUTDIR -p PORT

Where

- `OUTPUTDIR` is the same directory as above
- `PORT` is the port number chosen in the previous step
- `CLASSES` is an absolute path to an EAR/WAR/JAR or a folder containing class files of the application itself

Notice the `-i 1`. This will make the client write a trace file every minute and log to the console. After 1 minute has passed,
the client will indicate that it is trying to dump coverage. After that has happened,
end the client process (`Ctrl+C`) and check that:

- No errors were printed on the console
- A trace file was written to OUTPUTDIR and is not empty (trace files end with `.xml`)

You can upload the trace file to Teamscale to check whether the coverage is as expected.

If you got any errors, please refer to the below troubleshooting section and fix them before continuing.

### 3. Production setup of the teamscale-jacoco-client

If the above test worked, extract the log4j2.rolling-file.xml from the zip under `resources/main` and name it log4j2.xml. This file configures
the logging of the client.
The installation is simple, just put teamscale-jacoco-client.jar file and log4j2.xml in any directory of your choosing.
Edit log4j2.xml and change the log-path property:

    <Property name="log-path">OUTPUTDIR</Property>

Where `OUTPUTDIR` is any writable directory into which the resulting coverage trace files and log files are written by the client.

Then run the JAR file with the following command:

    java -Dlog4j.configurationFile=LOG4J2XML
    -jar teamscale-jacoco-client.jar watch
    -c CLASSES -i 60 -o OUTPUTDIR -p PORT

Where

- `OUTPUTDIR` is the same directory as above
- `LOG4J2XML` is the absolute path to the log4j2.xml file
- `PORT` is the port number chosen in the previous step
- `CLASSES` is an absolute path to an EAR/WAR/JAR or a folder containing class files of the application itself

The `-c` parameter may be repeated multiple times. Only CLASSES of the application itself have to be specified.
External libraries should not be specified.

# Running as a service

For Windows and Linux systemd there are README files within the distribution zip under `resources/main/windows` and
`resources/main/linux` that describe how to install the client as a service.

# Docker

The client is available as a Docker image if you would like to profile an application that is running inside a
Docker image.

You'll need these two images in addition to your application's image:

- Teamscale JaCoCo client: [cqse/teamscale-jacoco-client](https://hub.docker.com/r/cqse/teamscale-jacoco-client/)
- JaCoCo agent: [ictu/jacoco-agent-docker](https://hub.docker.com/r/ictu/jacoco-agent-docker/)

The client image has the same versioning scheme as the client itself: `CLIENTVERSION-jacoco-JACOCOVERSION`.
There is no `latest` tag.

__Please make sure the JaCoCo version of both of these images is the same.
Otherwise there may be compatibility problems!__

## Prepare your application

Make sure that your Java process is the root process in the Docker image (PID 1). Otherwise, it will not receive
the SIGTERM signal when the Docker image is stopped and JaCoCo will not dump its coverage (i.e. coverage is lost).
You can do this by either using `ENTRYPOINT ["java", ...]`, `CMD exec java ...` or `CMD ["java", ...]` to start
your application. For more information see [this StackOverflow answer][so-java-exec-answer].

Next, you'll need to make your application's bytecode (.jar/.war/.class files) available to the JaCoCo client
image. To do so, declare it as a volume in your Dockerfile:

    VOLUME /path/to/class/files

The volume must contain _all_ relevant Java bytecode for which you want to receive coverage.

Finally, make sure your Java process can somehow pick up the Java agent VM parameters, e.g.
via `JAVA_TOOL_OPTIONS`. If your docker images starts the Java process directly, this should
work out of the box. If you are using an application container (e.g. Tomcat), you'll have
to check how to pass these options to your application's VM.

## Compose the images

Here's an example Docker Compose file that instruments an application:

	services:
	  jacoco:
		image: ictu/jacoco-agent-docker:0.7.9
	  app:
		build: ./app
		environment:
		  JAVA_TOOL_OPTIONS: -javaagent:/jacoco/lib/jacocoagent.jar=dumponexit=true,output=tcpserver,port=9876,address=*
		expose:
		  - '9876'
		volumes_from:
		  - service:jacoco:ro
	  jacoco-client:
		image: cqse/teamscale-jacoco-client:2.0.1-jacoco-0.7.9
		volumes:
		  - ./traces:/output/traces
		  - ./logs:/output/logs
		volumes_from:
		  - service:app:ro
		links:
		  - 'app:app'
		environment:
		  CLASSES_DIR: /jars
		  HOSTNAME: app
		  PORT: 9876
	version: '2.0'

This configures the three images and links them together:

- your application mounts the volumes from the JaCoCo image, which contains the profiler binaries
- your application enables the profiler
- the profiler is configured to open a local port which is forwarded out of the Docker image
- the client mounts the volumes from your application's image, which contains a volume with
  all your bytecode (`/jars` in this example)
- the client is `link`ed to your application and can thus access the JaCoCo port we opened
- the client's output volumes are mounted to some form of persistent storage
  (`/output/traces` and `/output/logs`)

## Configuring the client image

There are several environment variables that allow you to configure the client image.
See the Dockerfile of the image for an explanation of each option.
You'll find this in the distribution zip of the JaCoCo client (ask CQSE if you don't
have one).

# Advanced usage of teamscale-jacoco-client

## Command line options

You can run the JAR file without arguments to get a list and description of all available options.

## Troubleshooting

### Produced coverage files are huge

You're probably profiling and analyzing more code than necessary (e.g. third-party libraries etc). Make sure to set restrictive include/exclude patterns for JaCoCo and also use
the `-f` and `-e` command line parameters of the tool to decide which code to analyze during the conversion stage.

Please note that these patterns are ANT-style patterns and *always* use forward slashes,
i.e. *not* the same syntax as JaCoCo's include patterns above. To see what is being filtered out, activate debug logging.

Patterns are ANT-style patterns matched against the Unix-style path of every found class file, e.g.
`./teamscale-jacoco-client-3.1.0-jacoco-0.7.9.jar@org/reactivestreams/Processor.class`. So to only include
classes in package `com.yourcompany` and `com.yourotherpackage` and all their
subpackages you can use `-f '**com/yourcompany/**:**com/yourotherpackage/**'`

Enable debug logging to see what is being filtered out and fine-tune these patterns.

### Error: "Can't add different class with same name"

This is a restriction of JaCoCo. You specified a class file location with the `-c` parameter that contains two versions of the same class that are not identical. This may happen e.g. when you
have multiple application versions under the `-c` path. It may also happen if your application simply contains such conflicting classes (which is not good, you should fix this!).
You have three options to fix this problem:

1. Make the `-c` parameter more concrete so it only includes the correct version of your application
2. Use the `-f` and `-e` to exclude one of the duplicates. Make sure to exclude the right one or you might not get accurate coverage for those files!
3. (Discouraged!) You can use the `-d true` parameter to simply suppress these errors. Note, however, that coverage reported for these duplicated classes may be inaccurate!

### How to change the log level

Modify the log4j2.xml as needed.

### How to prevent messages about the application not being reachable

Enable the marker filter in the logging config.

### How to see which files/folders are filtered due to the `-f` and `-e` parameters

Enable debug logging in the logging config. Warning: this may create a lot of log entries!

## One-time conversion

You can invoke the client with the `convert` command as well, to achieve a one-time conversion from .exec files to XML.

[so-java-exec-answer]: https://stackoverflow.com/questions/31836498/sigterm-not-received-by-java-process-using-docker-stop-and-the-official-java-i#31840306

