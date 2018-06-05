# Installs the javaws wrapper for the current user
# Run this as:
# powershell -ExecutionPolicy ByPass ./install.ps1

# taken from https://stackoverflow.com/a/10000292/1396068
function Test-IsAdmin {
    try {
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
        $principal = New-Object Security.Principal.WindowsPrincipal -ArgumentList $identity
        return $principal.IsInRole( [Security.Principal.WindowsBuiltInRole]::Administrator )
    } catch {
        throw "Failed to determine if the current user has elevated privileges. The error was: '{0}'." -f $_
    }

    <#
        .SYNOPSIS
            Checks if the current Powershell instance is running with elevated privileges or not.
        .EXAMPLE
            PS C:\> Test-IsAdmin
        .OUTPUTS
            System.Boolean
                True if the current Powershell is elevated, false if not.
    #>
}

if (-not (Test-IsAdmin)) {
	write-host "You must run this script as an administrator"
	exit 1
}

$currentSetting = ((cmd.exe /c ftype JNLPFile) | out-string) -replace "`r|`n", ""
write-host "Current setting: $currentSetting"

$enabledSetting = "JNLPFile=$PSScriptRoot\bin\javaws `"%1`""

if ($currentSetting -eq $enabledSetting) {
	write-host "Already installed. Not doing anything."
	exit
}


if (test-path "$PSScriptRoot/JNLPFile.ftype.bak") {
	write-host "It seems you already ran the installer once. Not installing again."
	write-host "You can delete $PSScriptRoot/JNLPFile.ftype.bak if you want to force reinstallation (not recommended)."
	exit 1
}

$newSetting = ((cmd.exe /c "ftype $enabledSetting") | out-string) -replace "`r|`n", ""

if ($newSetting -eq $enabledSetting) {
	$currentSetting | out-file "$PSScriptRoot/JNLPFile.ftype.bak"
	write-host "Installed successfully. New setting: $newSetting"
} else {
	write-host "Installation failed."
	write-host "Setting before running this script: $currentSetting"
	write-host "Current setting: $newSetting"
	write-host "Should be: $enabledSetting"
}
