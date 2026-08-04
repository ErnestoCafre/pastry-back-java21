# Malva Pastry Shop - Backend

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-13+-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Thymeleaf-3.x-green?style=for-the-badge&logo=thymeleaf" alt="Thymeleaf">
</p>

## Que es este proyecto

Este repositorio es la **version demo** de un sistema de gestion para una pasteleria
artesanal, desarrollado originalmente como **proyecto freelance**.

La demo no es el sistema entregado: es una **reescritura completa**, publicada para mostrar
el trabajo, en la que se rehicieron dos cosas a la vez.

1. **La plataforma tecnica**, actualizada por entero: Java 21, Spring Boot 4, Spring
   Security 7, Hibernate 7.2 y migraciones versionadas con Flyway.
2. **El diseno UX/UI, rehecho de cero.** Se abandono el tema generico de Bootstrap del
   sistema original y se construyo un design system propio en Tailwind, con una libreria
   de fragments Thymeleaf reutilizables. Ninguna pantalla quedo como estaba.

Funcionalmente cubre el mismo terreno que el sistema original — catalogo, recetas con
costeo, ventas con margen y vitrina publica — sobre dos interfaces: un **panel de
administracion** (Thymeleaf SSR) y una **API REST publica** de solo lectura que alimenta
el frontend de la vitrina.

> **Sobre los datos:** todo lo que se ve en la demo es contenido de demostracion generado
> para este repositorio: productos, precios, recetas, ventas y usuarios. **No hay datos
> reales de operacion ni informacion de clientes.** Las credenciales de mas abajo son
> publicas a proposito.

---

## Demo en Vivo

El sistema esta deployado en Render y disponible para visualizacion inmediata:

| | URL |
|---|-----|
| **Panel Admin** | [https://malva-pastry-backend.onrender.com/login](https://malva-pastry-backend.onrender.com/login) |
| **API REST** | [https://malva-pastry-backend.onrender.com/api/v1/products](https://malva-pastry-backend.onrender.com/api/v1/products) |

### Credenciales de acceso (demo)

Las mismas credenciales sirven en la demo desplegada y en un entorno local levantado con el perfil `dev`.

| Email | Password | Rol | Notas |
|-------|----------|-----|-------|
| `admin@malva.com` | `admin123` | Admin | Acceso completo, incluida la gestion de usuarios |
| `employee@malva.com` | `employee123` | Employee | Acceso limitado: no gestiona usuarios |

### Datos de demo

El seed carga un catalogo completo y operable, no solo el catalogo publico:

| Entidad | Cantidad | Detalle |
|---------|----------|---------|
| Categorias | 10 | Pasteles, cupcakes, pan dulce mexicano, veganos, temporada... |
| Ingredientes | 75 | Con costo unitario y unidad de medida |
| Productos | 50 | Todos visibles, con imagen `.webp` y categoria |
| Recetas | 297 lineas | Los 50 productos tienen receta; el costo de insumos queda entre el 20% y el 43% del precio |
| Tags / Secciones | 14 / 6 | Con 84 y 35 asociaciones a productos |
| Ventas | 42 | Repartidas en los ultimos 45 dias, con ventas del dia actual |
| Insumos de venta | 247 | Snapshot de costos congelado al momento de cada venta |

Gracias a las recetas y ventas, el dashboard, el calculo de costos y el detalle de venta muestran datos reales desde el primer arranque.

> **Nota:** Render Free Tier puede tardar ~1 minuto (se muestra página de carga de carga) en el primer request si el servicio esta inactivo. 

### Endpoints publicos de la API

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/v1/products` | Productos visibles (paginado, filtros por nombre y categoria) |
| GET | `/api/v1/products/{id}` | Detalle de producto con categoria y tags |
| GET | `/api/v1/categories` | Categorias activas (paginado) |
| GET | `/api/v1/categories/{id}` | Detalle de categoria |
| GET | `/api/v1/tags` | Tags activos |
| GET | `/api/v1/sections` | Secciones de vitrina con productos |
| GET | `/api/v1/sections/{slug}` | Seccion por slug con productos |

---

## Que cambio respecto del sistema original

### 1. Plataforma tecnica

| Aspecto             | Sistema original | Version demo   |
| ------------------- | ---------------- | -------------- |
| **Java**            | 17               | **21** (LTS)   |
| **Spring Boot**     | 3.x              | **4.0.1**      |
| **Spring Security** | 6.x              | **7.x**        |
| **Hibernate**       | 6.x              | **7.2**        |
| **Jakarta EE**      | 9                | **11**         |
| **Migraciones**     | Solo Hibernate   | **Flyway**     |

Lo que eso habilito:

- Java 21 (LTS) con Virtual Threads y pattern matching
- Autenticacion reescrita sobre Spring Security 7, con **doble filter chain**: sesion y
  CSRF para el panel, stateless y solo-GET para la API
- Esquema versionado con Flyway en produccion, forward-only y validado por checksum
- API REST publica documentada con OpenAPI/Swagger
- Soporte dual desde un mismo despliegue: panel admin (Thymeleaf) + API publica (JSON)

### 2. Diseno UX/UI

El sistema original resolvia el panel con un tema generico de Bootstrap sobre plantillas
server-side. **En la demo la capa de interfaz se rehizo entera**, no se adapto:

| Aspecto | Version demo |
|---|---|
| **Estilos** | Tailwind CSS 3 **compilado** (`npm run css`), con paleta propia versionada en `tailwind.config.js` |
| **Composicion** | Libreria de **9 archivos de fragments** con **34 fragments nombrados** (tablas, formularios, botones, alertas, paginacion, navegacion, breadcrumbs, iconos, toolbar) |
| **Cobertura** | Las **46 plantillas** del panel (9 de ellas, la propia libreria) construidas sobre un layout unico |
| **Formularios** | **Un unico `form.html` por entidad**: los 6 pares create/edit separados se fusionaron |
| **Navegacion** | Sidebar agrupado por dominio (Inventario / Storefront / Sistema), estado activo derivado del nombre de vista, drawer propio en mobile |
| **JavaScript** | **237 lineas propias, sin ninguna libreria de terceros** (confirmaciones, drawer mobile, modales, formularios dinamicos) |
| **Regresiones** | **15 clases de test de renderizado** que fallan si una plantilla rompe: rutas inexistentes, propiedades del modelo que no existen, formato de moneda, invariantes de fragments |

Dos decisiones del pipeline de assets, tomadas durante el desarrollo de la demo, que no
son cosmeticas:

- **Salio el Tailwind Play CDN.** Compilaba las clases en el navegador en cada carga y
  obligaba a un `<script>` inline; el CSS ahora es un archivo estatico y el panel puede
  servirse con una CSP sin `unsafe-inline`.
- **Salio Flowbite.** Se usaba solo para el drawer mobile, que hoy resuelve
  `/js/admin.js` en ~40 lineas.

---

## Arquitectura del Sistema

```
+---------------------------------------------------------------+
|                     MALVA PASTRY SHOP                         |
+-----------------------------+---------------------------------+
|   PANEL ADMIN               |   API PUBLICA                   |
|   (Thymeleaf + Sesion)      |   (REST JSON, solo lectura)     |
|   /login, /dashboard        |   /api/v1/products              |
|   /products, /categories    |   /api/v1/categories            |
|   /ingredients, /sales      |   /api/v1/tags                  |
|   /tags, /sections, /users  |   /api/v1/sections              |
+-----------------------------+---------------------------------+
|            Spring Boot 4.0 + Spring Security 7                |
+---------------------------------------------------------------+
|   PostgreSQL  |  Flyway (prod)  |  Hibernate ddl-auto (dev)   |
+---------------------------------------------------------------+
```

---

## Stack Tecnologico

### Backend
- **Framework:** Spring Boot 4.0.1
- **Lenguaje:** Java 21
- **Seguridad:** Spring Security 7
- **ORM:** Hibernate 7.2 / Spring Data JPA
- **Validacion:** Jakarta Validation
- **Migraciones:** Flyway (produccion)

### Frontend (Panel Admin)
- **Motor de plantillas:** Thymeleaf 3.x
- **Layout:** Thymeleaf Layout Dialect 3.3
- **Estilos:** Tailwind CSS 3 compilado (`npm run css` en `backend/`, salida en
  `static/css/admin.css`; el Dockerfile lo regenera al construir la imagen)
- **JavaScript:** sin dependencias externas (`static/js/`)

### Base de Datos
- **RDBMS:** PostgreSQL 13+

---

## Inicio Rapido

### Prerrequisitos

- Java 21+
- PostgreSQL 13+
- Maven 3.9+ (o usar el wrapper incluido)

### 1. Clonar el Repositorio

```bash
git clone https://github.com/ErnestoCafre/pastry-back-java21.git
cd pastry-back-java21/backend
```

### 2. Configurar Base de Datos

```sql
CREATE DATABASE malva_pastry_db;
```

### 3. Configurar Credenciales

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/malva_pastry_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
```

### 4. Ejecutar la Aplicacion

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### 5. Acceder al Sistema

- **Panel Admin:** http://localhost:8080/login
- **API REST:** http://localhost:8080/api/v1/products

---

## Documentacion

- **[backend/README.md](backend/README.md)** - Funcionalidades por modulo, roles y permisos, soft delete, snapshots de venta, API
- **[backend/ARCHITECTURE.md](backend/ARCHITECTURE.md)** - Arquitectura del sistema, bounded contexts, patrones de diseno, diagramas ER
- **[scripts/README.md](scripts/README.md)** - Keep-alive del despliegue en Render Free y sus limites

---

## Estructura del Proyecto

```
src/main/java/com/malva_pastry_shop/backend/
+-- config/                     # Configuracion
|   +-- SecurityConfig.java     # Spring Security 7 (dual chain)
|   +-- CorsConfig.java         # CORS para API
|   +-- OpenApiConfig.java      # Swagger/OpenAPI
|   +-- GlobalBindingAdvice.java # Trim global de strings en formularios
|   +-- DataSeeder.java         # Datos iniciales (solo dev)
|
+-- controller/                 # Capa de Presentacion
|   +-- admin/                  # Controladores MVC (Thymeleaf)
|   +-- api/                    # Controladores REST (JSON)
|
+-- domain/                     # Capa de Dominio (DDD)
|   +-- inventory/              # Contexto: Catalogo interno
|   +-- storefront/             # Contexto: Vitrina publica
|   +-- sales/                  # Contexto: Ventas
|   +-- auth/                   # Contexto: Autenticacion
|   +-- common/                 # Entidades Base
|
+-- dto/
|   +-- request/                # DTOs de entrada (Lombok classes)
|   +-- response/
|       +-- api/                # DTOs de salida para API (Java records)
|
+-- repository/                 # Spring Data JPA Repositories
+-- service/
|   +-- inventory/              # ProductService, CategoryService, IngredientService
|   +-- storefront/             # StorefrontSectionService, TagService
|   +-- sales/                  # SaleService
|   +-- UserService
|
+-- util/
|   +-- SlugUtil.java               # Slugs URL-friendly derivados del nombre
|   +-- ImageUrlResolver.java       # Resuelve URLs de imagen absolutas para la API

src/main/resources/
+-- application.properties          # Config dev (ddl-auto=create, Flyway off)
+-- application-prod.properties     # Config prod (ddl-auto=none, Flyway on)
+-- db/migration/
|   +-- V1__create_schema.sql       # Schema inicial
|   +-- V2__seed_roles_and_admin.sql # Roles (ADMIN, EMPLOYEE) + usuario sysadmin
|   +-- V3__drop_public_user_tables.sql # Elimina tablas de usuario publico no usadas
|   +-- V4__remove_system_admin_and_user_role.sql # Elimina rol USER + columna system_admin
|   +-- V5__remove_sysadmin_user.sql # Elimina sysadmin (redundante con admin@malva.com)
|   +-- V6__unique_product_ingredient.sql # UNIQUE (product_id, ingredient_id) en recetas
|   +-- R__seed_demo_data.sql       # Datos de demo (repeatable migration)
+-- templates/                      # Vistas Thymeleaf
+-- static/                         # CSS, JS, imagenes
```

### Bounded Contexts (DDD)

| Contexto | Paquete | Entidades | Descripcion |
|----------|---------|-----------|-------------|
| **Inventory** | `domain/inventory/` | Product, Category, Ingredient, ProductIngredient, UnitOfMeasure | Catalogo interno, recetas y costos |
| **Storefront** | `domain/storefront/` | StorefrontSection, StorefrontSectionProduct, Tag, ProductTag | Vitrina publica, secciones y etiquetas |
| **Sales** | `domain/sales/` | Sale, SaleIngredient | Registro de ventas con snapshots |
| **Auth** | `domain/auth/` | User, Role, RoleType | Autenticacion y autorizacion interna |

---

## Caracteristicas Principales

- Panel de administracion completo (SSR con Thymeleaf)
- CRUD de Productos, Categorias, Tags, Ingredientes
- Gestion de Secciones de Vitrina con ordenamiento
- Sistema de etiquetas con slugs URL-friendly
- Soft-delete con papelera y capacidad de restauracion
- Gestion de recetas (Product-Ingredient con costos)
- Registro de Ventas con snapshot de precios e ingredientes
- API REST publica para catalogo (Productos, Secciones, Categorias, Tags)
- Autenticacion basada en roles (ADMIN, EMPLOYEE)
- Migraciones de base de datos con Flyway (produccion)
- Documentacion OpenAPI/Swagger
- Entidades base con timestamps y auditoria

---

## Seguridad

### Panel de Administracion (Thymeleaf)
- Autenticacion basada en sesion con formulario de login
- CSRF protection habilitado
- Roles: `ADMIN` (acceso completo), `EMPLOYEE` (acceso limitado)
- Gestion de usuarios restringida a ADMIN

### API Publica (REST)
- Endpoints de solo lectura, sin autenticacion requerida
- Solo permite metodo GET
- CORS configurado para frontend
- Cualquier otro metodo HTTP es denegado

---
