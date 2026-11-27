# Keep-alive para Render Free

## El problema

Render suspende un web service del plan Free tras **15 minutos sin trafico entrante**
(antes eran 30; cambio en septiembre de 2025). El siguiente request lo despierta, pero
tarda **~1 minuto** en volver: es el "se muestra pagina de carga" que menciona el README
principal. Para una demo de portfolio, ese minuto en blanco es el primer contacto del
visitante con el proyecto.

## Las restricciones que condicionan la solucion

Verificadas contra [render.com/docs/free](https://render.com/docs/free) el 27/11/2026.

| Limite del plan Free | Valor | Por que importa aca |
|---|---|---|
| Spin-down por inactividad | 15 min | El ping debe ser mas frecuente que eso (usamos 10 min) |
| Instance-hours | **750/mes por workspace** | 24/7 = **744 h** en un mes de 31 dias. Solo 6 h de margen, y el cupo es **compartido con todos los servicios Free del workspace**. Agotarlo suspende todo hasta el 1ro del mes siguiente |
| Postgres Free | **expira 30 dias despues de crearse**, con 14 dias de gracia para pasarlo a pago; despues Render borra la base | El backend no arranca sin base. Ningun keep-alive salva esto |
| Trafico anomalo | Render puede suspender un servicio Free que genere "un volumen de trafico inusualmente alto" | Un ping cada 10 min es volumen trivial; no hace falta ir mas agresivo |

**La consecuencia de diseno:** pinguear 24/7 es tecnicamente posible pero deja el cupo al
borde. Por eso el ping se limita a una **ventana horaria** (09:00-01:00 hora argentina,
~496 h/mes) en vez de correr todo el dia. El servicio duerme de madrugada, que es cuando
nadie mira la demo.

## Por que se pinguea `/login`

`AuthController.login()` solo renderiza una plantilla Thymeleaf: **no toca la base de datos**.
Es el request mas barato que despierta el proceso, y ya es el `healthCheckPath` del
`render.yaml`. Pinguear `/api/v1/products` en cambio despertaria el pool de conexiones y
haria queries innecesarias cada 10 minutos.

## Los tres componentes

### 1. Monitor externo (cron-job.org) — el keep-alive real

Es lo unico que corre solo y a horario. Vive fuera del repo, asi que hay que crearlo a mano
en [cron-job.org](https://cron-job.org) (gratis, intervalo minimo de 1 minuto):

| Campo | Valor |
|---|---|
| URL | `https://malva-pastry-backend.onrender.com/login` |
| Schedule | Custom: minutos `0,10,20,30,40,50`; horas `9-23` y `0` |
| Time zone | `America/Argentina/Buenos_Aires` |
| Notificaciones | Email al fallar y al recuperarse |

Las horas `9-23` y `0` cubren 09:00-00:59, la misma ventana que `KEEPALIVE_START=9` /
`KEEPALIVE_END=1` de los scripts. **No lo dejes en "cada 10 minutos" a secas**: eso
pinguea 24/7 y consume las 744 h.

cron-job.org corta cada request a los **30 segundos**. Un cold start tarda ~60s, asi que el
primer ping del dia (09:00) puede figurar como fallido aunque haya despertado el servicio.
El de las 09:10 ya lo encuentra arriba. Un fallo aislado a las 09:00 es esperable; varios
seguidos significan que el servicio no levanta.

### 2. `.github/workflows/keep-alive.yml` — despertar manual

Solo `workflow_dispatch`: Actions → *Wake Render (manual)* → *Run workflow* antes de una
demo. Usa `keep-alive.sh --force`, que espera hasta 90s por intento, a diferencia de
los 30s del monitor.

Antes este workflow corria por cron cada 10 min. Se saco porque no funcionaba. Medido
sobre los 934 runs del 17/07/2026 al 10/10/2026:

- **GitHub demora los schedules bajo carga**, sobre todo al comienzo de cada hora (lo dice
  su propia documentacion). Mediana de 63 min entre runs y p90 de 6 h, contra los 10 min
  configurados. Solo 10 de 933 intervalos quedaron por debajo de los 15 min: casi cada
  ping encontraba el servicio dormido.
- **En un repo publico, GitHub desactiva los schedules tras 60 dias sin actividad.** Paso el
  10/10/2026, a 60 dias del ultimo commit en `main`, y nada aviso.

### 3. `keep-alive.sh` / `keep-alive.ps1` — uso local o en cualquier host

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

Los dos scripts cuentan 2xx y 3xx como servicio despierto y cualquier otra cosa como
fallo. Un 4xx en `/login` significa que la ruta se rompio, no que el ping sirvio.

Todo es configurable por variables de entorno en bash (`KEEPALIVE_URL`, `KEEPALIVE_START`,
`KEEPALIVE_END`, `KEEPALIVE_TZ`, `KEEPALIVE_INTERVAL`) y por parametros en PowerShell
(solo `KEEPALIVE_URL` se lee tambien del entorno).

## Lo que un keep-alive NO resuelve

Si el servicio no levanta, ningun ping lo arregla. Entre el 03/09/2026 y el 18/09/2026
fallaron todos los runs del cron: el servicio estuvo caido dos semanas. Ese patron (requests
que cuelgan minutos y terminan en `HTTP 503`, muy lejos del cold start de ~60s) no es
spin-down. El sospechoso principal es la **expiracion de los 30 dias del Postgres Free**: sin
base, Flyway falla, el health check de `/login` nunca pasa y Render devuelve 503. Se confirma
en el dashboard de Render, y hay 14 dias para actuar antes de que borre la base.
