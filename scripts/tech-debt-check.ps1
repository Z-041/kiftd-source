# Tech-debt static check script.
# Guards against new technical debt accumulation (see docs/TECH-DEBT-REGISTER.md).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/tech-debt-check.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/tech-debt-check.ps1 -Verbose
#
# Exit code:
#   0 = all metrics within thresholds (PASS)
#   1 = threshold exceeded (FAIL)
#   2 = script/setup error
#
# Thresholds must stay consistent with the "monitoring" section of docs/TECH-DEBT-ITERATION.md.

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$src  = Join-Path $root "src\main\java"

# Direct System.out/err output is only forbidden in the refactored core
# (newcore + server packages) where Printer/LogUtil must be used instead.
# Legacy entry points (mc launcher banner, Swing ui, Printer itself)
# legitimately write to the console, so they are excluded from this rule.
$coreServer = Join-Path $src "kohgylw\kiftd\server"
$coreNew    = Join-Path $src "kohgylw\kiftd\newcore"

if (-not (Test-Path $src)) {
    Write-Error "Source directory not found: $src"
    exit 2
}

# ---------- Thresholds ----------
$thresholds = [ordered]@{
    "TODO-comments"        = 50  # total TODO/FIXME/HACK/XXX comments allowed
    "System-out-print"     = 0   # System.out/err.print usages (should use Printer/LogUtil)
    "printStackTrace"      = 0   # direct printStackTrace usages (should be logged)
    "Ajax-protocol-literal"= 0   # raw AJAX protocol literals (return "ERROR" etc.)
}

# ---------- Rules ----------
$rules = [ordered]@{
    "TODO-comments"         = '//\s*(TODO|FIXME|HACK|XXX)\b'
    "System-out-print"      = 'System\.(out|err)\.print'
    "printStackTrace"       = '\.printStackTrace\('
    "Ajax-protocol-literal" = 'return\s+"(SUCCESS|ERROR|NOT_FOUND|notAccess|noAuthorized|errorParameter|foldersTotalOutOfLimit|filesTotalOutOfLimit|nameOccupied|createFolderSuccess|cannotCreateFolder|deleteFolderSuccess|cannotDeleteFolder|renameFolderSuccess|deleteError|deleteSuccess|deleteFileSuccess|cannotDeleteFile|renameFileSuccess|cannotMoveFiles)"'
}

function Get-RuleCount {
    param([string]$pattern, [string]$baseDir = $src)
    $total = 0
    Get-ChildItem -Path $baseDir -Recurse -Filter *.java -File | ForEach-Object {
        $m = Select-String -Path $_.FullName -Pattern $pattern -AllMatches
        foreach ($line in $m) {
            $total += $line.Matches.Count
        }
    }
    return $total
}

Write-Host ""
Write-Host "============================================="
Write-Host "  Tech-debt static check"
Write-Host "============================================="
Write-Host ("Source dir: " + $src)
Write-Host ""

$failed = $false
foreach ($name in $rules.Keys) {
    $base = $src
    if ($name -eq "System-out-print") {
        # Only scan the refactored core packages for this rule
        $count = (Get-RuleCount $rules[$name] $coreServer) + (Get-RuleCount $rules[$name] $coreNew)
    } else {
        $count = Get-RuleCount $rules[$name] $base
    }
    $limit = $thresholds[$name]
    $status = if ($count -le $limit) { "OK" } else { "FAIL" }
    if ($status -eq "FAIL") { $failed = $true }
    Write-Host ("  [{0,-4}] {1,-24} current: {2,4}  threshold: {3}" -f $status, $name, $count, $limit)
}

Write-Host ""
if ($failed) {
    Write-Host "  RESULT: FAIL - thresholds exceeded. Register new debt and schedule a fix." -ForegroundColor Red
    Write-Host "============================================="
    exit 1
} else {
    Write-Host "  RESULT: PASS - all metrics within thresholds." -ForegroundColor Green
    Write-Host "============================================="
    exit 0
}
