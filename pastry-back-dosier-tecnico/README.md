# Malva Pastry Shop — Dossier técnico del backend

> Documentación orientada a presentar la construcción del sistema ante un
> reclutador o entrevistador técnico. Resume arquitectura, patrones de diseño
> y prácticas de ingeniería con referencias a código real del repositorio.
>
> A diferencia de un README de producto, este dossier explica **por qué** el
> sistema es como es: qué alternativas se descartaron, qué cuesta cada
> decisión y qué problemas concretos se resolvieron en el camino.

## ¿Qué es Malva Pastry Shop?

**Backend de gestión para una pastelería artesanal**, con dos canales sobre un
mismo dominio: un **panel de administración** renderizado en el servidor
(Thymeleaf) para la operación diaria del negocio, y una **API REST pública de
solo lectura** que alimenta la vitrina web (un frontend React alojado en otro
dominio).

El núcleo funcional no es el CRUD sino el **costeo por receta**: cada producto
declara sus ingredientes con cantidad, cada ingrediente su costo unitario, y
cada venta congela un **snapshot** de precio y de insumos consumidos. Así el
margen histórico es un dato y no una reconstrucción.

El proyecto es una **refactorización completa** de un sistema anterior
(Java 17 / Spring Boot 3 / esquema gestionado por Hibernate) hacia
**Java 21 · Spring Boot 4 · Spring Security 7 · Hibernate 7.2 · Flyway**, con
la superficie muerta del sistema previo eliminada deliberadamente
(ver [caso 2](dossier-tecnico/03-producto/09-casos-de-estudio.md)).

Está **desplegado y operativo**, no es un entorno de demostración local:
[malva-pastry-backend.onrender.com](https://malva-pastry-backend.onrender.com/login).

## Números del proyecto

| Métrica | Valor |
|---|---|
| Archivos fuente Java | 71 (~7.000 líneas, `backend/src/main/java/`) |
| Bounded contexts | 4 (inventory · storefront · sales · auth) |
| Entidades de dominio | 12 + 2 superclases mapeadas + 2 enums |
| Tablas en PostgreSQL | 12 (5 con soft-delete) |
| Migraciones Flyway | 6 versionadas + 1 repetible (seed de demo) |
| Tests | 62 (JUnit 5 · Mockito · AssertJ) |
| Plantillas Thymeleaf | 40 |
| Endpoints de la API pública | 7 (todos `GET`) |
| Stack backend | Java 21 · Spring Boot 4.0.1 · Spring Security 7 · Hibernate 7.2 · PostgreSQL |
| Panel admin | SSR: Thymeleaf 3 · Layout Dialect · Tailwind + Flowbite |
| Infra | Docker multi-stage · Render (web service + PostgreSQL administrado) |

## Índice del dossier

1. **[Arquitectura general](dossier-tecnico/01-arquitectura-e-ingenieria/01-arquitectura.md)** — arquitectura de dos
   canales sobre un dominio único, capas y bounded contexts, panorama del
   modelo de datos, recorrido de un request de punta a punta, y las decisiones
   de arquitectura discutidas contra sus alternativas (monolito por capas,
   panel SSR y no SPA, snapshot en la venta, soft-delete, Flyway en producción
   con `ddl-auto` en desarrollo).
2. **[Patrones de diseño](dossier-tecnico/01-arquitectura-e-ingenieria/02-patrones-de-diseno.md)** — catálogo de
   patrones aplicados con su ubicación en el código: registro inmutable con
   snapshot, soft-delete de tres fases, `@EntityGraph` como contrato de fetch,
   doble cadena de filtros, DTOs segregados por audiencia, guardas de
   integridad antes del borrado, seam de configuración por entorno, espejo
   dev/prod del seed.
3. **[Seguridad](dossier-tecnico/01-arquitectura-e-ingenieria/03-seguridad.md)** — dos cadenas de filtros con
   modelos de sesión opuestos, autenticación por formulario con BCrypt, RBAC de
   dos roles aplicado en dos niveles, la API pública como superficie
   *deny-by-default*, CORS explícito, secretos fuera del repositorio, y un
   inventario honesto de lo que el sistema **no** tiene y por qué.
4. **[Calidad e ingeniería](dossier-tecnico/01-arquitectura-e-ingenieria/04-calidad-e-ingenieria.md)** — estrategia de
   testing y su cobertura real, disciplina de migraciones (inmutabilidad de lo
   aplicado, migración hacia adelante, colisión de versiones), la práctica de
   **validar en el servicio antes de que reviente la base**, documentación
   como parte de "terminado", y los gaps declarados con su plan.

### Contexto y proceso

5. **[Contexto y objetivo](dossier-tecnico/02-contexto-y-proceso/05-contexto-y-objetivo.md)** — el problema que
   resuelve y por qué; las tres decisiones de alcance que definen el sistema
   (refactorización total, doble canal desde el diseño, mono-negocio
   deliberado); en qué estado de madurez está.
6. **[Stack tecnológico y decisiones](dossier-tecnico/02-contexto-y-proceso/06-stack-tecnologico-y-decisiones.md)** —
   herramientas con versión y justificación; empaquetado Docker multi-stage;
   infraestructura Render servicio por servicio, incluida la restricción
   económica del plan Free y cómo se administra.
7. **[Proceso y desarrollo asistido por IA](dossier-tecnico/02-contexto-y-proceso/07-proceso-y-desarrollo-asistido-por-ia.md)**
   — cómo se decidió qué construir; el repositorio diseñado como contexto
   durable (`CONTEXT.md`, `ARCHITECTURE.md`); el commit como registro de
   decisión y de verificación; qué prácticas de *agentic coding* se aplicaron
   y cuáles todavía no.

### Producto y casos de estudio

8. **[Recorrido de producto](dossier-tecnico/03-producto/08-recorrido-de-producto.md)** — qué hace el sistema de
   cara al usuario: catálogo, recetas y costeo, ventas, vitrina, API pública,
   usuarios y roles; con separación explícita entre lo implementado y el
   roadmap. Buen punto de entrada para entender el *qué*.
9. **[Casos de estudio de ingeniería](dossier-tecnico/03-producto/09-casos-de-estudio.md)** — seis problemas reales
   (el seed que destapó tres bugs, el andamiaje muerto que costaba dinero, la
   colisión de versiones de Flyway, las imágenes rotas para un frontend
   remoto, la autoridad redundante y su cola, la validación en el borde)
   contados como síntoma → causa raíz → fix sistémico.

## Cómo leer este dossier

Cada documento referencia archivos reales del repositorio para que las
afirmaciones sean verificables. Salvo indicación en contrario, las rutas de
clases Java son relativas a
`backend/src/main/java/com/malva_pastry_shop/backend/`, y las de recursos a
`backend/src/main/resources/`.

Las citas de comentarios del código son literales: buena parte del
razonamiento de diseño está documentado **en el propio código**, en el módulo
donde se toma cada decisión.

Cuando el dossier dice que algo **no** está hecho, es información deliberada:
un inventario honesto de límites vale más que una lista de capacidades
infladas, y varias de esas ausencias son decisiones de alcance, no olvidos.
