package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.storefront.ProductTag;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Fija los fetch de {@link ProductTagRepository}.
 *
 * <p>La app corre con {@code spring.jpa.open-in-view=false}, así que la sesión
 * de Hibernate se cierra al salir del servicio: todo lo que la vista vaya a
 * tocar tiene que venir ya cargado. Cuando no es así el fallo es especialmente
 * feo, porque la excepción salta <em>a mitad del render</em>, con la respuesta
 * ya parcialmente enviada: el navegador recibe un HTTP 200 con la página
 * cortada, sin ningún error visible.
 *
 * <p>Fue exactamente lo que pasó en {@code /tags/&#123;id&#125;/products}:
 * {@code findByTagId} traía {@code product} pero no {@code product.category},
 * y la plantilla muestra la categoría en cada fila. La página decía
 * "10 producto(s)" y listaba uno solo.
 *
 * <p>Un {@code @WebMvcTest} no puede detectar esto: con los servicios
 * mockeados las entidades vienen completas y nunca hay proxies. Tiene que
 * verificarse a nivel repositorio.
 *
 * <p><b>Requisito:</b> este test usa una base propia para no tocar la de
 * desarrollo, que la app recrea en cada arranque. Hay que crearla una vez:
 *
 * <pre>
 * PGPASSWORD=postgres123 psql -h localhost -U postgres -c "CREATE DATABASE malva_pastry_test"
 * </pre>
 *
 * El esquema lo arma Hibernate con {@code create-drop} en cada corrida.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // Base propia: no puede tocar la de desarrollo, que ademas se recrea
        // en cada arranque de la app.
        "spring.datasource.url=jdbc:postgresql://localhost:5432/malva_pastry_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.show-sql=false"
})
@DisplayName("ProductTagRepository — fetch para vistas sin sesión abierta")
class ProductTagRepositoryFetchTest {

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private EntityManager entityManager;

    private Tag seedTagWithCategorisedProduct() {
        Category category = new Category();
        category.setName("Panadería");
        entityManager.persist(category);

        Product product = new Product();
        product.setName("Concha de Chocolate");
        product.setBasePrice(new BigDecimal("35.00"));
        product.setCategory(category);
        entityManager.persist(product);

        Tag tag = new Tag();
        tag.setName("Clásico");
        tag.setSlug("clasico");
        entityManager.persist(tag);

        ProductTag link = new ProductTag();
        link.setProduct(product);
        link.setTag(tag);
        entityManager.persist(link);

        entityManager.flush();
        return tag;
    }

    @Test
    @DisplayName("findByTagId deja product.category utilizable con la sesión cerrada")
    void findByTagIdFetchesProductCategory() {
        Long tagId = seedTagWithCategorisedProduct().getId();

        // Imprescindible vaciar ANTES de consultar. Si no, la query devuelve las
        // mismas instancias que acabamos de persistir —con la categoría ya
        // asignada— desde el caché de primer nivel, y el @EntityGraph no influye:
        // el test pasaría igual con el bug puesto.
        entityManager.clear();

        List<ProductTag> links = productTagRepository.findByTagId(tagId);
        assertThat(links).hasSize(1);
        Product product = links.get(0).getProduct();

        // Segundo clear: desasocia todo, como al cerrarse la sesión antes de
        // renderizar. Si category fuese un proxy perezoso, tocarla ahora tira
        // LazyInitializationException, que es justo lo que rompía la vista.
        entityManager.clear();

        assertThatCode(() -> assertThat(product.getCategory().getName()).isEqualTo("Panadería"))
                .doesNotThrowAnyException();
    }
}
