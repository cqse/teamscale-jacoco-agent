#!/bin/sh
DIR="${0%/*}"

# ensure that we don't accidentally profile the uninstaller
set JAVA_TOOL_OPTIONS=
set _JAVA_OPTIONS=

exec "$DIR/installer/installer-linux-x86_64/bin/installer" "$@"