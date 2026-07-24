-- ============================================================
-- R__seed_demo_data.sql
-- Datos demo para el sistema Malva Pastry Shop
-- Este archivo es una migración REPETIBLE de Flyway:
--   - Se re-ejecuta automáticamente cada vez que cambia su contenido
--   - Se ejecuta DESPUÉS de todas las migraciones versionadas (V1, V2, ...)
--   - Es seguro modificar este archivo en cualquier momento
-- ============================================================

-- ============================================================
-- 1. LIMPIEZA: Truncar tablas en orden seguro (dependientes primero)
-- ============================================================
TRUNCATE
    storefront_section_products,
    product_tags,
    product_ingredients,
    sale_ingredients,
    sales,
    products,
    categories,
    ingredients,
    tags,
    storefront_sections
CASCADE;

-- ============================================================
-- 2. RESETEAR SECUENCIAS para IDs predecibles desde 1
-- ============================================================
ALTER SEQUENCE categories_id_seq RESTART WITH 1;
ALTER SEQUENCE ingredients_id_seq RESTART WITH 1;
ALTER SEQUENCE products_id_seq RESTART WITH 1;
ALTER SEQUENCE tags_id_seq RESTART WITH 1;
ALTER SEQUENCE storefront_sections_id_seq RESTART WITH 1;
ALTER SEQUENCE product_tags_id_seq RESTART WITH 1;
ALTER SEQUENCE product_ingredients_id_seq RESTART WITH 1;
ALTER SEQUENCE storefront_section_products_id_seq RESTART WITH 1;
ALTER SEQUENCE sales_id_seq RESTART WITH 1;
ALTER SEQUENCE sale_ingredients_id_seq RESTART WITH 1;

-- ============================================================
-- 3. USUARIOS DEMO (adicionales al sysadmin de V2)
-- ============================================================
INSERT INTO users (name, last_name, email, password_hash, enabled, system_admin, role_id, inserted_at, updated_at)
SELECT 'Administrador', 'Demo', 'admin@malva.com',
       '$2a$10$98ow5cuvjd/mahLwVDL4hejQmnfBwZs4NTQDS6aJVBp8cD7J02ey.',
       TRUE, FALSE, r.id, NOW(), NOW()
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

INSERT INTO users (name, last_name, email, password_hash, enabled, system_admin, role_id, inserted_at, updated_at)
SELECT 'Empleado', 'Demo', 'employee@malva.com',
       '$2a$10$98ow5cuvjd/mahLwVDL4hejQmnfBwZs4NTQDS6aJVBp8cD7J02ey.',
       TRUE, FALSE, r.id, NOW(), NOW()
FROM roles r WHERE r.name = 'EMPLOYEE'
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

INSERT INTO users (name, last_name, email, password_hash, enabled, system_admin, role_id, inserted_at, updated_at)
SELECT 'Usuario', 'Demo', 'user@malva.com',
       '$2a$10$98ow5cuvjd/mahLwVDL4hejQmnfBwZs4NTQDS6aJVBp8cD7J02ey.',
       TRUE, FALSE, r.id, NOW(), NOW()
FROM roles r WHERE r.name = 'USER'
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

-- ============================================================
-- 4. CATEGORÍAS (10)
-- ============================================================
INSERT INTO categories (name, description, inserted_at, updated_at) VALUES
('Pasteles',                'Pasteles tradicionales y personalizados para toda ocasión. Desde cumpleaños hasta bodas, elaborados con los mejores ingredientes.', NOW(), NOW()),
('Cupcakes',                'Deliciosos cupcakes artesanales con variedad de sabores y decoraciones únicas. Perfectos para eventos y antojos.', NOW(), NOW()),
('Galletas',                'Galletas horneadas diariamente con recetas tradicionales y creativas. Crujientes por fuera, suaves por dentro.', NOW(), NOW()),
('Pan Dulce Mexicano',      'Auténtico pan dulce mexicano: conchas, cuernos, orejas, polvorones y más. Tradición en cada bocado.', NOW(), NOW()),
('Tartas y Pays',           'Tartas frutales y pays caseros con masas crujientes y rellenos cremosos. Recetas de la abuela.', NOW(), NOW()),
('Postres Individuales',    'Postres gourmet en porciones individuales: mousse, tiramisú, cheesecake y más. Elegancia en cada cucharada.', NOW(), NOW()),
('Panes Artesanales',       'Panes especiales horneados con masa madre y técnicas artesanales. Baguettes, ciabattas y focaccias.', NOW(), NOW()),
('Dulces y Confitería',     'Dulces tradicionales mexicanos y confitería fina: mazapanes, cocadas, jamoncillos y trufas artesanales.', NOW(), NOW()),
('Productos Veganos',       'Opciones veganas sin sacrificar sabor. Pasteles, galletas y postres elaborados sin ingredientes de origen animal.', NOW(), NOW()),
('Temporada y Festividades','Productos especiales para Día de Muertos, Navidad, San Valentín y otras celebraciones. Edición limitada.', NOW(), NOW());

-- ============================================================
-- 5. INGREDIENTES (75)
-- ============================================================

-- Harinas y Bases (8)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Harina de Trigo',       'Harina todo uso para repostería',                      25.00,  'KILOGRAMO', NOW(), NOW()),
('Harina Integral',       'Harina de trigo integral para productos saludables',   32.00,  'KILOGRAMO', NOW(), NOW()),
('Harina de Almendra',    'Harina de almendra para productos sin gluten',        180.00,  'KILOGRAMO', NOW(), NOW()),
('Maicena',               'Fécula de maíz para espesar y hornear',                35.00,  'KILOGRAMO', NOW(), NOW()),
('Harina de Avena',       'Harina de avena para galletas saludables',             45.00,  'KILOGRAMO', NOW(), NOW()),
('Harina de Coco',        'Harina de coco para recetas veganas',                  95.00,  'KILOGRAMO', NOW(), NOW()),
('Polvo para Hornear',    'Polvo de hornear (Royal o similar)',                    85.00,  'KILOGRAMO', NOW(), NOW()),
('Bicarbonato de Sodio',  'Bicarbonato para levado y reacciones químicas',         40.00,  'KILOGRAMO', NOW(), NOW());

-- Azúcares y Endulzantes (7)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Azúcar Blanca',    'Azúcar refinada blanca estándar',             22.00,  'KILOGRAMO', NOW(), NOW()),
('Azúcar Morena',    'Azúcar morena o mascabado',                   28.00,  'KILOGRAMO', NOW(), NOW()),
('Azúcar Glass',     'Azúcar glass (azúcar pulverizada) para decoración', 30.00, 'KILOGRAMO', NOW(), NOW()),
('Miel de Abeja',    'Miel natural de abeja pura',                 120.00,  'KILOGRAMO', NOW(), NOW()),
('Jarabe de Maple',  'Jarabe de arce natural',                     280.00,  'LITRO',     NOW(), NOW()),
('Piloncillo',       'Azúcar de caña sin refinar (panela)',          35.00,  'KILOGRAMO', NOW(), NOW()),
('Stevia',           'Endulzante natural bajo en calorías',        450.00,  'KILOGRAMO', NOW(), NOW());

-- Lácteos (8)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Leche Entera',       'Leche de vaca entera pasteurizada',                  18.00, 'LITRO',     NOW(), NOW()),
('Crema para Batir',   'Crema de leche para batir (35% grasa)',              65.00, 'LITRO',     NOW(), NOW()),
('Mantequilla',        'Mantequilla sin sal para repostería',                95.00, 'KILOGRAMO', NOW(), NOW()),
('Queso Crema',        'Queso crema estilo Philadelphia',                    85.00, 'KILOGRAMO', NOW(), NOW()),
('Leche Condensada',   'Leche condensada azucarada',                         45.00, 'KILOGRAMO', NOW(), NOW()),
('Leche Evaporada',    'Leche evaporada sin azúcar',                         32.00, 'LITRO',     NOW(), NOW()),
('Leche de Almendra',  'Leche vegetal de almendra para productos veganos',   55.00, 'LITRO',     NOW(), NOW()),
('Yogurt Natural',     'Yogurt natural sin azúcar',                          38.00, 'KILOGRAMO', NOW(), NOW());

-- Grasas (4)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Aceite Vegetal',    'Aceite vegetal neutro para hornear',                   35.00, 'LITRO',     NOW(), NOW()),
('Aceite de Oliva',   'Aceite de oliva extra virgen',                        95.00, 'LITRO',     NOW(), NOW()),
('Manteca Vegetal',   'Manteca vegetal para repostería',                     42.00, 'KILOGRAMO', NOW(), NOW()),
('Aceite de Coco',    'Aceite de coco virgen para productos veganos',       120.00, 'KILOGRAMO', NOW(), NOW());

-- Huevos y Proteínas (3)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Huevos',            'Huevos frescos de gallina tamaño grande',              2.50, 'UNIDAD',    NOW(), NOW()),
('Clara de Huevo',    'Clara de huevo pasteurizada líquida',                 75.00, 'LITRO',     NOW(), NOW()),
('Yema de Huevo',     'Yema de huevo pasteurizada',                          95.00, 'LITRO',     NOW(), NOW());

-- Chocolates (6)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Chocolate Amargo',       'Chocolate oscuro 70% cacao',              180.00, 'KILOGRAMO', NOW(), NOW()),
('Chocolate de Leche',     'Chocolate con leche premium',             150.00, 'KILOGRAMO', NOW(), NOW()),
('Chocolate Blanco',       'Chocolate blanco de cobertura',           165.00, 'KILOGRAMO', NOW(), NOW()),
('Cacao en Polvo',         'Cocoa en polvo sin azúcar',                95.00, 'KILOGRAMO', NOW(), NOW()),
('Nutella',                'Crema de avellanas con cacao',             85.00, 'KILOGRAMO', NOW(), NOW()),
('Chispas de Chocolate',   'Chips de chocolate semi-amargo',          125.00, 'KILOGRAMO', NOW(), NOW());

-- Levaduras y Agentes (3)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Levadura Fresca',   'Levadura fresca para panadería',              65.00, 'KILOGRAMO', NOW(), NOW()),
('Levadura Seca',     'Levadura seca instantánea',                  180.00, 'KILOGRAMO', NOW(), NOW()),
('Cremor Tártaro',    'Ácido para estabilizar merengues',           320.00, 'KILOGRAMO', NOW(), NOW());

-- Frutas (10)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Fresas Frescas',  'Fresas frescas de temporada',                    55.00, 'KILOGRAMO', NOW(), NOW()),
('Manzanas',        'Manzanas rojas o verdes para hornear',           35.00, 'KILOGRAMO', NOW(), NOW()),
('Limones',         'Limones frescos amarillos',                      28.00, 'KILOGRAMO', NOW(), NOW()),
('Naranjas',        'Naranjas frescas para jugo y ralladura',         25.00, 'KILOGRAMO', NOW(), NOW()),
('Frambuesas',      'Frambuesas frescas o congeladas',                95.00, 'KILOGRAMO', NOW(), NOW()),
('Moras',           'Moras azules (blueberries)',                     85.00, 'KILOGRAMO', NOW(), NOW()),
('Cerezas',         'Cerezas frescas sin hueso',                     120.00, 'KILOGRAMO', NOW(), NOW()),
('Plátanos',        'Plátanos maduros',                               22.00, 'KILOGRAMO', NOW(), NOW()),
('Piña',            'Piña fresca natural',                            30.00, 'KILOGRAMO', NOW(), NOW()),
('Duraznos',        'Duraznos en almíbar o frescos',                  45.00, 'KILOGRAMO', NOW(), NOW());

-- Frutos Secos y Semillas (8)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Nueces',               'Nueces pecanas o de castilla',              220.00, 'KILOGRAMO', NOW(), NOW()),
('Almendras',            'Almendras naturales sin piel',              185.00, 'KILOGRAMO', NOW(), NOW()),
('Avellanas',            'Avellanas tostadas',                        210.00, 'KILOGRAMO', NOW(), NOW()),
('Coco Rallado',         'Coco deshidratado rallado',                  75.00, 'KILOGRAMO', NOW(), NOW()),
('Pasas',                'Pasas de uva sin semilla',                   65.00, 'KILOGRAMO', NOW(), NOW()),
('Pistaches',            'Pistaches sin cáscara',                     350.00, 'KILOGRAMO', NOW(), NOW()),
('Cacahuates',           'Cacahuates tostados sin sal',                55.00, 'KILOGRAMO', NOW(), NOW()),
('Semillas de Amapola',  'Semillas de amapola para decoración',       180.00, 'KILOGRAMO', NOW(), NOW());

-- Especias y Saborizantes (10)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Extracto de Vainilla',  'Extracto puro de vainilla',               420.00, 'LITRO',     NOW(), NOW()),
('Vainilla en Vaina',     'Vainas de vainilla natural',                8.50, 'UNIDAD',    NOW(), NOW()),
('Canela en Polvo',       'Canela molida de Ceilán',                  95.00, 'KILOGRAMO', NOW(), NOW()),
('Canela en Rama',        'Ramas de canela entera',                  120.00, 'KILOGRAMO', NOW(), NOW()),
('Jengibre en Polvo',     'Jengibre molido',                         110.00, 'KILOGRAMO', NOW(), NOW()),
('Nuez Moscada',          'Nuez moscada molida',                     180.00, 'KILOGRAMO', NOW(), NOW()),
('Sal',                   'Sal fina de mesa',                          8.00, 'KILOGRAMO', NOW(), NOW()),
('Sal de Mar',            'Sal marina en escamas para decoración',    45.00, 'KILOGRAMO', NOW(), NOW()),
('Esencia de Almendra',   'Esencia artificial de almendra',           95.00, 'LITRO',     NOW(), NOW()),
('Azahar',                'Agua de azahar para pan de muerto',        85.00, 'LITRO',     NOW(), NOW());

-- Otros Ingredientes (8)
INSERT INTO ingredients (name, description, unit_cost, unit_of_measure, inserted_at, updated_at) VALUES
('Gelatina Sin Sabor',      'Grenetina en polvo o láminas',          180.00, 'KILOGRAMO', NOW(), NOW()),
('Café Instantáneo',        'Café soluble para saborizante',         120.00, 'KILOGRAMO', NOW(), NOW()),
('Colorante Alimentario',   'Colorantes vegetales variados',          15.00, 'UNIDAD',    NOW(), NOW()),
('Fondant',                 'Fondant para decoración de pasteles',    95.00, 'KILOGRAMO', NOW(), NOW()),
('Mermelada de Fresa',      'Mermelada para rellenos',                55.00, 'KILOGRAMO', NOW(), NOW()),
('Caramelo',                'Caramelo líquido o dulce de leche',      65.00, 'KILOGRAMO', NOW(), NOW()),
('Mazapán',                 'Mazapán para decoración',                85.00, 'KILOGRAMO', NOW(), NOW()),
('Sprinkles',               'Chispas de colores para decoración',    120.00, 'KILOGRAMO', NOW(), NOW());

-- ============================================================
-- 6. PRODUCTOS (50)
-- ============================================================

-- Pasteles (7)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pastel de Chocolate Triple',  'Tres capas de bizcocho de chocolate con ganache, cubierto de chocolate belga',       3, 450.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-chocolate-triple.webp', NOW(), NOW()),
('Pastel Red Velvet',           'Suave bizcocho rojo aterciopelado con frosting de queso crema',                      2, 420.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-red-velvet.webp',       NOW(), NOW()),
('Pastel de Vainilla Clásico',  'Bizcocho esponjoso de vainilla con betún de mantequilla',                           2, 380.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-vainilla-clasico.webp', NOW(), NOW()),
('Pastel de Zanahoria',         'Con nueces, pasas y frosting de queso crema con canela',                            2, 400.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-zanahoria.webp',        NOW(), NOW()),
('Pastel de Fresas con Crema',  'Bizcocho de vainilla con fresas frescas y crema chantilly',                         1, 480.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-fresas-crema.webp',     NOW(), NOW()),
('Pastel Tres Leches',          'Bizcocho bañado en tres tipos de leche con merengue italiano',                      1, 350.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-tres-leches.webp',      NOW(), NOW()),
('Pastel Selva Negra',          'Chocolate, cerezas y crema batida. Receta alemana tradicional',                     3, 520.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-selva-negra.webp',      NOW(), NOW());

-- Cupcakes (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Cupcake de Chocolate',   'Base de chocolate intenso con frosting de chocolate',                    1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-chocolate.webp',   NOW(), NOW()),
('Cupcake Red Velvet',     'Cupcake rojo aterciopelado con queso crema',                            1, 48.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-red-velvet.webp',  NOW(), NOW()),
('Cupcake de Limón',       'Sabor cítrico refrescante con frosting de limón',                       1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-limon.webp',       NOW(), NOW()),
('Cupcake de Nutella',     'Relleno de Nutella con frosting de avellanas',                          1, 55.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-nutella.webp',     NOW(), NOW()),
('Cupcake de Café Moka',   'Para los amantes del café con toque de chocolate',                      1, 50.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-cafe-moka.webp',   NOW(), NOW());

-- Galletas (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Galletas de Chispas de Chocolate', 'Clásicas con chips de chocolate semi-amargo. Docena',         1, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-chispas-chocolate.webp', NOW(), NOW()),
('Galletas de Avena con Pasas',      'Saludables y deliciosas con pasas y canela. Docena',          1, 75.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-avena-pasas.webp',       NOW(), NOW()),
('Galletas de Mantequilla',          'Tradicionales galletas danesas de mantequilla. Docena',       1, 70.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-mantequilla.webp',       NOW(), NOW()),
('Galletas de Jengibre',             'Especiadas y crujientes, perfectas con café. Docena',         1, 80.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-jengibre.webp',          NOW(), NOW()),
('Galletas Snickerdoodle',           'Con canela y azúcar, suaves por dentro. Docena',              1, 78.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-snickerdoodle.webp',     NOW(), NOW());

-- Pan Dulce Mexicano (6)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Concha de Vainilla',       'Pan dulce tradicional con costra de azúcar sabor vainilla',           0, 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/concha-vainilla.webp',     NOW(), NOW()),
('Concha de Chocolate',      'Concha clásica con costra de chocolate',                              0, 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/concha-chocolate.webp',    NOW(), NOW()),
('Cuerno de Mantequilla',    'Esponjoso cuerno bañado en mantequilla y azúcar glass',               0, 20.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/cuerno-mantequilla.webp',  NOW(), NOW()),
('Oreja',                    'Pan hojaldrado caramelizado, crujiente y dulce',                      0, 22.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/oreja.webp',               NOW(), NOW()),
('Polvorón Rosa',            'Galleta de mantequilla que se deshace en la boca',                    0, 15.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/polvoron-rosa.webp',       NOW(), NOW()),
('Garibaldi',                'Panecillo cubierto de chochitos de colores',                          0, 16.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/garibaldi.webp',           NOW(), NOW());

-- Tartas y Pays (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pay de Manzana',          'Manzanas caramelizadas con canela en masa crujiente',                  2, 280.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-manzana.webp',         NOW(), NOW()),
('Pay de Limón',            'Relleno cremoso de limón con merengue italiano',                       2, 260.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-limon.webp',           NOW(), NOW()),
('Pay de Nuez',             'Nueces caramelizadas en base de mantequilla',                          2, 320.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-nuez.webp',            NOW(), NOW()),
('Tarta de Frutos Rojos',  'Crema pastelera con frambuesas, moras y fresas',                       1, 350.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/tarta-frutos-rojos.webp',  NOW(), NOW()),
('Tarta Tatin',             'Tarta francesa invertida de manzana caramelizada',                     2, 300.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/tarta-tatin.webp',         NOW(), NOW());

-- Postres Individuales (6)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Tiramisú Individual',    'Capas de mascarpone, café y bizcocho. Receta italiana',                1, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/tiramisu-individual.webp',  NOW(), NOW()),
('Mousse de Chocolate',    'Mousse sedoso de chocolate belga 70% cacao',                           1, 75.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/mousse-chocolate.webp',     NOW(), NOW()),
('Cheesecake New York',    'Cremoso cheesecake estilo americano con base de galleta',              2, 90.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/cheesecake-new-york.webp',  NOW(), NOW()),
('Panna Cotta',            'Postre italiano de nata con coulis de frutos rojos',                   1, 70.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/panna-cotta.webp',          NOW(), NOW()),
('Crème Brûlée',           'Crema francesa con costra de caramelo crujiente',                      1, 80.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/creme-brulee.webp',         NOW(), NOW()),
('Profiteroles',           'Choux rellenos de crema con chocolate caliente',                       1, 95.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/profiteroles.webp',         NOW(), NOW());

-- Panes Artesanales (4)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Baguette Francesa',     'Pan crujiente de masa madre fermentada 24 horas',                       1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/baguette-francesa.webp',   NOW(), NOW()),
('Ciabatta Italiana',     'Pan rústico italiano con corteza crujiente y miga aireada',             1, 50.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/ciabatta-italiana.webp',   NOW(), NOW()),
('Focaccia de Romero',    'Pan plano italiano con aceite de oliva y romero fresco',                1, 65.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/focaccia-romero.webp',     NOW(), NOW()),
('Pan de Masa Madre',     'Hogaza artesanal fermentada naturalmente 48 horas',                     2, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/pan-masa-madre.webp',      NOW(), NOW());

-- Dulces y Confitería (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Mazapán de Almendra',    'Dulce tradicional de almendra molida. Caja 12 piezas',                 1, 120.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/mazapan-almendra.webp',   NOW(), NOW()),
('Cocadas',                'Dulce de coco rallado caramelizado. Bolsa 250g',                       1,  65.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/cocadas.webp',            NOW(), NOW()),
('Jamoncillo de Leche',    'Dulce de leche tradicional mexicano. Caja 8 piezas',                   1,  80.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/jamoncillo-leche.webp',   NOW(), NOW()),
('Trufas de Chocolate',    'Trufas artesanales de chocolate belga. Caja 6 piezas',                 1, 150.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/trufas-chocolate.webp',   NOW(), NOW()),
('Ate con Queso',          'Dulce de membrillo artesanal con queso manchego. 300g',                1,  95.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/ate-queso.webp',          NOW(), NOW());

-- Productos Veganos (4)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pastel Vegano de Chocolate',  'Sin huevo ni lácteos, igual de delicioso',                        2, 420.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/pastel-vegano-chocolate.webp', NOW(), NOW()),
('Cupcakes Veganos Variados',   'Set de 4 cupcakes veganos de sabores surtidos',                   1, 180.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/cupcakes-veganos.webp',        NOW(), NOW()),
('Galletas Veganas de Avena',   'Sin huevo, con chips de chocolate vegano. Docena',                1,  95.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/galletas-veganas-avena.webp',  NOW(), NOW()),
('Brownie Vegano',              'Brownie húmedo sin ingredientes animales',                        1,  55.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/brownie-vegano.webp',          NOW(), NOW());

-- Temporada y Festividades (3)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pan de Muerto',       'Tradicional pan de temporada con azahar. Disponible Oct-Nov',             1,  65.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/pan-de-muerto.webp',    NOW(), NOW()),
('Rosca de Reyes',      'Con frutas cristalizadas y muñequito. Disponible en Enero',               2, 280.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/rosca-de-reyes.webp',   NOW(), NOW()),
('Tronco de Navidad',   'Bûche de Noël con chocolate y crema de castañas',                        3, 450.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/tronco-navidad.webp',   NOW(), NOW());

-- ============================================================
-- 7. TAGS (14)
-- ============================================================
INSERT INTO tags (name, slug, description, inserted_at, updated_at) VALUES
('Bestseller',     'bestseller',     'Productos más vendidos y favoritos de nuestros clientes',                       NOW(), NOW()),
('Nuevo',          'nuevo',          'Incorporaciones recientes a nuestro menú',                                      NOW(), NOW()),
('Sin Gluten',     'sin-gluten',     'Productos elaborados sin gluten',                                               NOW(), NOW()),
('Vegano',         'vegano',         'Productos 100% libres de ingredientes de origen animal',                        NOW(), NOW()),
('Sin Lácteos',    'sin-lacteos',    'Productos elaborados sin lácteos ni derivados',                                 NOW(), NOW()),
('Bajo en Azúcar', 'bajo-en-azucar', 'Opciones con contenido reducido de azúcar',                                    NOW(), NOW()),
('Con Chocolate',  'con-chocolate',  'Productos que contienen chocolate en su preparación',                           NOW(), NOW()),
('Con Frutas',     'con-frutas',     'Productos elaborados con frutas frescas o naturales',                           NOW(), NOW()),
('Clásico',        'clasico',        'Recetas tradicionales que nunca pasan de moda',                                 NOW(), NOW()),
('Premium',        'premium',        'Selección gourmet con ingredientes de primera calidad',                         NOW(), NOW()),
('Artesanal',      'artesanal',      'Elaborados a mano con técnicas artesanales',                                    NOW(), NOW()),
('De Temporada',   'de-temporada',   'Productos disponibles por tiempo limitado según la época del año',              NOW(), NOW()),
('Para Compartir', 'para-compartir', 'Porciones ideales para disfrutar en grupo',                                     NOW(), NOW()),
('Individual',     'individual',     'Porciones perfectas para una persona',                                          NOW(), NOW());

-- ============================================================
-- 8. PRODUCT_TAGS — Asociaciones producto-tag
-- ============================================================

-- Bestseller (6)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'bestseller'
  AND p.name IN ('Pastel de Chocolate Triple', 'Concha de Vainilla', 'Galletas de Chispas de Chocolate', 'Pay de Manzana', 'Cupcake de Chocolate', 'Pastel Tres Leches');

-- Nuevo (4)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'nuevo'
  AND p.name IN ('Brownie Vegano', 'Cupcakes Veganos Variados', 'Focaccia de Romero', 'Crème Brûlée');

-- Sin Gluten (6)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'sin-gluten'
  AND p.name IN ('Mousse de Chocolate', 'Panna Cotta', 'Crème Brûlée', 'Trufas de Chocolate', 'Cocadas', 'Mazapán de Almendra');

-- Vegano (4)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'vegano'
  AND p.name IN ('Pastel Vegano de Chocolate', 'Cupcakes Veganos Variados', 'Galletas Veganas de Avena', 'Brownie Vegano');

-- Sin Lácteos (4)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'sin-lacteos'
  AND p.name IN ('Pastel Vegano de Chocolate', 'Galletas Veganas de Avena', 'Brownie Vegano', 'Cocadas');

-- Bajo en Azúcar (3)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'bajo-en-azucar'
  AND p.name IN ('Galletas de Avena con Pasas', 'Pan de Masa Madre', 'Baguette Francesa');

-- Con Chocolate (9)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'con-chocolate'
  AND p.name IN ('Pastel de Chocolate Triple', 'Cupcake de Chocolate', 'Galletas de Chispas de Chocolate', 'Mousse de Chocolate', 'Trufas de Chocolate', 'Pastel Selva Negra', 'Cupcake de Nutella', 'Brownie Vegano', 'Concha de Chocolate');

-- Con Frutas (6)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'con-frutas'
  AND p.name IN ('Pastel de Fresas con Crema', 'Tarta de Frutos Rojos', 'Pay de Manzana', 'Tarta Tatin', 'Pay de Limón', 'Pastel de Zanahoria');

-- Clásico (10)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'clasico'
  AND p.name IN ('Concha de Vainilla', 'Concha de Chocolate', 'Cuerno de Mantequilla', 'Oreja', 'Polvorón Rosa', 'Pan de Muerto', 'Rosca de Reyes', 'Pastel de Vainilla Clásico', 'Pastel Tres Leches', 'Galletas de Mantequilla');

-- Premium (7)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'premium'
  AND p.name IN ('Pastel Selva Negra', 'Profiteroles', 'Tiramisú Individual', 'Cheesecake New York', 'Tronco de Navidad', 'Pay de Nuez', 'Trufas de Chocolate');

-- Artesanal (7)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'artesanal'
  AND p.name IN ('Baguette Francesa', 'Ciabatta Italiana', 'Focaccia de Romero', 'Pan de Masa Madre', 'Mazapán de Almendra', 'Cocadas', 'Jamoncillo de Leche');

-- De Temporada (3)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'de-temporada'
  AND p.name IN ('Pan de Muerto', 'Rosca de Reyes', 'Tronco de Navidad');

-- Para Compartir (5)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'para-compartir'
  AND p.name IN ('Rosca de Reyes', 'Pay de Manzana', 'Pay de Nuez', 'Tarta de Frutos Rojos', 'Pastel de Chocolate Triple');

-- Individual (10)
INSERT INTO product_tags (product_id, tag_id, inserted_at, updated_at)
SELECT p.id, t.id, NOW(), NOW()
FROM products p, tags t
WHERE t.slug = 'individual'
  AND p.name IN ('Cupcake de Chocolate', 'Cupcake Red Velvet', 'Cupcake de Limón', 'Cupcake de Nutella', 'Cupcake de Café Moka', 'Tiramisú Individual', 'Mousse de Chocolate', 'Panna Cotta', 'Crème Brûlée', 'Profiteroles');

-- ============================================================
-- 9. STOREFRONT SECTIONS (6)
-- ============================================================
INSERT INTO storefront_sections (name, slug, description, display_order, visible, inserted_at, updated_at) VALUES
('Más Vendidos',        'mas-vendidos',        'Los productos favoritos de nuestros clientes',                    1, TRUE, NOW(), NOW()),
('Novedades',           'novedades',           'Las incorporaciones más recientes a nuestro menú',                2, TRUE, NOW(), NOW()),
('Pasteles Destacados', 'pasteles-destacados', 'Nuestra selección de pasteles más especiales',                    3, TRUE, NOW(), NOW()),
('Postres Premium',     'postres-premium',     'Postres gourmet elaborados con ingredientes de primera',          4, TRUE, NOW(), NOW()),
('Pan Dulce Mexicano',  'pan-dulce-mexicano',  'Auténtico pan dulce de tradición mexicana',                       5, TRUE, NOW(), NOW()),
('Para Compartir',      'para-compartir',      'Porciones ideales para disfrutar en grupo',                       6, TRUE, NOW(), NOW());

-- ============================================================
-- 10. STOREFRONT SECTION PRODUCTS — Asociaciones sección-producto
-- ============================================================

-- Más Vendidos (6 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Pastel de Chocolate Triple',      1),
         ('Concha de Vainilla',              2),
         ('Galletas de Chispas de Chocolate', 3),
         ('Pay de Manzana',                  4),
         ('Cupcake de Chocolate',            5),
         ('Pastel Tres Leches',              6)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'mas-vendidos';

-- Novedades (5 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Brownie Vegano',            1),
         ('Cupcakes Veganos Variados', 2),
         ('Focaccia de Romero',        3),
         ('Crème Brûlée',             4),
         ('Tarta Tatin',              5)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'novedades';

-- Pasteles Destacados (6 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Pastel de Chocolate Triple',  1),
         ('Pastel Red Velvet',           2),
         ('Pastel de Fresas con Crema',  3),
         ('Pastel Tres Leches',          4),
         ('Pastel Selva Negra',          5),
         ('Pastel de Zanahoria',         6)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'pasteles-destacados';

-- Postres Premium (7 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Tiramisú Individual',   1),
         ('Mousse de Chocolate',   2),
         ('Cheesecake New York',   3),
         ('Panna Cotta',           4),
         ('Crème Brûlée',         5),
         ('Profiteroles',          6),
         ('Trufas de Chocolate',   7)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'postres-premium';

-- Pan Dulce Mexicano (6 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Concha de Vainilla',     1),
         ('Concha de Chocolate',    2),
         ('Cuerno de Mantequilla',  3),
         ('Oreja',                  4),
         ('Polvorón Rosa',          5),
         ('Garibaldi',              6)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'pan-dulce-mexicano';

-- Para Compartir (5 productos)
INSERT INTO storefront_section_products (storefront_section_id, product_id, display_order, inserted_at, updated_at)
SELECT s.id, p.id, ord.display_order, NOW(), NOW()
FROM storefront_sections s,
     (VALUES
         ('Rosca de Reyes',              1),
         ('Pay de Manzana',              2),
         ('Pay de Nuez',                 3),
         ('Tarta de Frutos Rojos',       4),
         ('Pastel de Chocolate Triple',  5)
     ) AS ord(product_name, display_order)
JOIN products p ON p.name = ord.product_name
WHERE s.slug = 'para-compartir';

-- ============================================================
-- 11. PRODUCT_INGREDIENTS — Recetas (los 50 productos)
-- ============================================================
-- La cantidad se expresa SIEMPRE en la unidad de medida del ingrediente:
--   0.5000 de 'Harina de Trigo' (KILOGRAMO, $25.00/kg) = 0.5 kg = $12.50
--   6.0000 de 'Huevos'          (UNIDAD,    $2.50/u)   = 6 huevos = $15.00
-- Las recetas estan calibradas para que el costo de ingredientes quede
-- entre el 20% y el 45% del base_price del producto.
INSERT INTO product_ingredients (product_id, ingredient_id, quantity, inserted_at, updated_at)
SELECT p.id, i.id, v.quantity, NOW(), NOW()
FROM (VALUES
    -- Pasteles
    ('Pastel de Chocolate Triple', 'Harina de Trigo',       0.5000),
    ('Pastel de Chocolate Triple', 'Azúcar Blanca',         0.4000),
    ('Pastel de Chocolate Triple', 'Chocolate Amargo',      0.3500),
    ('Pastel de Chocolate Triple', 'Mantequilla',           0.2500),
    ('Pastel de Chocolate Triple', 'Huevos',                6.0000),
    ('Pastel de Chocolate Triple', 'Crema para Batir',      0.3000),
    ('Pastel de Chocolate Triple', 'Cacao en Polvo',        0.0800),

    ('Pastel Red Velvet', 'Harina de Trigo',        0.4500),
    ('Pastel Red Velvet', 'Azúcar Blanca',          0.3500),
    ('Pastel Red Velvet', 'Mantequilla',            0.2000),
    ('Pastel Red Velvet', 'Huevos',                 4.0000),
    ('Pastel Red Velvet', 'Queso Crema',            0.4000),
    ('Pastel Red Velvet', 'Crema para Batir',       0.2000),
    ('Pastel Red Velvet', 'Colorante Alimentario',  1.0000),
    ('Pastel Red Velvet', 'Cacao en Polvo',         0.0300),

    ('Pastel de Vainilla Clásico', 'Harina de Trigo',       0.5000),
    ('Pastel de Vainilla Clásico', 'Azúcar Blanca',         0.4000),
    ('Pastel de Vainilla Clásico', 'Mantequilla',           0.3000),
    ('Pastel de Vainilla Clásico', 'Huevos',                5.0000),
    ('Pastel de Vainilla Clásico', 'Leche Entera',          0.2500),
    ('Pastel de Vainilla Clásico', 'Azúcar Glass',          0.2000),
    ('Pastel de Vainilla Clásico', 'Extracto de Vainilla',  0.0200),
    ('Pastel de Vainilla Clásico', 'Polvo para Hornear',    0.0150),

    ('Pastel de Zanahoria', 'Harina Integral',   0.4000),
    ('Pastel de Zanahoria', 'Azúcar Morena',     0.3500),
    ('Pastel de Zanahoria', 'Aceite Vegetal',    0.2500),
    ('Pastel de Zanahoria', 'Huevos',            4.0000),
    ('Pastel de Zanahoria', 'Nueces',            0.1500),
    ('Pastel de Zanahoria', 'Pasas',             0.1000),
    ('Pastel de Zanahoria', 'Queso Crema',       0.3500),
    ('Pastel de Zanahoria', 'Canela en Polvo',   0.0100),

    ('Pastel de Fresas con Crema', 'Harina de Trigo',   0.4500),
    ('Pastel de Fresas con Crema', 'Azúcar Blanca',     0.3000),
    ('Pastel de Fresas con Crema', 'Huevos',            5.0000),
    ('Pastel de Fresas con Crema', 'Fresas Frescas',    0.6000),
    ('Pastel de Fresas con Crema', 'Crema para Batir',  0.5000),
    ('Pastel de Fresas con Crema', 'Mantequilla',       0.1500),
    ('Pastel de Fresas con Crema', 'Azúcar Glass',      0.1000),

    ('Pastel Tres Leches', 'Harina de Trigo',    0.4000),
    ('Pastel Tres Leches', 'Azúcar Blanca',      0.3000),
    ('Pastel Tres Leches', 'Huevos',             6.0000),
    ('Pastel Tres Leches', 'Leche Condensada',   0.4000),
    ('Pastel Tres Leches', 'Leche Evaporada',    0.3500),
    ('Pastel Tres Leches', 'Leche Entera',       0.3000),
    ('Pastel Tres Leches', 'Crema para Batir',   0.2500),

    ('Pastel Selva Negra', 'Harina de Trigo',    0.4000),
    ('Pastel Selva Negra', 'Azúcar Blanca',      0.3000),
    ('Pastel Selva Negra', 'Chocolate Amargo',   0.3000),
    ('Pastel Selva Negra', 'Cerezas',            0.4000),
    ('Pastel Selva Negra', 'Crema para Batir',   0.5000),
    ('Pastel Selva Negra', 'Huevos',             5.0000),
    ('Pastel Selva Negra', 'Cacao en Polvo',     0.0500),

    -- Cupcakes (receta por pieza)
    ('Cupcake de Chocolate', 'Harina de Trigo',    0.0500),
    ('Cupcake de Chocolate', 'Azúcar Blanca',      0.0400),
    ('Cupcake de Chocolate', 'Chocolate Amargo',   0.0300),
    ('Cupcake de Chocolate', 'Mantequilla',        0.0300),
    ('Cupcake de Chocolate', 'Huevos',             1.0000),
    ('Cupcake de Chocolate', 'Cacao en Polvo',     0.0100),

    ('Cupcake Red Velvet', 'Harina de Trigo',        0.0500),
    ('Cupcake Red Velvet', 'Azúcar Blanca',          0.0400),
    ('Cupcake Red Velvet', 'Queso Crema',            0.0500),
    ('Cupcake Red Velvet', 'Mantequilla',            0.0250),
    ('Cupcake Red Velvet', 'Huevos',                 1.0000),
    ('Cupcake Red Velvet', 'Colorante Alimentario',  0.1000),
    ('Cupcake Red Velvet', 'Cacao en Polvo',         0.0050),

    ('Cupcake de Limón', 'Harina de Trigo',  0.0500),
    ('Cupcake de Limón', 'Azúcar Blanca',    0.0450),
    ('Cupcake de Limón', 'Mantequilla',      0.0350),
    ('Cupcake de Limón', 'Huevos',           1.0000),
    ('Cupcake de Limón', 'Limones',          0.0800),
    ('Cupcake de Limón', 'Azúcar Glass',     0.0400),

    ('Cupcake de Nutella', 'Harina de Trigo',  0.0500),
    ('Cupcake de Nutella', 'Azúcar Blanca',    0.0350),
    ('Cupcake de Nutella', 'Nutella',          0.0600),
    ('Cupcake de Nutella', 'Mantequilla',      0.0300),
    ('Cupcake de Nutella', 'Huevos',           1.0000),
    ('Cupcake de Nutella', 'Avellanas',        0.0200),

    ('Cupcake de Café Moka', 'Harina de Trigo',      0.0500),
    ('Cupcake de Café Moka', 'Azúcar Blanca',        0.0400),
    ('Cupcake de Café Moka', 'Café Instantáneo',     0.0100),
    ('Cupcake de Café Moka', 'Chocolate de Leche',   0.0300),
    ('Cupcake de Café Moka', 'Mantequilla',          0.0300),
    ('Cupcake de Café Moka', 'Huevos',               1.0000),

    -- Galletas (receta por docena)
    ('Galletas de Chispas de Chocolate', 'Harina de Trigo',        0.2500),
    ('Galletas de Chispas de Chocolate', 'Azúcar Morena',          0.1200),
    ('Galletas de Chispas de Chocolate', 'Mantequilla',            0.1000),
    ('Galletas de Chispas de Chocolate', 'Huevos',                 1.0000),
    ('Galletas de Chispas de Chocolate', 'Chispas de Chocolate',   0.1000),
    ('Galletas de Chispas de Chocolate', 'Bicarbonato de Sodio',   0.0050),

    ('Galletas de Avena con Pasas', 'Harina de Avena',   0.2000),
    ('Galletas de Avena con Pasas', 'Harina de Trigo',   0.1000),
    ('Galletas de Avena con Pasas', 'Azúcar Morena',     0.1000),
    ('Galletas de Avena con Pasas', 'Mantequilla',       0.0900),
    ('Galletas de Avena con Pasas', 'Huevos',            1.0000),
    ('Galletas de Avena con Pasas', 'Pasas',             0.0800),
    ('Galletas de Avena con Pasas', 'Canela en Polvo',   0.0050),

    ('Galletas de Mantequilla', 'Harina de Trigo',        0.2500),
    ('Galletas de Mantequilla', 'Mantequilla',            0.1500),
    ('Galletas de Mantequilla', 'Azúcar Glass',           0.0800),
    ('Galletas de Mantequilla', 'Huevos',                 1.0000),
    ('Galletas de Mantequilla', 'Extracto de Vainilla',   0.0050),
    ('Galletas de Mantequilla', 'Sal',                    0.0020),

    ('Galletas de Jengibre', 'Harina de Trigo',        0.2500),
    ('Galletas de Jengibre', 'Piloncillo',             0.1000),
    ('Galletas de Jengibre', 'Mantequilla',            0.1000),
    ('Galletas de Jengibre', 'Jengibre en Polvo',      0.0100),
    ('Galletas de Jengibre', 'Canela en Polvo',        0.0080),
    ('Galletas de Jengibre', 'Huevos',                 1.0000),
    ('Galletas de Jengibre', 'Bicarbonato de Sodio',   0.0050),

    ('Galletas Snickerdoodle', 'Harina de Trigo',    0.2500),
    ('Galletas Snickerdoodle', 'Azúcar Blanca',      0.1500),
    ('Galletas Snickerdoodle', 'Mantequilla',        0.1200),
    ('Galletas Snickerdoodle', 'Huevos',             1.0000),
    ('Galletas Snickerdoodle', 'Canela en Polvo',    0.0100),
    ('Galletas Snickerdoodle', 'Cremor Tártaro',     0.0040),

    -- Pan Dulce Mexicano (receta por pieza)
    ('Concha de Vainilla', 'Harina de Trigo',        0.0800),
    ('Concha de Vainilla', 'Azúcar Blanca',          0.0300),
    ('Concha de Vainilla', 'Mantequilla',            0.0200),
    ('Concha de Vainilla', 'Huevos',                 0.5000),
    ('Concha de Vainilla', 'Levadura Fresca',        0.0030),
    ('Concha de Vainilla', 'Extracto de Vainilla',   0.0010),

    ('Concha de Chocolate', 'Harina de Trigo',    0.0800),
    ('Concha de Chocolate', 'Azúcar Blanca',      0.0300),
    ('Concha de Chocolate', 'Mantequilla',        0.0200),
    ('Concha de Chocolate', 'Huevos',             0.5000),
    ('Concha de Chocolate', 'Levadura Fresca',    0.0030),
    ('Concha de Chocolate', 'Cacao en Polvo',     0.0080),

    ('Cuerno de Mantequilla', 'Harina de Trigo',    0.0900),
    ('Cuerno de Mantequilla', 'Mantequilla',        0.0400),
    ('Cuerno de Mantequilla', 'Azúcar Glass',       0.0100),
    ('Cuerno de Mantequilla', 'Huevos',             0.5000),
    ('Cuerno de Mantequilla', 'Levadura Fresca',    0.0030),
    ('Cuerno de Mantequilla', 'Sal',                0.0010),

    ('Oreja', 'Harina de Trigo',   0.0800),
    ('Oreja', 'Mantequilla',       0.0500),
    ('Oreja', 'Azúcar Blanca',     0.0400),
    ('Oreja', 'Sal',               0.0010),

    ('Polvorón Rosa', 'Harina de Trigo',        0.0600),
    ('Polvorón Rosa', 'Manteca Vegetal',        0.0300),
    ('Polvorón Rosa', 'Azúcar Glass',           0.0300),
    ('Polvorón Rosa', 'Colorante Alimentario',  0.0200),
    ('Polvorón Rosa', 'Huevos',                 0.2500),

    ('Garibaldi', 'Harina de Trigo',       0.0600),
    ('Garibaldi', 'Azúcar Blanca',         0.0250),
    ('Garibaldi', 'Mantequilla',           0.0200),
    ('Garibaldi', 'Huevos',                0.5000),
    ('Garibaldi', 'Mermelada de Fresa',    0.0150),
    ('Garibaldi', 'Sprinkles',             0.0050),

    -- Tartas y Pays
    ('Pay de Manzana', 'Harina de Trigo',    0.3500),
    ('Pay de Manzana', 'Mantequilla',        0.2000),
    ('Pay de Manzana', 'Azúcar Blanca',      0.1500),
    ('Pay de Manzana', 'Azúcar Morena',      0.0500),
    ('Pay de Manzana', 'Manzanas',           0.8000),
    ('Pay de Manzana', 'Canela en Polvo',    0.0100),
    ('Pay de Manzana', 'Huevos',             1.0000),

    ('Pay de Limón', 'Harina de Trigo',     0.3000),
    ('Pay de Limón', 'Mantequilla',         0.1800),
    ('Pay de Limón', 'Leche Condensada',    0.4000),
    ('Pay de Limón', 'Limones',             0.3500),
    ('Pay de Limón', 'Huevos',              4.0000),
    ('Pay de Limón', 'Azúcar Blanca',       0.1500),

    ('Pay de Nuez', 'Harina de Trigo',   0.3000),
    ('Pay de Nuez', 'Mantequilla',       0.1800),
    ('Pay de Nuez', 'Nueces',            0.3500),
    ('Pay de Nuez', 'Azúcar Morena',     0.2000),
    ('Pay de Nuez', 'Huevos',            3.0000),
    ('Pay de Nuez', 'Miel de Abeja',     0.0500),

    ('Tarta de Frutos Rojos', 'Harina de Trigo',   0.2500),
    ('Tarta de Frutos Rojos', 'Mantequilla',       0.1500),
    ('Tarta de Frutos Rojos', 'Frambuesas',        0.2500),
    ('Tarta de Frutos Rojos', 'Moras',             0.2500),
    ('Tarta de Frutos Rojos', 'Fresas Frescas',    0.2000),
    ('Tarta de Frutos Rojos', 'Leche Entera',      0.3000),
    ('Tarta de Frutos Rojos', 'Yema de Huevo',     0.0800),
    ('Tarta de Frutos Rojos', 'Azúcar Blanca',     0.1200),

    ('Tarta Tatin', 'Harina de Trigo',   0.2500),
    ('Tarta Tatin', 'Mantequilla',       0.2000),
    ('Tarta Tatin', 'Manzanas',          0.9000),
    ('Tarta Tatin', 'Azúcar Blanca',     0.2500),
    ('Tarta Tatin', 'Caramelo',          0.1000),

    -- Postres Individuales
    ('Tiramisú Individual', 'Harina de Trigo',     0.0500),
    ('Tiramisú Individual', 'Queso Crema',         0.1200),
    ('Tiramisú Individual', 'Café Instantáneo',    0.0100),
    ('Tiramisú Individual', 'Huevos',              2.0000),
    ('Tiramisú Individual', 'Azúcar Blanca',       0.0500),
    ('Tiramisú Individual', 'Crema para Batir',    0.0800),
    ('Tiramisú Individual', 'Cacao en Polvo',      0.0080),

    ('Mousse de Chocolate', 'Chocolate Amargo',      0.0800),
    ('Mousse de Chocolate', 'Crema para Batir',      0.1200),
    ('Mousse de Chocolate', 'Huevos',                2.0000),
    ('Mousse de Chocolate', 'Azúcar Blanca',         0.0300),
    ('Mousse de Chocolate', 'Gelatina Sin Sabor',    0.0030),

    ('Cheesecake New York', 'Queso Crema',            0.2000),
    ('Cheesecake New York', 'Azúcar Blanca',          0.0800),
    ('Cheesecake New York', 'Huevos',                 2.0000),
    ('Cheesecake New York', 'Crema para Batir',       0.1000),
    ('Cheesecake New York', 'Mantequilla',            0.0400),
    ('Cheesecake New York', 'Harina de Trigo',        0.0600),
    ('Cheesecake New York', 'Extracto de Vainilla',   0.0040),

    ('Panna Cotta', 'Crema para Batir',      0.2000),
    ('Panna Cotta', 'Leche Entera',          0.1000),
    ('Panna Cotta', 'Azúcar Blanca',         0.0500),
    ('Panna Cotta', 'Gelatina Sin Sabor',    0.0050),
    ('Panna Cotta', 'Frambuesas',            0.0600),
    ('Panna Cotta', 'Vainilla en Vaina',     0.5000),

    ('Crème Brûlée', 'Crema para Batir',    0.2200),
    ('Crème Brûlée', 'Yema de Huevo',       0.0600),
    ('Crème Brûlée', 'Azúcar Blanca',       0.0600),
    ('Crème Brûlée', 'Vainilla en Vaina',   1.0000),

    ('Profiteroles', 'Harina de Trigo',     0.0800),
    ('Profiteroles', 'Mantequilla',         0.0600),
    ('Profiteroles', 'Huevos',              3.0000),
    ('Profiteroles', 'Leche Entera',        0.1500),
    ('Profiteroles', 'Chocolate Amargo',    0.0600),
    ('Profiteroles', 'Crema para Batir',    0.1200),
    ('Profiteroles', 'Azúcar Blanca',       0.0300),

    -- Panes Artesanales
    ('Baguette Francesa', 'Harina de Trigo',    0.4000),
    ('Baguette Francesa', 'Levadura Fresca',    0.0060),
    ('Baguette Francesa', 'Sal',                0.0080),

    ('Ciabatta Italiana', 'Harina de Trigo',    0.4000),
    ('Ciabatta Italiana', 'Aceite de Oliva',    0.0200),
    ('Ciabatta Italiana', 'Levadura Seca',      0.0040),
    ('Ciabatta Italiana', 'Sal',                0.0080),

    ('Focaccia de Romero', 'Harina de Trigo',    0.3500),
    ('Focaccia de Romero', 'Aceite de Oliva',    0.0600),
    ('Focaccia de Romero', 'Levadura Seca',      0.0040),
    ('Focaccia de Romero', 'Sal de Mar',         0.0060),
    ('Focaccia de Romero', 'Sal',                0.0060),

    ('Pan de Masa Madre', 'Harina de Trigo',    0.5000),
    ('Pan de Masa Madre', 'Harina Integral',    0.1500),
    ('Pan de Masa Madre', 'Levadura Fresca',    0.0040),
    ('Pan de Masa Madre', 'Sal',                0.0120),

    -- Dulces y Confitería
    ('Mazapán de Almendra', 'Almendras',      0.2500),
    ('Mazapán de Almendra', 'Azúcar Glass',   0.1800),

    ('Cocadas', 'Coco Rallado',       0.2000),
    ('Cocadas', 'Leche Condensada',   0.1500),
    ('Cocadas', 'Azúcar Blanca',      0.0500),

    ('Jamoncillo de Leche', 'Leche Entera',        0.6000),
    ('Jamoncillo de Leche', 'Azúcar Blanca',       0.2000),
    ('Jamoncillo de Leche', 'Leche Condensada',    0.1000),
    ('Jamoncillo de Leche', 'Nueces',              0.0400),

    ('Trufas de Chocolate', 'Chocolate Amargo',    0.2500),
    ('Trufas de Chocolate', 'Crema para Batir',    0.1000),
    ('Trufas de Chocolate', 'Mantequilla',         0.0300),
    ('Trufas de Chocolate', 'Cacao en Polvo',      0.0200),

    ('Ate con Queso', 'Manzanas',              0.4000),
    ('Ate con Queso', 'Azúcar Blanca',         0.2500),
    ('Ate con Queso', 'Limones',               0.0500),
    ('Ate con Queso', 'Gelatina Sin Sabor',    0.0040),

    -- Productos Veganos
    ('Pastel Vegano de Chocolate', 'Harina de Trigo',        0.4500),
    ('Pastel Vegano de Chocolate', 'Azúcar Morena',          0.3500),
    ('Pastel Vegano de Chocolate', 'Cacao en Polvo',         0.1000),
    ('Pastel Vegano de Chocolate', 'Aceite de Coco',         0.1500),
    ('Pastel Vegano de Chocolate', 'Leche de Almendra',      0.4000),
    ('Pastel Vegano de Chocolate', 'Chocolate Amargo',       0.1500),
    ('Pastel Vegano de Chocolate', 'Bicarbonato de Sodio',   0.0080),

    ('Cupcakes Veganos Variados', 'Harina de Trigo',        0.2000),
    ('Cupcakes Veganos Variados', 'Azúcar Morena',          0.1500),
    ('Cupcakes Veganos Variados', 'Aceite de Coco',         0.0600),
    ('Cupcakes Veganos Variados', 'Leche de Almendra',      0.1800),
    ('Cupcakes Veganos Variados', 'Cacao en Polvo',         0.0300),
    ('Cupcakes Veganos Variados', 'Chispas de Chocolate',   0.0500),
    ('Cupcakes Veganos Variados', 'Polvo para Hornear',     0.0080),

    ('Galletas Veganas de Avena', 'Harina de Avena',         0.2000),
    ('Galletas Veganas de Avena', 'Azúcar Morena',           0.1200),
    ('Galletas Veganas de Avena', 'Aceite de Coco',          0.0800),
    ('Galletas Veganas de Avena', 'Chispas de Chocolate',    0.0800),
    ('Galletas Veganas de Avena', 'Leche de Almendra',       0.0600),
    ('Galletas Veganas de Avena', 'Bicarbonato de Sodio',    0.0040),

    ('Brownie Vegano', 'Harina de Trigo',      0.0600),
    ('Brownie Vegano', 'Azúcar Morena',        0.0500),
    ('Brownie Vegano', 'Cacao en Polvo',       0.0200),
    ('Brownie Vegano', 'Aceite de Coco',       0.0300),
    ('Brownie Vegano', 'Leche de Almendra',    0.0500),
    ('Brownie Vegano', 'Nueces',               0.0200),

    -- Temporada y Festividades
    ('Pan de Muerto', 'Harina de Trigo',    0.2500),
    ('Pan de Muerto', 'Mantequilla',        0.0800),
    ('Pan de Muerto', 'Azúcar Blanca',      0.0600),
    ('Pan de Muerto', 'Huevos',             2.0000),
    ('Pan de Muerto', 'Azahar',             0.0050),
    ('Pan de Muerto', 'Levadura Fresca',    0.0050),
    ('Pan de Muerto', 'Naranjas',           0.0500),

    ('Rosca de Reyes', 'Harina de Trigo',    0.5000),
    ('Rosca de Reyes', 'Mantequilla',        0.2000),
    ('Rosca de Reyes', 'Azúcar Blanca',      0.1500),
    ('Rosca de Reyes', 'Huevos',             5.0000),
    ('Rosca de Reyes', 'Levadura Fresca',    0.0080),
    ('Rosca de Reyes', 'Duraznos',           0.1000),
    ('Rosca de Reyes', 'Naranjas',           0.0800),
    ('Rosca de Reyes', 'Azahar',             0.0060),

    ('Tronco de Navidad', 'Harina de Trigo',     0.3000),
    ('Tronco de Navidad', 'Chocolate Amargo',    0.2500),
    ('Tronco de Navidad', 'Crema para Batir',    0.3500),
    ('Tronco de Navidad', 'Huevos',              6.0000),
    ('Tronco de Navidad', 'Azúcar Blanca',       0.2000),
    ('Tronco de Navidad', 'Avellanas',           0.1000),
    ('Tronco de Navidad', 'Cacao en Polvo',      0.0400)
) AS v(product_name, ingredient_name, quantity)
JOIN products p    ON p.name = v.product_name
JOIN ingredients i ON i.name = v.ingredient_name;

-- ============================================================
-- 12. SALES — Historial de ventas demo
-- ============================================================
-- Distribuidas en los ultimos 45 dias, con ventas del dia actual para que
-- el dashboard (ventasHoy / ingresoHoy / ingresoMes) siempre tenga datos.
-- La fecha se ancla a date_trunc('day', NOW()) para que "hoy" sea hoy
-- independientemente de la hora del deploy.
INSERT INTO sales (sale_date, registered_by_id, product_id, product_name, quantity, unit_price,
                   total_amount, notes, customer_name, customer_dni, customer_phone, inserted_at, updated_at)
SELECT date_trunc('day', NOW())
           - (v.days_ago || ' days')::INTERVAL
           + (v.hour     || ' hours')::INTERVAL,
       u.id, p.id, p.name, v.quantity, v.unit_price,
       v.unit_price * v.quantity,
       v.notes, v.customer_name, v.customer_dni, v.customer_phone, NOW(), NOW()
FROM (VALUES
    -- Hoy
    ('Pastel de Chocolate Triple',       1, 450.00,  0, 10, 'María González Ruiz',   '30125478', '55 1234 5601', 'Entrega en sucursal centro',        'employee@malva.com'),
    ('Concha de Vainilla',              12,  18.00,  0, 11, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Cupcake de Chocolate',             6,  45.00,  0, 13, 'Laura Méndez',          '28994512', '55 1234 5602', 'Pedido para oficina',               'employee@malva.com'),
    ('Galletas de Chispas de Chocolate', 2,  85.00,  0, 16, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    -- Ayer
    ('Pastel Tres Leches',               1, 350.00,  1, 12, 'Jorge Ramírez',         '31450098', '55 1234 5603', 'Cumpleaños familiar',               'employee@malva.com'),
    ('Pay de Manzana',                   1, 280.00,  1, 15, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Cuerno de Mantequilla',           10,  20.00,  1, 17, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    -- Esta semana
    ('Cheesecake New York',              4,  90.00,  2, 11, 'Patricia Soto',         '27883421', '55 1234 5604', NULL,                                'employee@malva.com'),
    ('Baguette Francesa',                6,  45.00,  2, 18, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Pastel Red Velvet',                1, 420.00,  3,  9, 'Andrea Villalobos',     '30012765', '55 1234 5605', 'Decoración especial solicitada',    'admin@malva.com'),
    ('Cupcake Red Velvet',              12,  48.00,  3, 14, 'Escuela Primaria Sol',  '30559981', '55 1234 5606', 'Festival escolar',                  'employee@malva.com'),
    ('Trufas de Chocolate',              3, 150.00,  4, 16, 'Ricardo Fuentes',       '29334410', '55 1234 5607', NULL,                                'employee@malva.com'),
    ('Concha de Chocolate',             20,  18.00,  4, 10, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    ('Tarta de Frutos Rojos',            1, 350.00,  5, 13, 'Sofía Herrera',         '31220845', '55 1234 5608', 'Aniversario',                       'employee@malva.com'),
    ('Galletas de Avena con Pasas',      3,  75.00,  5, 17, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Pastel de Fresas con Crema',       1, 480.00,  6, 11, 'Familia Ortega',        '26778123', '55 1234 5609', 'Recoger 18:00 hs',                  'admin@malva.com'),
    ('Tiramisú Individual',              8,  85.00,  6, 15, 'Café Aurora',           '30991256', '55 1234 5610', 'Pedido mayorista semanal',          'employee@malva.com'),
    -- Semanas anteriores
    ('Pan de Masa Madre',                4,  85.00,  8, 12, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Mousse de Chocolate',              6,  75.00,  9, 14, 'Daniela Cruz',          '31008877', '55 1234 5611', NULL,                                'employee@malva.com'),
    ('Pastel Selva Negra',               1, 520.00, 10, 10, 'Empresa Nexus SA',      '30447712', '55 1234 5612', 'Factura a nombre de la empresa',    'admin@malva.com'),
    ('Oreja',                           15,  22.00, 11, 16, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Pay de Limón',                     2, 260.00, 12, 13, 'Marcos Iturbe',         '28110934', '55 1234 5613', NULL,                                'employee@malva.com'),
    ('Brownie Vegano',                  10,  55.00, 13, 15, 'Tienda Verde',          '31556602', '55 1234 5614', 'Pedido vegano recurrente',          'employee@malva.com'),
    ('Cupcake de Nutella',               6,  55.00, 14, 12, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    ('Profiteroles',                     5,  95.00, 15, 17, 'Valeria Nava',          '29887340', '55 1234 5615', NULL,                                'employee@malva.com'),
    ('Pastel de Zanahoria',              1, 400.00, 16, 11, 'Hugo Salazar',          '30778215', '55 1234 5616', 'Sin nueces en la cobertura',        'employee@malva.com'),
    ('Galletas de Mantequilla',          4,  70.00, 17, 14, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    ('Panna Cotta',                      6,  70.00, 18, 16, 'Restaurante Bellavista','30223391', '55 1234 5617', 'Pedido para servicio de cena',      'employee@malva.com'),
    ('Polvorón Rosa',                   24,  15.00, 19, 10, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Pastel de Vainilla Clásico',       1, 380.00, 20, 13, 'Elena Paredes',         '27665048', '55 1234 5618', 'Cumpleaños infantil',               'admin@malva.com'),
    ('Ciabatta Italiana',                8,  50.00, 21, 18, 'Bistró La Esquina',     '31447790', '55 1234 5619', 'Pedido mayorista',                  'employee@malva.com'),
    ('Crème Brûlée',                     4,  80.00, 23, 15, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Mazapán de Almendra',              2, 120.00, 25, 12, 'Gabriel Lozano',        '29556614', '55 1234 5620', NULL,                                'admin@malva.com'),
    ('Pay de Nuez',                      1, 320.00, 27, 11, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Cocadas',                          6,  65.00, 29, 16, 'Mercadito Artesanal',   '30889945', '55 1234 5621', 'Puesto de feria',                   'employee@malva.com'),
    ('Focaccia de Romero',               5,  65.00, 31, 14, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com'),
    ('Pastel Vegano de Chocolate',       1, 420.00, 33, 12, 'Camila Duarte',         '31770223', '55 1234 5622', 'Sin trazas de lácteos',             'employee@malva.com'),
    ('Galletas de Jengibre',             3,  80.00, 35, 17, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Tarta Tatin',                      2, 300.00, 38, 13, 'Fernando Aguirre',      '28993377', '55 1234 5623', NULL,                                'admin@malva.com'),
    ('Jamoncillo de Leche',              4,  80.00, 40, 15, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'employee@malva.com'),
    ('Cupcakes Veganos Variados',        2, 180.00, 42, 11, 'Tienda Verde',          '31556602', '55 1234 5614', 'Segundo pedido del mes',            'employee@malva.com'),
    ('Cupcake de Café Moka',             6,  50.00, 45, 16, 'Venta de mostrador',    NULL,       NULL,           NULL,                                'admin@malva.com')
) AS v(product_name, quantity, unit_price, days_ago, hour, customer_name, customer_dni, customer_phone, notes, seller_email)
JOIN products p ON p.name  = v.product_name
JOIN users    u ON u.email = v.seller_email;

-- ============================================================
-- 13. SALE_INGREDIENTS — Snapshot de insumos por venta
-- ============================================================
-- Derivado de la receta vigente, replicando exactamente SaleService.create():
--   quantity_used = receta.quantity * venta.quantity
--   total_cost    = quantity_used   * ingrediente.unit_cost
-- unit_of_measure guarda el displayName del enum UnitOfMeasure ('Kilogramo'),
-- no su nombre ('KILOGRAMO'), igual que hace la aplicacion.
INSERT INTO sale_ingredients (sale_id, ingredient_id, ingredient_name, quantity_used, unit_cost,
                              unit_of_measure, total_cost, inserted_at, updated_at)
SELECT s.id,
       i.id,
       i.name,
       pi.quantity * s.quantity,
       i.unit_cost,
       CASE i.unit_of_measure
           WHEN 'GRAMO'       THEN 'Gramo'
           WHEN 'KILOGRAMO'   THEN 'Kilogramo'
           WHEN 'MILIGRAMO'   THEN 'Miligramo'
           WHEN 'LIBRA'       THEN 'Libra'
           WHEN 'ONZA'        THEN 'Onza'
           WHEN 'MILILITRO'   THEN 'Mililitro'
           WHEN 'LITRO'       THEN 'Litro'
           WHEN 'TAZA'        THEN 'Taza'
           WHEN 'CUCHARADA'   THEN 'Cucharada'
           WHEN 'CUCHARADITA' THEN 'Cucharadita'
           WHEN 'UNIDAD'      THEN 'Unidad'
           WHEN 'DOCENA'      THEN 'Docena'
           WHEN 'PAQUETE'     THEN 'Paquete'
           WHEN 'PIEZA'       THEN 'Pieza'
       END,
       ROUND(pi.quantity * s.quantity * i.unit_cost, 2),
       NOW(), NOW()
FROM sales s
JOIN product_ingredients pi ON pi.product_id = s.product_id
JOIN ingredients i          ON i.id = pi.ingredient_id;
