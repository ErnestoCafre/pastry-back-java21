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
 */
public interface IdCount {

    Long getId();

    long getTotal();
}
