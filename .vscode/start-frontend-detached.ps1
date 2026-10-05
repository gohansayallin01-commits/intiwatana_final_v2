# Arranque DESACOPLADO del frontend para INTIWATANA.
#
# Se invoca como preLaunchTask del F5, por lo que DEBE TERMINAR rapido:
# lanza Vite en un proceso independiente y luego finaliza. De este modo
# ningun servidor de larga duracion bloquea al Java Debugger.
#
# - Si el puerto 5173 ya responde con la app de Vite, NO lanza una segunda
#   instancia (evita el aviso "Port 5173 is in use, trying another on...").
# - Lanza el lanzador del navegador tambien desacoplado; ese script espera
#   a que esten disponibles 5173 y 8080 antes de abrir la ventana.
#
# Uso de "localhost" (no 127.0.0.1): Vite escucha en el loopback IPv6 ::1.

param(
    [int]$VitePort    = 5173,
    [int]$ViteTimeout = 90
)

$ErrorActionPreference = 'Stop'

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$frontendDir   = Join-Path $workspaceRoot 'frontend\Web-Intiwatana'
$browserScript = Join-Path $PSScriptRoot 'open-browser-after-backend.ps1'
$logDir        = Join-Path $PSScriptRoot 'logs'
$viteLog       = Join-Path $logDir 'vite.log'
$viteErrLog    = Join-Path $logDir 'vite-error.log'

function Test-ViteReady {
    <# Devuelve $true si el puerto ya sirve la aplicacion de Vite. #>
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:$VitePort/" -UseBasicParsing -TimeoutSec 2
        return ($r.StatusCode -eq 200 -and $r.Content -match 'vite')
    } catch { return $false }
}

if (-not (Test-Path -LiteralPath $frontendDir)) {
    Write-Host "INTIWATANA ERROR: no existe el directorio frontend '$frontendDir'."
    exit 1
}

if (-not (Test-Path -LiteralPath $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

# ── 1. Vite ────────────────────────────────────────────────────────────────
if (Test-ViteReady) {
    Write-Host "INTIWATANA: Vite ya activo en http://localhost:$VitePort/ - no se lanza otra instancia."
} else {
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
    if (-not $npm) { $npm = (Get-Command npm -ErrorAction SilentlyContinue).Source }
    if (-not $npm) {
        Write-Host "INTIWATANA ERROR: no se encontro 'npm' en el PATH."
        exit 1
    }

    Write-Host "INTIWATANA: iniciando Vite desacoplado desde $frontendDir"

    # Start-Process sin -Wait: el proceso hijo queda independiente y esta tarea
    # termina de inmediato, liberando el F5. stdout/stderr van a archivo para
    # no heredar el handle del terminal (que lo mantendria abierto).
    Start-Process -FilePath $npm `
                  -ArgumentList 'run', 'dev' `
                  -WorkingDirectory $frontendDir `
                  -WindowStyle Hidden `
                  -RedirectStandardOutput $viteLog `
                  -RedirectStandardError  $viteErrLog | Out-Null

    # Confirmacion de arranque (no bloquea el F5 mas alla de ViteTimeout)
    $ready = $false
    for ($i = 1; $i -le $ViteTimeout; $i++) {
        if (Test-ViteReady) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }

    if ($ready) {
        Write-Host "INTIWATANA: Vite respondio en http://localhost:$VitePort/ (log: $viteLog)"
    } else {
        Write-Host "INTIWATANA: Vite no respondio en $ViteTimeout s. Revisa $viteErrLog"
    }
}

# ── 2. Navegador (proceso totalmente desacoplado) ─────────────────────────
if (Test-Path -LiteralPath $browserScript) {
    Start-Process -FilePath 'powershell.exe' `
                  -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', `
                                '-WindowStyle', 'Hidden', '-File', $browserScript `
                  -WindowStyle Hidden | Out-Null
    Write-Host "INTIWATANA: lanzador del navegador desacoplado (espera 5173 y 8080)."
} else {
    Write-Host "INTIWATANA: no se encontro $browserScript; el navegador no se abrira."
}

Write-Host "INTIWATANA: preparacion terminada. El Java Debugger puede arrancar Spring Boot."
exit 0
