package com.malva_pastry_shop.backend.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariantes de las plantillas, comprobadas leyendo los archivos.
 *
 * <p>No levanta Spring ni renderiza nada: son comprobaciones estáticas que
 * corren en milisegundos. Existen porque cada una corresponde a una forma de
 * romper el panel que ya ocurrió y que <b>no falla en ningún lado</b>: la
 * página se sirve igual y el defecto aparece cuando alguien la mira.
 *
 * <p>Los tests de renderizado siguen siendo imprescindibles para la lógica de
 * cada plantilla. Estos cubren lo transversal, que es justamente lo que un test
 * por plantilla no ve.
 */
@DisplayName("Invariantes de las plantillas")
class TemplateInvariantsTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path CSS = Path.of("src/main/resources/static/css/admin.css");
    private static final Path JS = Path.of("src/main/resources/static/js");

    // ---------- utilidades ----------

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

    /**
     * Los comentarios HTML se sirven al navegador, y los de este proyecto
     * explican qué markup reemplazaron: contienen las mismas cadenas que estos
     * tests buscan. Sin quitarlos, los tests se disparan con su propia
     * documentación (pasó tres veces).
     */
    private static String withoutComments(String html) {
        return html.replaceAll("(?s)<!--.*?-->", "");
    }

    private static String rel(Path p) {
        return TEMPLATES.relativize(p).toString().replace('\\', '/');
    }

    // ---------- 1. El CSS compilado contiene toda clase que se usa ----------

    /**
     * Tokens que aparecen donde este test busca clases pero no lo son.
     *
     * <p>Casi todos son literales de SpEL: los valores de `tone` que comparan
     * los fragments de botones, y los nombres de campo que recibe
     * {@code #fields.hasErrors('x')}. Si al agregar un tono nuevo este test
     * falla, el arreglo es sumarlo acá; si falla por cualquier otra cosa, es
     * una clase que falta de verdad.
     */
    private static final Set<String> NOT_CLASSES = Set.of(
            // valores de `tone` de fragments/buttons.html
            "primary", "secondary", "danger", "ghost", "edit", "info", "success", "neutral", "safe",
            // nombres de campo de #fields.hasErrors(...)
            "name", "lastName", "email", "password", "roleId", "productId", "quantity",
            "unitPrice", "unitCost", "unitOfMeasure", "customerName", "customerDni", "customerPhone",
            // rol comparado en users/*
            "ADMIN",
            // enganche de JS, no de estilo
            "edit-btn");

    /** Clases que se sabe que no hacen nada, con su motivo. */
    private static final Map<String, String> KNOWN_NOOP = Map.of(
            "prose", "requiere @tailwindcss/typography, que nunca se cargó (tampoco con el Play CDN)");

    @Test
    @DisplayName("toda clase usada en una plantilla existe en el CSS compilado")
    void everyClassUsedIsCompiled() {
        Set<String> generated = compiledClasses();
        Map<String, Set<String>> used = usedClasses();

        Map<String, Set<String>> missing = new TreeMap<>();
        used.forEach((cls, where) -> {
            if (!generated.contains(cls) && !NOT_CLASSES.contains(cls) && !KNOWN_NOOP.containsKey(cls)) {
                missing.put(cls, where);
            }
        });

        assertThat(missing)
                .as("""
                        Hay clases de Tailwind usadas en plantillas que NO están en el CSS compilado.

                        Esto no rompe nada visible en los logs: la página se sirve igual y solo se
                        ve mal. Casi siempre significa que falta correr el build tras escribir
                        markup nuevo:

                            cd backend && npm run css

                        Si el token no es una clase (un literal de SpEL, un nombre de campo),
                        sumalo a NOT_CLASSES en este test.
                        """)
                .isEmpty();
    }

    // Se probó además comparar la fecha del CSS contra la de las plantillas, y
    // es un mal test: salta con un cambio de comentario, que no genera ninguna
    // clase, y git no preserva mtimes, así que sería intermitente después de
    // cada clone. El test de arriba comprueba el invariante de verdad —que la
    // clase exista— y es determinista.

    /** Selectores de clase del CSS, des-escapados: {@code .focus\\:ring-2:focus} -> {@code focus:ring-2}. */
    private static Set<String> compiledClasses() {
        String css = read(CSS);
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\.((?:\\\\.|[-\\w\\[\\]()%./!])+)").matcher(css);
        while (m.find()) {
            out.add(m.group(1).replace("\\", ""));
        }
        return out;
    }

    /** Clases declaradas en las plantillas y en el JS que las manipula. */
    private static Map<String, Set<String>> usedClasses() {
        Map<String, Set<String>> used = new LinkedHashMap<>();
        Pattern staticClass = Pattern.compile("\\bclass=\"([^\"]*)\"");
        Pattern dynamicClass = Pattern.compile("th:class(?:append)?=\"(.*?)\"", Pattern.DOTALL);
        Pattern literal = Pattern.compile("'([^']*)'");

        for (Path p : templates()) {
            String s = withoutComments(read(p));
            Matcher m = staticClass.matcher(s);
            while (m.find()) {
                String value = m.group(1);
                // Un class="..." con expresión adentro no es una lista de clases.
                if (!value.contains("${") && !value.contains("__")) {
                    addTokens(used, value, rel(p));
                }
            }
            m = dynamicClass.matcher(s);
            while (m.find()) {
                Matcher lit = literal.matcher(m.group(1));
                while (lit.find()) {
                    addTokens(used, lit.group(1), rel(p));
                }
            }
        }
        return used;
    }

    private static void addTokens(Map<String, Set<String>> used, String value, String where) {
        for (String token : value.trim().split("\\s+")) {
            if (!token.isEmpty() && token.matches("[-a-zA-Z0-9:/.\\[\\]()%_]+")
                    && !Character.isDigit(token.charAt(0))) {
                used.computeIfAbsent(token, k -> new LinkedHashSet<>()).add(where);
            }
        }
    }

    // ---------- 2. Las llamadas a fragments coinciden con su firma ----------

    @Test
    @DisplayName("toda llamada a un fragment existe y pasa la cantidad de argumentos que declara")
    void everyFragmentCallMatchesItsSignature() {
        Map<String, Integer> signatures = fragmentSignatures();
        List<String> problems = new ArrayList<>();

        Pattern call = Pattern.compile("~\\{fragments/(\\w+)\\s*::\\s*(\\w+)(\\(((?:[^()]|\\([^()]*\\))*)\\))?",
                Pattern.DOTALL);

        for (Path p : templates()) {
            String s = withoutComments(read(p));
            Matcher m = call.matcher(s);
            while (m.find()) {
                String key = m.group(1) + " :: " + m.group(2);
                Integer declared = signatures.get(key);
                if (declared == null) {
                    problems.add(rel(p) + " llama a " + key + ", que no existe");
                    continue;
                }
                String args = m.group(4);
                int given = (m.group(3) == null || args == null || args.isBlank()) ? 0 : splitArgs(args).size();
                if (given != declared) {
                    problems.add(rel(p) + " llama a " + key + " con " + given
                            + " argumentos, y el fragment declara " + declared);
                }
            }
        }

        assertThat(problems)
                .as("Thymeleaf no valida esto en compilación: una llamada con argumentos de más "
                        + "o de menos revienta la página al abrirla, y nada más lo detecta.")
                .isEmpty();
    }

    private static Map<String, Integer> fragmentSignatures() {
        Map<String, Integer> out = new TreeMap<>();
        Pattern withArgs = Pattern.compile("th:fragment=\"(\\w+)\\(([^)]*)\\)\"");
        Pattern noArgs = Pattern.compile("th:fragment=\"(\\w+)\"");

        for (Path p : templates()) {
            if (!p.getParent().getFileName().toString().equals("fragments")) {
                continue;
            }
            String file = p.getFileName().toString().replace(".html", "");
            String s = read(p);
            Matcher m = withArgs.matcher(s);
            while (m.find()) {
                out.put(file + " :: " + m.group(1), splitArgs(m.group(2)).size());
            }
            m = noArgs.matcher(s);
            while (m.find()) {
                out.putIfAbsent(file + " :: " + m.group(1), 0);
            }
        }
        return out;
    }

    /** Separa argumentos de nivel superior: ignora comas dentro de (), {}, [] o comillas. */
    private static List<String> splitArgs(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean quoted = false;
        for (char c : s.toCharArray()) {
            if (c == '\'') {
                quoted = !quoted;
            }
            if (!quoted) {
                if (c == '(' || c == '{' || c == '[') {
                    depth++;
                } else if (c == ')' || c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    if (!current.toString().isBlank()) {
                        out.add(current.toString().trim());
                    }
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (!current.toString().isBlank()) {
            out.add(current.toString().trim());
        }
        return out;
    }

    // ---------- 3. El JS inline no se expande ----------

    /**
     * Ninguna. Ya no queda JavaScript embebido en ninguna plantilla: todo el
     * comportamiento vive en archivos de {@code static/js/} enganchados por
     * atributos {@code data-*}.
     *
     * <p>Eso deja al panel en condiciones de servir una Content-Security-Policy
     * sin {@code 'unsafe-inline'}, que es el motivo de haber empezado por acá.
     * <b>Falta configurar la cabecera</b>: mientras no exista, esto es una
     * precondición cumplida y nada más.
     *
     * <p>El conjunto vacío es a propósito: si aparece un manejador inline
     * nuevo, el test falla y nombra la plantilla.
     */
    private static final Set<String> INLINE_JS_PENDING = Set.of();

    @Test
    @DisplayName("el JavaScript inline solo puede desaparecer, nunca aparecer")
    void inlineJavaScriptDoesNotSpread() {
        Pattern handler = Pattern.compile("\\bon(submit|click|input|error|change)=");
        Pattern block = Pattern.compile("<script(?![^>]*\\bsrc)");

        Set<String> found = new LinkedHashSet<>();
        for (Path p : templates()) {
            String s = withoutComments(read(p));
            if (handler.matcher(s).find() || block.matcher(s).find()) {
                found.add(rel(p));
            }
        }

        assertThat(found)
                .as("""
                        Cambió el conjunto de plantillas con JavaScript embebido.

                        Si migraste una, sacala de INLINE_JS_PENDING en este test: la lista es el
                        marcador de cuánto falta para poder servir una CSP sin 'unsafe-inline'.
                        Si aparece una nueva, el markup nuevo trajo un manejador inline y conviene
                        moverlo a un archivo de static/js/ como se hizo con el resto.
                        """)
                .isEqualTo(INLINE_JS_PENDING);
    }

    // ---------- 4. Ninguna condición junto a un th:replace ----------

    /**
     * Es la regla que más veces rompió este proyecto, y la única que además es
     * un agujero de seguridad.
     *
     * <p>{@code th:replace} tiene precedencia 100; {@code th:if} y
     * {@code sec:authorize}, 300. En el mismo elemento el reemplazo ocurre
     * primero y se lleva puesto el elemento con sus otros atributos, así que la
     * condición <b>nunca se evalúa</b> y el fragment se dibuja siempre. Con
     * {@code sec:authorize} eso significa que un ítem restringido aparece con
     * cualquier rol.
     *
     * <p>La forma correcta es envolver en un {@code <th:block>} con la
     * condición, o poner el {@code th:if} adentro del fragment, sobre el
     * elemento que lleva {@code th:fragment} (precedencia 800).
     */
    @Test
    @DisplayName("ninguna condición convive con un th:replace en el mismo elemento")
    void noConditionOnAnElementThatIsReplaced() {
        // Cada etiqueta completa, para poder mirar sus atributos juntos.
        Pattern tag = Pattern.compile("<[a-zA-Z][a-zA-Z0-9:]*\\s[^>]*>", Pattern.DOTALL);
        Pattern replace = Pattern.compile("\\bth:(replace|insert)\\s*=");
        Pattern condition = Pattern.compile("\\b(th:if|th:unless|sec:authorize)\\s*=");

        List<String> problems = new ArrayList<>();
        for (Path p : templates()) {
            Matcher m = tag.matcher(withoutComments(read(p)));
            while (m.find()) {
                String element = m.group();
                if (replace.matcher(element).find() && condition.matcher(element).find()) {
                    problems.add(rel(p) + ": " + element.replaceAll("\\s+", " ").trim());
                }
            }
        }

        assertThat(problems)
                .as("""
                        Hay una condición en el mismo elemento que un th:replace, y por precedencia
                        no se va a evaluar: el fragment se dibuja SIEMPRE.

                        Con sec:authorize esto es una fuga de permisos, no un defecto visual.

                        Envolvé en <th:block th:if="..."> o <th:block sec:authorize="...">.
                        """)
                .isEmpty();
    }

    // ---------- 5. Los formularios POST llevan token CSRF ----------

    /**
     * Spring Security inyecta el hidden {@code _csrf} a través del
     * {@code RequestDataValueProcessor}, que se dispara con {@code th:action}.
     * Un formulario POST escrito con {@code action="..."} plano no lo recibe y
     * la cadena de seguridad lo rechaza con 403.
     *
     * <p>No es hipotético: el modal de products/recipe estuvo así desde antes
     * de esta migración, y "editar cantidad" mostraba la página de sesión
     * expirada en vez de guardar.
     */
    @Test
    @DisplayName("todo formulario POST lleva th:action o el token CSRF explícito")
    void everyPostFormCarriesCsrf() {
        Pattern form = Pattern.compile("<form\\b[^>]*>", Pattern.DOTALL);
        List<String> problems = new ArrayList<>();

        for (Path p : templates()) {
            String s = withoutComments(read(p));
            Matcher m = form.matcher(s);
            while (m.find()) {
                String open = m.group();
                boolean isPost = open.contains("method=\"post\"");
                boolean delegates = open.contains("th:replace") || open.contains("th:fragment");
                if (!isPost || delegates || open.contains("th:action")) {
                    continue;
                }
                // Sin th:action: el token tiene que estar escrito a mano dentro.
                int close = s.indexOf("</form>", m.end());
                String body = close > 0 ? s.substring(m.end(), close) : "";
                if (!body.contains("_csrf")) {
                    problems.add(rel(p) + ": " + open.replaceAll("\\s+", " ").trim());
                }
            }
        }

        assertThat(problems)
                .as("Un POST sin token CSRF lo rechaza la cadena de seguridad con 403. "
                        + "Usá th:action, que hace que Spring Security inyecte el hidden, o "
                        + "escribí el token a mano si la URL la fija el JS.")
                .isEmpty();
    }

    // ---------- 6. No vuelven los idiomas que se retiraron ----------

    @Test
    @DisplayName("ninguna plantilla vuelve a formatear importes a mano")
    void noTemplateFormatsMoneyByHand() {
        List<String> offenders = new ArrayList<>();
        for (Path p : templates()) {
            if (withoutComments(read(p)).contains("#numbers.formatDecimal")) {
                offenders.add(rel(p));
            }
        }

        assertThat(offenders)
                .as("""
                        #numbers.formatDecimal volvió a aparecer. Su forma de tres argumentos no
                        emite separador de miles, y el separador decimal queda a merced del
                        Accept-Language del navegador.

                        Usá ${@money.format(x)} para importes y ${@money.quantity(x)} para medidas.
                        """)
                .isEmpty();
    }

    @Test
    @DisplayName("ninguna plantilla vuelve a escribir su propia paginación")
    void noTemplateRollsItsOwnPagination() {
        List<String> offenders = new ArrayList<>();
        for (Path p : templates()) {
            if (rel(p).equals("fragments/pagination.html")) {
                continue;
            }
            if (withoutComments(read(p)).contains("totalPages")) {
                offenders.add(rel(p));
            }
        }

        assertThat(offenders)
                .as("Hay paginación escrita a mano. Usá ~{fragments/pagination :: bar(...)}, "
                        + "que además recorta a una ventana de 5 páginas en vez de dibujar "
                        + "un botón por página.")
                .isEmpty();
    }

    // ---------- 7. Los archivos de JS que se enlazan existen ----------

    @Test
    @DisplayName("todo script enlazado por una plantilla existe en static/js")
    void everyLinkedScriptExists() {
        Pattern src = Pattern.compile("th:src=\"@\\{/js/([^}\"]+)\\}\"");
        List<String> missing = new ArrayList<>();

        for (Path p : templates()) {
            Matcher m = src.matcher(withoutComments(read(p)));
            while (m.find()) {
                if (!Files.exists(JS.resolve(m.group(1)))) {
                    missing.add(rel(p) + " enlaza /js/" + m.group(1) + ", que no existe");
                }
            }
        }

        // Un 404 de script no rompe la página: simplemente el comportamiento
        // desaparece, que es peor porque no deja rastro.
        assertThat(missing).isEmpty();
    }
}
