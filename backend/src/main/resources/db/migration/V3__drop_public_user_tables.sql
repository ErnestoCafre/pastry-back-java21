-- ============================================================
-- V3: Eliminar el andamiaje "Public User" no implementado
-- ============================================================
-- Las tablas public_users, favorites y product_reviews se crearon en V1
-- para features de storefront (login con Google, favoritos y reseñas) que
-- nunca se implementaron (sin servicios, controladores ni endpoints).
-- Se eliminan junto con el código de dominio/seguridad correspondiente.
--
-- Orden seguro: primero las tablas dependientes (FK -> public_users),
-- luego la tabla padre. CASCADE por robustez ante FKs residuales.
-- ============================================================

DROP TABLE IF EXISTS product_reviews CASCADE;
DROP TABLE IF EXISTS favorites CASCADE;
DROP TABLE IF EXISTS public_users CASCADE;
