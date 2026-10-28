<#
.SYNOPSIS
    One-time setup of the release signing key and the GitHub secrets the
    release workflow uses.

.DESCRIPTION
    - Creates a PKCS12 keystore OUTSIDE the repo (and outside OneDrive).
    - Generates a random password and shows it once.
    - Uploads the four secrets to the GitHub repository with `gh secret set`.

    BACK UP THE KEYSTORE AND ITS PASSWORD. Android only installs an update
    signed with the same key as the installed app. Lose either one and every
    installed copy has to be uninstalled before it can update again.

.EXAMPLE
    pwsh scripts/setup-release-signing.ps1
#>
param(
    [string]$Repo = "Anedre/asistencia-novasys-android",
    [string]$KeystoreDir = (Join-Path $HOME ".nova-signing"),
    [string]$Alias = "nova-release"
)

$ErrorActionPreference = "Stop"
$keystore = Join-Path $KeystoreDir "nova-release.jks"

if (Test-Path $keystore) {
    throw "Ya existe $keystore. No la sobrescribo: perderla impide actualizar las apps instaladas."
}

$keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\keytool.exe" } else { "keytool" }
gh auth status *> $null
if ($LASTEXITCODE -ne 0) { throw "Inicia sesión primero con: gh auth login" }

New-Item -ItemType Directory -Force $KeystoreDir | Out-Null

# 32 random bytes as URL-safe base64: strong, and safe to pass on a command line.
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$password = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

# PKCS12 keystores use one password for both the store and the key.
& $keytool -genkeypair -v `
    -storetype PKCS12 `
    -keystore $keystore `
    -alias $Alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $password -keypass $password `
    -dname "CN=Novasys Peru, O=Novasys Peru, C=PE" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "keytool falló" }

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))

# Values go through stdin so they never show up in the process list.
$keystoreBase64 | gh secret set NOVA_KEYSTORE_BASE64 --repo $Repo
$password       | gh secret set NOVA_KEYSTORE_PASSWORD --repo $Repo
$Alias          | gh secret set NOVA_KEY_ALIAS --repo $Repo
$password       | gh secret set NOVA_KEY_PASSWORD --repo $Repo

Write-Host ""
Write-Host "Listo. Secrets cargados en $Repo." -ForegroundColor Green
Write-Host ""
Write-Host "Keystore:   $keystore"
Write-Host "Alias:      $Alias"
Write-Host "Contraseña: $password"
Write-Host ""
Write-Host "GUARDA AHORA la contraseña en tu gestor de contraseñas y haz una copia" -ForegroundColor Yellow
Write-Host "del keystore fuera de esta PC. Sin ambos no podrás publicar actualizaciones." -ForegroundColor Yellow
