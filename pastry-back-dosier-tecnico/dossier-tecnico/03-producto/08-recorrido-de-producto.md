# 8. Recorrido de producto — qué hace el sistema

> Los documentos 1–7 explican *cómo está construido* el sistema. Este recorre
> *qué hace* de cara al usuario, agrupado por área funcional. Se distingue
> explícitamente lo **implementado** de lo que está en **roadmap**, para no
> confundir visión con estado real.

Malva Pastry Shop es el sistema de operación diaria de una pastelería
artesanal *más* la fuente de datos de su vitrina online. El personal entra al
panel; el público consume la API desde el sitio de la pastelería.

Se puede recorrer sin instalar nada:
[malva-pastry-backend.onrender.com/login](https://malva-pastry-backend.onrender.com/login)
(credenciales en el README del repositorio, con datos de demo cargados).

## 1. Acceso y usuarios

- **Login por email y contraseña** con sesión; la contraseña se guarda hasheada
  con BCrypt.
- **Dos roles operativos**: `ADMIN` (acceso completo, incluida la gestión de
  usuarios y la papelera) y `EMPLOYEE` (opera el catálogo y registra ventas, sin
  acceso a usuarios ni a la papelera).
- **Gestión de usuarios** (solo ADMIN): alta, edición, cambio de rol y
  **activar/desactivar** una cuenta sin borrarla — un empleado que se va deja de
  entrar pero su historial de ventas y de creaciones sigue atribuido.
- **La UI se adapta al rol**: un empleado no ve las opciones que le darían un
  403. Es comodidad; el control real está en el servidor.

## 2. Catálogo e inventario

El núcleo del dominio y lo que diferencia al sistema de un ABM genérico.

- **Productos** con nombre, descripción, precio base, días de preparación,
  imagen, categoría y un flag **`visible`** que decide si aparece en la vitrina
  pública. Un producto puede existir en el sistema sin estar publicado.
- **Categorías** para organizar el catálogo interno, con su vista de productos
  asociados.
- **Ingredientes** con **costo unitario** y **unidad de medida** (14 unidades de
  peso, volumen y conteo). Son la base del costeo.
- **Papelera con restauración** en las cinco entidades de catálogo (productos,
  categorías, ingredientes, tags, secciones): borrar es reversible, muestra
  **quién** borró y cuándo, y el borrado permanente está reservado a un
  administrador — con una guarda que lo impide si el elemento tiene ventas o
  asociaciones, explicando cuántas.

## 3. Recetas y costeo

- **Receta por producto**: se agregan ingredientes con su cantidad desde una
  pantalla dedicada (`/products/{id}/recipe`), con edición de cantidad y
  eliminación de línea.
- **Costo calculado**: el sistema suma `cantidad × costo unitario` de cada
  ingrediente y devuelve el costo real del producto. Contra su precio base, el
  margen deja de ser una intuición.
- **Validación en el borde**: la cantidad se valida contra la precisión real de
  la columna (hasta 4 decimales, hasta 10 dígitos enteros) **antes** de intentar
  guardarla, de modo que el usuario recibe un mensaje y no una pantalla de
  error.

En los datos de demo esto se ve funcionando: 50 productos con 297 líneas de
receta, con el costo de insumos entre el 20 % y el 43 % del precio de venta.

## 4. Ventas

- **Registro de venta**: producto, cantidad, precio unitario, notas y datos
  opcionales del cliente (nombre, documento, teléfono). El total se calcula y se
  valida contra los límites de la columna.
- **Snapshot automático de insumos**: al registrar la venta, el sistema expande
  la receta del producto y guarda una línea por ingrediente con la cantidad
  consumida y su costo **del momento**. Esa foto no cambia nunca más.
- **Detalle de venta** con el desglose de insumos y su costo total: se puede ver
  el margen real de una venta concreta, meses después, aunque el precio de la
  harina haya cambiado tres veces.
- **Listado con filtros combinables** por rango de fechas y por nombre de
  producto, con el **total facturado** del subconjunto filtrado.
- **La venta no se edita ni se borra.** Es una decisión de diseño, no una
  funcionalidad faltante: un registro histórico no se corrige
  (ver [patrones](../01-arquitectura-e-ingenieria/02-patrones-de-diseno.md#snapshot-en-el-registro-de-venta)).

## 5. Dashboard

Pantalla de entrada al panel, con los indicadores de la operación:

- Totales de productos, categorías e ingredientes activos.
- **Ventas del día** y **ingreso del día**.
- **Ingreso del mes** en curso.

Con el seed de demo cargado —42 ventas repartidas en 45 días, incluidas ventas
del día actual— el dashboard muestra datos reales desde el primer arranque, sin
tener que registrar nada a mano.

## 6. Vitrina: secciones y etiquetas

La vitrina no es un sistema aparte: es un conjunto de **decisiones de
exhibición** sobre el catálogo interno.

- **Secciones de vitrina** ("Destacados", "Temporada"…) con slug URL-friendly
  derivado del nombre, descripción, orden y visibilidad.
- **Productos dentro de cada sección**, con **orden de exhibición** que se
  autoasigna al agregar (siguiente número disponible) y se puede reordenar a
  mano.
- **Tags** con slug propio, aplicables a productos desde el producto o desde el
  tag, para filtrar y agrupar en el frontend.
- **Un tag en uso no se puede eliminar permanentemente** sin desasociarlo antes:
  la misma guarda de integridad que protege al resto del catálogo.

## 7. API pública

Siete endpoints de solo lectura (`GET`) que alimentan el frontend de la vitrina:

| Método | Endpoint | Devuelve |
|---|---|---|
| GET | `/api/v1/products` | Productos visibles, paginados, con filtros opcionales por nombre y/o categoría |
| GET | `/api/v1/products/{id}` | Detalle del producto con su categoría y sus tags |
| GET | `/api/v1/categories` | Categorías activas, paginadas |
| GET | `/api/v1/categories/{id}` | Detalle de categoría |
| GET | `/api/v1/tags` | Tags activos |
| GET | `/api/v1/sections` | Secciones de vitrina con sus productos |
| GET | `/api/v1/sections/{slug}` | Sección por slug, con sus productos |

Cuatro propiedades del contrato, todas deliberadas:

- **Solo lo publicado.** El filtro de visibilidad y de soft-delete está en la
  consulta: un producto oculto devuelve 404, no 403 — no revela ni su
  existencia.
- **Solo DTOs**, nunca entidades: un campo interno nuevo no se filtra al público
  por serializarse sin querer.
- **Documentada en OpenAPI/Swagger**, generada desde las anotaciones de los
  controladores y acotada a `/api/v1/**`.
- **URLs de imagen absolutas cuando corresponde**: si el frontend vive en otro
  dominio, la API antepone el origen del backend para que las imágenes carguen
  (ver [caso 4](09-casos-de-estudio.md)).

## 8. Operación y demo

- **Desplegado en Render** con Docker y PostgreSQL administrado; el merge a
  `main` dispara el deploy.
- **Migraciones al arranque**: el esquema de producción lo gobierna Flyway.
- **Datos de demo completos** cargados por migración repetible, con paridad
  exacta respecto del seed de desarrollo — incluidos los hashes de contraseña,
  para que las credenciales del README valgan en los dos entornos.
- **Keep-alive** con GitHub Actions para que el enlace de la demo responda al
  instante dentro de la ventana horaria configurada, en vez de tardar un minuto
  en despertar.

## Roadmap

Lo que el sistema **no** hace hoy, ordenado por lo que más aportaría:

- **Gestión de stock de ingredientes.** Hoy un ingrediente tiene costo y unidad,
  pero no existencias: la venta registra cuánto se consumió, no descuenta de un
  saldo. Es la extensión natural del modelo y la que más cambiaría la operación
  diaria.
- **Reportes de rentabilidad.** Los datos ya están —cada venta guarda su precio
  y su costo de insumos—; falta la pantalla que los agregue por producto, por
  período y por margen.
- **Carga de imágenes desde el panel.** Hoy el producto guarda una URL; subir el
  archivo desde el formulario requiere resolver primero dónde se almacena (el
  sistema de archivos de un contenedor efímero no sirve).
- **Cobertura de tests sobre los servicios de negocio** y un **pipeline de CI**
  que los ejecute. Es un ítem de producto, no solo de ingeniería: sin él, cada
  funcionalidad nueva de esta lista es más riesgosa que la anterior
  (ver [4. Calidad e ingeniería](../01-arquitectura-e-ingenieria/04-calidad-e-ingenieria.md#gaps-declarados)).
- **Frontend de la vitrina.** La API existe y está documentada; el sitio público
  que la consume es el proyecto complementario.

Para el detalle de ingeniería detrás de varias de estas funciones —snapshot de
venta, soft-delete, guardas de integridad— ver
[2. Patrones de diseño](../01-arquitectura-e-ingenieria/02-patrones-de-diseno.md).
