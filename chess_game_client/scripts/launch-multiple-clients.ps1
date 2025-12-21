<#
Launch multiple independent instances of the JavaFX client.

Usage:
  .\launch-multiple-clients.ps1 -Count 3 -JavaHome 'C:\Program Files\Java\jdk-22'

By default the script will use the existing `JAVA_HOME` environment variable.
Each instance is launched in a new PowerShell window running `mvnw.cmd javafx:run`.

Note: building a runnable jar and launching `java -jar` is faster for many windows.
#>

param(
  [int]$Count = 2,
  [string]$JavaHome = $env:JAVA_HOME
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Resolve-Path (Join-Path $scriptDir "..")

Write-Host "Launching $Count client instance(s) from: $projectDir"
if ($JavaHome) { Write-Host "Using JAVA_HOME: $JavaHome" } else { Write-Host "JAVA_HOME not set; using system default java." }

for ($i = 1; $i -le $Count; $i++) {
  $title = "ChessClient-$i"
  $cmd = "cd '$projectDir';"
  if ($JavaHome) { $cmd += "$env:JAVA_HOME='$JavaHome';" }
  $cmd += ".\\mvnw.cmd javafx:run"

  Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit","-Command", $cmd) -WindowStyle Normal
  Start-Sleep -Milliseconds 400
}

Write-Host "Launched $Count instances." -ForegroundColor Green
