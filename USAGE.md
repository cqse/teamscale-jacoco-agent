# Teamscale JaCoCo Client

## Installation

This document explains how to instrument your application so coverage is recorded and run the
teamscale-jacoco-client which writes the recorded coverage to disk at regular intervals.

This document assumes a Unix environment but the tool works just as well under Windows. Just
replace Unix paths with Windows paths etc.

### 1. Instrument the application with the profiler

Store the jacocoagent.jar in any directory of your choosing. The directory must be readable by the
application's process. Then make sure that the application is started with the following JVM parameters:

    -javaagent:/absolute/path/to/jacocoagent.jar=dumponexit=true,output=tcpserver,
    port=PORT,includes=INCLUDES

(there must be no space between `tcpserver,` and `port`!)

Where

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

### 2. Install the teamscale-jacoco-client

Extract the log4j.rolling-file.xml from the teamscale-jacoco-client.jar and name it log4j2.xml. This file configures
the logging of the client.
The installation is simple, just put teamscale-jacoco-client.jar file and log4j2.xml in any directory of your choosing.
Edit log4j2.xml and change the log-path property:

        <Property name="log-path">OUTPUTDIR</Property>

Where `OUTPUTDIR` is any writable directory into which the resulting coverage trace files and log files are written by the client.

Then run the JAR file with the following command:

    java -Dlog4j.configurationFile=LOG4J2XML
    -jar teamscale-jacoco-client.jar
    -c CLASSES -i 60 -o OUTPUTDIR -p PORT

Where

- `OUTPUTDIR` is the same directory as above
- `LOG4J2XML` is the absolute path to the log4j2.xml file
- `PORT` is the port number chosen in the previous step
- `CLASSES` is an absolute path to an EAR/WAR/JAR or a folder containing class files of the application itself

The `-c` parameter may be repeated multiple times. Only CLASSES of the application itself have to be specified.
External libraries should not be specified.

### 3. Testing the teamscale-jacoco-client

To test the setup, start the instrumented application, then start the client manually:

    java -jar teamscale-jacoco-client.jar
    -c CLASSES -i 1 -o OUTPUTDIR -p PORT

Notice the `-i 1` and the missing `-Dlog4j.configurationFile`.
This will make the client write a trace file every minute and log to the console. After 1 minute has passed,
the client will indicate that it is trying to dump coverage. After that has happened,
end the client process (`Ctrl+C`) and check that:

- No errors were printed on the console
- A trace file was written to OUTPUTDIR and is not empty (trace files end with `.xml`)

You can upload the trace file to Teamscale to check whether the coverage is as expected.

# Advanced usage of teamscale-jacoco-client

## Available options

Run `java -jar jacoco-client.jar` to see all available options

## Filtering during dump conversion

You can use `-f` to filter out unwanted class files. Note that at the moment these are *include* patterns only! This is useful in two situations:

1. Traces are too big and contain lots of third-party classes (e.g. from libraries or application servers)
2. There are duplicate classes in third-party components and the tool is crashing when analyzing the execution data of a dump. Use the patterns to filter out the duplicate classes

## How To...

### change the log level

Modify the log4j2.xml as needed.

### See which files/folders are filtered due to the `-f` parameter

Enable debug logging in the logging config. Warning: this may create a lot of log entries!

### Prevent messages about the application not being reachable

Enable the marker filter in the logging config.

