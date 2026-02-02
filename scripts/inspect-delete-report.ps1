$project = "D:\D backup\My Documents\projects\lock-pc-android"
Set-Location -LiteralPath $project
$p = Join-Path $project 'build\reports\problems\problems-report.html'
if (Test-Path $p) {
    Get-Item $p | Format-List Name,Length,Attributes,LastWriteTime
    Write-Output '---icacls---'
    & icacls $p
    Write-Output '---try delete---'
    try {
        Remove-Item $p -Force -ErrorAction Stop
        Write-Output 'DELETED'
    } catch {
        Write-Output "DELETE_FAILED: $_"
        Write-Output '---sample files under build\reports---'
        Get-ChildItem -Path (Split-Path $p) -Recurse -Force | Select-Object -First 10 | ForEach-Object { $_.FullName }
    }
} else {
    Write-Output 'MISSING'
}
