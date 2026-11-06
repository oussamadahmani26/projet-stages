# Démarre les 6 services et l'orchestrateur, après "mvn -DskipTests package".
# Usage : powershell -ExecutionPolicy Bypass -File scripts\demarrer.ps1
$racine = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $racine ".logs"
New-Item -ItemType Directory -Force $logs | Out-Null
$fichierPids = Join-Path $logs "pids.txt"
if (Test-Path $fichierPids) {
    Write-Host "Des services semblent déjà lancés. Exécutez d'abord scripts\arreter.ps1." -ForegroundColor Yellow
    exit 1
}

# Le port 8080 est volontairement évité : Bonita Studio l'utilise.
$services = @(
    @{ Nom = "soap-etudiants";     Port = 8081 },
    @{ Nom = "soap-entreprises";   Port = 8082 },
    @{ Nom = "soap-offres";        Port = 8083 },
    @{ Nom = "rest-candidatures";  Port = 8084 },
    @{ Nom = "rest-notifications"; Port = 8085 },
    @{ Nom = "rest-stages";        Port = 8086 },
    @{ Nom = "bpmn";               Port = 8090 }
)

foreach ($s in $services) {
    $jar = Join-Path $racine "$($s.Nom)\target\$($s.Nom)-1.0-SNAPSHOT-app.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "Jar absent : $jar. Exécutez d'abord : mvn -DskipTests package" -ForegroundColor Red
        exit 1
    }
    if (Get-NetTCPConnection -State Listen -LocalPort $s.Port -ErrorAction SilentlyContinue) {
        Write-Host "Le port $($s.Port) ($($s.Nom)) est déjà utilisé par un autre programme." -ForegroundColor Red
        exit 1
    }
}

foreach ($s in $services) {
    $jar = Join-Path $racine "$($s.Nom)\target\$($s.Nom)-1.0-SNAPSHOT-app.jar"
    $env:PORT = "$($s.Port)"
    $p = Start-Process java -ArgumentList "-jar", "`"$jar`"" -WorkingDirectory $racine -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs "$($s.Nom).log") -RedirectStandardError (Join-Path $logs "$($s.Nom).err.log")
    Add-Content $fichierPids $p.Id
    Write-Host "$($s.Nom) lancé (port $($s.Port), processus $($p.Id))"
}
Remove-Item Env:\PORT

Write-Host "Attente de la plateforme..."
# En HTTPS, le certificat autosigné n'est pas reconnu par PowerShell : on contrôle alors l'ouverture du port.
$https = [bool]$env:SOA_KEYSTORE
$adresse = if ($https) { "https://localhost:8090/" } else { "http://localhost:8090/" }
for ($i = 0; $i -lt 60; $i++) {
    try {
        if ($https) {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $tcp.Connect("localhost", 8090)
            $tcp.Close()
            $pret = $true
        } else {
            $sante = Invoke-RestMethod -TimeoutSec 15 "http://localhost:8090/api/sante"
            $pret = @($sante.PSObject.Properties | Where-Object { -not $_.Value }).Count -eq 0
        }
        if ($pret) {
            Write-Host "Plateforme prête : $adresse" -ForegroundColor Green
            exit 0
        }
    } catch {
        # Pas encore démarrée
    }
    Start-Sleep -Seconds 1
}
Write-Host "La plateforme ne répond pas correctement : consultez le dossier .logs" -ForegroundColor Red
exit 1
