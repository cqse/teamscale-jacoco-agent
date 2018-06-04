# Wrapper around javaws to allow profiling Web Start applications

## Installation

Please review the Security section below before installing!

Put all files in any directory, as long as it is readable by all users that want to start
Web Start applications.

Under Windows, run `bin\javaws install` as an administrator. This will register the wrapper as the handler for JNLP files
(for the current user) and fix the security policy file (for the JVM installation under which it is run).

Under Linux, associate JNLP files with the wrapper (`bin/javaws`). If necessary, replace the JVM's `javaws.policy`
with the contents of the `agent.policy`.

## Uninstallation

For Windows, run `bin\javaws uninstall`. This will restore the old file type mapping (for the current user)
and the javaws security policy file (for the JVM installation under which it is run).

Under Linux, revert whatever you did to associate the wrapper with JNLP files and undo any changes to the `javaws.policy`.

## Configuration

The wrapper expects a file called `javaws.properties` in the same directory as the wrapper is located.
An example is provided. If you used the Windows installer (see above), this will be partly preconfigured for you.
Please fill out all properties in that file before first use of the wrapper.

The `agentArguments` must be filled according to the guidelines from the agent's userguide.

As per the Java properties file spec, the file must use the ISO 8859-1 encoding!
Please remember that backslashes have to be escaped in the properties file! E.g.:

    javaws=C:\\Program Files (x86)\\Java\\jre1.8.0_171\\bin\\javaws.exe

Alternatively, just use forward slashes. They work under Windows as well!

## Security

In order for the wrapper to be able to profile classes and send the data to disk or Teamscale, it needs
more permissions than a Java Web Start application normally has. These are granted by the `agent.policy`
policy file. Reducing these permissions may result in the wrapper failing with security exceptions or
failing silently without the application launching - i.e. it won't work.

In order for the wrapper to work under Windows under all JVMs, the Windows installer replaces the JVM
installation's `javaws.policy` file, i.e. security settings for all Java Web Start processes are
changed! Please make sure that this is OK before installing this tool.

