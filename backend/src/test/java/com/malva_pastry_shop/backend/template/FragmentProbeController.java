package com.malva_pastry_shop.backend.template;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller de pruebas: renderiza fragments con entradas arbitrarias.
 *
 * <p>Existe porque varios fragments aceptan parámetros que ningún listado
 * migrado ejercita todavía. {@code pagination :: bar}, por ejemplo, solo se
 * había probado con un filtro de tipo String, pero {@code sales/list} necesita
 * tres y {@code products/list} le pasa un Long. Esperar a migrar esos listados
 * para descubrir si el fragment los soporta es al revés: la librería tiene que
 * ser confiable <em>antes</em> de que 11 listados se apoyen en ella.
 *
 * <p>Vive en las fuentes de test, así que no se empaqueta en el jar. Las
 * plantillas que sirve están en {@code src/test/resources/templates/probe/}.
 *
 * <p>El modelo llega desde el test por atributos de petición, no por SpEL: así
 * se pueden pasar mapas con nulos, Longs y orden controlado, que es
 * precisamente lo que hay que verificar.
 */
@Controller
public class FragmentProbeController {

    static final String PAGE = "probePage";
    static final String PARAMS = "probeParams";

    @GetMapping("/probe/pagination")
    @SuppressWarnings("unchecked")
    public String pagination(jakarta.servlet.http.HttpServletRequest request, Model model) {
        model.addAttribute("page", request.getAttribute(PAGE));
        model.addAttribute("params", (Map<String, Object>) request.getAttribute(PARAMS));
        return "probe/pagination";
    }

    /** Tres filtros dentro de un solo formulario: la forma de sales/list. */
    @GetMapping("/probe/filters")
    public String filters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model) {
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "probe/filters";
    }

    /** Estado vacío; clearUrl != null simula "hay filtros activos". */
    @GetMapping("/probe/empty")
    public String empty(@RequestParam(required = false) String clearUrl, Model model) {
        model.addAttribute("items", List.of());
        model.addAttribute("clearUrl", clearUrl);
        return "probe/empty";
    }
}
