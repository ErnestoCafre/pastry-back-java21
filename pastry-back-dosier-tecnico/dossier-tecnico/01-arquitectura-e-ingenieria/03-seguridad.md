# 3. Seguridad

El sistema tiene dos superficies con perfiles de riesgo opuestos: un **panel
interno** con escritura completa sobre el negocio, y una **API pública sin
autenticación** que cualquiera puede consultar. La estrategia es separarlas de
raíz y aplicarle a cada una lo que corresponde, en vez de una política promedio
que le quede mal a las dos.

Este documento describe también, al final, **lo que el sistema no tiene**. Para
un backend desplegado en internet, un inventario honesto de límites es parte de
la postura de seguridad.

## Dos cadenas, un solo deploy

**Dónde:** `config/SecurityConfig.java`

Spring Security permite varios `SecurityFilterChain` con `@Order`; el primero
cuyo `securityMatcher` coincida atiende el request. El sistema usa exactamente
dos:

| | Cadena API — `@Order(1)` | Cadena panel — `@Order(2)` |
|---|---|---|
| **Matcher** | `/api/**` | todo lo demás (sin matcher) |
| **Sesión** | `STATELESS` — no crea ni usa `HttpSession` | Sesión + form login en `/login` |
| **CSRF** | Deshabilitado | **Habilitado** |
| **CORS** | Habilitado, orígenes explícitos | No aplica |
| **Autorización** | Solo `GET` a 4 prefijos → `permitAll`; **`anyRequest().denyAll()`** | `/users/**` → `ADMIN`; resto → `ADMIN` o `EMPLOYEE` |
| **Errores** | JSON (`401` / `403` con cuerpo) | Redirect a `/login` / página de acceso denegado |

Las dos configuraciones son **mutuamente incompatibles** —una es sin estado y
sin CSRF, la otra con ambos— y por eso están separadas en vez de fusionadas con
condicionales. Deshabilitar CSRF globalmente para que la API funcione sería el
atajo habitual: dejaría el panel expuesto a *cross-site request forgery* en cada
formulario de escritura.

Un detalle deliberado: la cadena de la API termina en **`denyAll()`**, no en
`authenticated()`. La diferencia importa. Con `authenticated()`, agregar mañana
un `POST /api/v1/products` lo dejaría accesible a cualquier sesión válida del
panel por omisión. Con `denyAll()`, **todo lo que no está explícitamente
permitido está prohibido**: el endpoint nuevo devuelve 403 hasta que alguien
decida conscientemente su regla. El default es cerrado.

## Autenticación

**Dónde:** `service/UserService.java` (implementa `UserDetailsService`),
`domain/auth/User.java`, `SecurityConfig.passwordEncoder()`

- **Contraseñas con BCrypt** (`BCryptPasswordEncoder`, fuerza 10 por defecto).
  El hash se genera en el servicio al crear o cambiar la contraseña; el texto
  plano no se guarda ni se registra en logs en ningún punto.
- **Login por formulario** en `/login`, con `usernameParameter("email")` porque
  la identidad del usuario es su email (columna `UNIQUE`), no un nombre de
  usuario aparte.
- **`User` implementa `UserDetails`**, así que la entidad de dominio es
  directamente el principal de Spring Security. Eso permite
  `@AuthenticationPrincipal User user` en los controladores (15 usos), y de ahí
  sale el actor de cada operación auditada: quién registró una venta
  (`sales.registered_by_id`), quién creó un producto (`products.user_id`), quién
  mandó algo a la papelera (`deleted_by_id`).
- **Logout explícito**: invalida la sesión y borra la cookie `JSESSIONID`.

Que el actor salga del principal y no de un campo del formulario es lo que hace
confiable la auditoría: no hay forma de registrar una venta a nombre de otro
usuario manipulando el POST.

## Autorización: dos roles, dos niveles de enforcement

El modelo es deliberadamente chico — **`ADMIN` y `EMPLOYEE`** — y llegó a serlo
por reducción: el proyecto **eliminó** un tercer rol `USER` y un flag
`system_admin` que no otorgaban nada distinto
(ver [caso 5](../03-producto/09-casos-de-estudio.md)). Menos autoridades es
menos superficie de confusión.

El enforcement se aplica en dos niveles complementarios:

```mermaid
flowchart TD
    Req["Request al panel"] --> Auth{"¿Sesión autenticada?"}
    Auth -->|No| Login["302 → /login"]
    Auth -->|Sí| Csrf{"¿Token CSRF válido? (escrituras)"}
    Csrf -->|No| Deny403["403"]
    Csrf -->|Sí| Path{"Nivel 1 — la ruta: ¿/users/** ?"}

    Path -->|Sí| NeedAdmin{"hasRole('ADMIN')"}
    Path -->|No| NeedAny{"hasAnyRole('ADMIN','EMPLOYEE')"}

    NeedAdmin -->|No| Deny403
    NeedAny -->|No| Deny403
    NeedAdmin -->|Sí| Method
    NeedAny -->|Sí| Method{"Nivel 2 — el método: ¿@PreAuthorize?"}

    Method -->|Sin anotación| Run["Ejecutar handler"]
    Method -->|hasRole('ADMIN') y lo cumple| Run
    Method -->|hasRole('ADMIN') y no lo cumple| Deny403
```

- **Nivel 1 — por ruta** (`authorizeHttpRequests`): la gestión de usuarios
  completa queda detrás de `ADMIN`; el resto del panel exige `ADMIN` o
  `EMPLOYEE`. Es un cerco grueso, imposible de saltear por olvido en un handler.
- **Nivel 2 — por método** (`@EnableMethodSecurity` + `@PreAuthorize`, 16
  anotaciones): dentro de una sección accesible a los dos roles, **la papelera y
  todo lo que se hace sobre ella** —verla, restaurar, eliminar
  permanentemente— se reserva a `ADMIN`. Un empleado ve el catálogo, crea, edita
  y manda a la papelera; solo un administrador decide qué vuelve y qué se
  destruye para siempre.

Los dos niveles responden preguntas distintas —"¿esta *sección* es tuya?" contra
"¿esta *acción* es tuya?"— y por eso conviven. Poner todo en el matcher de rutas
obligaría a partir las URLs por rol; poner todo en `@PreAuthorize` dejaría el
acceso a la sección dependiendo de que ningún handler se olvide de la anotación.

La UI acompaña el mismo criterio con `sec:authorize` de
`thymeleaf-extras-springsecurity`: un empleado no ve botones que le van a dar
403. Eso es **ergonomía, no control** — el control está en el servidor, y las
plantillas solo evitan ofrecer lo que va a fallar.

## La API pública: superficie mínima por construcción

La API no tiene autenticación **a propósito** —expone el catálogo de una
vitrina, información que ya es pública— y por eso su seguridad es una cuestión
de **superficie**, no de credenciales.

Cuatro capas la mantienen chica:

1. **Solo `GET`.** Los matchers son `HttpMethod.GET` explícito sobre cuatro
   prefijos. Un `POST` o `DELETE` a la misma URL no cae en una regla más
   permisiva: cae en `denyAll()`.
2. **Solo lo publicado.** Los métodos que sirve la API filtran en la consulta,
   no después: `findByVisibleTrueAndDeletedAtIsNull`,
   `findByIdAndVisibleTrueAndDeletedAtIsNull`. Un producto oculto o en la
   papelera es indistinguible de uno inexistente para un cliente de la API —
   devuelve 404, no 403, así que no revela ni siquiera su existencia.
3. **Solo DTOs.** Los controladores de `controller/api/` devuelven records de
   `dto/response/api/`, nunca entidades. Un campo interno nuevo en `Product`
   —costo, creador, fechas de borrado— no se filtra al público por accidente al
   serializarse.
4. **Sin datos internos en los errores.** `ApiExceptionHandler` responde
   `"Error interno del servidor"` ante cualquier excepción no prevista; el
   stack trace y el mensaje original quedan del lado del servidor.

**CORS** (`config/CorsConfig.java`) se configura solo para `/api/**`, con la
lista de orígenes tomada de `app.cors.allowed-origins` (variable de entorno
`CORS_ALLOWED_ORIGINS`, definida en `render.yaml` para producción). Es una lista
explícita, no un comodín.

## Configuración y secretos

- **Nada sensible en el repositorio.** `application.properties` no contiene
  credenciales literales: todas las claves son `${VAR:default}`, con defaults de
  desarrollo local. La contraseña y el host de la base en producción llegan
  desde el entorno.
- **Inyección desde el proveedor.** En `render.yaml`, las credenciales de la
  base se declaran con `fromDatabase` — Render las inyecta desde la instancia
  administrada; no hay un valor escrito en ningún archivo del repo.
- **`.env.example` versionado, `.env` ignorado**: el contrato de configuración
  es público, los valores no.
- **Contenedor sin privilegios**: el `Dockerfile` crea un usuario `spring` y
  corre como `spring:spring`, no como root.
- **Swagger apagado por defecto en producción**
  (`springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}`): la documentación
  interactiva es útil en desarrollo y superficie innecesaria en producción.

El contrato de configuración se mantiene sincronizado con el código: una
auditoría posterior encontró que `.env.example` seguía documentando variables de
JWT, OAuth y carga de archivos que **ya no existen**, sobrevivientes del barrido
de andamiaje muerto (ver [caso 2](../03-producto/09-casos-de-estudio.md)) porque
ese commit limpió `render.yaml` pero no el ejemplo. Se eliminaron, y en el mismo
paso se agregaron las que el código sí lee y el ejemplo omitía —`PUBLIC_BASE_URL`
y las de conexión del perfil `prod`—. Un archivo de ejemplo que enumera secretos
imaginarios y calla los reales es peor que no tenerlo: enseña un modelo de
amenaza equivocado.

## Protecciones que aporta el framework

Spring Security 7 aplica por defecto, y el proyecto no las desactiva, una base
de cabeceras en las respuestas del panel: `X-Content-Type-Options: nosniff`,
`X-Frame-Options: DENY` (anti-clickjacking), `Cache-Control` restrictivo en
páginas autenticadas y HSTS sobre HTTPS. La protección CSRF del panel es también
la implementación del framework, no una propia.

Es una decisión: **no reimplementar lo que el framework ya hace bien**. La
contrapartida es saber exactamente dónde termina ese default, que es lo que
enumera la sección siguiente.

## Lo que el sistema no tiene (y por qué)

| Ausencia | Estado | Razonamiento |
|---|---|---|
| **MFA / segundo factor** | No implementado | Dos usuarios internos y un panel sin datos personales de terceros más allá de nombre y teléfono del cliente. Sería la primera adición si el sistema sumara operadores. |
| **Rate limiting** | No implementado | La API es de solo lectura sobre datos públicos; el riesgo es de costo, no de datos. Hoy lo acota el propio plan de hosting. |
| **CSP explícita** | No configurada | El panel carga Tailwind y Flowbite desde CDN, lo que obliga a una política con orígenes externos; hacerla bien requiere primero servir esos assets localmente. Es la tarea previa, no un olvido aislado. |
| **Bloqueo por intentos fallidos de login** | No implementado | Complementa a BCrypt contra fuerza bruta. Pendiente. |
| **Auditoría de dependencias en CI** | No implementado | No hay pipeline de CI (ver [4. Calidad](04-calidad-e-ingenieria.md)); es parte del mismo faltante. |
| **Página de acceso denegado propia** | **Defecto conocido** | `accessDeniedPage("/error/403")` apunta a una ruta sin controlador ni plantilla: un 403 en el panel termina en la página de error genérica en vez de una propia. No es un agujero de autorización —el acceso **sí** se deniega— pero la experiencia es pobre y está pendiente de arreglo. |

La última fila es el tipo de hallazgo que un dossier honesto incluye: el control
funciona, la presentación del control no. Declararlo vale más que omitirlo.

## Auditoría de dominio

El sistema registra **quién** hizo lo que importa, en la propia fila del dato:

- `products.user_id` — quién creó el producto.
- `sales.registered_by_id` — quién registró la venta.
- `deleted_by_id` en las cinco entidades con soft-delete — quién la mandó a la
  papelera (y la vista de papelera lo muestra, que es la razón por la que ese
  `deletedBy` está en el `@EntityGraph`).
- `inserted_at` / `updated_at` en todas las tablas, vía `TimestampedEntity`.

No es un log de auditoría separado —no hay tabla de eventos ni traza de campos
modificados— sino atribución a nivel de entidad. Para el tamaño del equipo y del
negocio es la relación costo/beneficio correcta; un `@EntityListener` con
historial completo es la evolución natural si el sistema suma operadores.
