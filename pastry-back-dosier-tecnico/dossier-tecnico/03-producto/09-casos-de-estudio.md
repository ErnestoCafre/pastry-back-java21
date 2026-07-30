# 9. Casos de estudio de ingeniería

> Problemas concretos que surgieron construyendo el sistema, cómo se
> diagnosticaron y cómo se resolvieron. El hilo conductor es una regla del
> proyecto: **ante un bug, auditar toda la clase del problema y aplicar un fix
> sistémico, no un parche puntual**. Cada caso sigue el mismo arco: síntoma →
> causa raíz → fix sistémico → qué deja.
>
> Los primeros seis están reconstruidos desde el historial de commits del
> repositorio, donde cada uno quedó documentado con su porqué y su verificación;
> el séptimo salió de una auditoría de consistencia posterior.

## Caso 1 — El seed que destapó tres bugs invisibles

**Síntoma.** El seed de demo cargaba el catálogo público —categorías, productos,
tags, secciones— pero **no las recetas ni las ventas**, que son el núcleo
funcional del sistema. Con el sistema recién arrancado, el dashboard mostraba
ceros, el costeo no tenía nada que calcular y el detalle de venta no existía. El
proyecto se veía peor de lo que era.

**Lo que pasó al arreglarlo.** Sembrar 297 líneas de receta y 42 ventas hizo que
por primera vez se ejercitaran rutas de código que nunca habían corrido con
datos reales. Aparecieron **tres bugs**, todos latentes desde antes:

1. **La venta con receta no se podía guardar.** `quantityUsed` tiene escala 4 y
   `unitCost` escala 2, así que su producto tiene **escala 6** y violaba
   `@Digits(fraction = 2)`: cualquier venta de un producto con receta explotaba
   con `ConstraintViolationException` al persistir. El bug existía desde que se
   escribió la entidad; nunca se había registrado una venta de un producto que
   tuviera receta.
2. **El borrado permanente devolvía 500.** Eliminar de la papelera un producto,
   tag o sección con referencias era rechazado por la FK (`NO ACTION`) y llegaba
   a la capa web como `DataIntegrityViolationException`, es decir, como error
   500 sin explicación. Antes no había ventas ni asociaciones, así que nunca se
   había disparado.
3. **Las cinco papeleras devolvían HTML truncado.** Las plantillas muestran
   `deletedBy.fullName`, una relación `LAZY`, y con `open-in-view=false` la
   sesión de Hibernate ya está cerrada al renderizar: `LazyInitializationException`
   en medio del render, con la respuesta ya iniciada. El resultado era el peor
   posible: **HTTP 200 con la página cortada a la mitad**, sin error visible en
   ningún lado.

**Fix sistémico.** Ninguno se arregló solo donde falló:

- El redondeo a escala 2 se puso en el **constructor de `SaleIngredient`**, no
  en el servicio que lo llama, para que ninguna ruta futura pueda saltearlo. El
  comentario explica qué falla si se quita.
- La guarda de integridad se agregó a los **cinco servicios de catálogo** a la
  vez, no solo al que falló, y con el patrón ya existente en el proyecto:
  contar referencias y lanzar `IllegalStateException` con el número exacto.
- `deletedBy` se agregó al `@EntityGraph` de las **cinco consultas de papelera**,
  con un comentario en el repositorio que explica por qué es obligatorio.

De paso se corrigió una **mentira en la documentación del código**: varios
comentarios de entidad afirmaban `SET NULL on delete` cuando el esquema usa
`NO ACTION`. Una descripción incorrecta al lado del código es peor que ninguna,
porque se cree.

**Deja:** la constatación de que **el dato ausente es una forma de deuda
técnica**. Un sistema sin datos representativos no ejercita sus propias rutas, y
los bugs esperan al usuario en vez de al desarrollador. El seed pasó a incluir el
núcleo funcional, no solo lo que se ve.

## Caso 2 — El andamiaje muerto que costaba dinero

**Síntoma.** El repositorio contenía un contexto completo de "usuario público"
—`PublicUser`, `Favorite`, `ProductReview`— y un paquete `security/` con
generación de JWT, verificación de tokens de Google y un principal propio. La
documentación los presentaba como bounded contexts reales.

**Causa raíz.** Nada de eso estaba implementado: sin servicios, sin
controladores, sin endpoints, sin un solo camino de código que emitiera un JWT o
creara un `PublicUser`. Era andamiaje de una vitrina con login social, favoritos
y reseñas que nunca se construyó.

**Por qué no era inocuo.** El costo era concreto y medible:

- Un **SDK de AWS S3** sin usar, con su árbol transitivo completo, y un starter
  de OAuth2 client también sin usar.
- Una variable de entorno **`JWT_SECRET` obligatoria en producción** para una
  funcionalidad inexistente: sin ella la aplicación **no arrancaba**.
- Un `JwtAuthenticationFilter` registrado automáticamente que corría en **cada
  request de la API** y no podía autenticar a nadie.
- Y el costo menos visible: cualquier lector —humano o agente— que tratara de
  entender el sistema empezaba estudiando cuatro entidades y un modelo de
  seguridad que no existían.

**Fix sistémico.** Se eliminó la clase entera de una vez: 4 archivos de dominio,
4 de seguridad, 3 repositorios, 2 tests, cinco dependencias del `pom.xml`, los
bloques de propiedades `app.jwt.*` y `app.google.*` en los dos perfiles, las
variables muertas del `render.yaml`, el `TRUNCATE` de las tablas eliminadas en el
seed repetible, y la documentación que las describía.

Las tablas se eliminaron con una **migración hacia adelante** (`V3`), no editando
la `V1` que las creó. El commit razona la restricción:

> *V1 is an applied versioned migration and cannot be edited without breaking
> Flyway's validate-on-migrate; the forward migration is the safe path.*

**Deja:** dos reglas. **El código muerto no es neutral** — cuesta dependencias,
configuración obligatoria, superficie de ataque y comprensión. Y **el historial
de migraciones es inmutable**: se corrige agregando, nunca reescribiendo.
El esquema de desarrollo pasó de 15 a 12 tablas.

## Caso 3 — La colisión de versiones que habría roto el arranque

**Síntoma.** Dos ramas de trabajo agregaron cada una su migración numerada
`V3__`: una eliminaba las tablas del andamiaje muerto (caso 2) y otra el rol
`USER` con la columna `system_admin` (caso 5). Al converger, el repositorio tenía
**dos migraciones con la misma versión**.

**Por qué era grave.** Las migraciones corren al arranque de la aplicación con
`validate-on-migrate=true`. Con dos archivos en la versión 3, Flyway aborta con
*"more than one migration with version 3"* — es decir, **el servicio no habría
podido arrancar en producción**. Y no se detectaba localmente: el perfil de
desarrollo usa `ddl-auto=create` y ni siquiera consulta a Flyway, así que la
aplicación levantaba perfecto en la máquina de quien lo mergeaba.

**Fix.** Renumerar antes del merge, dejando la cadena `V1 → V2 → V3 (drop public
user) → V4 (remove system_admin + USER) → R__`, y registrar en el README la
migración que faltaba en la lista — un commit dedicado solo a eso.

**Lo que este caso deja pendiente, y se dice.** El chequeo fue **manual**: lo
atrapó una revisión, no una verificación automática. Un job de CI que aplique la
cadena completa sobre una base limpia lo detectaría siempre, y por eso encabeza
los [gaps declarados](../01-arquitectura-e-ingenieria/04-calidad-e-ingenieria.md#gaps-declarados).
Es el caso que mejor ilustra el costo de no tener pipeline: la disciplina
funcionó **esta vez**.

**Deja:** la conciencia de que **el perfil de desarrollo y el de producción
gestionan el esquema de formas distintas**, y que todo lo que dependa de Flyway
—colisiones, checksums, orden— es invisible en local por construcción.

## Caso 4 — Las imágenes que se rompían del otro lado del dominio

**Síntoma.** Las imágenes de producto se sirven desde el propio backend y se
siembran con ruta relativa (`/images/products/x.webp`). En el panel se ven
perfectas. Consumidas por un frontend React alojado en **otro dominio**, el
navegador resuelve esa ruta contra el origen del frontend y **todas las imágenes
salen rotas**.

**Causa raíz.** Una ruta relativa es una respuesta que depende de quién
pregunta, y la API tiene consumidores en otro origen. El dato sembrado no era
"incorrecto": era ambiguo.

**Alternativas descartadas.** Sembrar URLs absolutas rompería el panel y ataría
los datos al dominio de despliegue; concatenar el prefijo en cada controlador
esparciría la misma decisión por cuatro lugares con cuatro oportunidades de
diferir en un detalle (la barra duplicada, la URL que ya era absoluta, el valor
nulo).

**Fix sistémico.** Un componente de una sola responsabilidad,
`util/ImageUrlResolver`, configurado por propiedad (`app.public.base-url`, que
en producción llega como `PUBLIC_BASE_URL` desde `render.yaml`):

- Con la base **vacía** —el default— devuelve la ruta tal cual: el
  comportamiento previo se conserva para consumo desde el mismo origen.
- Con base configurada antepone el origen, normalizando las barras finales de la
  base y la inicial de la ruta para no duplicarlas.
- Respeta las URLs que ya son absolutas y los valores nulos o en blanco.

La decisión quedó cubierta por **7 tests** que recorren esas seis ramas y no
necesitan levantar Spring — es una función pura con una dependencia de
configuración.

**Deja:** el patrón de **seam de configuración por entorno**: cuando un
comportamiento depende de dónde está desplegado el sistema, se aísla en un punto
inyectable, se le da un default que preserva lo anterior, y se lo testea. La
misma clase de problema —una decisión de despliegue infiltrada en la lógica— se
resuelve así en el resto del sistema.

## Caso 5 — Autoridad redundante: el rol que no otorgaba nada

**Síntoma.** El modelo de acceso tenía **tres roles** (`ADMIN`, `EMPLOYEE`,
`USER`) y además un flag booleano `system_admin` en la tabla de usuarios, que
producía una autoridad extra `ROLE_SYSTEM_ADMIN`. Quince anotaciones
`@PreAuthorize` decían `hasAnyRole('ADMIN','SYSTEM_ADMIN')`.

**Causa raíz.** Ninguna de las dos autoridades otorgaba nada distinto:

- El rol `USER` **no daba acceso a ninguna vista del panel**, porque toda ruta
  exige `ADMIN` o `EMPLOYEE`. Un usuario con ese rol podía iniciar sesión y no
  ver absolutamente nada.
- El flag `system_admin` era un **segundo camino para decir `ADMIN`**: dos
  representaciones del mismo permiso, que hay que mantener sincronizadas y que
  se pueden contradecir.

Un modelo de permisos con autoridades que no significan nada es peor que uno más
chico: cada lector tiene que descubrir por su cuenta que la distinción es
ficticia, y cada regla nueva tiene que decidir si la incluye.

**Fix sistémico.** Reducción completa a **dos roles operativos**, tocando todas
las capas en un solo cambio coherente: el enum `RoleType`, el campo y los métodos
de `User`, la lógica de `UserService` (incluido el rol por defecto `EMPLOYEE` al
crear sin especificar), las 15 anotaciones de los controladores, el seeder, las
plantillas, los tests y los tres documentos de arquitectura. En base de datos, la
migración `V4` elimina el rol y la columna — otra vez hacia adelante, dejando
`V1` y `V2` intactas.

**La cola del caso, que es la parte interesante.** Eliminado el flag,
`sysadmin@malva.com` quedó como un `ADMIN` común, funcionalmente idéntico a
`admin@malva.com`: redundante. Borrarlo parecía trivial, pero en la **base de la
demo pública** alguien pudo haber iniciado sesión con esa cuenta y registrado
ventas o mandado cosas a la papelera — y todas esas FKs son `NO ACTION`. Un
`DELETE` directo habría funcionado en una base nueva y **roto el arranque en
producción**.

La migración `V5` lo resuelve con un bloque `DO $$` que sale temprano si el
usuario no existe y, si existe, **reasigna al administrador las siete columnas**
que podrían apuntarlo (`sales.registered_by_id`, `products.user_id`, y el
`deleted_by_id` de las cinco entidades con papelera) antes de borrarlo. En una
base nueva son no-ops.

**Deja:** dos cosas. **Simplificar el modelo de permisos es trabajo de
ingeniería**, no cosmética: menos autoridades es menos superficie de error. Y
**una migración de datos debe escribirse para el estado real de producción**, no
para el caso feliz de una base limpia — la diferencia entre las dos es
exactamente donde viven los deploys rotos.

## Caso 6 — Validar en el borde: el `""` que no era un valor

**Síntoma.** Editar un usuario sin tocar su contraseña **fallaba la validación**.
El formulario enviaba el campo vacío, es decir `""`, que violaba
`@Size(min = 6)`. La intención del usuario era "no la cambies"; el sistema
entendía "poné una contraseña de cero caracteres".

**Causa raíz.** Un formulario HTML no distingue entre *ausente* y *vacío*: todo
campo presente llega como string, y un campo opcional sin completar llega como
`""`. Toda validación de tamaño o formato sobre un campo opcional del panel
tenía el mismo problema latente. No era un bug de la pantalla de usuarios: era
un bug del **binding de formularios** del sistema entero.

**Fix sistémico.** Un `@ControllerAdvice` global —`GlobalBindingAdvice`— que
registra un `StringTrimmerEditor(true)`: recorta espacios y convierte los
strings vacíos en `null` **antes** de que corra la validación, en **todos** los
formularios del panel. Con `null`, la restricción de un campo opcional
sencillamente no se dispara.

La alternativa era un `if (password == null || password.isBlank())` en el
controlador de usuarios. Habría arreglado el síntoma reportado y dejado los
otros siete formularios esperando su turno.

El mismo cambio aprovechó para cerrar la clase completa: límite superior a la
contraseña (`@Size(min = 6, max = 32)`, que faltaba), `required` en los campos
obligatorios de **todos** los formularios del panel, y el campo de contraseña
dejando de reimprimir su valor al re-renderizar tras un error.

**El mismo principio, en otro lugar.** `ProductService.validateRecipeQuantity`
es la versión de este patrón para números: valida escala y precisión contra los
límites reales de la columna antes de persistir, porque sin eso la
`ConstraintViolationException` de Hibernate llega al usuario como un error 500.
El comentario del método lo dice explícitamente.

**Deja:** la regla de **validar en el borde con el vocabulario del borde**. Un
formulario habla de campos vacíos; una columna `NUMERIC(14,4)` habla de escala y
precisión. Traducir esas realidades a la semántica del dominio —"opcional",
"cantidad inválida"— es trabajo de la capa que las recibe, y hacerlo una sola vez
para toda la clase es lo que impide que el mismo bug reaparezca en la próxima
pantalla.

## Caso 7 — Dos nociones de identidad para la misma entidad

**Síntoma.** Crear un tag llamado `Citricos` cuando ya existía `Cítricos`
—o `Sin-Gluten` junto a `Sin Gluten`— devolvía un **error 500**. La validación de
nombre duplicado, que existía y funcionaba, no se disparaba.

**Causa raíz.** El sistema tenía **dos definiciones de "es el mismo tag"** que
nunca se reconciliaron. `validateTagName` comparaba el **nombre** vía
`findByNameIgnoreCase`: sólo ignora mayúsculas. La restricción
`uq_tags_slug` comparaba el **slug**, que `SlugUtil.generateSlug` deriva
descomponiendo en NFD, quitando diacríticos y colapsando todo lo no alfanumérico.
La transformación no es inyectiva, así que la brecha entre ambas nociones era
precisamente el conjunto de nombres que pasaban la validación y violaban la
restricción.

Dos agravantes que sólo aparecieron al mirar el ciclo completo:

- **La papelera no libera el slug.** El `UNIQUE` cubre la tabla entera y el
  soft-delete deja la fila, así que un tag eliminado sigue ocupando su slug. Para
  el nombre exacto había un mensaje explicativo; para una variante ortográfica,
  un 500.
- **El slug podía quedar vacío.** Un nombre sin alfanuméricos (`***`) pasa
  `@NotBlank` del request y produce `""`, que viola la columna. En secciones,
  además, habría dejado la sección inalcanzable en `GET /api/v1/sections/{slug}`.

Que terminara en 500 y no en un mensaje tiene su propia causa:
`ApiExceptionHandler` está acotado a `controller.api`, y los controladores del
panel sólo capturan `IllegalArgumentException` y `EntityNotFoundException`.

**Fix sistémico.** Un `buildSlug(name, excludeId)` en `TagService` **y** en
`StorefrontSectionService` —los dos únicos servicios con slug— que deriva el
slug, verifica que no sea vacío y que no esté tomado, consultando sin filtrar
`deletedAt` para cubrir también la papelera. El mensaje distingue los dos casos y
nombra al elemento en conflicto.

La alternativa disponible era tentadora: `SlugUtil` ya tenía un
`generateUniqueSlug(base, counter)` sin usar, que habría resuelto la colisión
agregando un sufijo. Se descartó por dos razones —dos filas visualmente idénticas
en el panel con enlaces distintos, y sufijos acumulándose en cada renombrado,
porque `update()` regenera el slug— y se **eliminó el método**: una colisión de
slug no es un problema de nomenclatura a esquivar, es la señal de un duplicado
que la validación de nombre no vio.

**Deja:** que **una entidad con identidad derivada tiene dos identidades**, y que
validar sólo la que el usuario escribe deja la otra a cargo de la base de datos
—que sabe rechazarla pero no explicarla. Es la misma regla del caso 6 aplicada a
la unicidad: cuando la base ya conoce un invariante, la aplicación lo verifica
antes para poder contarlo. La diferencia es que acá el invariante no estaba sobre
un campo del formulario, sino sobre algo que el sistema calcula solo.

## El patrón detrás de los siete casos

Ninguno se resolvió con un parche local. En todos, el bug puntual fue la pista
de una **clase** de problema: si una papelera rompía el render, las cinco lo
hacían; si un `hardDelete` devolvía 500, los cinco lo hacían; si un campo
opcional fallaba al llegar vacío, todos los formularios lo hacían.

El arreglo fue siempre estructural — el redondeo en el constructor y no en el
llamador, el advice global y no el `if` local, el resolvedor inyectable y no la
concatenación repetida, la guarda en los dos servicios con slug y no en el que
falló, la migración hacia adelante y no la edición del historial. Esa es la
diferencia entre tapar un síntoma y subir el piso de todo el sistema.

Y hay un segundo hilo, menos habitual en un dossier: **cuatro de los siete casos
se resolvieron eliminando cosas** — un rol, un flag, un usuario, un contexto
entero con sus dependencias, un helper que ofrecía la solución equivocada. Quitar
lo que no aporta es tan trabajo de ingeniería como agregar lo que falta, y suele
ser el que más mejora la comprensión de un sistema.
