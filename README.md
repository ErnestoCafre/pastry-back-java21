# Malva Pastry Shop - Backend

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-13+-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Thymeleaf-3.x-green?style=for-the-badge&logo=thymeleaf" alt="Thymeleaf">
</p>

## Descripcion

Sistema backend para la gestion de una pasteleria artesanal. Incluye un **panel de administracion** (Thymeleaf SSR) y una **API REST publica** para un futuro frontend React.

Este proyecto es una **refactorizacion completa** de un sistema anterior, modernizando la arquitectura y actualizando todas las dependencias a sus versiones mas recientes.

---

## Demo en Vivo

El sistema esta deployado en Render y disponible para visualizacion inmediata:

| | URL |
|---|-----|
| **Panel Admin** | [https://malva-pastry-backend.onrender.com/login](https://malva-pastry-backend.onrender.com/login) |
| **API REST** | [https://malva-pastry-backend.onrender.com/api/v1/products](https://malva-pastry-backend.onrender.com/api/v1/products) |

### Credenciales de acceso (demo)

| Email | Password | Rol |
|-------|----------|-----|
| `admin@malva.com` | `sysadmin123` | Admin |
| `employee@malva.com` | `sysadmin123` | Employee |

> **Nota:** Render Free Tier puede tardar ~30 segundos en el primer request si el servicio esta inactivo.

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

## Cambios Principales en la Refactorizacion

| Aspecto             | Version Anterior | Version Actual |
| ------------------- | ---------------- | -------------- |
| **Java**            | 17               | **21** (LTS)   |
| **Spring Boot**     | 3.x              | **4.0.1**      |
| **Spring Security** | 6.x              | **7.x**        |
| **Hibernate**       | 6.x              | **7.2**        |
| **Jakarta EE**      | 9                | **11**         |
| **Migraciones**     | Solo Hibernate   | **Flyway**     |

### Mejoras Implementadas

- Migracion a Spring Boot 4.0 con las ultimas mejoras de rendimiento
- Actualizacion a Java 21 con soporte para Virtual Threads y Pattern Matching
- Nuevo sistema de autenticacion con Spring Security 7
- Migraciones de base de datos con Flyway (produccion)
- API REST publica con documentacion OpenAPI/Swagger
- Soporte dual: panel admin (Thymeleaf) + API publica (REST JSON)

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
- **Estilos:** Tailwind CSS (CDN) + Flowbite

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
git clone https://github.com/c97Ernesto/pastry-back-java21.git
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

- **[ARCHITECTURE.md](backend/ARCHITECTURE.md)** - Arquitectura del sistema, patrones de diseno, diagramas ER

---

## Estructura del Proyecto

```
src/main/java/com/malva_pastry_shop/backend/
+-- config/                     # Configuracion
|   +-- SecurityConfig.java     # Spring Security 7 (dual chain)
|   +-- CorsConfig.java         # CORS para API
|   +-- OpenApiConfig.java      # Swagger/OpenAPI
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
|   +-- publicuser/             # Contexto: Usuarios publicos
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
+-- security/                   # JWT y Google OAuth
|
+-- util/

src/main/resources/
+-- application.properties          # Config dev (ddl-auto=create, Flyway off)
+-- application-prod.properties     # Config prod (ddl-auto=none, Flyway on)
+-- db/migration/
|   +-- V1__create_schema.sql       # Schema completo (15 tablas)
|   +-- V2__seed_roles_and_admin.sql # Roles + admin para produccion
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
| **Public User** | `domain/publicuser/` | PublicUser, Favorite, ProductReview, ReviewStatus | Usuarios Google, favoritos y resenas |
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
