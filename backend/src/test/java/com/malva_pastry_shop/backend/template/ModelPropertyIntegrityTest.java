package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.inventory.ProductIngredient;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.domain.sales.SaleIngredient;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSectionProduct;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cruza cada {@code ${entidad.propiedad}} de las plantillas contra las
 * propiedades reales de las clases de dominio, siguiendo la herencia.
 *
 * <p>Existe por un caso concreto: {@code users/show} pedía
 * {@code user.createdAt}, que no existe —{@link User} hereda
 * {@code insertedAt}/{@code updatedAt} de {@code TimestampedEntity}—. SpEL
 * falla al evaluarlo y el render se corta ahí. Como Thymeleaf ya había enviado
 * la cabecera, la respuesta salía con <b>status 200</b> y la página terminaba a
 * la mitad: 17.028 bytes sin {@code </html>} en vez de 20.567 completos. Un
 * barrido que mira el status da la página por buena.
 *
 * <p>La otra mitad del arreglo es
 * {@code spring.thymeleaf.servlet.produce-partial-output-while-processing=false}
 * (ver {@link RenderCompletenessTest}), que convierte ese 200 truncado en un
 * 500 honesto. Este test ataca la causa más frecuente antes de llegar a correr
 * nada.
 *
 * <h2>Lo que NO cubre</h2>
 * <ul>
 *   <li>{@code *{...}} contra el {@code th:object}: son DTOs de formulario, no
 *       dominio.</li>
 *   <li>Cualquier cosa detrás de una colección o un {@code Page}: la erasure
 *       borra el tipo de elemento, así que el recorrido se detiene ahí.</li>
 *   <li>Accesos sobre {@code Map}, donde toda clave es válida.</li>
 * </ul>
 * En los tres casos el recorrido para y no reporta nada, que es lo correcto:
 * un detector que adivina es peor que uno que declara su límite.
 */
@DisplayName("Integridad de las propiedades del modelo")
class ModelPropertyIntegrityTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    /**
     * Raíz de expresión -> tipo, declarado a mano y a propósito.
     *
     * <p>Inferirlo por nombre no funciona ({@code section} es
     * {@code StorefrontSection}, {@code sp} y {@code pi} no se parecen a nada),
     * y sacarlo de los controllers exigiría resolver el tipo de elemento de
     * cada {@code List<T>}, que la erasure no deja ver.
     *
     * <p>Las raíces de bucle salen de los {@code th:each}. Ojo con
     * {@code ing}/{@code pi}: iteran la <b>misma</b> colección
     * ({@code ${ingredients}}) con tipos distintos, y ninguno de los dos es
     * {@code Ingredient} —{@code ing} es un {@link SaleIngredient}, que guarda
     * el nombre y el costo congelados al momento de la venta—. Este mapa
     * empezó diciendo {@code ing -> Ingredient} y el propio test lo desmintió
     * en la primera corrida.
     */
    private static final Map<String, Class<?>> MODEL_TYPES = Map.ofEntries(
            Map.entry("category", Category.class),
            Map.entry("ingredient", Ingredient.class),
            Map.entry("ing", SaleIngredient.class),
            Map.entry("pi", ProductIngredient.class),
            Map.entry("product", Product.class),
            Map.entry("sale", Sale.class),
            Map.entry("section", StorefrontSection.class),
            Map.entry("sp", StorefrontSectionProduct.class),
            Map.entry("tag", Tag.class),
            Map.entry("unit", UnitOfMeasure.class),
            Map.entry("user", User.class));

    // ---------- 1. Toda propiedad existe ----------

    @Test
    @DisplayName("toda propiedad que pide una plantilla existe en su clase de dominio")
    void everyPropertyExistsOnItsType() {
        Map<String, List<String>> problems = new TreeMap<>();

        for (Path p : templates()) {
            String rel = TEMPLATES.relativize(p).toString().replace('\\', '/');
            List<String> found = unknownProperties(read(p));
            if (!found.isEmpty()) {
                problems.put(rel, found);
            }
        }

        assertThat(problems)
                .as("""
                        Hay plantillas que piden propiedades que no existen en el dominio.

                        Esto no falla en compilación y, sin la propiedad de buffering puesta,
                        tampoco falla al servir: SpEL revienta a mitad de render, la respuesta
                        ya salió con 200 y la página termina cortada, sin </html>.

                        Si la propiedad se renombró, actualizá la plantilla. Si la raíz es
                        nueva, sumala a MODEL_TYPES en este test.
                        """)
                .isEmpty();
    }

    // ---------- 2. El detector se comprueba a sí mismo ----------

    /**
     * Control negativo: un detector que devuelve la lista vacía es
     * indistinguible de un detector roto.
     *
     * <p>Cuando se escribió, la comprobación fue manual —reintroducir
     * {@code user.createdAt} y ver que saltara—. Manual significa que se pierde
     * apenas alguien toca la extracción. Acá queda fija: el mismo motor que usa
     * el test de arriba recibe el bug original y tiene que reportarlo.
     */
    @Test
    @DisplayName("el detector reporta el bug que lo originó")
    void theDetectorActuallyDetects() {
        assertThat(unknownProperties("<span th:text=\"${user.createdAt}\">"))
                .as("el detector dejó de encontrar el caso que lo originó: "
                        + "si la extracción se rompió, el test de arriba pasa por vacío")
                .containsExactly("user.createdAt (User no tiene 'createdAt')");

        assertThat(unknownProperties("<span th:text=\"${sale.registeredBy.fullName}\">"))
                .as("una cadena válida de tres niveles no puede dar falso positivo")
                .isEmpty();

        assertThat(unknownProperties("<span th:text=\"${user.role.name.name}\">"))
                .as("SpEL resuelve 'x()' además de getX()/isX(): sin eso, "
                        + "RoleType.name() sale como falso positivo")
                .isEmpty();

        assertThat(unknownProperties("<span th:text=\"${products.content.loQueSea}\">"))
                .as("una raíz que no está declarada no se inventa un tipo")
                .isEmpty();
    }

    // ---------- 3. Ninguna entrada declarada de más ----------

    @Test
    @DisplayName("toda raíz declarada en MODEL_TYPES sigue usándose en alguna plantilla")
    void everyDeclaredRootIsStillUsed() {
        Set<String> roots = new LinkedHashSet<>();
        for (Path p : templates()) {
            for (String expression : variableExpressions(withoutComments(read(p)))) {
                chains(stripLiterals(expression)).forEach(c -> roots.add(c.parts().getFirst()));
            }
        }

        List<String> unused = MODEL_TYPES.keySet().stream().filter(root -> !roots.contains(root)).sorted().toList();

        assertThat(unused)
                .as("Hay raíces declaradas que ya no usa ninguna plantilla. Una entrada que "
                        + "sobrevive a su motivo deja de documentar y pasa a tapar: si mañana "
                        + "vuelve una variable con ese nombre y otro tipo, se valida contra el "
                        + "tipo viejo.")
                .isEmpty();
    }

    // ---------- el motor ----------

    /**
     * Devuelve las propiedades inexistentes de un fragmento de markup.
     *
     * <p>Es una función pura sobre el texto —no lee del disco— justamente para
     * que {@link #theDetectorActuallyDetects()} pueda alimentarla con casos
     * sintéticos.
     */
    private static List<String> unknownProperties(String html) {
        List<String> problems = new ArrayList<>();
        Map<String, String> seen = new LinkedHashMap<>();

        for (String expression : variableExpressions(withoutComments(html))) {
            // __${...}__ es preprocesamiento: el nombre real no está acá.
            if (expression.contains("__")) {
                continue;
            }
            for (Chain chain : chains(stripLiterals(expression))) {
                String problem = walk(chain);
                if (problem != null) {
                    seen.putIfAbsent(problem, "");
                }
            }
        }
        problems.addAll(seen.keySet());
        return problems;
    }

    /** Una cadena {@code a.b.c}, y si la última parte se invoca como método. */
    private record Chain(List<String> parts, boolean lastIsCall) {
        String upTo(int i) {
            return String.join(".", parts.subList(0, i + 1));
        }
    }

    /** Recorre la cadena resolviendo tipo a tipo. Devuelve el problema, o null. */
    private static String walk(Chain chain) {
        Class<?> type = MODEL_TYPES.get(chain.parts().getFirst());
        if (type == null) {
            return null;
        }

        for (int i = 1; i < chain.parts().size(); i++) {
            String part = chain.parts().get(i);
            boolean isCall = chain.lastIsCall() && i == chain.parts().size() - 1;

            Class<?> next = isCall ? methodReturn(type, part) : propertyType(type, part);
            if (next == null) {
                return chain.upTo(i) + " (" + type.getSimpleName() + " no tiene '" + part + "')";
            }
            // Detrás de una colección o un Map no hay tipo que seguir: la
            // erasure ya borró el del elemento. Se corta sin reportar.
            if (Collection.class.isAssignableFrom(next) || Map.class.isAssignableFrom(next)
                    || next == Object.class) {
                return null;
            }
            type = next;
        }
        return null;
    }

    /**
     * Resuelve una propiedad como lo hace el {@code ReflectivePropertyAccessor}
     * de SpEL: {@code getX()}, {@code isX()} y también {@code x()} a secas.
     *
     * <p>La tercera forma no es un detalle: sin ella {@code ${unit.toString}} y
     * {@code ${user.role.name.name}} —que cae en {@code RoleType.name()}— salen
     * como falsos positivos.
     */
    private static Class<?> propertyType(Class<?> type, String property) {
        String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (String candidate : List.of("get" + capitalized, "is" + capitalized, property)) {
            Class<?> returned = methodReturn(type, candidate);
            if (returned != null) {
                return returned;
            }
        }
        try {
            return type.getField(property).getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /** {@code getMethods()} ya trae los heredados, así que la herencia sale gratis. */
    private static Class<?> methodReturn(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0 && m.getReturnType() != void.class) {
                return m.getReturnType();
            }
        }
        return null;
    }

    /**
     * Contenido de cada {@code ${...}}, contando llaves.
     *
     * <p>Con una regex esto corta en la primera llave de
     * {@code @{/products/{id}(id=${product.id})}} y pierde la expresión. Es el
     * mismo error que dio tres rondas de falsos positivos en
     * {@code RouteIntegrityTest}.
     */
    private static List<String> variableExpressions(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while ((i = s.indexOf("${", i)) >= 0) {
            int depth = 0;
            int j = i + 1;
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
                break;
            }
            out.add(s.substring(i + 2, j));
            i = j + 1;
        }
        return out;
    }

    /** Cadenas {@code a.b.c} de una expresión, marcando si la última se invoca. */
    private static List<Chain> chains(String expression) {
        List<Chain> out = new ArrayList<>();
        int i = 0;
        while (i < expression.length()) {
            if (!isIdentifierStart(expression.charAt(i)) || (i > 0 && isIdentifierPart(expression.charAt(i - 1)))) {
                i++;
                continue;
            }
            int start = i;
            while (i < expression.length()
                    && (isIdentifierPart(expression.charAt(i)) || expression.charAt(i) == '.')) {
                i++;
            }
            String raw = expression.substring(start, i);
            if (raw.contains(".") && !raw.endsWith(".")) {
                boolean isCall = i < expression.length() && expression.charAt(i) == '(';
                out.add(new Chain(List.of(raw.split("\\.")), isCall));
            }
        }
        return out;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Un literal 'a.b' no es un acceso a propiedad. */
    private static String stripLiterals(String expression) {
        return expression.replaceAll("'[^']*'", "''");
    }

    private static String withoutComments(String html) {
        return html.replaceAll("(?s)<!--.*?-->", "");
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
}
