package com.malva_pastry_shop.backend.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Atiende {@code /error/403}, la página a la que Spring Security hace forward
 * cuando deniega el acceso ({@code accessDeniedPage} en SecurityConfig).
 *
 * <p>Sin este handler la ruta no existía, así que toda denegación terminaba en
 * un 404 con la Whitelabel Error Page de Spring Boot —o directamente en un
 * JSON de error— en vez de un mensaje entendible.
 *
 * <p>Distingue dos causas que llegan por el mismo camino porque
 * {@code CsrfException} extiende {@code AccessDeniedException}:
 *
 * <ul>
 *   <li><b>Token CSRF inválido</b>: casi siempre un formulario viejo, la sesión
 *       expirada o el servidor reiniciado. No es un problema de permisos y el
 *       usuario lo resuelve reintentando, así que se lo manda al login.</li>
 *   <li><b>Falta de permisos</b>: por ejemplo un EMPLOYEE entrando por URL a
 *       {@code /users} o a una papelera. Ahí sí corresponde "no tenés
 *       permiso".</li>
 * </ul>
 *
 * <p>Mapea todos los métodos HTTP a propósito: el forward conserva el método
 * original, y el caso de CSRF llega como POST.
 */
@Controller
public class ErrorPageController {

    @RequestMapping("/error/403")
    public String accessDenied(HttpServletRequest request, Model model) {
        Object thrown = request.getAttribute(WebAttributes.ACCESS_DENIED_403);
        boolean staleForm = thrown instanceof CsrfException;

        model.addAttribute("staleForm", staleForm);
        model.addAttribute("detail", thrown instanceof AccessDeniedException ex ? ex.getMessage() : null);
        return "error/403";
    }
}
