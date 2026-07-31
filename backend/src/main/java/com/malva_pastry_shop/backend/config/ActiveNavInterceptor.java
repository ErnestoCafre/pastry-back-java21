package com.malva_pastry_shop.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Publica {@code activeNav} para que el sidebar sepa qué ítem resaltar.
 *
 * <p>Antes esa decisión se tomaba comparando {@code pageTitle} —el título
 * visible de la página— contra listas de literales en el propio HTML:
 *
 * <pre>
 * ${pageTitle == 'Productos' || pageTitle == 'Nuevo Producto'
 *   || #strings.startsWith(pageTitle, 'Receta:') ...}
 * </pre>
 *
 * <p>Eso hacía que un mismo string cumpliera dos roles incompatibles: copy
 * para el usuario y clave de routing. Y estaba roto: las fichas de detalle
 * ponen {@code pageTitle} igual al nombre de la entidad, que no coincide con
 * ninguna regla, así que <em>ninguna</em> página de detalle resaltaba nada.
 * Tampoco lo hacían {@code /sales/new} ("Nueva Venta" no empieza con "Venta")
 * ni {@code /categories/&#123;id&#125;/products} ("Productos de X" no empieza
 * con "Productos de:"). Además una entidad llamada "Ventas" resaltaba el ítem
 * equivocado.
 *
 * <p>Acá la sección sale del primer segmento de la URL, que es exactamente el
 * prefijo {@code @RequestMapping} de cada controller. No hay literales que
 * mantener sincronizados y agregar una pantalla nueva bajo un prefijo ya
 * existente funciona sin tocar nada.
 */
@Component
public class ActiveNavInterceptor implements HandlerInterceptor {

    static final String ATTRIBUTE = "activeNav";

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) {

        if (modelAndView == null || !modelAndView.hasView()) {
            return;
        }
        String viewName = modelAndView.getViewName();
        // Las redirecciones no renderizan el layout; el interceptor corre de
        // nuevo sobre el destino.
        if (viewName == null || viewName.startsWith("redirect:")) {
            return;
        }
        modelAndView.addObject(ATTRIBUTE, sectionOf(request.getRequestURI(), request.getContextPath()));
    }

    /**
     * Primer segmento de la ruta, sin el context path.
     * {@code /products/5/recipe} -> {@code products}; {@code /dashboard} ->
     * {@code dashboard}; la raíz -> cadena vacía.
     */
    static String sectionOf(String requestUri, String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        int from = path.startsWith("/") ? 1 : 0;
        int slash = path.indexOf('/', from);
        return slash < 0 ? path.substring(from) : path.substring(from, slash);
    }
}
