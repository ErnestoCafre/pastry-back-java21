# 5. Contexto y objetivo del proyecto

> Este documento da el *porqué* antes del *cómo*: qué problema resuelve el
> sistema, qué decisiones de alcance lo definen y en qué estado está — un
> backend desplegado y operativo, con el núcleo funcional completo y brechas
> declaradas.

## El problema que resuelve

Una pastelería artesanal chica gestiona su operación en cuadernos, planillas y
la cabeza del dueño. Funciona hasta que deja de funcionar, y el punto de quiebre
casi nunca es el volumen: es **no saber cuánto cuesta realmente lo que se
vende**.

Un pastel tiene entre cinco y quince insumos con precios que se mueven. El
precio de venta se pone "a ojo" —por lo que cobra el de al lado, o por lo que
salía el año pasado— y el margen real se descubre a fin de mes, si se descubre.
Cuando sube la manteca, nadie sabe qué productos dejaron de ser rentables.

A eso se suma un segundo problema: la vitrina. Mostrar el catálogo online exige
mantener una lista de productos **en otro lado** —una red social, un sitio
armado por un tercero— que se desincroniza del catálogo real desde el primer
día.

El sistema ataca las dos cosas con un solo modelo de datos:

1. **Catálogo con receta y costeo.** Cada producto declara sus ingredientes con
   cantidad; cada ingrediente, su costo unitario y su unidad. El costo del
   producto es un cálculo sobre datos vivos, no una estimación.
2. **Registro de ventas con snapshot.** Cada venta congela precio y costo de
   insumos al momento en que ocurre, así el margen histórico es un dato exacto
   y no una reconstrucción contra el catálogo de hoy.
3. **Vitrina alimentada por el mismo catálogo.** Lo que se publica es un
   atributo del producto (`visible`) y una decisión de exhibición
   (sección, orden, tags), no una copia en otro sistema. La vitrina no puede
   desincronizarse porque no hay una segunda fuente.

## Las tres decisiones de alcance

El proyecto no es "otro CRUD de productos": tres decisiones de **alcance**
—tomadas al inicio, sobre *qué* se construye— determinan casi toda la
arquitectura posterior. Las decisiones de **implementación** que se derivan de
ellas (monolito por capas, panel SSR, snapshot, soft-delete, Flyway en
producción) están discutidas contra sus alternativas en
[1. Arquitectura](../01-arquitectura-e-ingenieria/01-arquitectura.md).

### 1. Refactorización total, no evolución incremental

El sistema es la **reescritura completa** de un backend anterior sobre un stack
al día:

| Aspecto | Versión anterior | Versión actual |
|---|---|---|
| Java | 17 | **21** (LTS) |
| Spring Boot | 3.x | **4.0.1** |
| Spring Security | 6.x | **7.x** |
| Hibernate | 6.x | **7.2** |
| Jakarta EE | 9 | **11** |
| Esquema | Solo Hibernate `ddl-auto` | **Flyway** en producción |

La alternativa era migrar por partes sobre la base existente. Se eligió la
reescritura porque el salto de Spring Boot 3 a 4 cambia la configuración de
seguridad, el arranque y la gestión del esquema a la vez: migrar por partes
significaba sostener dos formas de hacer cada cosa durante meses.

La decisión tiene una consecuencia que se ejerció con disciplina: **si es una
reescritura, lo que no se usa no se copia**. Dos commits del historial eliminan
funcionalidad heredada —el andamiaje de "usuario público" con JWT, login con
Google y S3, y el rol `USER` con el flag `system_admin`— porque no tenían un
solo camino de código que los ejercitara
(ver [casos 2 y 5](../03-producto/09-casos-de-estudio.md)). El costo de ese
andamiaje era concreto y medible: dependencias transitivas grandes, una variable
de entorno obligatoria en producción para una funcionalidad inexistente, y un
filtro de autenticación registrado en cada request de la API que no podía
autenticar a nadie.

### 2. Doble canal desde el diseño, no una API agregada después

El sistema sirve dos audiencias —el personal de la pastelería y el público de la
vitrina— y eso se modeló **desde el primer día**, no como una capa REST agregada
sobre un panel existente.

De ahí salen las decisiones más estructurales: dos cadenas de filtros de
seguridad con modelos de sesión opuestos, dos formas de DTO (clases mutables
para el binding de formularios, records inmutables para el contrato JSON), dos
tratamientos del mismo error (HTML con flash en el panel, JSON con status en la
API) y un contrato público documentado en OpenAPI.

El punto no técnico detrás: la vitrina la consume un **frontend React alojado en
otro dominio**, no el mismo servidor. Esa sola condición produjo CORS explícito
por origen, y el resolvedor de URLs absolutas de imagen que existe precisamente
porque una ruta relativa se rompe cuando el cliente vive en otra parte
(ver [caso 4](../03-producto/09-casos-de-estudio.md)).

### 3. Un solo negocio, deliberadamente

El sistema modela **una** pastelería. No hay tenant, no hay subdominio, no hay
aislamiento entre organizaciones: hay usuarios con roles dentro de un negocio
único.

Es una decisión de alcance, no una limitación heredada. Multi-tenancy no es una
funcionalidad que se agregue: es un eje que atraviesa cada tabla, cada consulta,
cada índice y cada test del sistema. Construirlo "por si acaso" para un negocio
que no lo necesita significa pagar toda esa complejidad por adelantado a cambio
de nada.

Lo que sí se hizo es **no cerrarse el camino**: el dominio está partido en
bounded contexts con fronteras explícitas, las consultas pasan todas por
repositorios, y la lógica vive en servicios y no en controladores. Si el sistema
tuviera que servir a varias pastelerías, el cambio sería invasivo —lo es siempre—
pero tendría dónde apoyarse.

## Cómo se eligió la tecnología

La elección no fue por inercia. El caso testigo es el par
**Thymeleaf + API REST**: la respuesta cómoda era una SPA para todo, y se
descartó porque el panel tiene dos usuarios internos y un ciclo de formularios,
mientras que la vitrina tiene un consumidor externo con contrato estable. Dos
problemas distintos, dos herramientas distintas, un solo dominio detrás.

Las otras dos elecciones definitorias siguieron el mismo criterio de "elegir
para el problema que existe":

- **Java 21 + Spring Boot 4** porque el objetivo declarado del proyecto era
  demostrar dominio del stack **actual**, no del que estaba maduro hace tres
  años. Adoptar Spring Boot 4 y Security 7 implica leer las notas de migración y
  resolver los cambios de API sin recetas de Stack Overflow todavía escritas —
  que es exactamente el ejercicio que justifica la reescritura.
- **PostgreSQL** por el mismo motor en local y en producción: el sistema depende
  de precisión decimal exacta (`NUMERIC(12,2)` y `NUMERIC(14,4)` en costos y
  cantidades de receta) y de FKs con comportamiento predecible. Una base
  embebida en desarrollo habría escondido justamente los errores de escala y de
  integridad referencial que el proyecto terminó encontrando.

El inventario completo de herramientas con su justificación está en
[6. Stack tecnológico y decisiones](06-stack-tecnologico-y-decisiones.md), y el
proceso de construcción en
[7. Proceso y desarrollo asistido por IA](07-proceso-y-desarrollo-asistido-por-ia.md).

## Estado y madurez de ingeniería

El sistema está **desplegado y operativo**, con el núcleo funcional completo:
catálogo, recetas y costeo, ventas con snapshot, vitrina con secciones y tags,
API pública documentada, usuarios y roles. No es un entorno de demostración: la
instancia pública corre sobre Docker con PostgreSQL administrado y la cadena de
migraciones aplicada.

Lo que sostiene esa etapa:

- **5 migraciones Flyway versionadas** más una repetible, con el historial
  tratado como inmutable y las correcciones hechas hacia adelante.
- **Seed de demo completo y con paridad dev/prod**: 50 productos, 75
  ingredientes, 297 líneas de receta y 42 ventas, de modo que el dashboard, el
  costeo y el detalle de venta muestran datos reales desde el primer arranque —
  que además fue lo que destapó tres bugs latentes
  (ver [caso 1](../03-producto/09-casos-de-estudio.md)).
- **62 tests** con el contexto de Spring verificado contra PostgreSQL real.
- **Historial de commits como bitácora**: cada cambio no trivial explica el
  porqué y enumera lo verificado.

Y lo que le falta, dicho sin rodeos: **no hay pipeline de CI**, y los servicios
de negocio (costeo, ventas, guardas de borrado) **no tienen tests
automatizados**. Ambas cosas, con su plan, están en
[4. Calidad e ingeniería](../01-arquitectura-e-ingenieria/04-calidad-e-ingenieria.md#gaps-declarados).
El roadmap de producto está en
[8. Recorrido de producto](../03-producto/08-recorrido-de-producto.md), y los
episodios donde se ejerció la disciplina de ingeniería, en
[9. Casos de estudio](../03-producto/09-casos-de-estudio.md).
