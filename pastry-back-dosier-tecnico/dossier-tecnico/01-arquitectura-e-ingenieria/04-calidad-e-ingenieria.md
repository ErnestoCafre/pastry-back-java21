# 4. Calidad e ingeniería

Las prácticas que sostienen el sistema día a día. La regla general del
proyecto: **ante un bug, auditar toda la clase del problema y aplicar el fix
sistémico, no el parche puntual** — las guardas de borrado, el `@EntityGraph` de
las papeleras y el trim global de formularios existen porque se aplicó esa regla
a un solo síntoma y se corrigieron los cinco casos hermanos a la vez
(ver [9. Casos de estudio](../03-producto/09-casos-de-estudio.md)).

Este documento describe también, sin adornos, **dónde la cobertura es fina**.

## Testing: 62 tests, y qué cubren realmente

| Suite | Tests | Tipo | Qué verifica |
|---|---|---|---|
| `UserServiceTest` | 21 | Unitario con Mockito | Alta, edición, cambio de estado y consultas de usuarios: email duplicado, rol inexistente, rol por defecto `EMPLOYEE`, hash de contraseña, actualización de contraseña solo si viene |
| `UserControllerTest` | 16 | Unitario con Mockito | Contrato del controlador: vista devuelta, atributos del modelo, redirects, repoblado del formulario en cada rama de error |
| `ImageUrlResolverTest` | 7 | Unitario puro | Las seis ramas del resolvedor de URLs de imagen (ver [patrones](02-patrones-de-diseno.md#seam-de-configuración-por-entorno)) |
| `TagServiceTest` | 6 | Unitario con Mockito | Derivación y unicidad del slug: colisión entre nombres distintos, slug retenido por un elemento en papelera, nombre sin alfanuméricos, y el no-conflicto consigo mismo al editar |
| `StorefrontSectionServiceTest` | 5 | Unitario con Mockito | Las mismas garantías sobre secciones de vitrina, donde además el slug es la clave pública del endpoint |
| `SlugUtilTest` | 6 | Unitario puro | Normalización (acentos, separadores) y, sobre todo, que la transformación **no es inyectiva**: el caso que obliga a validar antes de persistir |
| `MalvaPastryShopApplicationTests` | 1 | Integración | `contextLoads` contra PostgreSQL real: el contexto de Spring arranca, los beans se cablean y el esquema se crea |

Los tests están escritos con **JUnit 5 + Mockito + AssertJ**, organizados en
clases `@Nested` con `@DisplayName` en castellano, un bloque por método bajo
prueba. Un fallo dice qué comportamiento se rompió, no en qué línea explotó.

Dos decisiones que valen más que el número:

- **Se testea con inyección por constructor, no con `@SpringBootTest`.** Los
  tests de servicio y controlador construyen la clase a mano con mocks y corren
  en milisegundos, sin levantar el contenedor. Es la contrapartida útil del
  patrón de inyección por constructor.
- **`contextLoads` no es un test trivial acá.** Corre contra una base
  PostgreSQL real y ejercita el arranque completo: si un `@EntityGraph` nombra
  un atributo inexistente, si una entidad no mapea, si un bean tiene una
  dependencia sin resolver o si el esquema derivado no se puede crear, ese único
  test falla. Es el test más barato con mayor cobertura del proyecto.

**Dónde la cobertura es fina, y es un hecho declarado:** los tests cubren el
contexto de autenticación, dos utilidades y la derivación de slugs en los dos
servicios de vitrina; **no hay tests automatizados sobre `ProductService`,
`SaleService`, `CategoryService` ni `IngredientService`**, y de `TagService` y
`StorefrontSectionService` solo está cubierto el camino del slug. La lógica más
valiosa del sistema —el cálculo de costo de receta, la expansión de la venta con
sus snapshots y sus validaciones de escala, las guardas de borrado, el ciclo de
la papelera— se verifica hoy manualmente. Es la brecha más importante del
proyecto y encabeza la sección de gaps.

La forma en que se cerró la parte que sí está cubierta ilustra la regla: los
diecisiete tests que se sumaron no nacieron de una meta de cobertura sino de un
bug concreto —una colisión de slug que devolvía un 500— y cubren **la clase
entera del problema**, incluidos los dos servicios afectados y la utilidad que
los alimenta, no el caso reportado
(ver [caso 7](../03-producto/09-casos-de-estudio.md)).

## Verificación: manual, pero registrada

Sin CI, el bucle de verificación es manual — y la disciplina consiste en que
**quede escrito qué se verificó**. Los mensajes de commit del proyecto no
describen solo el cambio: cierran con lo comprobado. Literal de la historia:

> *Verificado: cadena Flyway V1-V3+R__ sobre Postgres real (idempotente), app en
> perfil dev sin errores, login y dashboard (4 ventas / S/1106 hoy / S/11862
> mes), 5 papeleras renderizando completas, 39 tests en verde.*

> *Verified: mvn test green (17 unit tests + contextLoads against real Postgres,
> app boots and DataSeeder runs); dev schema drops from 15 to 12 tables.*

Lo que esto compra: un cambio que tocó el esquema deja constancia de que se
probó **la cadena de migraciones completa contra PostgreSQL**, no solo que
compiló. La verificación es reproducible por el siguiente lector porque está
enumerada. Lo que **no** compra —y hay que decirlo— es que se repita sola en
cada push: eso es exactamente lo que hace un pipeline de CI, y es el gap
principal.

## Disciplina de migraciones

Seis archivos en `resources/db/migration/`: `V1`…`V5` versionadas y
`R__seed_demo_data.sql` repetible. Con `validate-on-migrate=true` y las
migraciones corriendo al arranque, la consistencia del historial es un
**invariante operativo**: un historial inválido rompe el boot del servicio, no
una consulta cualquiera más tarde.

De ahí salen tres reglas que el proyecto sostiene:

**1. Lo aplicado es inmutable.** `V1` no se edita nunca, ni siquiera para
corregir algo que quedó mal: Flyway valida el checksum de cada migración ya
aplicada y una edición rompe el arranque en producción. Cuando hubo que
eliminar tres tablas creadas en `V1` que nunca se usaron, la solución fue **`V3`
hacia adelante**, no reescribir `V1`. El propio commit lo razona:

> *V1 is an applied versioned migration and cannot be edited without breaking
> Flyway's validate-on-migrate; the forward migration is the safe path.*

**2. Un solo número por versión.** Dos ramas paralelas que agregan migración
producen dos archivos `V3__`, y `validate-on-migrate` falla con *"more than one
migration with version 3"* — al arrancar en producción, no al mergear. Pasó
(ver [caso 3](../03-producto/09-casos-de-estudio.md)) y se resolvió renumerando
antes del merge. La verificación es manual y por eso la sección de gaps propone
automatizarla.

**3. La migración de datos preserva las referencias.** `V5` no es un `DELETE`:
antes de borrar el usuario redundante reasigna al administrador **las siete
columnas** que podrían apuntarlo (`sales.registered_by_id`, `products.user_id`,
y el `deleted_by_id` de las cinco entidades con papelera), dentro de un bloque
`DO $$` que sale temprano si el usuario no existe. En una base nueva son no-ops;
en la base de la demo pública, donde alguien pudo haber iniciado sesión con esa
cuenta y registrado ventas, evita violar una FK. Una migración que asume el caso
feliz habría funcionado en local y roto el deploy.

## Fail-loud: validar antes de que reviente la base

Patrón consistente en todo el código: **cuando la base ya conoce un invariante,
el servicio lo verifica antes para poder explicarlo**. La constraint sigue
siendo la red final; lo que cambia es quién le da la noticia al usuario.

- **Precisión y escala de `BigDecimal`.** `SaleService` valida que el total y
  cada cantidad de insumo no excedan los 10 dígitos enteros de la columna;
  `ProductService.validateRecipeQuantity` valida escala ≤ 4 y ≤ 10 dígitos
  enteros. Sin eso, una cantidad con cinco decimales llega a Hibernate y sale
  como `ConstraintViolationException`, es decir, **500 en pantalla**. El
  comentario del método lo dice exactamente así.
- **Integridad referencial antes del borrado.** Las cinco guardas de
  `hardDelete` cuentan referencias y lanzan `IllegalStateException` con el
  número, en vez de dejar que la FK `NO ACTION` produzca un
  `DataIntegrityViolationException`.
- **Redondeo en el constructor, no en el llamador.** `SaleIngredient` redondea
  su `totalCost` a escala 2 donde se construye, para que ninguna ruta futura
  pueda saltearlo.
- **Excepciones con semántica, no genéricas.** `EntityNotFoundException` (no
  existe), `IllegalArgumentException` (entrada inválida) e
  `IllegalStateException` (regla de negocio violada) tienen significados
  distintos y consistentes en todos los servicios. `ApiExceptionHandler` mapea
  las dos primeras a 404 y 400 apoyándose en esa convención.

La contracara de fail-loud es **no filtrar detalle**: en la API, cualquier
excepción no prevista responde `"Error interno del servidor"` y el detalle queda
en el servidor.

## El código documenta sus decisiones

Los puntos no obvios llevan comentarios que explican **por qué**, qué
alternativa se descartó y qué invariante protegen — junto al código que lo
necesita, no en una wiki que se desactualiza. Cuatro ejemplos verificables:

- `ProductRepository`: *"deletedBy es obligatorio en el fetch: la vista muestra
  quién eliminó y con open-in-view=false un proxy lazy rompería el render de la
  plantilla."*
- `SaleIngredient`: por qué el `totalCost` se redondea en el constructor y qué
  falla si no.
- `SaleIngredient.ingredient`: *"La columna admite null para poder conservar el
  histórico, pero la FK es NO ACTION (no hay ON DELETE SET NULL en el esquema)"*
  — escrito después de detectar que los comentarios anteriores afirmaban lo
  contrario y corregirlos.
- `ImageUrlResolver`: el escenario completo (frontend en otro dominio, imagen
  rota) y por qué el default vacío conserva el comportamiento previo.
- `keep-alive.sh`: la aritmética de las 750 instance-hours mensuales que
  justifica la ventana horaria, y dos trampas de portabilidad de `date`
  documentadas en el lugar donde muerden.

Complementos fuera del código:

- **`ARCHITECTURE.md`** (versionado) — capas, bounded contexts, diagrama ER
  completo con tipos y constraints.
- **`README.md`** de raíz y de `backend/` (versionados) — puesta en marcha,
  credenciales de demo, inventario del dataset sembrado.
- **`scripts/README.md`** (versionado) — operación del keep-alive, con las
  restricciones del plan Free que justifican cada parámetro.
- **`CONTEXT.md`** — referencia técnica compacta, escrita explícitamente como
  contexto de alta señal (ver
  [7. Proceso](../02-contexto-y-proceso/07-proceso-y-desarrollo-asistido-por-ia.md)).
  **No está versionado**: `.gitignore` lo excluye bajo la sección `### LLM ###`,
  junto con `.claude`.
- **`personal-docs/render-deployment-guide.md`** — el procedimiento de deploy.
  Tampoco versionado (`personal-docs` está en `.gitignore`).

Que los dos últimos sean locales es una decisión con una consecuencia: el
contexto de mayor densidad técnica del proyecto no viaja con el repositorio, así
que un colaborador que clone tiene `ARCHITECTURE.md` y los README, pero no la
referencia compacta. Está anotado como pendiente a resolver — versionarlos o
asumir explícitamente que son artefactos personales.

Mantener esos documentos sincronizados es parte de "terminado": varios commits
del historial actualizan README, ARCHITECTURE y CONTEXT **en el mismo commit**
que el cambio de código, y hay uno dedicado exclusivamente a registrar en el
README una migración que faltaba en la lista.

## Flujo de trabajo

- **Ramas de feature → Pull Request → `dev` → `main`**, con `main` como rama de
  release: es la que Render observa (`branch: main` en `render.yaml`), así que
  mergear a `main` **es** desplegar.
- **Commits descriptivos con estructura**: tipo y alcance en el asunto
  (`fix(db):`, `refactor(auth):`, `feat(seed):`), cuerpo que explica el porqué y
  el efecto, y cierre con lo verificado. El historial se lee como una bitácora
  de decisiones, no como una lista de diffs.
- **Refactorización por reducción.** Dos commits del historial **eliminan**
  funcionalidad: el andamiaje JWT/OAuth/S3 que nunca se implementó y el rol
  `USER` con el flag `system_admin` que no otorgaban nada. Quitar código muerto
  con su migración, sus dependencias y su documentación se trata como trabajo de
  ingeniería de primera clase, no como limpieza opcional.

## Gaps declarados

| Gap | Impacto | Plan |
|---|---|---|
| **Sin CI** | El único workflow de GitHub Actions es el keep-alive; los tests no corren automáticamente en ningún push ni PR | Un workflow con `mvn test` sobre un servicio PostgreSQL es el primer paso, y habilita todo lo demás de esta tabla |
| **Servicios de negocio sin tests** | Costeo de receta, creación de venta con snapshots, guardas de borrado y ciclo de papelera se verifican a mano | Tests unitarios con mocks para las validaciones puras (escala, precisión, guardas) y `@DataJpaTest` para las consultas derivadas |
| **Migraciones sin verificación automática** | Una colisión de versiones o un checksum roto se descubre al arrancar en producción | Un job de CI que aplique la cadena completa sobre una base limpia y falle si Flyway se queja |
| **Divergencia dev/prod del esquema** | `ddl-auto=create` en dev y Flyway en prod son dos representaciones del mismo esquema | Un test de CI que compare el esquema derivado de las entidades contra el migrado detectaría el drift sin perder el ciclo rápido de desarrollo |
| **Auditoría de dependencias** | Sin verificación de CVEs conocidos en el árbol de dependencias | `dependency-check` o Dependabot, una vez que exista el pipeline |
| **Página 403 propia** | Un acceso denegado en el panel cae en la página de error genérica (ver [3. Seguridad](03-seguridad.md#lo-que-el-sistema-no-tiene-y-por-qué)) | Controlador y plantilla para `/error/403` |

El orden no es arbitrario: **el pipeline de CI es la pieza que desbloquea a las
demás**. Sin él, cada verificación adicional que se escriba depende de que
alguien se acuerde de correrla — que es exactamente la clase de dependencia que
el resto del proyecto elimina sistemáticamente.
