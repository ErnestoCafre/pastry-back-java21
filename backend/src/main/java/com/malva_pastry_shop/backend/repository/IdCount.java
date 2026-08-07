package com.malva_pastry_shop.backend.repository;

/**
 * Una fila de un {@code GROUP BY}: el id del grupo y cuántas filas tiene.
 *
 * <p>Existe para que los contadores de un listado se resuelvan en <b>una</b>
 * consulta en vez de una por fila. La versión anterior preguntaba dentro del
 * bucle que arma el modelo, así que un listado de 50 filas emitía 51 consultas
 * —y la de tags además hidrataba cada {@code Product} con su {@code Category}
 * para después contarlos en memoria—.
 *
 * <p>Es una proyección de interfaz de Spring Data: los alias de la consulta
 * ({@code as id}, {@code as total}) tienen que coincidir con los getters.
 *
 * <p>Ojo al consumirla: un {@code GROUP BY} <b>no devuelve fila para los grupos
 * vacíos</b>. Una categoría sin productos no aparece en el resultado, así que
 * el mapa no la trae y el llamador tiene que tratar la ausencia como cero.
 *
 * <p>Los dos getters devuelven envoltorio y no primitivo, a propósito. Un getter
 * primitivo en una proyección de interfaz lanza {@code NullPointerException} al
 * desenvolver si la columna viene nula, y el nombre de esta interfaz invita a
 * reusarla para cualquier agregado por grupo: {@code count()} nunca es nulo, pero
 * {@code sum()} sobre un grupo sin filas <b>sí</b>. El envoltorio saca ese NPE del
 * proxy y deja los dos getters simétricos; el boxing ocurría igual, porque los
 * tres consumidores arman un {@code Map<Long, Long>}.
 *
 * <p>Lo que el envoltorio <b>no</b> hace es volver seguro ese caso de punta a
 * punta. Los tres consumidores recolectan con {@code Collectors.toMap}, que
 * rechaza los valores nulos con {@code NullPointerException} —medido, no
 * supuesto: {@code HashMap.merge} no admite valor nulo, aunque {@code put} sí—.
 * O sea que un agregado nulable sigue cortando el stream, solo que en el
 * recolector y no al desenvolver. Si alguna vez esta proyección lleva uno, hay
 * que cambiar también cómo se recolecta.
 */
public interface IdCount {

    Long getId();

    Long getTotal();
}
