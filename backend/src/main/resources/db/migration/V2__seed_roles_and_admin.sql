-- ============================================================
-- V2: Datos estructurales de producción
-- Roles y usuario administrador del sistema
-- ============================================================

-- Roles base del sistema
INSERT INTO roles (name, description, inserted_at, updated_at)
VALUES
    ('ADMIN',    'Administrador con acceso completo al sistema',        NOW(), NOW()),
    ('EMPLOYEE', 'Empleado con acceso limitado a gestión de productos', NOW(), NOW()),
    ('USER',     'Usuario básico del sistema',                          NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Usuario administrador del sistema
-- IMPORTANTE: Reemplazar el password_hash antes del primer deploy.
-- Generar con BCryptPasswordEncoder (strength 12):
--   spring security: new BCryptPasswordEncoder(12).encode("TU_PASSWORD_SEGURO")
-- O con htpasswd: htpasswd -nbBC 12 "" TU_PASSWORD | tr -d ':\n' | sed 's/$2y/$2a/'
INSERT INTO users (name, last_name, email, password_hash, enabled, system_admin, role_id, inserted_at, updated_at)
SELECT
    'Administrador',
    'Sistema',
    'sysadmin@malva.com',
    '$2a$12$REEMPLAZAR_CON_HASH_BCRYPT_REAL',
    TRUE,
    TRUE,
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;
