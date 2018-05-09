@echo off
echo Installation will fail unless you have Administrator rights!

ftype JNLPFile > JNLPFile.ftype.bak
ftype JNLPFile=%~dp0\bin\javaws "%%1"

echo Installed. Please restart your web browser if it is still running!
echo Run uninstall.bat to restore the old behaviour without a profiler.
