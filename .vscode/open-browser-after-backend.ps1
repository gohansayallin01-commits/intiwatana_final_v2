# Lanzador del navegador para INTIWATANA.
#
# Proceso DESACOPLADO: se lanza con Start-Process desde
# start-frontend-detached.ps1 y no depende del ciclo de vida de VS Code,
# del terminal ni del Java Debugger.
#
# Espera a que respondan AMBOS servicios antes de abrir el navegador:
#   - TCP/HTTP localhost:5173  (frontend Vite)
#   - TCP/HTTP localhost:8080  (Spring Boot)
#
# Se usa "localhost" y no 127.0.0.1 porque Vite escucha en el loopback
# IPv6 ::1, donde 127.0.0.1 no conecta.

$ErrorActionPreference = 'SilentlyContinue'

$FRONTEND     = 'http://localhost:5173/'
$BACKEND      = 'http://localhost:8080/actuator/health'
$MAX_ITER     = 240     # 4 minutos como máximo
$SLEEP        = 1       # segundos entre intentos

function Test-Port {
    <# $true si el puerto TCP acepta conexiones en localhost (IPv4 o IPv6). #>
    param([int]$Port)
    try {
        $c = New-Object System.Net.Sockets.TcpClient('localhost', $Port)
        $c.Close()
        return $true
    } catch { return $false }
}

function Test-HttpOk {
    <# $true si la URL responde HTTP 200. #>
    param([string]$Url)
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -eq 200)
    } catch { return $false }
}

$front = $false
$back  = $false

for ($i = 1; $i -le $MAX_ITER; $i++) {

    if (-not $front) { $front = (Test-Port 5173) -and (Test-HttpOk $FRONTEND) }
    if ($front -and -not $back) { $back = (Test-Port 8080) -and (Test-HttpOk $BACKEND) }

    if ($front -and $back) {
        Write-Host "INTIWATANA: 5173 y 8080 disponibles. Abriendo $FRONTEND"
        Start-Process $FRONTEND
        exit 0
    }

    if ($i % 15 -eq 0) {
        Write-Host "INTIWATANA: esperando servicios... 5173=$front 8080=$back (intento $i/$MAX_ITER)"
    }

    Start-Sleep -Seconds $SLEEP
}

# Tiempo agotado: se abre igualmente el frontend si existe, para que el
# usuario vea el estado real en vez de una pantalla en blanco.
if ($front) {
    Write-Host "INTIWATANA: 8080 no respondio a tiempo; se abre el frontend de todos modos."
    Start-Process $FRONTEND
} else {
    Write-Host "INTIWATANA: no se detecto el frontend en 5173; no se abre el navegador."
}
