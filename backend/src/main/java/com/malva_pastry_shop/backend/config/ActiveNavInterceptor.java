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
 * <p>La sección sale de la carpeta del nombre de vista ({@code products/recipe}
 * -&gt; {@code products}), que es exactamente cómo están organizadas las
 * plantillas: cada controller devuelve vistas de una sola carpeta y esa carpeta
 * coincide con su ítem de menú. Tomarlo de la vista y no del primer segmento de
 * la URL evita tener que descontar el context path y hace que el resaltado
 * dependa de lo que se está renderizando, no de por qué URL se llegó.
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
        // nuevo sobre el destino. Un View resuelto en vez de un nombre (getViewName
        // null) tampoco es una plantilla del panel.
        if (viewName == null || viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
            return;
        }
        modelAndView.addObject(ATTRIBUTE, sectionOf(viewName));
    }

    /**
     * Carpeta del nombre de vista. {@code products/recipe} -&gt;
     * {@code products}; {@code dashboard/index} -&gt; {@code dashboard}; una
     * vista sin carpeta se devuelve tal cual y simplemente no coincide con
     * ningún ítem del menú.
     */
    static String sectionOf(String viewName) {
        if (viewName == null) {
            return "";
        }
        int slash = viewName.indexOf('/');
        return slash < 0 ? viewName : viewName.substring(0, slash);
    }
}
