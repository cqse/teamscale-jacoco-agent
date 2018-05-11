@echo off
if exist JNLPFile.ftype.bak (
    set /p oldftype=<JNLPFile.ftype.bak
	ftype %oldftype%
	echo Uninstalled. Reverted back to:
	ftype JNLPFile
) else (
    echo No backup file found, cannot restore old file association. You can restore it manually or by reinstalling Java.
)

