param(
    [string]$Mode = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$MavenWrapper = Join-Path $ProjectRoot "mvnw.cmd"

Set-Location -LiteralPath $ProjectRoot
$Host.UI.RawUI.WindowTitle = "Life HUD"

function Invoke-LifeHudMaven {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    if (-not (Test-Path -LiteralPath $MavenWrapper)) {
        Write-Host "[ERROR] mvnw.cmd not found in the project root." -ForegroundColor Red
        Read-Host "Press Enter to return"
        return
    }

    Write-Host ""
    Write-Host ("Running: mvnw.cmd " + ($Arguments -join " ")) -ForegroundColor Cyan
    Write-Host ""

    try {
        & $MavenWrapper @Arguments
        $exitCode = $LASTEXITCODE
    }
    catch {
        Write-Host ("[ERROR] " + $_.Exception.Message) -ForegroundColor Red
        $exitCode = 1
    }

    Write-Host ""
    if ($exitCode -eq 0) {
        Write-Host "[OK] Command completed." -ForegroundColor Green
    }
    else {
        Write-Host ("[FAILED] Command exited with code " + $exitCode + ".") -ForegroundColor Red
    }
    Read-Host "Press Enter to return to the menu"
}

function Show-LifeHudHelp {
    Write-Host "Life HUD launcher" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  run-lifehud.ps1             Show the menu"
    Write-Host "  run-lifehud.ps1 start       Start the application"
    Write-Host "  run-lifehud.ps1 test        Run tests"
    Write-Host "  run-lifehud.ps1 clean-test  Clean and run tests"
    Write-Host "  run-lifehud.ps1 package     Build the executable jar"
    Read-Host "Press Enter to close"
}

function Show-LifeHudMenu {
    while ($true) {
        Clear-Host
        Write-Host ""
        Write-Host "========================================" -ForegroundColor DarkCyan
        Write-Host "              Life HUD" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor DarkCyan
        Write-Host "  [1] Start application   http://localhost:8025"
        Write-Host "  [2] Run tests"
        Write-Host "  [3] Clean and run tests"
        Write-Host "  [4] Build package"
        Write-Host "  [5] Exit"
        Write-Host "========================================" -ForegroundColor DarkCyan

        $option = Read-Host "Select an option (1-5)"
        switch ($option) {
            "1" { Invoke-LifeHudMaven @("spring-boot:run") }
            "2" { Invoke-LifeHudMaven @("test") }
            "3" { Invoke-LifeHudMaven @("clean", "test") }
            "4" { Invoke-LifeHudMaven @("clean", "package") }
            "5" { return }
            default {
                Write-Host "Please enter 1, 2, 3, 4 or 5." -ForegroundColor Yellow
                Start-Sleep -Seconds 1
            }
        }
    }
}

switch ($Mode.ToLowerInvariant()) {
    "start"      { Invoke-LifeHudMaven @("spring-boot:run"); break }
    "test"       { Invoke-LifeHudMaven @("test"); break }
    "clean-test" { Invoke-LifeHudMaven @("clean", "test"); break }
    "package"    { Invoke-LifeHudMaven @("clean", "package"); break }
    "help"       { Show-LifeHudHelp; break }
    ""           { Show-LifeHudMenu; break }
    default      { Show-LifeHudHelp; break }
}
