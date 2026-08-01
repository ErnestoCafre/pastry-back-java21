package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.AuthController;
import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.DashboardController;
import com.malva_pastry_shop.backend.controller.admin.IngredientController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.controller.admin.SaleController;
import com.malva_pastry_shop.backend.controller.admin.StorefrontSectionController;
import com.malva_pastry_shop.backend.controller.admin.TagController;
import com.malva_pastry_shop.backend.controller.admin.UserController;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.sales.SaleService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que todo enlace de las plantillas apunte a una ruta que existe.
 *
 * <p>Las rutas no se leen de las anotaciones a mano: salen del
 * {@link RequestMappingHandlerMapping}, o sea del mismo registro que usa Spring
 * para despachar. Si el test dice que una ruta existe, existe.
 *
 * <p>Un enlace roto en una plantilla no falla en compilación ni en ningún test
 * de renderizado: la página se sirve perfecta y el 404 aparece cuando alguien
 * hace clic. Es el riesgo concreto de mover acciones entre pantallas, que es
 * justamente lo que hizo la unificación de acciones de fila.
 */
@WebMvcTest(controllers = { AuthController.class, DashboardController.class, CategoryController.class,
        IngredientController.class, ProductController.class, SaleController.class,
        StorefrontSectionController.class, TagController.class, UserController.class })
@DisplayName("Integridad de los enlaces")
class RouteIntegrityTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean private TagService tagService;
    @MockitoBean private ProductService productService;
    @MockitoBean private CategoryService categoryService;
    @MockitoBean private StorefrontSectionService sectionService;
    @MockitoBean private IngredientService ingredientService;
    @MockitoBean private UserService userService;
    @MockitoBean private SaleService saleService;
    @MockitoBean private RoleRepository roleRepository;

    /** Rutas que no sirve ningún controller: las atiende Spring o el filtro de seguridad. */
    private static final Set<String> EXTERNAL = Set.of("/logout");

    /** Prefijos de recursos estáticos, que tampoco pasan por un controller. */
    private static final List<String> STATIC_PREFIXES = List.of("/css/", "/js/", "/images/", "/webjars/");

    @Test
    @DisplayName("ningún enlace apunta a una ruta que no existe")
    void everyLinkTargetsARealRoute() {
        Set<String> routes = registeredRoutes();
        Map<String, Set<String>> links = linksInTemplates();

        Map<String, Set<String>> broken = new LinkedHashMap<>();
        links.forEach((url, where) -> {
            boolean isStatic = STATIC_PREFIXES.stream().anyMatch(url::startsWith);
            if (!routes.contains(url) && !EXTERNAL.contains(url) && !isStatic) {
                broken.put(url, where);
            }
        });

        assertThat(broken)
                .as("""
                        Hay enlaces a rutas que ningún controller sirve.

                        Nada de esto falla al renderizar: la página sale bien y el 404 aparece
                        recién cuando alguien hace clic. Revisá si la ruta se renombró o si el
                        enlace quedó de una pantalla que ya no existe.
                        """)
                .isEmpty();
    }

    /** Patrones registrados por Spring, con las variables normalizadas a {}. */
    private Set<String> registeredRoutes() {
        Set<String> out = new LinkedHashSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            info.getPathPatternsCondition().getPatternValues()
                    .forEach(pattern -> out.add(normalize(pattern)));
        }
        return out;
    }

    /**
     * Reduce cualquier forma de escribir una URL a su patrón de ruta:
     * {@code @{/products/{id}(id=${p.id})}} y {@code '/products/' + ${p.id}}
     * caen las dos en {@code /products/{}}.
     */
    private static String normalize(String url) {
        String u = url.trim();
        u = u.replaceAll("\\([^)]*\\)", "");            // parámetros de @{...(a=b)}
        u = u.replaceAll("\\{[^}]*\\}", "{}");          // {id} de Thymeleaf y de Spring
        u = u.replaceAll("'\\s*\\+\\s*\\$\\{[^}]*\\}\\s*\\+\\s*'", "{}");
        u = u.replaceAll("'\\s*\\+\\s*\\$\\{[^}]*\\}", "{}");
        u = u.replace("'", "").split("\\?")[0];
        u = u.replaceAll("/{2,}", "/");
        return u.length() > 1 && u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    /** URLs que viajan como argumento de un fragment en vez de en un th:href. */
    private static final List<Pattern> FRAGMENT_ARGS = List.of(
            Pattern.compile("(?:iconLink|iconDelete|iconSubmit|pill)\\(\\s*([^,]+),"),
            Pattern.compile("buttons :: link\\(\\s*([^,]+),"),
            Pattern.compile("breadcrumb :: back\\(\\s*'[^']*',\\s*([^)]+)\\)"),
            Pattern.compile("breadcrumb :: trail\\(\\s*'[^']*',\\s*([^,]+),"),
            Pattern.compile("listBar\\(\\s*([^,]+),"),
            Pattern.compile("pagination :: bar\\(\\s*[^,]+,\\s*([^,]+),"));

    private static Map<String, Set<String>> linksInTemplates() {
        Map<String, Set<String>> links = new LinkedHashMap<>();

        for (Path p : templates()) {
            String s = read(p).replaceAll("(?s)<!--.*?-->", "");
            String rel = TEMPLATES.relativize(p).toString().replace('\\', '/');

            for (String raw : linkExpressions(s)) {
                add(links, raw, rel);
            }
            for (Pattern pattern : FRAGMENT_ARGS) {
                Matcher m = pattern.matcher(s);
                while (m.find()) {
                    add(links, m.group(1), rel);
                }
            }
        }
        return links;
    }

    private static void add(Map<String, Set<String>> links, String raw, String where) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String url = normalize(raw);
        if (url.startsWith("/")) {
            links.computeIfAbsent(url, k -> new LinkedHashSet<>()).add(where);
        }
    }

    /**
     * Devuelve el contenido de cada expresión de enlace {@code @{...}}.
     *
     * <p>Se recorre contando llaves en vez de usar una expresión regular: el
     * contenido lleva llaves anidadas —{@code @{/products/{id}(id=${p.id})}}—
     * y cualquier regex simple corta en la primera que encuentra, dejando
     * "/products/{id". Ese error de parseo dio tres rondas de falsos positivos
     * antes de escribirlo así.
     */
    private static List<String> linkExpressions(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while ((i = s.indexOf("@{", i)) >= 0) {
            int depth = 0;
            int j = i + 1;                 // parado en la '{' de "@{"
            for (; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
            }
            if (j >= s.length()) {
                break;                     // expresión sin cerrar: la ignora
            }
            out.add(s.substring(i + 2, j));
            i = j + 1;
        }
        return out;
    }

    private static List<Path> templates() {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            return files.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("toda vista que devuelve un controller existe como archivo")
    void everyViewNameResolvesToATemplate() {
        Path java = Path.of("src/main/java/com/malva_pastry_shop/backend/controller");
        Pattern viewName = Pattern.compile("return \"([a-z]+/[a-zA-Z]+)\"");
        List<String> missing = new ArrayList<>();

        try (Stream<Path> files = Files.walk(java)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = viewName.matcher(read(p));
                while (m.find()) {
                    Path template = TEMPLATES.resolve(m.group(1) + ".html");
                    if (!Files.exists(template)) {
                        missing.add(p.getFileName() + " devuelve \"" + m.group(1) + "\", que no existe");
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertThat(missing)
                .as("Un nombre de vista sin plantilla revienta con 500 al llegar a esa rama del "
                        + "controller, que suele ser la de error y por eso la que menos se prueba.")
                .isEmpty();
    }
}
