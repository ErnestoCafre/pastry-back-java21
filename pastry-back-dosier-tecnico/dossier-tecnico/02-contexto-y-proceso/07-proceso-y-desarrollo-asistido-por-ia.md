# 7. Proceso de trabajo y desarrollo asistido por IA

> Cómo se decidió *qué* construir y *cómo* se construyó. Dos cosas caracterizan
> el proceso: el repositorio está deliberadamente **diseñado como contexto
> durable**, y una parte del trabajo se hizo **asistido por IA (Claude)** dentro
> de un andamiaje de verificación explícito.
>
> Este documento distingue lo que el proyecto **hace hoy** de lo que sería el
> siguiente paso. Presentar buenas intenciones como prácticas instaladas sería
> exactamente el tipo de afirmación que el resto del dossier evita.

## Fase de requisitos: el dominio antes que el código

El sistema no nació de una lista de pantallas sino de una pregunta de negocio:
*¿cuánto cuesta realmente producir cada cosa?* De ahí salieron los conceptos que
había que modelar sí o sí, y que definieron el esquema antes que cualquier
controlador:

- **La receta como entidad de primer orden** (`ProductIngredient` con cantidad),
  no como un texto libre en el producto. Sin eso el costo no es calculable.
- **La unidad de medida como enum cerrado** (`UnitOfMeasure`, 14 valores con su
  representación de presentación), porque "500 g" y "0,5 kg" tienen que poder
  compararse y sumarse.
- **El snapshot en la venta**, porque el margen histórico no puede depender del
  catálogo de hoy. Es la decisión de modelado que más consecuencias tuvo
  (ver [1. Arquitectura](../01-arquitectura-e-ingenieria/01-arquitectura.md#panorama-del-modelo-de-datos)).
- **Dos canales con audiencias distintas**, panel interno y vitrina pública,
  como requisito y no como agregado posterior.

La otra mitad de la fase fue de **investigación técnica**: el proyecto es una
reescritura sobre Spring Boot 4 y Security 7, versiones lo bastante nuevas como
para que buena parte de la documentación disponible en la web todavía describa
la generación anterior. Elegir la configuración correcta de cadenas de filtros,
de starters de test segregados o de Flyway con `baseline-version` exigió leer
las notas de versión y contrastar alternativas, no copiar una receta.

## El repositorio como contexto durable

La práctica más visible del proyecto: **la documentación está escrita para ser
leída por quien llega sin contexto** — un colaborador nuevo, el propio autor en
seis meses, o un agente de IA en una sesión nueva.

| Documento | Versionado | Rol |
|---|---|---|
| `backend/ARCHITECTURE.md` | Sí | El porqué estructural: capas, bounded contexts, diagrama ER completo con tipos y constraints. |
| `README.md` (raíz y `backend/`) | Sí | Puesta en marcha, credenciales de demo, inventario del dataset sembrado, endpoints públicos. |
| `scripts/README.md` | Sí | Operación del keep-alive, con las restricciones del plan Free que justifican cada parámetro. |
| Comentarios en el código | Sí | El razonamiento local: por qué `deletedBy` está en el `@EntityGraph`, por qué el `totalCost` se redondea en el constructor, por qué la ventana del keep-alive es la que es. |
| `backend/CONTEXT.md` | **No** | Referencia técnica compacta: estructura, modelo de dominio, patrones, seguridad, convenciones y guías de "cómo agregar una entidad". Su encabezado declara el propósito: *"Technical reference for LLMs and developers to quickly understand the codebase architecture, patterns, and conventions."* |
| `personal-docs/render-deployment-guide.md` | **No** | El procedimiento de deploy, fuera del README para no inflarlo. |

Las dos últimas filas son una tensión sin resolver del proyecto: `.gitignore`
excluye `backend/CONTEXT.md` bajo la sección `### LLM ###` y `personal-docs`
entera, de modo que **el documento con mayor densidad técnica del repositorio no
viaja con el repositorio**. Tiene una lectura defendible —son artefactos de
trabajo personal, no documentación de producto— pero choca con su propio
encabezado, que se dirige a *"LLMs **and developers**"*. Quien clone el proyecto
recibe `ARCHITECTURE.md` y los README, y no la referencia que resume todo en una
página.

La distinción que hace útil este esquema es de **altitud**: los invariantes
estables y de alta señal viven en `CONTEXT.md`; el razonamiento local vive junto
al código que lo necesita, donde no se desactualiza sin que alguien lo vea. Un
documento largo que repita lo que el código ya dice envejece mal y hace que se
ignore lo importante.

## El commit como registro de decisión

El historial del proyecto está tratado como parte del producto. Un commit no
trivial tiene tres partes:

1. **Qué cambió**, con tipo y alcance en el asunto (`fix(db):`,
   `refactor(auth):`, `feat(seed):`).
2. **Por qué**, incluida la alternativa descartada. El commit que renombra una
   migración explica el modo de falla exacto que evita:
   *"validate-on-migrate=true: 'more than one migration with version 3'"*.
3. **Qué se verificó**, enumerado. *"cadena Flyway V1-V3+R__ sobre Postgres real
   (idempotente), app en perfil dev sin errores, login y dashboard, 5 papeleras
   renderizando completas, 39 tests en verde."*

La tercera parte es la que hace el trabajo. Sin CI, la verificación depende de
que se corra a mano; escribirla en el commit la vuelve **auditable**: el
siguiente lector sabe qué se probó y, sobre todo, qué no. Los primeros seis casos
de estudio del documento 9 están reconstruidos íntegramente desde estos mensajes.

## Desarrollo asistido por IA

Parte del trabajo se hizo con **Claude** como asistente de programación: 14 de
los 96 commits del historial llevan su co-autoría declarada
(`Co-Authored-By: Claude`), y son —no por casualidad— los cambios más
transversales: la eliminación del andamiaje muerto, la refactorización del
modelo de roles, el seed con recetas y ventas junto a los tres bugs que destapó,
el trim global de formularios.

Lo que hizo que sirviera, en orden de importancia:

- **Un contexto de alta señal disponible desde el arranque.** `CONTEXT.md` y
  `ARCHITECTURE.md` existen precisamente para que una sesión nueva no tenga que
  re-derivar la arquitectura leyendo 71 archivos. El costo de escribirlos se
  recupera en cada sesión que no empieza de cero.
- **Verificación real como bucle cerrado.** El valor de un asistente cae en
  picada sin algo que compruebe su salida. Acá el bucle fue `mvn test` más el
  arranque de la aplicación contra PostgreSQL real y el recorrido de las
  pantallas afectadas — modesto comparado con una suite completa, pero
  suficiente para atrapar lo que importaba: las tres regresiones del caso 1 se
  detectaron **arrancando la app con datos sembrados**, no leyendo código.
- **Auditar la clase, no el síntoma.** Cuando falló el `hardDelete` de un
  producto con ventas, la corrección abarcó **los cinco servicios de catálogo**;
  cuando una papelera rompió el render, se revisaron **las cinco**. Un asistente
  es especialmente bueno barriendo un repositorio buscando "los otros casos
  iguales a este", y ese es exactamente el uso que amplifica la regla de fix
  sistémico del proyecto.
- **Eliminar, no solo agregar.** Dos de los commits asistidos **quitan**
  funcionalidad: el andamiaje JWT/OAuth/S3 y el rol redundante. Ambos incluyen
  el rastreo completo de lo que había que tocar —código, dependencias del
  `pom.xml`, propiedades, migración, plantillas, tests y documentación— que es
  la parte tediosa y la que se hace mal cuando se hace a mano.
- **Criterio humano en las decisiones.** La IA no eligió el alcance ni la
  arquitectura: qué construir, qué no construir (multi-tenancy), qué stack
  adoptar y qué eliminar fueron decisiones tomadas y sostenidas por el autor.
  El asistente ejecutó, exploró alternativas y barrió el repositorio en busca de
  casos hermanos.

**Qué no es esto.** No fue generación de código sin revisión: cada cambio pasó
por compilación, tests, arranque de la aplicación y revisión del diff en un pull
request. La distinción importa porque es la que separa el uso de IA con
andamiaje del que produce código que nadie entiende.

## Lo que todavía no está, y sería el siguiente paso

Un dossier honesto distingue la práctica instalada de la aspiración:

| Práctica | Estado |
|---|---|
| Contexto durable escrito (`ARCHITECTURE.md`, `CONTEXT.md`, READMEs) | **Hecho**, con una salvedad: `CONTEXT.md` y `personal-docs/` están en `.gitignore`, así que no acompañan al repositorio |
| Commits con porqué y verificación enumerada | **Hecho** |
| Fix sistémico sobre parche puntual | **Hecho**, y con casos documentados |
| Revisión del diff en pull request antes de mergear | **Hecho** |
| Verificación automatizada en cada push | **No** — no hay CI; el bucle es manual |
| Instrucciones de proyecto para el agente (`CLAUDE.md`) | **No** — el contexto vive en `CONTEXT.md`, pensado para humanos y agentes, pero no hay un archivo de convenciones que se cargue solo |
| Memoria de decisiones entre sesiones | **No** — el registro es el historial de commits |

Las tres ausencias tienen la misma raíz que el gap principal del proyecto: **sin
un pipeline de CI, toda verificación depende de que alguien se acuerde de
correrla**. Es la primera pieza del plan de
[4. Calidad e ingeniería](../01-arquitectura-e-ingenieria/04-calidad-e-ingenieria.md#gaps-declarados),
y también la que más mejoraría el trabajo asistido: un agente con un comando de
verificación que puede ejecutar y leer itera solo; sin él, el bucle se cierra a
mano en cada vuelta.

## Qué demuestra este enfoque

Para un lector técnico, el punto no es "se usó IA", sino **cómo**:

- Con **andamiaje de verificación** —tests, arranque real contra PostgreSQL,
  revisión en PR— y no como reemplazo del criterio de ingeniería.
- Sobre un repositorio **diseñado para que el contexto sobreviva** a la sesión:
  documentación de alta señal, comentarios donde se toma cada decisión y
  commits que explican el porqué.
- Aplicando la regla que más se beneficia de un asistente: **ante un bug,
  auditar toda la clase del problema**, no el caso que se reportó.
