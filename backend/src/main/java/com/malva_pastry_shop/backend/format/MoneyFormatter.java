package com.malva_pastry_shop.backend.format;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de importes y cantidades del panel.
 *
 * <p>Expuesto a las plantillas como bean, así que se usa inline sin cambiar el
 * markup:
 *
 * <pre>
 *   th:text="${@money.format(product.basePrice)}"      -&gt; $13.132,00
 *   th:text="${@money.quantity(pi.quantity)}"          -&gt; 0,5000
 * </pre>
 *
 * <p><b>Por qué existe.</b> Antes cada plantilla escribía
 * {@code ${'$' + #numbers.formatDecimal(x, 1, 2)}}, con dos problemas:
 *
 * <ul>
 *   <li>Esa forma de tres argumentos <b>no emite separador de miles</b>: un
 *       precio de trece mil pesos salía {@code $13132,00}.</li>
 *   <li>El separador decimal lo resolvía el locale de la petición, y la app no
 *       fijaba ninguno. El mismo importe se veía {@code $13132.00} en un
 *       navegador en inglés y {@code $13132,00} en uno en español.</li>
 * </ul>
 *
 * <p>Acá el formato es explícito y no depende del locale entrante. El locale
 * fijo de {@code WebMvcConfig} cubre además cualquier {@code #numbers} o
 * {@code #temporals} que quede suelto en una plantilla.
 *
 * <p>Formato del negocio: <b>es-AR</b> — punto para miles, coma para
 * decimales.
 *
 * <p>No lleva {@code @Component}: se declara como {@code @Bean} en
 * {@code WebMvcConfig}. Un {@code @Component} suelto no entra en el slice de
 * {@code @WebMvcTest}, así que los tests de renderizado fallaban con "No bean
 * named 'money' available" — el mismo error que vería la app si el escaneo de
 * componentes cambiara. Declarado junto al LocaleResolver, además, queda claro
 * que son la misma decisión.
 */
public class MoneyFormatter {

    /** Punto para miles, coma para decimales. */
    public static final Locale BUSINESS_LOCALE = Locale.of("es", "AR");

    private static final String NULL_PLACEHOLDER = "—";

    /**
     * Importe con símbolo: {@code $13.132,00}. Siempre dos decimales.
     *
     * <p>Devuelve un guion largo si el valor es nulo. Varias plantillas ya
     * protegen el caso con un {@code th:if}, pero
     * {@code #numbers.formatDecimal(null, ...)} explotaba, y este método se
     * usa también en concatenaciones donde el guard no está a la vista.
     */
    public String format(BigDecimal value) {
        if (value == null) {
            return NULL_PLACEHOLDER;
        }
        return "$" + formatter(2, 2).format(value);
    }

    /**
     * Cantidad sin símbolo: {@code 0,5000}. Cuatro decimales, que es la escala
     * con la que se guardan las cantidades de receta.
     *
     * <p>Las medidas no son dinero: la regla del panel es que solo los
     * importes llevan el símbolo.
     */
    public String quantity(BigDecimal value) {
        if (value == null) {
            return NULL_PLACEHOLDER;
        }
        return formatter(4, 4).format(value);
    }

    /**
     * DecimalFormat no es thread-safe y esto lo llaman varios hilos de
     * servlet, así que se crea uno por invocación en lugar de compartir una
     * instancia. Es un objeto barato frente al costo de renderizar la página.
     */
    private DecimalFormat formatter(int minDecimals, int maxDecimals) {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(BUSINESS_LOCALE));
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        format.setMinimumIntegerDigits(1);
        format.setMinimumFractionDigits(minDecimals);
        format.setMaximumFractionDigits(maxDecimals);
        // Media hacia arriba: es lo que espera quien mira un precio, y evita
        // el redondeo bancario que trae DecimalFormat por defecto (HALF_EVEN),
        // donde 0,125 -> 0,12 pero 0,135 -> 0,14.
        format.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return format;
    }
}
