@echo off
set DIR="%~dp0"
set BAT_EXEC="%DIR:"=%\bin\install.bat"

pushd %DIR% & %BAT_EXEC% %* & popd
