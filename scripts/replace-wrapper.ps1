$project = "D:\D backup\My Documents\projects\lock-pc-android"
Set-Location -LiteralPath $project
$zip = Join-Path $project 'gradle-8.13-bin.zip'
$dest = Join-Path $project 'gradle-dist-temp'
if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
Expand-Archive -LiteralPath $zip -DestinationPath $dest
$src = Get-ChildItem -Path $dest -Recurse -Filter 'gradle-wrapper.jar' | Select-Object -First 1
if ($null -eq $src) { Write-Error 'wrapper not found in zip'; exit 1 }
Write-Output "Found: $($src.FullName)"
$targetDir = Join-Path $project 'gradle\wrapper'
if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir | Out-Null }
Copy-Item -Force $src.FullName (Join-Path $targetDir 'gradle-wrapper.jar')
Get-Item (Join-Path $targetDir 'gradle-wrapper.jar') | Format-List Name,Length,LastWriteTime
