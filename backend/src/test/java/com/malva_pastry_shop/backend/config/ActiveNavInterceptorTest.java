package com.malva_pastry_shop.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActiveNavInterceptor")
class ActiveNavInterceptorTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "/dashboard,                  dashboard",
            "/products,                   products",
            "/products/new,               products",
            "/products/5,                 products",
            "/products/5/edit,            products",
            "/products/5/recipe,          products",
            "/products/5/tags,            products",
            "/products/deleted,           products",
            "/categories/9/products,      categories",
            "/sales,                      sales",
            "/sales/new,                  sales",
            "/sales/12,                   sales",
            "/tags/3,                     tags",
            "/sections/2/products,        sections",
            "/users/1,                    users",
    })
    @DisplayName("la sección sale del primer segmento de la ruta")
    void resolvesSection(String uri, String expected) {
        assertThat(ActiveNavInterceptor.sectionOf(uri.trim(), "")).isEqualTo(expected);
    }

    @Test
    @DisplayName("descuenta el context path")
    void stripsContextPath() {
        assertThat(ActiveNavInterceptor.sectionOf("/admin/products/5", "/admin")).isEqualTo("products");
    }

    @Test
    @DisplayName("la raíz no resuelve a ninguna sección")
    void rootResolvesToEmpty() {
        assertThat(ActiveNavInterceptor.sectionOf("/", "")).isEmpty();
    }

    /**
     * El esquema anterior comparaba pageTitle contra literales, así que una
     * entidad llamada como otra sección resaltaba el ítem equivocado. Con la
     * ruta como fuente, el nombre de la entidad no influye.
     */
    @Test
    @DisplayName("el nombre de la entidad no puede secuestrar otra sección")
    void entityNameCannotHijackAnotherSection() {
        assertThat(ActiveNavInterceptor.sectionOf("/categories/7", "")).isEqualTo("categories");
    }
}
