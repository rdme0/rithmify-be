# debug_build.ps1
# Runs the gradle build with plain console and redirects output to a log file, then displays it.
# This ensures compilation errors are captured even if the terminal buffer is limited.

Write-Host "Running Gradle build with log capture..."
./gradlew classes --no-daemon --console=plain > build_log.txt 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build Failed! Displaying the last 50 lines of the log:" -ForegroundColor Red
    Get-Content build_log.txt -Tail 50
} else {
    Write-Host "Build Succeeded!" -ForegroundColor Green
}
