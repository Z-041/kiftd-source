# =============================================
#  Import order check (IMP-002 gate)
#  Checks each java source file's import list is
#  grouped and ordered: java.* -> jakarta./javax.*
#  -> org.*/com.* -> kohgylw.* , and sorted within group.
#  Usage:
#    powershell -ExecutionPolicy Bypass -File scripts\check-import-order.ps1
#  Exit code: 0 = PASS; 1 = violations found; 2 = script error.
# =============================================
$ErrorActionPreference = 'Stop'

$srcDir = Join-Path $PSScriptRoot '..\src\main\java'
if (-not (Test-Path $srcDir)) { Write-Host 'ERROR: src dir not found'; exit 2 }

function Get-ImportGroup([string]$imp) {
    # imp is full import text after "import " (static imports skipped)
    if ($imp -match '^(java|jdk)\.') { return 0 }
    if ($imp -match '^(jakarta|javax)\.') { return 1 }
    if ($imp -match '^(org|com)\.') { return 2 }
    if ($imp -match '^kohgylw\.') { return 3 }
    return 4
}

$files = Get-ChildItem -Path $srcDir -Recurse -Filter *.java
$violations = @()
$checked = 0
foreach ($f in $files) {
    $lines = Get-Content -LiteralPath $f.FullName
    $imports = @()
    foreach ($line in $lines) {
        $t = $line.Trim()
        if ($t -match '^import\s+static\s+') {
            # static imports are exempt from ordering rules (test conventions)
            continue
        }
        if ($t -match '^import\s+([A-Za-z0-9_.]+);\s*$') {
            $name = $Matches[1].Trim()
            $imports += ,@($name, $t)
        }
    }
    if ($imports.Count -lt 2) { continue }
    $checked++
    $prevGroup = -1
    $prevName = $null
    foreach ($item in $imports) {
        $name = $item[0]
        $g = Get-ImportGroup $name
        if ($g -lt $prevGroup) {
            $violations += "$($f.Name): group order break [$prevName] -> [$name]"
            break
        }
        if ($g -eq $prevGroup -and $prevName -ne $null -and [System.StringComparer]::Ordinal.Compare($name, $prevName) -lt 0) {
            $violations += "$($f.Name): unsorted within group [$prevName] vs [$name]"
            break
        }
        $prevGroup = $g
        $prevName = $name
    }
}

Write-Host "============================================="
Write-Host "  Import order check (IMP-002)"
Write-Host "============================================="
Write-Host "  Files with imports checked : $checked"
if ($violations.Count -gt 0) {
    Write-Host "  Violations                : $($violations.Count)"
    $violations | Select-Object -First 40 | ForEach-Object { Write-Host "  [KO] $_" }
    Write-Host ""
    Write-Host "  RESULT: FAIL - fix import order above."
    exit 1
}
Write-Host "  Violations                : 0"
Write-Host ""
Write-Host "  RESULT: PASS - all import lists are grouped and sorted."
exit 0
