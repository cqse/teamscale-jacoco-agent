# Teamscale JaCoCo Client

## Building

To build the fat jar: `gradlew clean shadow`. The jar will be under build/libs. Rename to `jacoco-client.jar`.

## Running

- `java -jar jacoco-client.jar` to see all available options
- To specify a different log4j2 configuration: `-Dlog4j.configurationFile=path/to/file.xml`. Example configs are inside the jar. The default is to log to the console with level info.
- If debug logging is enabled, useful info about filtered class files is printed (if the `-f` parameter is used).
- Required JaCoCo options: `dumponexit=true,output=tcpserver,port=PORT` where PORT is any free port.
- Usually you want to run the tool with `java -jar jacoco-client.jar -c path/to/classes/jar/wars/ears -i 60 -o /path/to/output/dir -p PORT` where PORT is the same port that JaCoCo uses.
- You can use `-f` to filter out unwanted class files. Note that at the moment these are *include* patterns only! This is useful in two situations:
  1. Traces are too big and contain lots of third-party classes (e.g. from libraries or application servers)
  2. There are duplicate classes in third-party components and the tool is crashing when analyzing the execution data of a dump. Use the patterns to filter out the duplicate classes

