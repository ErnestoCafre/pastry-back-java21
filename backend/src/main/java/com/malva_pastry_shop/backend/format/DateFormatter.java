package com.malva_pastry_shop.backend.format;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/**
 * Formato de fechas del panel, hermano de {@link MoneyFormatter}.
 *
 * <pre>
 *   th:text="${@dates.format(product.updatedAt)}"   -&gt; 14/03/2026 10:30
 *   th:text="${@dates.day(sale.saleDate)}"          -&gt; 14/03/2026
 *   th:text="${@dates.time(sale.saleDate)}"         -&gt; 10:30
 * </pre>
 *
 * <p><b>Por qué existe.</b> El patrón {@code 'dd/MM/yyyy HH:mm'} estaba
 * escrito a mano en 21 lugares de 13 plantillas. Es exactamente la forma que ya
 * habia causado dos problemas con los importes: una regla de formato copiada
 * en cada sitio de uso diverge sin que nada falle, porque cada copia se ve
 * bien por separado.
 *
 * <p>Y ya habia divergido: de los 21 sitios, <b>2 protegían el nulo</b> con un
 * ternario que devuelve un guion y <b>16 no</b>. Acá el guard es del
 * formateador, así que no se puede olvidar en el sitio 22.
 *
 * <p>No lleva {@code @Component}, por el mismo motivo que
 * {@code MoneyFormatter}: un componente suelto no entra en el slice de
 * {@code @WebMvcTest} y todos los tests de renderizado fallarían con "No bean
 * named 'dates' available".
 */
public class DateFormatter {

    private static final String NULL_PLACEHOLDER = "—";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Fecha y hora: {@code 14/03/2026 10:30}. Guion largo si es nula. */
    public String format(Temporal value) {
        return value == null ? NULL_PLACEHOLDER : DATE_TIME.format(value);
    }

    /** Solo el día: {@code 14/03/2026}. Guion largo si es nula. */
    public String day(Temporal value) {
        return value == null ? NULL_PLACEHOLDER : DATE.format(value);
    }

    /** Solo la hora: {@code 10:30}. Guion largo si es nula. */
    public String time(Temporal value) {
        return value == null ? NULL_PLACEHOLDER : TIME.format(value);
    }
}
