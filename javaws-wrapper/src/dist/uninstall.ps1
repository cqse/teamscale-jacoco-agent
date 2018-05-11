# Uninstalls the javaws wrapper for the current user
# Run this as:
# powershell -ExecutionPolicy ByPass uninstall.ps1

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

if (-not (test-path "$PSScriptRoot/JNLPFile.ftype.bak")) {
	write-host "$PSScriptRoot/JNLPFile.ftype.bak not found. It seems the wrapper is not installed"
	exit 1
}

$oldSetting = (get-content "$PSScriptRoot/JNLPFile.ftype.bak" | out-string) -replace "`r|`n", ""
$newSetting = ((cmd.exe /c "ftype $oldSetting") | out-string) -replace "`r|`n", ""

if ($newSetting -eq $oldSetting) {
	remove-item "$PSScriptRoot/JNLPFile.ftype.bak"
	write-host "Uninstalled successfully. New setting: $newSetting"
} else {
	write-host "Uninstallation failed."
	write-host "Setting before running this script: $currentSetting"
	write-host "Current setting: $newSetting"
	write-host "Should be: $oldSetting"
}
