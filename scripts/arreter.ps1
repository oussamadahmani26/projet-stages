# Arrête les processus lancés par demarrer.ps1 (et eux seuls).
$fichierPids = Join-Path (Split-Path -Parent $PSScriptRoot) ".logs\pids.txt"
if (-not (Test-Path $fichierPids)) {
    Write-Host "Aucun service à arrêter."
    exit 0
}
foreach ($id in Get-Content $fichierPids) {
    $p = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($p -and $p.ProcessName -eq "java") {
        Stop-Process -Id $id -Force
        Write-Host "Processus $id arrêté."
    }
}
Remove-Item $fichierPids
