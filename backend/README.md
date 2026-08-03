# Malva Pastry Shop - Backend

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-13+-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Tailwind%20CSS-3.4%20compilado-06B6D4?style=for-the-badge&logo=tailwindcss" alt="Tailwind CSS">
</p>

Sistema de gestion integral para una pasteleria artesanal. Permite administrar el catalogo de productos, recetas con costos de ingredientes, ventas con calculo de margen de ganancia, y una vitrina publica organizada por secciones y etiquetas.

El sistema expone dos interfaces:
- **Panel de Administracion** (Thymeleaf SSR): gestion interna del negocio con autenticacion por sesion
- **API REST publica** (JSON, solo lectura): endpoints para alimentar un frontend de clientes

> **Version demo.** Este repositorio es la reescritura demostrativa de un sistema
> desarrollado como proyecto freelance: se actualizo por completo el stack tecnico y se
> rehizo de cero el diseno UX/UI del panel. Los datos que trae sembrados son de
> demostracion, no de operacion real. El detalle del antes/despues esta en el
> [README principal](../README.md).

Este documento describe **que hace** el sistema. Para **como esta construido** —
bounded contexts, capas, patrones y diagrama ER — ver
[ARCHITECTURE.md](ARCHITECTURE.md).

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

- **Usuarios**: Gestion de cuentas del panel de administracion. Crear, editar, habilitar/deshabilitar cuentas.

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

### URLs de imagen

Las respuestas devuelven la ruta de imagen tal como esta guardada (`/images/...`), valida mientras el frontend consuma desde el mismo origen. Si se define `PUBLIC_BASE_URL`, la API antepone ese origen y devuelve URLs absolutas, para que un frontend en otro dominio pueda cargarlas. Una URL ya absoluta se deja sin tocar.

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

---

## Interfaz del Panel

El panel se rediseno por completo en esta version demo. La capa de vistas no es un tema
de terceros adaptado, sino una libreria propia sobre la que se construyen las 46
plantillas.

### Libreria de fragments

`templates/fragments/` reune 34 fragments nombrados en 9 archivos:

| Archivo | Fragments | Resuelve |
|---|---|---|
| `table.html` | `th`, `thCenter`, `thRight`, `emptyRow`, `emptyBody`, `muted` | Encabezados alineados y estados vacios de los listados |
| `form.html` | `text`, `number`, `textarea`, `select`, `checkbox`, `actions`, `searchField`, `filterField` | Campos con label, error de validacion y filtros |
| `buttons.html` | `link`, `submit`, `iconLink`, `iconSubmit`, `iconDelete`, `action`, `pill` | Acciones de fila y de formulario |
| `alerts.html` | `flash`, `successBox`, `errorBox`, `formErrors` | Mensajes flash y bloques de error |
| `nav.html` | `item`, `group` | Sidebar con estado activo y agrupacion por dominio |
| `breadcrumb.html` | `trail`, `trailDeep`, `back` | Navegacion de vuelta desde fichas y sublistados |
| `pagination.html` | `bar` | Paginado que conserva los filtros activos |
| `icons.html` | `icon`, `solid` | Iconos SVG inline, sin icon font |
| `toolbar.html` | `listBar` | Barra de busqueda, filtros y alta de cada listado |

### Decisiones de la capa de vistas

- **CSS compilado, no CDN.** `npm run css` genera `static/css/admin.css` desde
  `src/main/css/admin.css`. El compilado se commitea a proposito, para que
  `mvn spring-boot:run` y los tests de renderizado funcionen sin instalar Node; el
  Dockerfile lo regenera al construir la imagen, asi que produccion no depende de que
  alguien se haya acordado de correrlo.
- **Un formulario por entidad.** Alta y edicion comparten `form.html`; los 6 pares
  `create`/`edit` separados se fusionaron.
- **JavaScript propio.** 237 lineas en `static/js/`, sin librerias externas:
  confirmaciones `data-confirm`, drawer mobile, modales `<dialog>` nativos y los
  formularios dinamicos de receta y venta.
- **Precedencia de Thymeleaf.** `th:replace` (100) se procesa antes que `sec:authorize`
  (300) y que `th:if`; en el mismo elemento, el segundo atributo no llega a evaluarse.
  Por eso las condiciones van en un `<th:block>` envolvente.

### Tests de renderizado

15 clases de test cubren los invariantes que no fallan al compilar: enlaces a rutas que
ningun controller sirve, propiedades del modelo inexistentes, formato de moneda en es-AR,
paginacion que pierde filtros y contratos de los fragments. Rompen el build antes de que
la pagina rompa en el navegador.
