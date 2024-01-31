@echo off
set DIR="%~dp0"
set BAT_EXEC="%DIR:"=%\installer\bin\installer.bat"

pushd %DIR% & %BAT_EXEC% %* & popd
