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
    favorites,
    product_reviews,
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
ALTER SEQUENCE favorites_id_seq RESTART WITH 1;
ALTER SEQUENCE product_reviews_id_seq RESTART WITH 1;

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
-- 5. INGREDIENTES (58)
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
('Pastel de Chocolate Triple',  'Tres capas de bizcocho de chocolate con ganache, cubierto de chocolate belga',       3, 450.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-chocolate-triple.jpg', NOW(), NOW()),
('Pastel Red Velvet',           'Suave bizcocho rojo aterciopelado con frosting de queso crema',                      2, 420.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-red-velvet.jpg',       NOW(), NOW()),
('Pastel de Vainilla Clásico',  'Bizcocho esponjoso de vainilla con betún de mantequilla',                           2, 380.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-vainilla-clasico.jpg', NOW(), NOW()),
('Pastel de Zanahoria',         'Con nueces, pasas y frosting de queso crema con canela',                            2, 400.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-zanahoria.jpg',        NOW(), NOW()),
('Pastel de Fresas con Crema',  'Bizcocho de vainilla con fresas frescas y crema chantilly',                         1, 480.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-fresas-crema.jpg',     NOW(), NOW()),
('Pastel Tres Leches',          'Bizcocho bañado en tres tipos de leche con merengue italiano',                      1, 350.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-tres-leches.jpg',      NOW(), NOW()),
('Pastel Selva Negra',          'Chocolate, cerezas y crema batida. Receta alemana tradicional',                     3, 520.00, TRUE, (SELECT id FROM categories WHERE name = 'Pasteles'), '/images/products/pastel-selva-negra.jpg',      NOW(), NOW());

-- Cupcakes (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Cupcake de Chocolate',   'Base de chocolate intenso con frosting de chocolate',                    1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-chocolate.jpg',   NOW(), NOW()),
('Cupcake Red Velvet',     'Cupcake rojo aterciopelado con queso crema',                            1, 48.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-red-velvet.jpg',  NOW(), NOW()),
('Cupcake de Limón',       'Sabor cítrico refrescante con frosting de limón',                       1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-limon.jpg',       NOW(), NOW()),
('Cupcake de Nutella',     'Relleno de Nutella con frosting de avellanas',                          1, 55.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-nutella.jpg',     NOW(), NOW()),
('Cupcake de Café Moka',   'Para los amantes del café con toque de chocolate',                      1, 50.00, TRUE, (SELECT id FROM categories WHERE name = 'Cupcakes'), '/images/products/cupcake-cafe-moka.jpg',   NOW(), NOW());

-- Galletas (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Galletas de Chispas de Chocolate', 'Clásicas con chips de chocolate semi-amargo. Docena',         1, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-chispas-chocolate.jpg', NOW(), NOW()),
('Galletas de Avena con Pasas',      'Saludables y deliciosas con pasas y canela. Docena',          1, 75.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-avena-pasas.jpg',       NOW(), NOW()),
('Galletas de Mantequilla',          'Tradicionales galletas danesas de mantequilla. Docena',       1, 70.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-mantequilla.jpg',       NOW(), NOW()),
('Galletas de Jengibre',             'Especiadas y crujientes, perfectas con café. Docena',         1, 80.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-jengibre.jpg',          NOW(), NOW()),
('Galletas Snickerdoodle',           'Con canela y azúcar, suaves por dentro. Docena',              1, 78.00, TRUE, (SELECT id FROM categories WHERE name = 'Galletas'), '/images/products/galletas-snickerdoodle.jpg',     NOW(), NOW());

-- Pan Dulce Mexicano (6)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Concha de Vainilla',       'Pan dulce tradicional con costra de azúcar sabor vainilla',           0, 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/concha-vainilla.jpg',     NOW(), NOW()),
('Concha de Chocolate',      'Concha clásica con costra de chocolate',                              0, 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/concha-chocolate.jpg',    NOW(), NOW()),
('Cuerno de Mantequilla',    'Esponjoso cuerno bañado en mantequilla y azúcar glass',               0, 20.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/cuerno-mantequilla.jpg',  NOW(), NOW()),
('Oreja',                    'Pan hojaldrado caramelizado, crujiente y dulce',                      0, 22.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/oreja.jpg',               NOW(), NOW()),
('Polvorón Rosa',            'Galleta de mantequilla que se deshace en la boca',                    0, 15.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/polvoron-rosa.jpg',       NOW(), NOW()),
('Garibaldi',                'Panecillo cubierto de chochitos de colores',                          0, 16.00, TRUE, (SELECT id FROM categories WHERE name = 'Pan Dulce Mexicano'), '/images/products/garibaldi.jpg',           NOW(), NOW());

-- Tartas y Pays (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pay de Manzana',          'Manzanas caramelizadas con canela en masa crujiente',                  2, 280.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-manzana.jpg',         NOW(), NOW()),
('Pay de Limón',            'Relleno cremoso de limón con merengue italiano',                       2, 260.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-limon.jpg',           NOW(), NOW()),
('Pay de Nuez',             'Nueces caramelizadas en base de mantequilla',                          2, 320.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/pay-nuez.jpg',            NOW(), NOW()),
('Tarta de Frutos Rojos',  'Crema pastelera con frambuesas, moras y fresas',                       1, 350.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/tarta-frutos-rojos.jpg',  NOW(), NOW()),
('Tarta Tatin',             'Tarta francesa invertida de manzana caramelizada',                     2, 300.00, TRUE, (SELECT id FROM categories WHERE name = 'Tartas y Pays'), '/images/products/tarta-tatin.jpg',         NOW(), NOW());

-- Postres Individuales (6)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Tiramisú Individual',    'Capas de mascarpone, café y bizcocho. Receta italiana',                1, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/tiramisu-individual.jpg',  NOW(), NOW()),
('Mousse de Chocolate',    'Mousse sedoso de chocolate belga 70% cacao',                           1, 75.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/mousse-chocolate.jpg',     NOW(), NOW()),
('Cheesecake New York',    'Cremoso cheesecake estilo americano con base de galleta',              2, 90.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/cheesecake-new-york.jpg',  NOW(), NOW()),
('Panna Cotta',            'Postre italiano de nata con coulis de frutos rojos',                   1, 70.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/panna-cotta.jpg',          NOW(), NOW()),
('Crème Brûlée',           'Crema francesa con costra de caramelo crujiente',                      1, 80.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/creme-brulee.jpg',         NOW(), NOW()),
('Profiteroles',           'Choux rellenos de crema con chocolate caliente',                       1, 95.00, TRUE, (SELECT id FROM categories WHERE name = 'Postres Individuales'), '/images/products/profiteroles.jpg',         NOW(), NOW());

-- Panes Artesanales (4)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Baguette Francesa',     'Pan crujiente de masa madre fermentada 24 horas',                       1, 45.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/baguette-francesa.jpg',   NOW(), NOW()),
('Ciabatta Italiana',     'Pan rústico italiano con corteza crujiente y miga aireada',             1, 50.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/ciabatta-italiana.jpg',   NOW(), NOW()),
('Focaccia de Romero',    'Pan plano italiano con aceite de oliva y romero fresco',                1, 65.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/focaccia-romero.jpg',     NOW(), NOW()),
('Pan de Masa Madre',     'Hogaza artesanal fermentada naturalmente 48 horas',                     2, 85.00, TRUE, (SELECT id FROM categories WHERE name = 'Panes Artesanales'), '/images/products/pan-masa-madre.jpg',      NOW(), NOW());

-- Dulces y Confitería (5)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Mazapán de Almendra',    'Dulce tradicional de almendra molida. Caja 12 piezas',                 1, 120.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/mazapan-almendra.jpg',   NOW(), NOW()),
('Cocadas',                'Dulce de coco rallado caramelizado. Bolsa 250g',                       1,  65.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/cocadas.jpg',            NOW(), NOW()),
('Jamoncillo de Leche',    'Dulce de leche tradicional mexicano. Caja 8 piezas',                   1,  80.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/jamoncillo-leche.jpg',   NOW(), NOW()),
('Trufas de Chocolate',    'Trufas artesanales de chocolate belga. Caja 6 piezas',                 1, 150.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/trufas-chocolate.jpg',   NOW(), NOW()),
('Ate con Queso',          'Dulce de membrillo artesanal con queso manchego. 300g',                1,  95.00, TRUE, (SELECT id FROM categories WHERE name = 'Dulces y Confitería'), '/images/products/ate-queso.jpg',          NOW(), NOW());

-- Productos Veganos (4)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pastel Vegano de Chocolate',  'Sin huevo ni lácteos, igual de delicioso',                        2, 420.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/pastel-vegano-chocolate.jpg', NOW(), NOW()),
('Cupcakes Veganos Variados',   'Set de 4 cupcakes veganos de sabores surtidos',                   1, 180.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/cupcakes-veganos.jpg',        NOW(), NOW()),
('Galletas Veganas de Avena',   'Sin huevo, con chips de chocolate vegano. Docena',                1,  95.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/galletas-veganas-avena.jpg',  NOW(), NOW()),
('Brownie Vegano',              'Brownie húmedo sin ingredientes animales',                        1,  55.00, TRUE, (SELECT id FROM categories WHERE name = 'Productos Veganos'), '/images/products/brownie-vegano.jpg',          NOW(), NOW());

-- Temporada y Festividades (3)
INSERT INTO products (name, description, preparation_days, base_price, visible, category_id, image_url, inserted_at, updated_at) VALUES
('Pan de Muerto',       'Tradicional pan de temporada con azahar. Disponible Oct-Nov',             1,  65.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/pan-de-muerto.jpg',    NOW(), NOW()),
('Rosca de Reyes',      'Con frutas cristalizadas y muñequito. Disponible en Enero',               2, 280.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/rosca-de-reyes.jpg',   NOW(), NOW()),
('Tronco de Navidad',   'Bûche de Noël con chocolate y crema de castañas',                        3, 450.00, TRUE, (SELECT id FROM categories WHERE name = 'Temporada y Festividades'), '/images/products/tronco-navidad.jpg',   NOW(), NOW());

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
