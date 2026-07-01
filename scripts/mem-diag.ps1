Write-Host "=== Memory Overview ==="
$os = Get-CimInstance Win32_OperatingSystem
$totalRAM = [math]::Round($os.TotalVisibleMemorySize/1MB,1)
$freeRAM = [math]::Round($os.FreePhysicalMemory/1MB,1)
$usedRAM = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory)/1MB,1)
$pct = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / $os.TotalVisibleMemorySize * 100, 1)
Write-Host "  Total RAM : $totalRAM GB"
Write-Host "  Used      : $usedRAM GB ($pct%)"
Write-Host "  Free      : $freeRAM GB"

Write-Host ""
Write-Host "=== Page File (pagefile.sys) ==="
$pf = Get-CimInstance Win32_PageFileUsage
Write-Host "  Current Usage : $([math]::Round($pf.CurrentUsage/1MB,1)) GB"
Write-Host "  Peak Usage    : $([math]::Round($pf.PeakUsage/1MB,1)) GB"
Write-Host "  Allocated     : $([math]::Round($pf.AllocatedBaseSize/1MB,1)) GB"

Write-Host ""
Write-Host "=== WSL2 VM (vmmem) Memory ==="
Get-Process -Name 'vmmem*' -ErrorAction SilentlyContinue | ForEach-Object {
    $ws = [math]::Round($_.WorkingSet64/1GB,2)
    $pm = [math]::Round($_.PrivateMemorySize64/1GB,2)
    Write-Host "  $($_.Name) (PID $($_.Id)): WorkingSet=$ws GB, Private=$pm GB"
}

Write-Host ""
Write-Host "=== Top 15 Memory Consumers ==="
Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 15 Name,Id,@{N='WS_GB';E={[math]::Round($_.WorkingSet64/1GB,2)}},@{N='PM_GB';E={[math]::Round($_.PrivateMemorySize64/1GB,2)}} | Format-Table -AutoSize

Write-Host "=== Docker Container Stats ==="
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>&1
