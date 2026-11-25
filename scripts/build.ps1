param(
    [ValidateSet('both', 'fabric', 'neoforge')][string]$Loader = 'both',
    [ValidateSet('1.21.1', '26.3', 'all')][string]$Target = 'all'
)
$ErrorActionPreference = 'Stop'
$previousLoader = $env:RELEASE_LOADER
$previousTarget = $env:COINAGE_TARGET
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    $targets = if ($Target -eq 'all') { @('1.21.1', '26.3') } else { @($Target) }
    $task = if ($Loader -eq 'both') { 'build' } else { ":${Loader}:build" }
    foreach ($selected in $targets) {
        & .\gradlew.bat "-PcoinageTarget=$selected" "-PcoinageLoader=$Loader" $task --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build failed for $selected" }
        $env:RELEASE_LOADER = if ($Loader -eq 'both') { $null } else { $Loader }
        $env:COINAGE_TARGET = $selected
        & python scripts/verify_release.py
        if ($LASTEXITCODE -ne 0) { throw "Release verification failed for $selected" }
    }
} finally {
    $env:RELEASE_LOADER = $previousLoader
    $env:COINAGE_TARGET = $previousTarget
    Pop-Location
}
