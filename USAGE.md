# JaCoCo Coverage Converter

- build fat jar: `gradlew shadow`
- to specify a different log4j2 configuration: `-Dlog4j.configurationFile=path/to/file.xml`. Example configs are inside the jar
- required JaCoCo options: `dumponexit=true,output=tcpserver,port=???`

