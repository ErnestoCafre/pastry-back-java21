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
            // Una vista por sección alcanza para fijar el mapeo; lo que importa
            // es que TODA vista de una carpeta caiga en el mismo ítem de menú.
            "dashboard/index,      dashboard",
            "products/list,        products",
            "products/create,      products",
            "products/show,        products",
            "products/recipe,      products",
            "products/tags,        products",
            "products/deleted,     products",
            "categories/products,  categories",
            "ingredients/list,     ingredients",
            "sales/create,         sales",
            "sales/show,           sales",
            "tags/show,            tags",
            "sections/products,    sections",
            "users/show,           users",
    })
    @DisplayName("la sección es la carpeta del nombre de vista")
    void resolvesSectionFromViewFolder(String viewName, String expected) {
        assertThat(ActiveNavInterceptor.sectionOf(viewName.trim())).isEqualTo(expected);
    }

    @Test
    @DisplayName("una vista sin carpeta no coincide con ningún ítem")
    void viewWithoutFolderIsHarmless() {
        assertThat(ActiveNavInterceptor.sectionOf("index")).isEqualTo("index");
        assertThat(ActiveNavInterceptor.sectionOf(null)).isEmpty();
    }

    /**
     * El esquema anterior comparaba pageTitle contra literales, así que una
     * entidad llamada como otra sección resaltaba el ítem equivocado. Con la
     * vista como fuente, el nombre de la entidad no influye.
     */
    @Test
    @DisplayName("el nombre de la entidad no puede secuestrar otra sección")
    void entityNameCannotHijackAnotherSection() {
        assertThat(ActiveNavInterceptor.sectionOf("categories/show")).isEqualTo("categories");
    }
}
