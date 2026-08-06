# =============================================
#  Import reorder tool (IMP-002)
#  Re-sorts every java source file's import block:
#    group order: java.* -> jakarta/javax.* -> org/com.*
#    -> kohgylw.* -> other, then static imports (last).
#    Sorted ordinal within each group.
#  UTF-8 (no BOM) read/write keeps Chinese comments intact.
#  Usage:
#    powershell -ExecutionPolicy Bypass -File scripts\reorder-imports.ps1
#  Exit code: 0 always on success.
# =============================================
$ErrorActionPreference = 'Stop'

$srcDir = Join-Path $PSScriptRoot '..\src\main\java'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$comp = [System.StringComparer]::Ordinal

function Get-ImportGroup([string]$imp) {
    if ($imp -match '^(java|jdk)\.') { return 0 }
    if ($imp -match '^(jakarta|javax)\.') { return 1 }
    if ($imp -match '^(org|com)\.') { return 2 }
    if ($imp -match '^kohgylw\.') { return 3 }
    return 4
}

$files = Get-ChildItem -Path $srcDir -Recurse -Filter *.java
$changed = 0
foreach ($f in $files) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName, $utf8NoBom)
    $importIdx = @()
    $importText = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $t = $lines[$i].Trim()
        if ($t -match '^import\s+(static\s+)?[A-Za-z0-9_.*]+;\s*$') {
            $importIdx += $i
            $importText += $t
        }
    }
    if ($importText.Count -eq 0) { continue }

    # build sort keys (Name = import target without "import " prefix,
    # so ordering matches check-import-order.ps1 exactly)
    $keyed = @()
    for ($k = 0; $k -lt $importText.Count; $k++) {
        $t = $importText[$k]
        $name = (($t -replace '^import\s+', '') -replace ';\s*$', '')
        if ($t -match '^import\s+static') { $g = 5 } else { $g = Get-ImportGroup $name }
        $keyed += ,[pscustomobject]@{ G = $g; Name = $name; T = $t }
    }

    $sortedText = @()
    foreach ($g in 0..5) {
        $group = @($keyed | Where-Object { $_.G -eq $g })
        if ($group.Count -gt 0) {
            $names = [string[]]@($group | ForEach-Object { $_.Name })
            [System.Array]::Sort($names, $comp)
            $map = @{}
            foreach ($item in $group) { $map[$item.Name] = $item.T }
            foreach ($n in $names) { $sortedText += $map[$n] }
        }
    }

    # skip if already sorted
    $same = ($sortedText.Count -eq $importText.Count)
    if ($same) {
        for ($k = 0; $k -lt $importText.Count; $k++) {
            if ($comp.Compare($importText[$k], $sortedText[$k]) -ne 0) { $same = $false; break }
        }
    }
    if ($same) { continue }

    $first = $importIdx[0]
    $last = $importIdx[$importIdx.Count - 1]
    $head = @($lines[0..($first - 1)])
    $tail = @($lines[($last + 1)..($lines.Count - 1)])
    $out = @()
    $out += $head
    $out += $sortedText
    $out += ''
    $out += $tail
    [System.IO.File]::WriteAllLines($f.FullName, $out, $utf8NoBom)
    $changed++
    Write-Host "  [SORTED] $($f.Name)"
}
Write-Host "============================================="
Write-Host "  Import reorder (IMP-002)"
Write-Host "============================================="
Write-Host "  Files reordered: $changed"
exit 0
