# Teamscale JaCoCo Client

## Running

- `java -jar jacoco-client.jar` to see all available options
- Required JaCoCo options: `dumponexit=true,output=tcpserver,port=PORT` where PORT is any free port.
- Usually you want to run the tool with `java -jar jacoco-client.jar -c path/to/classes/jar/wars/ears -i 60 -o /path/to/output/dir -p PORT` where PORT is the same port that JaCoCo uses.
- You can use `-f` to filter out unwanted class files. Note that at the moment these are *include* patterns only! This is useful in two situations:
  1. Traces are too big and contain lots of third-party classes (e.g. from libraries or application servers)
  2. There are duplicate classes in third-party components and the tool is crashing when analyzing the execution data of a dump. Use the patterns to filter out the duplicate classes

## How To...

### Log to a log file or change the log level

Use the log4j XML with a rolling file appender inside the jar and add `-Dlog4j.configurationFile=path/to/file.xml`.
Modify as needed.

### See which files/folders are filtered due to the `-f` parameter

Enable debug logging in the logging config XML file.

### Prevent messages about the application not being reachable

Enable the marker filter in the rolling file logging config.

