# Wrapper around javaws to allow profiling Web Start applications

## Installation

Put all files in any directory, as long as it is readable by all users that want to start
Web Start applications.

Under Windows, run `powershell -ExecutionPolicy ByPass install.ps1`. This will register the wrapper as the handler for JNLP files
(for the current user).

Under Linux, associate JNLP files with the wrapper (`bin/javaws`).

## Uninstallation

For Windows, run `powershell -ExecutionPolicy ByPass uninstall.bat`. This will restore the old file type mapping (for the current user).

Under Linux, revert whatever you did to associate the wrapper with JNLP files.

## Configuration

The wrapper expects a file called `javaws.properties` in the same directory as the wrapper is located.
An example is provided.
Please remember that backslashes have to be escaped in the properties file! E.g.:

    javaws=C:\\Program Files (x86)\\Java\\jre1.8.0_171\\bin\\javaws.exe

Alternatively, just use forward slashes. They work under Windows as well!

## Security

In order for the agent to be able to profile classes and send the data to disk or Teamscale, it needs
more permissions than a Java Web Start application normally has. These are granted by the `agent.policy`
policy file. Reducing these permissions may result in the agent failing with security exceptions, i.e.
it won't work.

