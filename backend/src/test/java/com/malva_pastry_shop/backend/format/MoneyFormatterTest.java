package com.malva_pastry_shop.backend.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Formato de importes (es-AR)")
class MoneyFormatterTest {

    private final MoneyFormatter money = new MoneyFormatter();

    @Nested
    @DisplayName("Importes")
    class Importes {

        /**
         * El caso que originó el cambio. La forma de tres argumentos de
         * #numbers.formatDecimal no emite separador de miles, así que un precio
         * de trece mil pesos se mostraba "$13132,00" — legible como ciento
         * treinta y un mil de un vistazo.
         */
        @Test
        @DisplayName("agrupa los miles")
        void groupsThousands() {
            assertThat(money.format(new BigDecimal("13132.00"))).isEqualTo("$13.132,00");
        }

        @Test
        @DisplayName("agrupa también los millones")
        void groupsMillions() {
            assertThat(money.format(new BigDecimal("1284550.75"))).isEqualTo("$1.284.550,75");
        }

        @Test
        @DisplayName("usa coma decimal y siempre dos decimales")
        void alwaysTwoDecimals() {
            assertThat(money.format(new BigDecimal("4.25"))).isEqualTo("$4,25");
            assertThat(money.format(new BigDecimal("4.2"))).isEqualTo("$4,20");
            assertThat(money.format(new BigDecimal("4"))).isEqualTo("$4,00");
            assertThat(money.format(BigDecimal.ZERO)).isEqualTo("$0,00");
        }

        /**
         * DecimalFormat redondea HALF_EVEN por defecto, que sobre importes da
         * resultados que nadie espera: 0,125 -> 0,12 pero 0,135 -> 0,14.
         */
        @Test
        @DisplayName("redondea medio hacia arriba, no bancario")
        void roundsHalfUp() {
            assertThat(money.format(new BigDecimal("0.125"))).isEqualTo("$0,13");
            assertThat(money.format(new BigDecimal("0.135"))).isEqualTo("$0,14");
        }

        @Test
        @DisplayName("los negativos conservan el signo delante del símbolo")
        void keepsNegativeSign() {
            assertThat(money.format(new BigDecimal("-1500.50"))).isEqualTo("$-1.500,50");
        }

        @Test
        @DisplayName("nulo no explota")
        void nullIsSafe() {
            // #numbers.formatDecimal(null, ...) lanzaba excepción y tumbaba el
            // render de la página entera.
            assertThat(money.format(null)).isEqualTo("—");
        }
    }

    @Nested
    @DisplayName("Cantidades")
    class Cantidades {

        @Test
        @DisplayName("van sin símbolo y con cuatro decimales")
        void noSymbolFourDecimals() {
            assertThat(money.quantity(new BigDecimal("0.5"))).isEqualTo("0,5000");
            assertThat(money.quantity(new BigDecimal("2"))).isEqualTo("2,0000");
        }

        @Test
        @DisplayName("nulo no explota")
        void nullIsSafe() {
            assertThat(money.quantity(null)).isEqualTo("—");
        }
    }

    /**
     * La razón de ser del formateador: el resultado no puede depender de con
     * qué idioma esté configurada la máquina ni el navegador. Antes sí dependía,
     * y el mismo precio se veía distinto según quién mirara la pantalla.
     */
    @Test
    @DisplayName("El formato no depende del locale por defecto de la JVM")
    void ignoresDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertThat(money.format(new BigDecimal("13132.00"))).isEqualTo("$13.132,00");

            Locale.setDefault(Locale.GERMANY);
            assertThat(money.format(new BigDecimal("13132.00"))).isEqualTo("$13.132,00");
        } finally {
            Locale.setDefault(original);
        }
    }
}
