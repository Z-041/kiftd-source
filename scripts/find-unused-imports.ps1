# Find potentially unused imports in all java sources.
# Heuristic: a normal (non-static) import is reported when its simple class name
# does not appear in the file body (import lines excluded). Static wildcard
# imports (JUnit/Mockito conventions) are intentionally skipped.
# Conservative by design: matches inside comments/strings cause false negatives,
# which is the safe direction (keeps the import).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/find-unused-imports.ps1

param()

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$dirs = @(
    (Join-Path $root "src\main\java"),
    (Join-Path $root "src\test\java")
)

$results = @()

foreach ($d in $dirs) {
    if (-not (Test-Path $d)) { continue }
    Get-ChildItem -Path $d -Recurse -Filter *.java -File | ForEach-Object {
        $file = $_.FullName
        $content = Get-Content -Path $file -Raw
        if ($null -eq $content) { return }

        # Body without any import lines
        $bodyLines = $content -split "`n" | Where-Object { $_.Trim() -notmatch '^import\s' }
        $body = $bodyLines -join "`n"

        $lines = $content -split "`n"
        foreach ($line in $lines) {
            $t = $line.Trim()
            if ($t -notmatch '^import\s') { continue }
            if ($t -match '^import\s+static\b') { continue }  # static wildcard imports are conventions, skip
            if ($t -match '^import\s+(.+?)\s*;\s*$') {
                $name = $Matches[1]
                if ($name -match '\.\*$') { continue }
                $simple = ($name -split '\.')[-1]
                if ($body -notmatch [regex]::Escape($simple)) {
                    $results += [PSCustomObject]@{ File = $file.Substring($root.Length + 1); Import = $t; Kind = "normal" }
                }
            }
        }
    }
}

Write-Host ""
Write-Host "============================================="
Write-Host "  Unused import scan"
Write-Host "============================================="
$results | Sort-Object File | Format-Table -AutoSize | Out-String -Width 250 | Write-Host
Write-Host ("TOTAL_CANDIDATES=" + $results.Count)
Write-Host "============================================="
