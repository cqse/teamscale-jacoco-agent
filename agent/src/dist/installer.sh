#!/bin/sh
DIR="${0%/*}"

# ensure that we don't accidentally profile the uninstaller
export JAVA_TOOL_OPTIONS=
export _JAVA_OPTIONS=

exec "$DIR/installer/installer-linux-x86_64/bin/installer" "$@"