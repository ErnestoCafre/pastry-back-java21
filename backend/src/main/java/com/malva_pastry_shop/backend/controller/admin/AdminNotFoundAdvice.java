package com.malva_pastry_shop.backend.controller.admin;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

/**
 * Convierte "esa entidad no existe" en un 404 con una página que lo dice.
 *
 * <p>Antes cada handler GET lo atrapaba por su cuenta y devolvía
 * {@code redirect:/loQueSea}: 17 bloques {@code try/catch} iguales que dejaban
 * al usuario en el listado, con status 200 y sin ninguna explicación. Un
 * enlace roto, un id viejo en un favorito y un borrado por otra pestaña se
 * veían los tres igual —como si no hubiera pasado nada—, y ninguna herramienta
 * podía distinguirlos porque la respuesta era un 200 correcto.
 *
 * <p>Solo alcanza a los GET. Los POST siguen atrapándola ellos mismos y
 * redirigen con un mensaje flash, que ahí sí es la respuesta correcta: quien
 * borra algo que otro ya borró no necesita un 404, necesita enterarse de que
 * ya no está.
 *
 * <p>Restringido al paquete {@code admin} a propósito: la API tiene su propio
 * {@code ApiExceptionHandler} y responde JSON.
 */
@ControllerAdvice(basePackages = "com.malva_pastry_shop.backend.controller.admin")
public class AdminNotFoundAdvice {

    /**
     * Secciones cuyo listado se puede ofrecer como salida.
     *
     * <p>La lista se compara contra el primer segmento de la URL pedida, y
     * existe para no construir un enlace a partir de lo que mandó el cliente:
     * {@code /loQueSea/1} no tiene por qué producir un enlace a
     * {@code /loQueSea}.
     */
    private static final Set<String> SECTIONS = Set.of(
            "categories", "ingredients", "products", "sales", "sections", "tags", "users");

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(EntityNotFoundException e, HttpServletRequest request) {
        ModelAndView view = new ModelAndView("error/404");
        view.addObject("detail", e.getMessage());
        view.addObject("listUrl", listUrlFor(request.getRequestURI()));
        return view;
    }

    /** {@code /categories/999/edit} -> {@code /categories}; desconocido -> null. */
    private static String listUrlFor(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 1 && SECTIONS.contains(parts[1]) ? "/" + parts[1] : null;
    }
}
