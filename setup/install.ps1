param(
    [string]$Profile = "desktop",
    [string]$FileRoot = "",
    [string]$DshHome = "",
    [string]$PublicHostname = "",
    [switch]$DryRun,
    [switch]$Yes,
    [switch]$NoRelay,
    [switch]$NoNotify
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "dsh-mobile-setup.mjs"
if (-not (Test-Path -LiteralPath $script)) {
    throw "setup/dsh-mobile-setup.mjs not found"
}

$cliArgs = @($script, "install", "--profile", $Profile)
if ($FileRoot) { $cliArgs += @("--file-root", $FileRoot) }
if ($DshHome) { $cliArgs += @("--dsh-home", $DshHome) }
if ($PublicHostname) { $cliArgs += @("--public-hostname", $PublicHostname) }
if ($DryRun) { $cliArgs += "--dry-run" }
if ($Yes) { $cliArgs += "--yes" }
if ($NoRelay) { $cliArgs += "--no-relay" }
if ($NoNotify) { $cliArgs += "--no-notify" }

& node @cliArgs
exit $LASTEXITCODE
