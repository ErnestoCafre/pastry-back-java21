<#
.SYNOPSIS
    Mantiene despierto el web service Free de Render (equivalente Windows de keep-alive.sh).

.DESCRIPTION
    Render suspende un web service Free tras 15 minutos sin trafico entrante y tarda
    ~1 minuto en volver. Este script envia un request antes de que se cumpla ese plazo.

    El plan Free da 750 instance-hours por mes y por workspace. Un mes de 31 dias son
    744 horas, asi que mantenerlo 24/7 deja solo 6 horas de margen y cualquier otro
    servicio Free del workspace agota el cupo (se suspende todo hasta el mes siguiente).
    Por eso el ping se limita a una ventana horaria configurable.

.EXAMPLE
    .\keep-alive.ps1
    Un ping, respetando la ventana horaria.

.EXAMPLE
    .\keep-alive.ps1 -Loop
    Pinguea cada 10 minutos hasta Ctrl+C. Util mientras estas mostrando la demo.

.EXAMPLE
    .\keep-alive.ps1 -Force
    Un ping ignorando la ventana horaria (para despertarlo antes de una demo).
#>
[CmdletBinding()]
param(
    [string]$Url        = $(if ($env:KEEPALIVE_URL) { $env:KEEPALIVE_URL } else { 'https://malva-pastry-backend.onrender.com/login' }),
    [string]$TimeZone   = 'Argentina Standard Time',
    [int]$ActiveStart   = 9,    # hora local de inicio de la ventana
    [int]$ActiveEnd     = 1,    # hora local de fin (exclusiva); menor que Start = cruza medianoche
    [int]$IntervalSec   = 600,  # 10 min, por debajo del corte de 15 min
    [int]$TimeoutSec    = 90,   # un cold start de Spring Boot tarda ~60s
    [int]$MaxAttempts   = 3,
    [switch]$Force,
    [switch]$Loop
)

function Write-Log([string]$Message) {
    Write-Host ("{0}  {1}" -f (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ'), $Message)
}

function Test-ActiveWindow {
    $tz    = [System.TimeZoneInfo]::FindSystemTimeZoneById($TimeZone)
    $hour  = [System.TimeZoneInfo]::ConvertTimeFromUtc((Get-Date).ToUniversalTime(), $tz).Hour
    if ($ActiveStart -le $ActiveEnd) {
        return ($hour -ge $ActiveStart -and $hour -lt $ActiveEnd)
    }
    # Ventana que cruza medianoche, p.ej. 09:00 -> 01:00
    return ($hour -ge $ActiveStart -or $hour -lt $ActiveEnd)
}

function Invoke-Ping {
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $response = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec $TimeoutSec `
                -UseBasicParsing -Headers @{ 'User-Agent' = 'malva-keepalive/1.0' }
            $sw.Stop()
            Write-Log ("OK  HTTP {0} en {1:N1}s  ({2})" -f $response.StatusCode, $sw.Elapsed.TotalSeconds, $Url)
            return $true
        } catch {
            $sw.Stop()
            # Un 3xx o incluso un 4xx significa que el servicio esta despierto: eso alcanza.
            $code = $null
            if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
            if ($code -and $code -lt 500) {
                Write-Log ("OK  HTTP {0} en {1:N1}s  (servicio despierto)" -f $code, $sw.Elapsed.TotalSeconds)
                return $true
            }
            Write-Log ("FALLO intento {0}/{1}  {2}" -f $attempt, $MaxAttempts, $_.Exception.Message)
            if ($attempt -lt $MaxAttempts) { Start-Sleep -Seconds 15 }
        }
    }
    return $false
}

function Invoke-Cycle {
    if (-not $Force -and -not (Test-ActiveWindow)) {
        Write-Log ("Fuera de ventana activa ({0}:00-{1}:00 {2}). Sin ping." -f $ActiveStart, $ActiveEnd, $TimeZone)
        return $true
    }
    return Invoke-Ping
}

if ($Loop) {
    Write-Log ("Modo loop: ping cada {0}s. Ctrl+C para salir." -f $IntervalSec)
    while ($true) {
        if (-not (Invoke-Cycle)) { Write-Log 'El servicio no respondio; se reintenta en el proximo ciclo.' }
        Start-Sleep -Seconds $IntervalSec
    }
} else {
    if (Invoke-Cycle) { exit 0 } else { exit 1 }
}
