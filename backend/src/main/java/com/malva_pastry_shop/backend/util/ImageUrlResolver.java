package com.malva_pastry_shop.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la URL de imagen de un producto para la API pública.
 *
 * Las imágenes se siembran con ruta relativa ("/images/products/x.webp") porque
 * las sirve el propio backend. Un frontend alojado en otro dominio (React en
 * Vercel, por ejemplo) resolvería esa ruta contra su propio origen y la imagen
 * saldría rota. Con {@code app.public.base-url} configurado, este resolvedor
 * antepone el origen del backend para devolver una URL absoluta.
 *
 * Si la base está vacía (valor por defecto) se conserva la ruta relativa, lo que
 * mantiene el comportamiento previo para consumo desde el mismo origen.
 */
@Component
public class ImageUrlResolver {

    private final String baseUrl;

    public ImageUrlResolver(@Value("${app.public.base-url:}") String baseUrl) {
        // Normalizar: sin espacios y sin barras finales para no duplicarlas al concatenar.
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip().replaceAll("/+$", "");
    }

    /**
     * Devuelve la URL absoluta de la imagen si hay base configurada; en caso
     * contrario (o si ya es absoluta, o está vacía) la devuelve sin cambios.
     */
    public String resolve(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        if (baseUrl.isEmpty()
                || imageUrl.startsWith("http://")
                || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        return imageUrl.startsWith("/") ? baseUrl + imageUrl : baseUrl + "/" + imageUrl;
    }
}
