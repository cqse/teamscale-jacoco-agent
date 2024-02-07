#!/bin/sh
DIR="${0%/*}"

# ensure that we don't accidentally profile the uninstaller
unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS

exec "$DIR/installer/installer-linux-x86_64/bin/installer" "$@"