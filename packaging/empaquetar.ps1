<#
  Genera la version portable de KUMOC para Windows: dist\KUMOC-<version>-windows.zip

  Requisitos: JDK 25 (con jpackage) y Maven en el PATH. JAVA_HOME debe apuntar al JDK.
  El ZIP incluye su propio Java: quien lo descarga no necesita instalar nada.

  Uso (desde la raiz del proyecto):
      powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1
      powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1 -Version 1.1.0
#>
param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

$trabajo = Join-Path $raiz "target\empaquetado"
$entrada = Join-Path $trabajo "input"
$salida = Join-Path $trabajo "out"
$dist = Join-Path $raiz "dist"
$nombre = "KUMOC"

$jpackage = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\jpackage.exe" } else { "jpackage" }
if (-not (Test-Path $jpackage) -and $jpackage -ne "jpackage") {
    throw "No se encontro jpackage en JAVA_HOME ($env:JAVA_HOME). Se necesita un JDK 25."
}

# Limpia el empaquetado anterior (Windows/antivirus pueden retener el .exe unos segundos).
for ($i = 1; ($i -le 5) -and (Test-Path $trabajo); $i++) {
    Remove-Item $trabajo -Recurse -Force -ErrorAction SilentlyContinue
    if (Test-Path $trabajo) { Start-Sleep -Seconds 2 }
}
if (Test-Path $trabajo) { throw "No se pudo borrar $trabajo. Cerra KUMOC si esta abierto y reintenta." }

Write-Host "==> Compilando y reuniendo dependencias..."
& mvn -q clean package dependency:copy-dependencies "-DoutputDirectory=$entrada" "-DincludeScope=runtime"
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion de Maven." }

$jar = Get-ChildItem (Join-Path $raiz "target") -Filter "gestor-kumoc-*.jar" |
    Where-Object { $_.Name -notlike "*-sources*" } | Select-Object -First 1
if (-not $jar) { throw "No se encontro el jar de la aplicacion en target\." }
Copy-Item $jar.FullName $entrada

Write-Host "==> Empaquetando con jpackage (incluye un runtime de Java)..."
New-Item -ItemType Directory -Force $salida | Out-Null
& $jpackage `
    --type app-image `
    --name $nombre `
    --app-version $Version `
    --vendor "KUMOC" `
    --description "KUMOC - Control de stock, presupuestos y ordenes de trabajo" `
    --icon (Join-Path $raiz "packaging\kumoc.ico") `
    --input $entrada `
    --main-jar $jar.Name `
    --main-class org.kumoc.Main `
    --dest $salida `
    --add-modules "java.base,java.desktop,java.scripting,java.sql,java.logging,java.naming,java.xml,java.prefs,jdk.jfr,jdk.unsupported" `
    --java-options "--enable-native-access=ALL-UNNAMED"
if ($LASTEXITCODE -ne 0) { throw "Fallo jpackage." }

Write-Host "==> Comprimiendo..."
New-Item -ItemType Directory -Force $dist | Out-Null
$zip = Join-Path $dist "$nombre-$Version-windows.zip"
if (Test-Path $zip) { Remove-Item $zip }
Compress-Archive -Path (Join-Path $salida $nombre) -DestinationPath $zip

$mb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host ""
Write-Host "Listo: $zip ($mb MB)"
Write-Host "Para probar sin comprimir: $salida\$nombre\$nombre.exe"
