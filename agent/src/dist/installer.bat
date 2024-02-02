@echo off
set DIR="%~dp0"
set BAT_EXEC="%DIR:"=%\installer\installer-windows-x86_64\bin\installer.bat"

pushd %DIR% & %BAT_EXEC% %* & popd
