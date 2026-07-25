-- ============================================================
-- V5: Eliminar el usuario sysadmin@malva.com
-- ============================================================
-- Tras eliminar el flag system_admin (V4), sysadmin@malva.com quedo como
-- un ADMIN normal, funcionalmente identico a admin@malva.com. Se elimina
-- por redundante.
--
-- Antes de borrarlo se reasignan al admin las posibles referencias creadas
-- en la demo publica (alguien pudo iniciar sesion como sysadmin y registrar
-- ventas o borrar entidades), para no violar las FKs hacia users.
-- En una BD nueva no hay referencias y los UPDATE son no-ops.
-- ============================================================

DO $$
DECLARE
    sysadmin_id BIGINT;
    admin_id    BIGINT;
BEGIN
    SELECT id INTO sysadmin_id FROM users WHERE email = 'sysadmin@malva.com';
    IF sysadmin_id IS NULL THEN
        RETURN;
    END IF;

    SELECT id INTO admin_id FROM users WHERE email = 'admin@malva.com';

    UPDATE sales               SET registered_by_id = admin_id WHERE registered_by_id = sysadmin_id;
    UPDATE products            SET user_id          = admin_id WHERE user_id          = sysadmin_id;
    UPDATE products            SET deleted_by_id    = admin_id WHERE deleted_by_id    = sysadmin_id;
    UPDATE categories          SET deleted_by_id    = admin_id WHERE deleted_by_id    = sysadmin_id;
    UPDATE ingredients         SET deleted_by_id    = admin_id WHERE deleted_by_id    = sysadmin_id;
    UPDATE tags                SET deleted_by_id    = admin_id WHERE deleted_by_id    = sysadmin_id;
    UPDATE storefront_sections SET deleted_by_id    = admin_id WHERE deleted_by_id    = sysadmin_id;

    DELETE FROM users WHERE id = sysadmin_id;
END $$;
