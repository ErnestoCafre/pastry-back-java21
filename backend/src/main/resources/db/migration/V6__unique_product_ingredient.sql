-- ============================================================
-- V6: Unicidad de (product_id, ingredient_id) en product_ingredients
-- ============================================================
-- Las otras dos tablas puente del modelo declaran UNIQUE sobre su par de
-- claves foraneas: uk_product_tag en product_tags y uk_section_product en
-- storefront_section_products. product_ingredients quedo sin la suya.
--
-- ProductService.addIngredientToProduct comprueba la existencia del par antes
-- de insertar, pero esa comprobacion no es atomica: bajo READ COMMITTED dos
-- requests concurrentes -un doble submit del formulario de receta- pasan las
-- dos el chequeo y ambas insertan.
--
-- Lo que importa no es la probabilidad de esa carrera sino el modo de falla.
-- En las otras dos tablas el UNIQUE la convierte en un error visible. Aca la
-- fila duplicada entra en silencio y falsea numeros: el costo de receta suma
-- el ingrediente dos veces, y SaleService.create expande la receta al
-- registrar una venta, de modo que el costo equivocado queda congelado en el
-- snapshot inmutable de esa venta y ya no se corrige.
--
-- Se deduplica de forma defensiva antes de agregar la restriccion,
-- conservando la linea mas antigua de cada par. Si dos lineas duplicadas
-- tuvieran cantidades distintas, gana la primera que se cargo. En una base
-- que nunca sufrio la carrera el DELETE es un no-op; los datos sembrados
-- (297 lineas de receta, tanto en DataSeeder como en R__) no tienen pares
-- repetidos.
-- ============================================================

DELETE FROM product_ingredients pi
WHERE pi.id > (
    SELECT MIN(keep.id)
    FROM product_ingredients keep
    WHERE keep.product_id    = pi.product_id
      AND keep.ingredient_id = pi.ingredient_id
);

ALTER TABLE product_ingredients
    ADD CONSTRAINT uk_product_ingredient UNIQUE (product_id, ingredient_id);
