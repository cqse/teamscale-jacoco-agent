@echo off
set DIR="%~dp0"
set BAT_EXEC="%DIR:"=%\installer\installer-windows-x86_64\bin\installer.bat"

rem ensure that we don't accidentally profile the uninstaller
set JAVA_TOOL_OPTIONS=
set _JAVA_OPTIONS=

pushd %DIR% & %BAT_EXEC% %* & popd
