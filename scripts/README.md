# Keep-alive para Render Free

## El problema

Render suspende un web service del plan Free tras **15 minutos sin trafico entrante**
(antes eran 30; cambio en septiembre de 2025). El siguiente request lo despierta, pero
tarda **~1 minuto** en volver: es el "se muestra pagina de carga" que menciona el README
principal. Para una demo de portfolio, ese minuto en blanco es el primer contacto del
visitante con el proyecto.

## Las restricciones que condicionan la solucion

| Limite del plan Free | Valor | Por que importa aca |
|---|---|---|
| Spin-down por inactividad | 15 min | El ping debe ser mas frecuente que eso (usamos 10 min) |
| Instance-hours | **750/mes por workspace** | 24/7 = **744 h** en un mes de 31 dias. Solo 6 h de margen, y el cupo es **compartido con todos los servicios Free del workspace**. Agotarlo suspende todo hasta el 1ro del mes siguiente |
| Postgres Free | **expira 30 dias despues de crearse** | El backend no arranca sin base. Ningun keep-alive salva esto |
| Trafico anomalo | Render puede suspender un servicio Free que genere "un volumen de trafico inusualmente alto" | Un ping cada 10 min es volumen trivial; no hace falta ir mas agresivo |

**La consecuencia de diseno:** pinguear 24/7 es tecnicamente posible pero deja el cupo al
borde. Por eso los scripts usan una **ventana horaria** (por defecto 09:00-01:00 hora
argentina, ~496 h/mes) en vez de correr todo el dia. El servicio duerme de madrugada,
que es cuando nadie mira la demo.

## Por que se pinguea `/login`

`AuthController.login()` solo renderiza una plantilla Thymeleaf: **no toca la base de datos**.
Es el request mas barato que despierta el proceso, y ya es el `healthCheckPath` del
`render.yaml`. Pinguear `/api/v1/products` en cambio despertaria el pool de conexiones y
haria queries innecesarias cada 10 minutos.

## Los tres componentes

### 1. `.github/workflows/keep-alive.yml` — la solucion real (24/7, sin tu PC)

Cron cada 10 min dentro de la ventana. Ya esta listo; solo hay que commitearlo y pushearlo.

Dos advertencias sobre el cron de GitHub Actions:
- **No es puntual.** Bajo carga, GitHub retrasa los schedules varios minutos. Con intervalo
  de 10 min y corte de 15, un retraso ocasional puede dejar dormir el servicio. No es grave
  (solo significa un cold start suelto), pero si quieres puntualidad real usa un monitor
  externo como cron-job.org o UptimeRobot apuntando a la misma URL cada 5-10 min.
- **GitHub desactiva los workflows programados tras 60 dias sin commits en el repo.** Si el
  repo queda quieto, hay que reactivarlo a mano desde la pestaña Actions.

Consumo de runner: **GitHub factura cada job redondeando hacia arriba al minuto**, asi que
un ping de 30s cuenta como 1 minuto. Son ~96 runs/dia = ~96 min/dia ≈ **2976 min/mes**.

- **Repo publico:** gratis e ilimitado. Esta es la opcion correcta.
- **Repo privado:** 2976 min **supera los 2000 min/mes** del plan free de Actions. No uses
  este workflow: usa el monitor externo de la opcion 3.

La ventana del cron esta en **UTC** y la del script en **hora argentina**. Si cambias una,
cambia la otra: `09:00-01:00 ART == 12:00-04:00 UTC`.

### 2. `keep-alive.sh` / `keep-alive.ps1` — uso local o en cualquier host

```bash
./keep-alive.sh              # un ping, respeta la ventana
./keep-alive.sh --force      # un ping ya mismo (despertarlo antes de una demo)
./keep-alive.sh --loop       # ping cada 10 min hasta Ctrl+C
```

```powershell
.\keep-alive.ps1
.\keep-alive.ps1 -Force
.\keep-alive.ps1 -Loop       # util mientras estas mostrando el proyecto a alguien
```

Todo es configurable por variables de entorno (`KEEPALIVE_URL`, `KEEPALIVE_START`,
`KEEPALIVE_END`, `KEEPALIVE_TZ`, `KEEPALIVE_INTERVAL`) o por parametros en PowerShell.

### 3. Alternativa sin dependencias: monitor externo

cron-job.org (gratis, intervalo de 1 min, puntual) o UptimeRobot (5 min) apuntando a
`https://malva-pastry-backend.onrender.com/login`. Es la opcion mas confiable de las tres,
pero vive fuera del repo. Ojo: estos monitores pinguean 24/7 por defecto — hay que
configurarles la ventana horaria o te comes las 744 h.

## Lo que un keep-alive NO resuelve

Al momento de escribir esto (16/07/2026) el servicio desplegado **no responde**: los
requests cuelgan varios minutos y terminan en `HTTP 503`, muy lejos del cold start normal
de ~60s. Eso no es spin-down, es un servicio que no levanta. El sospechoso principal es la
**expiracion de los 30 dias del Postgres Free** (sin base, Flyway falla, el health check de
`/login` nunca pasa y Render devuelve 503). Hay que revisarlo en el dashboard de Render
antes de que el keep-alive tenga algo que mantener despierto.
