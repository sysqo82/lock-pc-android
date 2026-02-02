$project = "D:\D backup\My Documents\projects\lock-pc-android"
Set-Location -LiteralPath $project
Write-Output "Running replace-wrapper.ps1 (if present)..."
$replace = Join-Path $project 'scripts\replace-wrapper.ps1'
if (Test-Path $replace) {
    powershell -NoProfile -ExecutionPolicy Bypass -File $replace
} else {
    Write-Output "replace-wrapper.ps1 not found, skipping"
}
Write-Output "Checking gradle-wrapper.jar..."
Get-Item -LiteralPath .\gradle\wrapper\gradle-wrapper.jar -ErrorAction SilentlyContinue | Format-List Name,Length,LastWriteTime
Write-Output "Stopping any Gradle daemons (if any)..."
& .\gradlew.bat --stop 2>&1 | Tee-Object -FilePath .\build-gradle-output.log
$wrapper = Get-Item -LiteralPath .\gradle\wrapper\gradle-wrapper.jar -ErrorAction SilentlyContinue
if ($null -ne $wrapper -and $wrapper.Length -gt 0) {
    & .\gradlew.bat --stop 2>&1 | Tee-Object -FilePath .\build-gradle-output.log
    & .\gradlew.bat clean build --stacktrace 2>&1 | Tee-Object -FilePath .\build-gradle-output.log -Append
    $exit = $LASTEXITCODE
    Write-Output "Gradle (wrapper) exit code: $exit"
    if ($exit -ne 0) { Write-Output "See build-gradle-output.log for details"; exit $exit }
    Write-Output "Build completed successfully (wrapper)"
} else {
    Write-Output 'Wrapper JAR missing or empty - falling back to bundled Gradle distribution'
    $zip = Join-Path $project 'gradle-8.13-bin.zip'
    if (-not (Test-Path $zip)) { Write-Error 'gradle-8.13-bin.zip not found; cannot continue'; exit 4 }
    $dest = Join-Path $project 'gradle-dist-temp'
    if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
    Expand-Archive -LiteralPath $zip -DestinationPath $dest
    $gradleHome = Get-ChildItem -Path $dest | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    if ($null -eq $gradleHome) { Write-Error 'gradle folder not found in zip'; exit 5 }
    $gradleBin = Join-Path $gradleHome.FullName 'bin\\gradle.bat'
    Write-Output "Using gradle executable: $gradleBin"
    & $gradleBin --stop 2>&1 | Tee-Object -FilePath .\build-gradle-output.log -Append
    & $gradleBin -p $project clean build --stacktrace 2>&1 | Tee-Object -FilePath .\build-gradle-output.log -Append
    $exit = $LASTEXITCODE
    Write-Output "Gradle (dist) exit code: $exit"
    if ($exit -ne 0) { Write-Output "See build-gradle-output.log for details"; exit $exit }
    Write-Output 'Build completed successfully (dist)'
}
