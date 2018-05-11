@echo off

if exist JNLPFile.ftype.bak (
	echo Already installed.
	exit
)

ftype JNLPFile > JNLPFile.ftype.bak
ftype JNLPFile=%~dp0\javaws.exe "%%1"

echo Installed. Changed to:
ftype JNLPFile
echo Run uninstall.bat to restore the old behaviour without a profiler.

