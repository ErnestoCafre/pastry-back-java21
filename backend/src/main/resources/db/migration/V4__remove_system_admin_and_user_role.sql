-- ============================================================
-- V4: Eliminar el rol USER y el flag system_admin
-- El rol USER no otorgaba acceso a ninguna vista del panel admin
-- (requiere ADMIN o EMPLOYEE) y el flag system_admin era una
-- autoridad redundante frente a ROLE_ADMIN. Se retira toda su lógica.
-- ============================================================

-- 1. Eliminar usuarios asociados al rol USER (respetar la FK role_id)
DELETE FROM users
WHERE role_id IN (SELECT id FROM roles WHERE name = 'USER');

-- 2. Eliminar el rol USER
DELETE FROM roles WHERE name = 'USER';

-- 3. Eliminar la columna system_admin de users
ALTER TABLE users DROP COLUMN IF EXISTS system_admin;
