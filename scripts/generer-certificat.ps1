# Crée un certificat autosigné pour activer HTTPS entre les services (développement uniquement).
# Usage : powershell -ExecutionPolicy Bypass -File scripts\generer-certificat.ps1 -MotDePasse "votre_mot_de_passe"
param([Parameter(Mandatory = $true)][string]$MotDePasse)
$racine = Split-Path -Parent $PSScriptRoot
$dossier = Join-Path $racine "data"
New-Item -ItemType Directory -Force $dossier | Out-Null
$fichier = Join-Path $dossier "soa.p12"
& keytool -genkeypair -alias soa -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 `
    -keystore $fichier -storepass $MotDePasse -dname "CN=localhost, O=INPT, C=MA" -ext "SAN=dns:localhost,ip:127.0.0.1"
if ($LASTEXITCODE -eq 0) {
    Write-Host "Certificat créé : $fichier"
    Write-Host "Avant de démarrer, définissez :"
    Write-Host "  `$env:SOA_KEYSTORE = `"$fichier`""
    Write-Host "  `$env:SOA_KEYSTORE_PASSWORD = `"<votre mot de passe>`""
}
