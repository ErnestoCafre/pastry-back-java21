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

-- Usuario administrador del sistema (demo: sysadmin123)
INSERT INTO users (name, last_name, email, password_hash, enabled, system_admin, role_id, inserted_at, updated_at)
SELECT
    'Administrador',
    'Sistema',
    'sysadmin@malva.com',
    '$2a$10$98ow5cuvjd/mahLwVDL4hejQmnfBwZs4NTQDS6aJVBp8cD7J02ey.',
    TRUE,
    TRUE,
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;
