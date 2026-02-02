$project = "D:\D backup\My Documents\projects\lock-pc-android"
Set-Location -LiteralPath $project
$p = Join-Path $project 'build\reports\problems\problems-report.html'
if (-not (Test-Path $p)) { Write-Output 'MISSING'; exit 0 }
Write-Output 'Attempting takeown...'
try {
    & takeown.exe /F $p /A 2>&1 | ForEach-Object { Write-Output $_ }
} catch { Write-Output "takeown failed: $_" }
Write-Output 'Attempting icacls grant to current user...'
try {
    & icacls $p /grant "$env:USERNAME:F" 2>&1 | ForEach-Object { Write-Output $_ }
} catch { Write-Output "icacls failed: $_" }
Write-Output 'Attempting delete...'
try {
    Remove-Item $p -Force -ErrorAction Stop
    Write-Output 'DELETED'
} catch {
    Write-Output "DELETE_FAILED: $_"
}
