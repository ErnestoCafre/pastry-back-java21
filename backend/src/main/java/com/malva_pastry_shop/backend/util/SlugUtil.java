package com.malva_pastry_shop.backend.util;

import java.text.Normalizer;

/**
 * Utilidad para generar slugs URL-friendly a partir de strings.
 * Normaliza texto eliminando acentos, convirtiendo a minúsculas y
 * reemplazando espacios por guiones.
 *
 * <p><b>La transformación no es inyectiva:</b> nombres distintos pueden producir
 * el mismo slug ("Café" y "Cafe!", "Sin Gluten" y "sin-gluten"), y un nombre sin
 * caracteres alfanuméricos produce un slug vacío. Como las columnas de slug son
 * UNIQUE y NOT NULL, quien genere un slug para persistirlo debe validar el
 * resultado antes de guardarlo.
 */
public class SlugUtil {

    /**
     * Genera un slug a partir de un string.
     * 
     * Proceso:
     * 1. Normaliza el texto para eliminar acentos (NFD)
     * 2. Elimina marcas diacríticas
     * 3. Convierte a minúsculas
     * 4. Reemplaza espacios y caracteres especiales por guiones
     * 5. Elimina guiones duplicados
     * 6. Elimina guiones al inicio y final
     * 
     * @param text Texto a convertir en slug
     * @return Slug generado, o null si el texto es null o vacío
     */
    public static String generateSlug(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Normalizar para separar caracteres base de diacríticos
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);

        // Eliminar marcas diacríticas (acentos)
        String withoutAccents = normalized.replaceAll("\\p{M}", "");

        // Convertir a minúsculas
        String lowercase = withoutAccents.toLowerCase();

        // Reemplazar espacios y caracteres no alfanuméricos por guiones
        String slug = lowercase.replaceAll("[^a-z0-9]+", "-");

        // Eliminar guiones al inicio y final
        slug = slug.replaceAll("^-+|-+$", "");

        return slug;
    }
}
