<#
  Genera la version portable de KUMOC para Windows: dist\KUMOC-<version>-windows.zip

  Requisitos: JDK 25 (con jpackage) y Maven en el PATH. JAVA_HOME debe apuntar al JDK.
  El ZIP incluye su propio Java: quien lo descarga no necesita instalar nada.

  Uso (desde la raiz del proyecto):
      powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1
      powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1 -Version 1.1.0

  Con -Instalador genera ademas dist\KUMOC-<version>-setup.exe (instala en C:\Program Files\KUMOC,
  con acceso directo en el escritorio y el menu Inicio). Requiere WiX Toolset 3.x:
      winget install WiXToolset.WiXToolset      (en una terminal de administrador)
      powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1 -Instalador
#>
param(
    [string]$Version = "1.0.0",
    [switch]$Instalador
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

if ($Instalador) {
    # Requiere WiX Toolset 3.x (candle.exe y light.exe en el PATH): winget install WiXToolset.WiXToolset
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
        throw "Para generar el instalador hace falta WiX Toolset 3.x (candle.exe en el PATH). Instalalo con: winget install WiXToolset.WiXToolset (como administrador) y abri una terminal nueva."
    }

    Write-Host "==> Generando el instalador..."
    $instaladores = Join-Path $trabajo "instalador"
    New-Item -ItemType Directory -Force $instaladores | Out-Null

    # El UUID fijo hace que Windows trate las versiones nuevas como actualizacion del mismo programa.
    & $jpackage `
        --type exe `
        --app-image (Join-Path $salida $nombre) `
        --name $nombre `
        --app-version $Version `
        --vendor "KUMOC" `
        --description "KUMOC - Control de stock, presupuestos y ordenes de trabajo" `
        --win-shortcut `
        --win-menu `
        --win-menu-group $nombre `
        --win-dir-chooser `
        --win-upgrade-uuid "54aec0db-f1e8-41dc-b1f7-4382179eb667" `
        --dest $instaladores
    if ($LASTEXITCODE -ne 0) { throw "Fallo jpackage al generar el instalador." }

    $setup = Join-Path $dist "$nombre-$Version-setup.exe"
    if (Test-Path $setup) { Remove-Item $setup }
    Move-Item (Join-Path $instaladores "$nombre-$Version.exe") $setup
    $mbSetup = [math]::Round((Get-Item $setup).Length / 1MB, 1)
    Write-Host "Listo: $setup ($mbSetup MB)"
}

Write-Host "Para probar sin comprimir: $salida\$nombre\$nombre.exe"
