# Malva Pastry Shop - Backend

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-13+-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Tailwind%20CSS-CDN-06B6D4?style=for-the-badge&logo=tailwindcss" alt="Tailwind CSS">
</p>

Sistema de gestion integral para una pasteleria artesanal. Permite administrar el catalogo de productos, recetas con costos de ingredientes, ventas con calculo de margen de ganancia, y una vitrina publica organizada por secciones y etiquetas.

El sistema expone dos interfaces:
- **Panel de Administracion** (Thymeleaf SSR): gestion interna del negocio con autenticacion por sesion
- **API REST publica** (JSON, solo lectura): endpoints para alimentar un frontend de clientes

---

## Funcionalidades del Sistema

### Inventario

- **Productos**: CRUD completo con nombre, descripcion, precio base, dias de preparacion, imagen, visibilidad y categoria asociada. Cada producto puede tener una receta de ingredientes y multiples tags.
- **Categorias**: Agrupacion logica de productos (ej: Tortas, Galletas, Panes). Cada categoria muestra cuantos productos contiene.
- **Ingredientes**: Registro de materias primas con costo unitario y unidad de medida. Se soportan 14 unidades: gramos, kilogramos, miligramos, libras, onzas, mililitros, litros, tazas, cucharadas, cucharaditas, unidades, docenas, paquetes y piezas.
- **Recetas**: Asociacion producto-ingrediente con cantidades. Permite calcular el costo total de produccion de un producto sumando `cantidad * costoUnitario` de cada ingrediente.
- **Ventas**: Registro de ventas con datos opcionales de cliente (nombre, DNI, telefono). Calcula margen de ganancia (monto total - costo de ingredientes). Soporta filtros por nombre de producto y rango de fechas.

### Storefront (Vitrina publica)

- **Tags**: Etiquetas para clasificar productos (ej: "Sin Gluten", "Vegano", "Bestseller"). Generan slugs URL-friendly automaticamente. Se pueden asociar y desasociar de productos.
- **Secciones**: Agrupaciones de productos para la vitrina publica (ej: "Destacados", "Nuevos"). Cada seccion tiene orden de visualizacion configurable, visibilidad on/off, y productos asociados con orden propio dentro de la seccion.

### Sistema

- **Usuarios**: Gestion de cuentas del panel de administracion. Crear, editar, habilitar/deshabilitar cuentas. Los usuarios System Admin estan protegidos contra modificacion.

---

## Roles y Permisos

Control de acceso basado en roles (RBAC) con dos niveles operativos:

| Funcionalidad | ADMIN | EMPLOYEE |
|---|:---:|:---:|
| Dashboard con metricas (productos, categorias, ingredientes, ventas del dia, ingresos) | Si | Si |
| CRUD de productos, categorias, ingredientes | Si | Si |
| Gestionar recetas y tags de productos | Si | Si |
| CRUD de tags y secciones de vitrina | Si | Si |
| Registrar y consultar ventas | Si | Si |
| Ver elementos eliminados (papelera) | Si | No |
| Restaurar elementos desde papelera | Si | No |
| Eliminar permanentemente (hard delete) | Si | No |
| Gestionar usuarios del sistema | Si | No |

Existe un flag `systemAdmin` que protege ciertas cuentas: los usuarios marcados como System Admin no pueden ser editados, deshabilitados ni eliminados por ningun otro usuario.

---

## Soft Delete (Papelera)

Las entidades Product, Category, Ingredient, Tag y StorefrontSection implementan borrado logico:

1. **Soft delete**: marca el registro con `deletedAt` y `deletedBy` sin eliminarlo de la base de datos
2. **Papelera**: los ADMIN pueden ver los elementos eliminados en una vista separada
3. **Restaurar**: los ADMIN pueden reactivar elementos desde la papelera
4. **Hard delete**: los ADMIN pueden eliminar permanentemente desde la papelera

Las ventas (`Sale`) no tienen soft delete ya que son registros historicos inmutables.

---

## Snapshots de Ventas

Al registrar una venta, el sistema captura una fotografia inmutable del estado actual:

- **Nombre del producto** se copia como texto plano (el producto podria renombrarse o eliminarse despues)
- **Precio unitario** se congela al momento de la venta
- **Ingredientes y costos** se copian en `SaleIngredient` con: nombre del ingrediente, cantidad usada (`cantidadReceta * cantidadVendida`), costo unitario y costo total

Esto garantiza que los reportes historicos reflejen los valores reales al momento de cada transaccion, independientemente de cambios posteriores en productos o ingredientes.

---

## API REST Publica

Endpoints de solo lectura, sin autenticacion requerida.

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/v1/products` | Productos visibles, paginado (default 12). Filtros: `name`, `categoryId` |
| GET | `/api/v1/products/{id}` | Detalle de producto con categoria y tags |
| GET | `/api/v1/categories` | Categorias activas, paginado (default 20), ordenadas por nombre |
| GET | `/api/v1/categories/{id}` | Detalle de categoria |
| GET | `/api/v1/tags` | Todos los tags activos, ordenados por nombre |
| GET | `/api/v1/sections` | Secciones visibles con sus productos, ordenadas por displayOrder |
| GET | `/api/v1/sections/{slug}` | Seccion por slug con sus productos |

Respuestas paginadas:
```json
{
  "content": [...],
  "page": 0,
  "size": 12,
  "totalElements": 50,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

## Seguridad

### Panel Admin (Thymeleaf)
- Autenticacion por formulario (email + password, BCrypt)
- Sesiones HTTP con proteccion CSRF
- Rutas protegidas redirigen a `/login`
- `/users/**` requiere rol ADMIN; el resto requiere ADMIN o EMPLOYEE

### API Publica (REST)
- Stateless (sin sesiones), CORS habilitado
- Solo metodo GET permitido; cualquier otro metodo HTTP es denegado
- Sin autenticacion requerida

---

## Modelo de Datos

### Jerarquia de entidades base

```
TimestampedEntity (id, insertedAt, updatedAt)
  +-- SoftDeletableEntity (deletedAt, deletedBy)
  |     +-- Product, Category, Ingredient, Tag, StorefrontSection
  +-- Sale, SaleIngredient, ProductIngredient
```

### Bounded Contexts (DDD)

| Contexto | Entidades | Descripcion |
|----------|-----------|-------------|
| **Inventory** | Product, Category, Ingredient, ProductIngredient, UnitOfMeasure | Catalogo interno, recetas y costos |
| **Storefront** | StorefrontSection, StorefrontSectionProduct, Tag, ProductTag | Vitrina publica, secciones y etiquetas |
| **Sales** | Sale, SaleIngredient | Registro de ventas con snapshots inmutables |
| **Public User** | PublicUser, Favorite, ProductReview, ReviewStatus | Usuarios Google, favoritos y resenas |
| **Auth** | User, Role, RoleType | Autenticacion y autorizacion interna |

### Relaciones clave

- `Product` -> `Category` (N:1): un producto pertenece a una categoria
- `Product` <-> `Ingredient` (N:M via `ProductIngredient`): receta con cantidades
- `Product` <-> `Tag` (N:M via `ProductTag`): etiquetas de clasificacion
- `StorefrontSection` <-> `Product` (N:M via `StorefrontSectionProduct`): productos en vitrina con orden
- `Sale` -> `Product` (N:1, nullable): referencia al producto vendido
- `Sale` -> `SaleIngredient` (1:N): snapshot de ingredientes al momento de la venta

---

## Dashboard

El dashboard muestra metricas en tiempo real:

- Total de productos activos
- Total de categorias activas
- Total de ingredientes activos
- Cantidad de ventas del dia
- Ingreso del dia (suma de montos)
- Ingreso del mes (suma de montos)

Ademas incluye accesos rapidos agrupados por seccion: Inventario, Storefront y Sistema.
