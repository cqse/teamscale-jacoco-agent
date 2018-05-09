@echo off
echo Uninstallation will fail unless you have Administrator rights!

if exist JNLPFile.ftype.bak (
    set /p oldftype=<JNLPFile.ftype.bak
	ftype %oldftype%
	
	echo Uninstalled.
) else (
    echo No backup file found, cannot restore old file association. You can restore it manually or by reinstalling Java.
)
