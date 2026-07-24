package com.malva_pastry_shop.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageUrlResolver Tests")
class ImageUrlResolverTest {

    @Nested
    @DisplayName("Sin base-url configurada")
    class WithoutBaseUrl {

        private final ImageUrlResolver resolver = new ImageUrlResolver("");

        @Test
        @DisplayName("conserva la ruta relativa sin cambios")
        void keepsRelativePath() {
            assertThat(resolver.resolve("/images/products/x.webp"))
                    .isEqualTo("/images/products/x.webp");
        }

        @Test
        @DisplayName("trata null como base vacía")
        void nullBaseTreatedAsEmpty() {
            assertThat(new ImageUrlResolver(null).resolve("/images/x.webp"))
                    .isEqualTo("/images/x.webp");
        }
    }

    @Nested
    @DisplayName("Con base-url configurada")
    class WithBaseUrl {

        private final ImageUrlResolver resolver = new ImageUrlResolver("https://api.malva.com");

        @Test
        @DisplayName("antepone el origen a una ruta relativa que empieza con /")
        void prefixesLeadingSlashPath() {
            assertThat(resolver.resolve("/images/products/x.webp"))
                    .isEqualTo("https://api.malva.com/images/products/x.webp");
        }

        @Test
        @DisplayName("inserta una sola barra si la ruta no empieza con /")
        void insertsSingleSlash() {
            assertThat(resolver.resolve("images/x.webp"))
                    .isEqualTo("https://api.malva.com/images/x.webp");
        }

        @Test
        @DisplayName("normaliza barras finales de la base para no duplicarlas")
        void normalizesTrailingSlashes() {
            assertThat(new ImageUrlResolver("https://api.malva.com/").resolve("/images/x.webp"))
                    .isEqualTo("https://api.malva.com/images/x.webp");
        }

        @Test
        @DisplayName("no altera una URL que ya es absoluta")
        void keepsAbsoluteUrl() {
            assertThat(resolver.resolve("https://cdn.otro.com/x.webp"))
                    .isEqualTo("https://cdn.otro.com/x.webp");
        }

        @Test
        @DisplayName("devuelve null/blank sin cambios")
        void keepsNullAndBlank() {
            assertThat(resolver.resolve(null)).isNull();
            assertThat(resolver.resolve("   ")).isEqualTo("   ");
        }
    }
}
