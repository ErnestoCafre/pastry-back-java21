#!/usr/bin/env bash
#
# Mantiene despierto el web service Free de Render enviando un request HTTP
# antes de que se cumplan los 15 minutos de inactividad que disparan el spin-down.
#
# Render Free tiene un cupo de 750 instance-hours por mes POR WORKSPACE. Un mes de
# 31 dias son 744 horas: mantener el servicio 24/7 deja solo 6 horas de margen y
# cualquier segundo servicio Free del workspace agota el cupo y suspende TODO hasta
# el mes siguiente. Por eso el ping se limita a una ventana horaria (ver ACTIVE_*).
#
# Uso:
#   ./keep-alive.sh              # un ping, respeta la ventana horaria
#   ./keep-alive.sh --force      # un ping, ignora la ventana horaria
#   ./keep-alive.sh --loop       # pinguea cada PING_INTERVAL hasta Ctrl+C
#
# Codigos de salida: 0 = ok o fuera de ventana, 1 = el servicio no respondio.

set -uo pipefail

TARGET_URL="${KEEPALIVE_URL:-https://malva-pastry-backend.onrender.com/login}"
ACTIVE_TZ="${KEEPALIVE_TZ:-America/Argentina/Buenos_Aires}"
ACTIVE_START="${KEEPALIVE_START:-9}"   # hora local de inicio de la ventana
ACTIVE_END="${KEEPALIVE_END:-1}"       # hora local de fin (exclusiva); < START = cruza medianoche
PING_INTERVAL="${KEEPALIVE_INTERVAL:-600}"  # segundos entre pings en modo --loop (10 min < 15 min)
MAX_ATTEMPTS="${KEEPALIVE_ATTEMPTS:-3}"
TIMEOUT="${KEEPALIVE_TIMEOUT:-90}"     # un cold start de Spring Boot en Free tarda ~60s

log() { printf '%s  %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1"; }

# Devuelve 0 si la hora local actual cae dentro de la ventana activa.
in_active_window() {
  local hour
  # %H en vez de %-H: el flag '-' es de GNU date y BSD/macOS no lo soporta.
  # 10# fuerza base decimal, si no "08" y "09" se leerian como octal invalido.
  hour=$((10#$(TZ="$ACTIVE_TZ" date '+%H')))
  if [ "$ACTIVE_START" -le "$ACTIVE_END" ]; then
    [ "$hour" -ge "$ACTIVE_START" ] && [ "$hour" -lt "$ACTIVE_END" ]
  else
    # Ventana que cruza medianoche, p.ej. 09:00 -> 01:00
    [ "$hour" -ge "$ACTIVE_START" ] || [ "$hour" -lt "$ACTIVE_END" ]
  fi
}

# Un ping con reintentos. El primero puede tardar ~1 min si el servicio estaba dormido.
ping_once() {
  local attempt=1 code elapsed response
  while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
    response=$(curl -s -o /dev/null --max-time "$TIMEOUT" \
      -H 'User-Agent: malva-keepalive/1.0' \
      -w '%{http_code} %{time_total}' "$TARGET_URL" 2>/dev/null)
    code="${response%% *}"
    elapsed="${response##* }"

    # 2xx y 3xx cuentan como servicio despierto (/login puede redirigir).
    if [ -n "$code" ] && [ "$code" -ge 200 ] 2>/dev/null && [ "$code" -lt 400 ]; then
      log "OK  HTTP $code en ${elapsed}s  ($TARGET_URL)"
      return 0
    fi

    log "FALLO intento $attempt/$MAX_ATTEMPTS  HTTP ${code:-sin-respuesta} tras ${elapsed:-?}s"
    attempt=$((attempt + 1))
    [ "$attempt" -le "$MAX_ATTEMPTS" ] && sleep 15
  done
  return 1
}

FORCE=false
LOOP=false
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=true ;;
    --loop)  LOOP=true ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) log "Argumento desconocido: $arg"; exit 2 ;;
  esac
done

run() {
  if [ "$FORCE" = false ] && ! in_active_window; then
    log "Fuera de ventana activa (${ACTIVE_START}:00-${ACTIVE_END}:00 $ACTIVE_TZ). Sin ping."
    return 0
  fi
  ping_once
}

if [ "$LOOP" = true ]; then
  log "Modo loop: ping cada ${PING_INTERVAL}s. Ctrl+C para salir."
  while true; do
    run || log "El servicio no respondio; se reintenta en el proximo ciclo."
    sleep "$PING_INTERVAL"
  done
else
  run
fi
