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
import java.util.regex.MatchResult;
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
     *
     * <p>La lista la mantiene honesta {@link #noExceptionOutlivesItsReason()}:
     * una entrada que ya no aparece en ninguna plantilla no filtra nada y hay
     * que sacarla.
     */
    private static final Set<String> NOT_CLASSES = Set.of(
            // valores de `tone` de fragments/buttons.html
            "primary", "danger", "ghost", "edit", "info", "success",
            // nombres de campo de #fields.hasErrors(...)
            "productId", "quantity", "unitPrice", "customerName", "customerDni", "customerPhone",
            // rol comparado en users/*
            "ADMIN",
            // enganche de JS, no de estilo
            "edit-btn");

    /**
     * Clases que se sabe que no hacen nada, con su motivo.
     *
     * <p>Vacío, y a propósito. Tuvo {@code prose}, que necesitaba
     * {@code @tailwindcss/typography} —un plugin que nunca se cargó, tampoco
     * con el Play CDN—. Al reescribir las cuatro fichas esos contenedores se
     * volvieron {@code <p>} y la clase desapareció sola.
     */
    private static final Map<String, String> KNOWN_NOOP = Map.of();

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

    /**
     * Las dos listas de excepciones de arriba solo pueden crecer, y una
     * excepción que sobrevive a su motivo deja de documentar: pasa a
     * <b>suprimir</b>.
     *
     * <p>Pasó con {@code prose}. Se declaró en {@code KNOWN_NOOP} porque las
     * cuatro fichas la usaban sin que el plugin de tipografía estuviera
     * cargado; al reescribir las fichas esos contenedores se volvieron
     * {@code <p>} y la clase desapareció de las plantillas. La entrada quedó, y
     * a partir de ahí tapaba el aviso: si alguien volvía a escribir
     * {@code class="prose"}, el test callaba.
     *
     * <p>El invariante es la simetría — declarar una excepción tiene que costar
     * lo mismo que retirarla.
     */
    @Test
    @DisplayName("toda excepción declarada sigue haciendo falta")
    void noExceptionOutlivesItsReason() {
        Set<String> used = usedClasses().keySet();

        List<String> expired = Stream.concat(NOT_CLASSES.stream(), KNOWN_NOOP.keySet().stream())
                .filter(token -> !used.contains(token))
                .sorted()
                .toList();

        assertThat(expired)
                .as("""
                        Hay tokens declarados como excepción que ya no aparecen en ninguna
                        plantilla. No filtran nada, y si el token vuelve, lo dejan pasar sin
                        aviso: la excepción se aplica antes de que nadie mire.

                        Sacalos de NOT_CLASSES o de KNOWN_NOOP.
                        """)
                .isEmpty();
    }

    /**
     * El hueco que deja el test de arriba: compara <b>nombres</b> de clase, no
     * valores.
     *
     * <p>Si alguien cambia un color en {@code tailwind.config.js} y no corre
     * {@code npm run css}, la clase {@code bg-primary-600} sigue existiendo en
     * el CSS —con el color viejo—. Nada falla, la página se sirve, y el panel
     * queda pintado de la paleta anterior hasta el próximo build. Es el mismo
     * modo de fallo que motivó todo este archivo: el defecto no rompe nada,
     * solo espera a que alguien lo mire.
     *
     * <p>La etapa {@code css} del Dockerfile también lo corrige, pero recién en
     * el deploy. Esto lo detecta al correr los tests.
     */
    @Test
    @DisplayName("todo color declarado en tailwind.config.js está en el CSS compilado")
    void everyDeclaredColorIsCompiled() {
        String css = read(CSS);
        List<String> missing = new ArrayList<>();

        Matcher m = Pattern.compile("'(#[0-9a-fA-F]{6})'").matcher(read(TAILWIND_CONFIG));
        while (m.find()) {
            String hex = m.group(1).toLowerCase();
            // Tailwind emite el hex en los gradientes y la terna rgb en el
            // resto, según la utilidad. Con que aparezca de una forma alcanza.
            if (!css.toLowerCase().contains(hex) && !css.contains(rgbTriplet(hex))) {
                missing.add(hex);
            }
        }

        assertThat(missing)
                .as("""
                        Hay colores declarados en tailwind.config.js que no están en el CSS
                        compilado: el config cambió y el build no se volvió a correr.

                            cd backend && npm run css

                        Ningún otro test lo ve. everyClassUsedIsCompiled compara nombres de
                        clase, y el nombre no cambia cuando cambia el color: bg-primary-600
                        sigue existiendo, pintada del color anterior.
                        """)
                .isEmpty();
    }

    private static final Path TAILWIND_CONFIG = Path.of("tailwind.config.js");

    /** {@code #7347af} -> {@code 115 71 175}, la forma que emite Tailwind. */
    private static String rgbTriplet(String hex) {
        return Integer.parseInt(hex.substring(1, 3), 16) + " "
                + Integer.parseInt(hex.substring(3, 5), 16) + " "
                + Integer.parseInt(hex.substring(5, 7), 16);
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
     * <p>Eso es lo que permite servir una Content-Security-Policy sin
     * {@code 'unsafe-inline'}, que es el motivo de haber empezado por acá. La
     * cabecera ya se envía: {@code ADMIN_CSP} en {@code SecurityConfig}. O sea
     * que este test dejó de ser una precondición y pasó a sostener algo que
     * está en producción: si vuelve un manejador inline, la CSP lo descarta en
     * silencio y el comportamiento simplemente no ocurre.
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

    /**
     * La otra mitad de la precondición de la CSP, y la que nadie estaba
     * mirando: el ratchet de arriba cubre {@code script-src}, pero
     * {@code style-src 'self'} exige que tampoco haya estilos embebidos.
     *
     * <p>Hoy no hay ninguno, así que la política los prohíbe. Un
     * {@code style="..."} nuevo no rompería la página de forma visible: el
     * navegador descarta el estilo en silencio y el elemento se dibuja sin él.
     */
    @Test
    @DisplayName("tampoco hay estilos embebidos")
    void noInlineStylesEither() {
        Pattern inline = Pattern.compile("\\b(style=\"|th:style=)|<style[\\s>]");

        Set<String> found = new LinkedHashSet<>();
        for (Path p : templates()) {
            if (inline.matcher(withoutComments(read(p))).find()) {
                found.add(rel(p));
            }
        }

        assertThat(found)
                .as("""
                        Apareció un estilo embebido, y la CSP del panel declara style-src 'self'.

                        El navegador lo descarta sin avisar: el elemento se dibuja sin ese
                        estilo y no queda rastro en ningún log. Movelo a una clase de Tailwind
                        o a src/main/css/admin.css.
                        """)
                .isEmpty();
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

    /**
     * La regla de formato de importes vive en dos lenguajes y no hay forma de
     * evitarlo: el total que previsualiza {@code sales/create} se calcula en el
     * navegador y el que muestra la ficha lo formatea {@code @money.format} en
     * el servidor. Duplicar es inevitable; <b>divergir</b> no.
     *
     * <p>Ya divergió una vez: el JS usaba {@code toFixed(2)}, así que el total
     * previsualizado decía {@code $12.50} y la misma venta, ya guardada, salía
     * {@code $12,50} en la ficha.
     */
    @Test
    @DisplayName("el JavaScript formatea importes con el mismo locale que el servidor")
    void clientSideMoneyMatchesTheServerLocale() {
        List<String> offenders = new ArrayList<>();

        for (Path p : scripts()) {
            // Igual que con las plantillas: el comentario de sale-form.js
            // explica que ANTES usaba toFixed(2), así que sin quitarlo el test
            // se dispara con la documentación del propio arreglo.
            String s = withoutJsComments(read(p));
            String name = p.getFileName().toString();
            if (s.contains("toFixed(")) {
                offenders.add(name + " usa toFixed(), que da punto decimal");
            }
            Matcher m = Pattern.compile("Intl\\.NumberFormat\\(\\s*'([^']*)'").matcher(s);
            while (m.find()) {
                if (!m.group(1).equals(PANEL_LOCALE)) {
                    offenders.add(name + " formatea con locale '" + m.group(1) + "'");
                }
            }
        }

        assertThat(offenders)
                .as("""
                        El formato de importes del cliente no coincide con el del servidor.

                        El panel está fijo en es-AR (FixedLocaleResolver en WebMvcConfig), así
                        que un importe calculado en el navegador tiene que salir igual que el
                        que emite ${@money.format(x)}: separador decimal coma.

                        Usá new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2 }).
                        """)
                .isEmpty();
    }

    /** El mismo que fija el FixedLocaleResolver de WebMvcConfig. */
    private static final String PANEL_LOCALE = "es-AR";

    private static String withoutJsComments(String js) {
        return js.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static List<Path> scripts() {
        try (Stream<Path> files = Files.walk(JS)) {
            return files.filter(p -> p.toString().endsWith(".js")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("ninguna plantilla vuelve a formatear fechas a mano")
    void noTemplateFormatsDatesByHand() {
        List<String> offenders = new ArrayList<>();
        for (Path p : templates()) {
            if (withoutComments(read(p)).contains("#temporals.format")) {
                offenders.add(rel(p));
            }
        }

        assertThat(offenders)
                .as("""
                        Volvió #temporals.format. El patrón 'dd/MM/yyyy HH:mm' estaba escrito
                        a mano en 21 lugares de 13 plantillas, y ya había divergido: 2 sitios
                        protegían el nulo con un ternario y 16 no.

                        Usá ${@dates.format(x)} para fecha y hora, ${@dates.day(x)} para el
                        día y ${@dates.time(x)} para la hora. El guard de nulo es del
                        formateador, así que no se puede olvidar.
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

    // ---------- 7. Las confirmaciones salen de messages.properties ----------

    /**
     * Toda confirmación viene de una clave, no de un literal.
     *
     * <p>Es la regla que sostiene el trabajo de copy: el diagnóstico inicial
     * encontró <b>15 redacciones distintas para 14 pares (acción, entidad)</b>.
     * El par de más es la demostración: el borrado de tag se confirmaba en
     * {@code tags/list.html} y otra vez en {@code tags/show.html}, y las dos
     * copias ya habían divergido en tildes cuando se midió. Dos copias del
     * mismo texto en archivos distintos no se rompen: se separan, porque cada
     * una se ve bien por su cuenta.
     *
     * <p>Se detecta por el signo de apertura de pregunta, que es lo que tienen
     * en común todas y solo ellas.
     */
    @Test
    @DisplayName("ninguna confirmación está escrita como literal en una plantilla")
    void confirmationsComeFromMessages() {
        Map<String, Set<String>> literals = new TreeMap<>();

        for (Path p : templates()) {
            Matcher m = Pattern.compile("¿[^'\"<>]{4,120}\\?").matcher(withoutComments(read(p)));
            while (m.find()) {
                literals.computeIfAbsent(m.group(), k -> new LinkedHashSet<>()).add(rel(p));
            }
        }

        assertThat(literals)
                .as("""
                        Hay confirmaciones escritas a mano en las plantillas.

                        Van en messages.properties como confirm.*, y la plantilla las pide con
                        #{clave}. El motivo no es la traducción: es que la misma acción se
                        confirma desde el listado y desde la ficha, y con dos literales las dos
                        redacciones se separan sin que falle nada.
                        """)
                .isEmpty();
    }

    // ---------- 8. Los archivos de JS que se enlazan existen ----------

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

    // ---------- 9. Las columnas de dinero se alinean a la derecha ----------

    /**
     * Un importe alineado a la izquierda no se puede comparar de un vistazo:
     * {@code $1.200,00} y {@code $980,50} quedan con las comas en distinta
     * columna y hay que leer los dos números enteros para saber cuál es mayor.
     *
     * <p>El proyecto tenía {@code thRight} desde el principio y lo usaba
     * <b>solo</b> para "Acciones": las cinco columnas de importe iban con
     * {@code th}, o sea a la izquierda. No es un defecto que se vea como error
     * —la tabla se dibuja perfecta— y por eso sobrevivió a doce migraciones.
     *
     * <p>Comprueba las dos mitades a la vez, que es lo que hace útil el test:
     * el {@code <th>} de la columna y el {@code <td>} de la celda. Arreglar una
     * sola deja la tabla peor que antes.
     */
    @Test
    @DisplayName("toda columna que formatea dinero está alineada a la derecha, encabezado y celda")
    void moneyColumnsAreRightAligned() {
        List<String> problems = new ArrayList<>();
        for (Path p : templates()) {
            problems.addAll(moneyAlignmentProblems(rel(p), withoutComments(read(p))));
        }

        assertThat(problems)
                .as("""
                        Hay importes que no se alinean a la derecha.

                        La columna va con fragments/table :: thRight y la celda con text-right.
                        Las dos: con una sola, el título y los números quedan en bordes opuestos.
                        """)
                .isEmpty();
    }

    private static final Pattern THEAD = Pattern.compile("(?s)<thead\\b.*?</thead>");
    private static final Pattern TH_CALL =
            Pattern.compile("fragments/table\\s*::\\s*(thLeft|thCenter|thRight)\\s*\\(");
    private static final Pattern BODY_ROW = Pattern.compile("<tr\\s[^>]*th:each=");

    /**
     * Empareja la columna k del {@code <thead>} con la celda k de la fila de
     * datos. Paquete-privado para que el control negativo lo alimente con un
     * caso sintético.
     *
     * <p>Da por sentado que la plantilla tiene una sola tabla y una sola fila
     * {@code th:each} —cierto en las 12 del panel—. Si eso cambia, el
     * emparejamiento deja de valer, así que lo <b>reporta</b> en vez de callar:
     * un detector que se rinde en silencio es peor que no tenerlo.
     */
    static List<String> moneyAlignmentProblems(String where, String html) {
        List<String> out = new ArrayList<>();

        List<String> heads = THEAD.matcher(html).results().map(MatchResult::group).toList();
        if (heads.isEmpty() || !html.contains("@money.format")) {
            return out;
        }
        if (heads.size() > 1) {
            out.add(where + " tiene " + heads.size() + " <thead>: el emparejamiento"
                    + " columna↔celda de este test ya no aplica y hay que extenderlo");
            return out;
        }

        List<String> alignment = TH_CALL.matcher(heads.get(0)).results()
                .map(m -> m.group(1)).toList();

        Matcher row = BODY_ROW.matcher(html);
        if (!row.find()) {
            return out;
        }
        int end = html.indexOf("</tr>", row.start());
        List<String> cells = splitCells(html.substring(row.start(), end < 0 ? html.length() : end));

        if (cells.size() != alignment.size()) {
            out.add(where + " tiene " + alignment.size() + " columnas en el <thead> y "
                    + cells.size() + " celdas en la fila de datos");
            return out;
        }

        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            if (!cell.contains("@money.format")) {
                continue;
            }
            String openTag = cell.substring(0, Math.max(cell.indexOf('>') + 1, 1));
            if (!"thRight".equals(alignment.get(i))) {
                out.add(where + ": la columna " + (i + 1) + " formatea dinero y su encabezado"
                        + " usa " + alignment.get(i) + " en vez de thRight");
            }
            if (!openTag.contains("text-right")) {
                out.add(where + ": la celda de la columna " + (i + 1)
                        + " formatea dinero y no lleva text-right");
            }
        }
        return out;
    }

    /** Parte una fila en celdas. Las 12 tablas no anidan tablas, así que alcanza. */
    private static List<String> splitCells(String rowHtml) {
        List<String> cells = new ArrayList<>();
        Matcher m = Pattern.compile("<td[\\s>]").matcher(rowHtml);
        int start = -1;
        while (m.find()) {
            if (start >= 0) {
                cells.add(rowHtml.substring(start, m.start()));
            }
            start = m.start();
        }
        if (start >= 0) {
            cells.add(rowHtml.substring(start));
        }
        return cells;
    }

    /**
     * Un detector que devuelve la lista vacía es indistinguible de uno roto,
     * así que este caso sintético tiene que reportar siempre. Mismo contrato
     * que {@code ModelPropertyIntegrityTest#theDetectorActuallyDetects}.
     */
    @Test
    @DisplayName("el detector de alineación realmente detecta")
    void theAlignmentDetectorActuallyDetects() {
        String broken = """
                <table><thead><tr>
                  <th th:replace="~{fragments/table :: thLeft('Nombre')}"></th>
                  <th th:replace="~{fragments/table :: thLeft('Precio')}"></th>
                </tr></thead>
                <tbody>
                  <tr th:each="p : ${products.content}">
                    <td class="px-6 py-4"><span th:text="${p.name}"></span></td>
                    <td class="px-6 py-4"><span th:text="${@money.format(p.basePrice)}"></span></td>
                  </tr>
                </tbody></table>
                """;

        // Dos hallazgos: el th no es thRight y el td no lleva text-right.
        assertThat(moneyAlignmentProblems("sintetico.html", broken)).hasSize(2);
    }

    // ---------- 10. El estado vacío abarca todas las columnas ----------

    /**
     * {@code emptyRow} recibe el colspan como número literal y nada lo ata al
     * {@code <thead>}: agregar o quitar un {@code <th>} no lo actualiza. La
     * fila de "no hay nada" queda abarcando de menos y el resto de la tabla
     * vacío al lado.
     *
     * <p>No falla, no se loguea, y solo se ve cuando la tabla queda sin filas
     * —o sea casi nunca en desarrollo y siempre en una instalación nueva—.
     *
     * <p>Los 12 coincidían al escribir este test. Existe porque el PR que
     * reordenó {@code ingredients/deleted} de 6 a 7 columnas es exactamente el
     * momento en que se olvida.
     */
    @Test
    @DisplayName("el colspan del estado vacío coincide con las columnas del thead")
    void everyEmptyRowSpansItsHeaders() {
        List<String> problems = new ArrayList<>();
        for (Path p : templates()) {
            problems.addAll(emptyRowMismatches(rel(p), withoutComments(read(p))));
        }

        assertThat(problems)
                .as("""
                        El estado vacío de una tabla no abarca todas sus columnas.

                        El colspan de fragments/table :: emptyRow es un literal, así que
                        agregar o quitar un <th> no lo actualiza: hay que tocarlo a mano.
                        """)
                .isEmpty();
    }

    // <th[\s>] no matchea <thead> —la 'e' impide el corte de \b— ni <th:block>,
    // que sí matchearía con <th\b porque ':' no es carácter de palabra.
    private static final Pattern TH_TAG = Pattern.compile("<th[\\s>]");
    private static final Pattern EMPTY_ROW_CALL = Pattern.compile(
            "fragments/table\\s*::\\s*emptyRow\\(((?:[^()]|\\([^()]*\\))*)\\)", Pattern.DOTALL);

    /** Paquete-privado para que el control negativo lo alimente con un caso sintético. */
    static List<String> emptyRowMismatches(String where, String html) {
        List<String> out = new ArrayList<>();

        List<String> heads = THEAD.matcher(html).results().map(MatchResult::group).toList();
        if (heads.isEmpty()) {
            return out;
        }
        if (heads.size() > 1) {
            out.add(where + " tiene " + heads.size() + " <thead>: el emparejamiento"
                    + " thead↔emptyRow de este test ya no aplica y hay que extenderlo");
            return out;
        }

        int columns = (int) TH_TAG.matcher(heads.get(0)).results().count();

        Matcher call = EMPTY_ROW_CALL.matcher(html);
        while (call.find()) {
            List<String> args = splitArgs(call.group(1));
            if (args.size() < 2) {
                continue;   // lo reporta everyFragmentCallMatchesItsSignature
            }
            String colspan = args.get(1).trim();
            if (!colspan.equals(String.valueOf(columns))) {
                out.add(where + ": el thead tiene " + columns
                        + " columnas y el estado vacío declara colspan=" + colspan);
            }
        }
        return out;
    }

    /** Control negativo fijo, por el mismo motivo que el de alineación. */
    @Test
    @DisplayName("el detector de colspan realmente detecta")
    void theColspanDetectorActuallyDetects() {
        String broken = """
                <table><thead><tr>
                  <th th:replace="~{fragments/table :: thLeft('A')}"></th>
                  <th th:replace="~{fragments/table :: thLeft('B')}"></th>
                  <th th:replace="~{fragments/table :: thRight('C')}"></th>
                </tr></thead><tbody>
                  <tr th:replace="~{fragments/table :: emptyRow(${x}, 2, 'tag',
                          'T', 'S', null, null, null)}"></tr>
                </tbody></table>
                """;

        assertThat(emptyRowMismatches("sintetico.html", broken)).hasSize(1);
    }

    // ---------- 11. Ningún fragment comparte nombre con una etiqueta vecina ----------

    /**
     * {@code ~{archivo :: nombre}} es un <b>markup selector</b>: un nombre
     * suelto matchea un {@code th:fragment} <i>o</i> cualquier elemento con ese
     * nombre de etiqueta. Si el archivo tiene las dos cosas, el selector
     * devuelve varios elementos y se dibujan <b>todos</b>.
     *
     * <p>Fue el bug del encabezado. {@code table.html} declaraba
     * {@code th:fragment="th(label)"} sobre un {@code <th>}, y tenía otros dos
     * {@code <th>} hermanos —{@code thCenter} y {@code thRight}—. Cada
     * {@code :: th('X')} los seleccionaba a los tres: una tabla de 5 columnas
     * servía 13 celdas de encabezado, en los 12 listados, durante toda la vida
     * del fragment. Nada fallaba: la página se servía completa y con 200.
     *
     * <p>Lo que hace peligroso al patrón no es el nombre en sí. Hoy también hay
     * fragments llamados {@code select} y {@code textarea}, y no se rompen
     * porque sus elementos homónimos están <b>adentro</b> del propio fragment:
     * el selector devuelve el ancestro y el descendiente viaja dentro, no como
     * un segundo resultado. El bug aparece cuando el homónimo es un
     * <b>hermano</b>, fuera del subárbol del fragment.
     *
     * <p>Por eso el test comprueba la posición y no el nombre: prohibir todo
     * nombre que coincida con una etiqueta obligaría a renombrar {@code select},
     * {@code textarea} y {@code link} sin que ninguno esté roto.
     */
    @Test
    @DisplayName("ningún fragment comparte nombre con una etiqueta fuera de su propio subárbol")
    void noFragmentIsShadowedByASiblingTag() {
        List<String> problems = new ArrayList<>();
        for (Path p : templates()) {
            if (!p.getParent().getFileName().toString().equals("fragments")) {
                continue;
            }
            problems.addAll(shadowedFragments(rel(p), withoutComments(read(p))));
        }

        assertThat(problems)
                .as("""
                        Un fragment tiene el nombre de una etiqueta que también aparece fuera de él.

                        El selector va a devolver los dos y la plantilla va a dibujar de más, sin
                        fallar. Renombrá el fragment a algo que no sea un nombre de etiqueta
                        —fue el caso de `th`, que pasó a llamarse `thLeft`—.
                        """)
                .isEmpty();
    }

    private static final Pattern FRAGMENT_DECL = Pattern.compile("th:fragment=\"(\\w+)");

    /** Paquete-privado para que el control negativo lo alimente con un caso sintético. */
    static List<String> shadowedFragments(String where, String html) {
        List<String> out = new ArrayList<>();

        // Los fragments de estos archivos son hermanos de primer nivel, así que
        // el subárbol de cada uno va desde su declaración hasta la siguiente.
        List<MatchResult> decls = FRAGMENT_DECL.matcher(html).results().toList();

        for (int i = 0; i < decls.size(); i++) {
            MatchResult decl = decls.get(i);
            String name = decl.group(1);
            int from = decl.start();
            int to = (i + 1 < decls.size()) ? decls.get(i + 1).start() : html.length();

            Matcher tag = Pattern.compile("<" + Pattern.quote(name) + "[\\s>]").matcher(html);
            while (tag.find()) {
                if (tag.start() < from || tag.start() >= to) {
                    out.add(where + ": el fragment '" + name + "' comparte nombre con un <"
                            + name + "> que está fuera de su subárbol; el selector devuelve los dos");
                }
            }
        }
        return out;
    }

    /** Control negativo fijo: el archivo de encabezados como estaba cuando fallaba. */
    @Test
    @DisplayName("el detector de nombres ensombrecidos realmente detecta")
    void theShadowDetectorActuallyDetects() {
        String broken = """
                <th th:fragment="th(label)" scope="col" th:text="${label}" class="text-left">Columna</th>
                <th th:fragment="thCenter(label)" scope="col" th:text="${label}" class="text-center">Columna</th>
                <th th:fragment="thRight(label)" scope="col" th:text="${label}" class="text-right">Columna</th>
                """;

        // El fragment `th` queda ensombrecido por los <th> de thCenter y thRight.
        assertThat(shadowedFragments("table.html", broken)).hasSize(2);
    }

    // ---------- 12. La unidad de medida se lee igual en todo el panel ----------

    /**
     * Plantillas donde {@code unitOfMeasure} no es el enum y por lo tanto no
     * elige nada al renderizarse sola.
     *
     * <p>{@code SaleIngredient.unitOfMeasure} es un {@code String}: la venta
     * congela la unidad al vender, y lo que guarda es {@code getDisplayName()}
     * —ver {@code SaleService}—. Ahí {@code ${ing.unitOfMeasure}} imprime ese
     * texto, no un {@code toString()} de enum, y ya coincide con el resto.
     */
    private static final Map<String, String> UNIT_IS_NOT_THE_ENUM = Map.of(
            "sales/show.html", "SaleIngredient.unitOfMeasure es el String congelado en la venta");

    /**
     * Nadie renderiza {@code UnitOfMeasure} dejándolo caer en su
     * {@code toString()}.
     *
     * <p>El enum tiene dos lecturas —{@code displayName} "Kilogramo" y
     * {@code toString()} "Kilogramo (kg)"— y una expresión pelada
     * {@code ${x.unitOfMeasure}} toma la segunda sin decirlo. Así divergieron:
     * la misma píldora gris junto al nombre del ingrediente decía "Kilogramo" en
     * {@code ingredients/*} y "Kilogramo (kg)" en {@code products/recipe}.
     *
     * <p>El caso que lo vuelve visible es el diálogo de cantidad: su etiqueta ya
     * dice {@code Cantidad (...)} y recibía el {@code toString()} por
     * {@code data-unit}, así que se leía <b>"Cantidad (Kilogramo (kg))"</b>, con
     * el paréntesis anidado.
     *
     * <p>La regla no es qué forma usar sino <b>elegirla</b>: píldora y token
     * llevan {@code displayName}, la unidad inline dentro de un paréntesis o
     * después de "por" lleva {@code abbreviation}, y el campo de detalle
     * etiquetado y las {@code <option>} del select llevan {@code toString()},
     * que es donde enseñar la abreviatura sirve.
     */
    @Test
    @DisplayName("ninguna plantilla renderiza la unidad de medida por toString implícito")
    void unitOfMeasureIsNeverRenderedImplicitly() {
        List<String> offenders = new ArrayList<>();
        for (Path p : templates()) {
            if (UNIT_IS_NOT_THE_ENUM.containsKey(rel(p))) {
                continue;
            }
            offenders.addAll(implicitUnitRenderings(rel(p), withoutComments(read(p))));
        }

        assertThat(offenders)
                .as("""
                        Hay una unidad de medida que se renderiza sola y cae en el toString()
                        del enum ("Kilogramo (kg)") sin haberlo elegido. Es como divergieron
                        la píldora de ingredients/* y la de products/recipe, y como el diálogo
                        de cantidad terminó diciendo "Cantidad (Kilogramo (kg))".

                        Elegí la lectura y escribila: .displayName para una píldora o token,
                        .abbreviation para la unidad inline (dentro de un paréntesis o después
                        de "por"), .toString() para el campo de detalle y las <option>.
                        """)
                .isEmpty();
    }

    /**
     * Una expresión que termina en {@code .unitOfMeasure} sin pedir una lectura.
     *
     * <p>El punto antes del nombre es lo que hace de filtro: deja afuera
     * {@code th:field="*{unitOfMeasure}"} y {@code #fields.hasErrors('unitOfMeasure')},
     * que nombran el campo del formulario en vez de imprimirlo. Por eso alcanza
     * con exigirlo y se pueden cubrir las dos clases de expresión —variable
     * {@code ${...}} y selección {@code *{...}}—, que imprimen igual.
     */
    private static final Pattern IMPLICIT_UNIT = Pattern.compile("[$*]\\{[^}]*\\.unitOfMeasure\\}");

    private static List<String> implicitUnitRenderings(String file, String html) {
        List<String> found = new ArrayList<>();
        Matcher m = IMPLICIT_UNIT.matcher(html);
        while (m.find()) {
            found.add(file + ": " + m.group());
        }
        return found;
    }

    /**
     * Control negativo fijo: las cuatro expresiones de recipe.html como estaban,
     * más una de selección, que imprime igual y también hay que ver.
     */
    @Test
    @DisplayName("el detector de unidad implícita realmente detecta")
    void theUnitDetectorActuallyDetects() {
        String broken = """
                <span th:text="${pi.ingredient.unitOfMeasure}"></span>
                <button th:data-unit="${pi.ingredient.unitOfMeasure}"></button>
                <span th:text="${ingredient.unitOfMeasure}"></span>
                <p th:text="${'Costo: ' + @money.format(ingredient.unitCost) + ' por ' + ingredient.unitOfMeasure}"></p>
                <span th:text="*{ingredient.unitOfMeasure}"></span>
                """;

        assertThat(implicitUnitRenderings("sintetico.html", broken)).hasSize(5);

        // Y las formas explícitas no se reportan, incluida la del formulario.
        String fine = """
                <span th:text="${ingredient.unitOfMeasure.displayName}"></span>
                <span th:text="${ingredient.unitOfMeasure.abbreviation}"></span>
                <dd th:text="${ingredient.unitOfMeasure.toString()}"></dd>
                <select th:field="*{unitOfMeasure}"></select>
                <p th:if="${#fields.hasErrors('unitOfMeasure')}"></p>
                """;

        assertThat(implicitUnitRenderings("sintetico.html", fine)).isEmpty();
    }

    /**
     * La excepción de {@link #UNIT_IS_NOT_THE_ENUM} tiene que seguir haciendo
     * falta, por la misma simetría que {@link #noExceptionOutlivesItsReason()}:
     * si la plantilla dejó de nombrar la unidad, la excepción no filtra nada y
     * deja pasar sin aviso lo que vuelva a aparecer ahí.
     */
    @Test
    @DisplayName("la excepción de la unidad sigue haciendo falta")
    void theUnitExceptionStillApplies() {
        for (String file : UNIT_IS_NOT_THE_ENUM.keySet()) {
            Path p = TEMPLATES.resolve(file);
            assertThat(implicitUnitRenderings(file, withoutComments(read(p))))
                    .as("%s ya no renderiza unitOfMeasure: sacá la excepción (%s)",
                            file, UNIT_IS_NOT_THE_ENUM.get(file))
                    .isNotEmpty();
        }
    }
}
