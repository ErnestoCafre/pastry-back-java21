package com.malva_pastry_shop.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlugUtil Tests")
class SlugUtilTest {

    @Nested
    @DisplayName("Normalización")
    class Normalization {

        @Test
        @DisplayName("pasa a minúsculas y reemplaza espacios por guiones")
        void lowercasesAndHyphenates() {
            assertThat(SlugUtil.generateSlug("Sin Gluten")).isEqualTo("sin-gluten");
        }

        @Test
        @DisplayName("elimina los acentos")
        void stripsAccents() {
            assertThat(SlugUtil.generateSlug("Café con Leche")).isEqualTo("cafe-con-leche");
        }

        @Test
        @DisplayName("colapsa separadores repetidos y recorta los de los extremos")
        void collapsesAndTrimsSeparators() {
            assertThat(SlugUtil.generateSlug("  ¡Nuevos!!  Destacados  ")).isEqualTo("nuevos-destacados");
        }

        @Test
        @DisplayName("devuelve null para null o blanco")
        void returnsNullForBlank() {
            assertThat(SlugUtil.generateSlug(null)).isNull();
            assertThat(SlugUtil.generateSlug("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("Casos que obligan a validar el resultado")
    class ResultMustBeValidated {

        // La transformacion no es inyectiva: estos pares son nombres distintos
        // para una validacion de nombre, pero un unico slug para la columna UNIQUE.
        @Test
        @DisplayName("nombres distintos pueden producir el mismo slug")
        void differentNamesCollapseToSameSlug() {
            assertThat(SlugUtil.generateSlug("Cítricos")).isEqualTo(SlugUtil.generateSlug("Citricos"));
            assertThat(SlugUtil.generateSlug("Sin Gluten")).isEqualTo(SlugUtil.generateSlug("sin-gluten"));
            assertThat(SlugUtil.generateSlug("Café")).isEqualTo(SlugUtil.generateSlug("¡Cafe!"));
        }

        @Test
        @DisplayName("un nombre sin alfanuméricos produce un slug vacío, no null")
        void nonAlphanumericNameProducesEmptySlug() {
            assertThat(SlugUtil.generateSlug("***")).isEmpty();
            assertThat(SlugUtil.generateSlug("★")).isEmpty();
        }
    }
}
