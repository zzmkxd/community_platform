Write-Host "=== Top 15 Processes by Memory ==="
Get-Process | Sort-Object -Property WorkingSet -Descending | Select-Object -First 15 Name,Id,@{N='Mem_MB';E={[math]::Round($_.WorkingSet/1MB,0)}},CPU | Format-Table -AutoSize

Write-Host "=== Java Processes ==="
Get-Process -Name java -ErrorAction SilentlyContinue | Select-Object Name,Id,@{N='Mem_MB';E={[math]::Round($_.WorkingSet/1MB,0)}},CPU | Format-Table -AutoSize

Write-Host "=== Docker/WSL Processes ==="
Get-Process | Where-Object {$_.Name -like '*docker*' -or $_.Name -like '*vmmem*' -or $_.Name -like '*wsl*'} | Select-Object Name,Id,@{N='Mem_MB';E={[math]::Round($_.WorkingSet/1MB,0)}} | Format-Table -AutoSize

Write-Host "=== Listening Ports ==="
netstat -ano | Select-String "LISTENING" | Select-String "8080|8081|8082|8083|8084|8091|8848|3308|6381|9200|9876"

Write-Host "=== Disk I/O - Top Read ==="
Get-Process | Sort-Object -Property ReadOperationCount -Descending | Select-Object -First 5 Name,Id,ReadOperationCount,WriteOperationCount | Format-Table -AutoSize
