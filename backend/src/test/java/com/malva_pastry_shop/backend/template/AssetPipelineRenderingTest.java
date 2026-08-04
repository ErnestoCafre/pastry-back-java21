package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.AuthController;
import com.malva_pastry_shop.backend.controller.admin.ErrorPageController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.inventory.ProductIngredient;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el cambio de A10: sacar el Tailwind Play CDN y Flowbite, y pasar el
 * modal artesanal de products/recipe a &lt;dialog&gt; nativo.
 *
 * Son aserciones sobre el HTML servido, no sobre archivos de configuración,
 * porque lo que importa es lo que llega al navegador. Si alguien vuelve a
 * meter el CDN o un onclick inline en una plantilla, esto falla.
 *
 * De paso le da a products/recipe su primera cobertura de renderizado: era una
 * de las 6 plantillas sin ningún test que la abriera.
 */
// AuthController y ErrorPageController entran porque login, error/403 y
// error.html NO decoran layout/main (necesitan renderizar sin sesión), así que
// cada una enlazaba su propio Play CDN con su propia copia de la paleta. Sacar
// el CDN solo del layout dejaba esas tres páginas rotas.
@WebMvcTest(controllers = { ProductController.class, AuthController.class, ErrorPageController.class })
@DisplayName("Pipeline de assets y modal nativo (A10)")
class AssetPipelineRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    private User admin;

    @BeforeEach
    void setUp() {
        Role role = new Role(RoleType.ADMIN);
        role.setId(1L);

        admin = new User();
        admin.setId(1L);
        admin.setName("Ada");
        admin.setLastName("Lovelace");
        admin.setEmail("ada@malva.com");
        admin.setPasswordHash("irrelevante");
        admin.setEnabled(true);
        admin.setRole(role);
    }

    // Dos cosas que se fijan a propósito:
    //
    // locale: la receta imprime importes con #numbers.formatDecimal, que
    // resuelve el separador decimal contra el locale de la petición.
    //
    // csrf(): bajo MockMvc el CsrfRequestDataValueProcessor de Spring Security
    // no llega a inyectar el hidden en NINGÚN formulario (ni siquiera en el de
    // /logout, que sí tiene th:action). El post-processor publica el atributo
    // _csrf de la petición, que es lo que leen tanto el processor como la
    // expresión ${_csrf} de la plantilla, y deja el render igual al de
    // producción. Sin esto, un test sobre el token estaría midiendo el entorno
    // de test y no la plantilla.
    private String render(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url).locale(Locale.US).with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String renderRecipe() throws Exception {
        Product product = new Product();
        product.setId(7L);
        product.setName("Cheesecake");
        product.setBasePrice(new BigDecimal("13132.00"));

        Ingredient queso = new Ingredient();
        queso.setId(3L);
        queso.setName("Queso crema");
        queso.setUnitCost(new BigDecimal("8.50"));
        queso.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);

        ProductIngredient linea = new ProductIngredient();
        linea.setProduct(product);
        linea.setIngredient(queso);
        linea.setQuantity(new BigDecimal("0.5000"));

        Ingredient disponible = new Ingredient();
        disponible.setId(4L);
        disponible.setName("Azúcar");
        disponible.setUnitCost(new BigDecimal("1.20"));
        disponible.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);

        when(productService.findById(anyLong())).thenReturn(product);
        when(productService.getProductIngredients(anyLong())).thenReturn(List.of(linea));
        when(productService.getAvailableIngredientsForProduct(anyLong())).thenReturn(List.of(disponible));
        when(productService.calculateRecipeCost(anyLong())).thenReturn(new BigDecimal("4.25"));

        return render("/products/7/recipe");
    }

    // ---------- Pipeline de CSS ----------

    @Test
    @DisplayName("El layout enlaza el CSS compilado y no el Play CDN")
    void layoutServesBuiltCss() throws Exception {
        String html = renderRecipe();

        assertThat(html).contains("/css/admin.css");
        assertThat(html).doesNotContain("cdn.tailwindcss.com");
        // La asignación de la config de la paleta, que vivía en un <script>
        // inline junto al CDN. Se afirma sobre "tailwind.config =" y no sobre
        // "tailwind.config" a secas porque el layout menciona el archivo
        // tailwind.config.js en un comentario, y los comentarios HTML se sirven.
        assertThat(html).doesNotContain("tailwind.config =");
    }

    @Test
    @DisplayName("Ningún <script> del panel es inline: habilita una CSP sin 'unsafe-inline'")
    void everyScriptIsExternal() throws Exception {
        String html = withoutComments(renderRecipe());

        // Si las dos cuentas coinciden, todo <script> del documento trae src y
        // por lo tanto no hay código embebido que una CSP estricta bloquearía.
        assertThat(countOf(html, "<script")).isGreaterThan(0);
        assertThat(countOf(html, "<script")).isEqualTo(countOf(html, "<script src="));
    }

    /**
     * Thymeleaf sirve los comentarios HTML tal cual (solo los de la forma
     * {@code <!--/* ... *}{@code /-->} se descartan al renderizar). Varios
     * comentarios de las plantillas explican justamente qué markup
     * reemplazaron, así que contienen las mismas cadenas que estos tests
     * buscan. Sin quitarlos, el test se dispara con su propia documentación.
     */
    private static String withoutComments(String html) {
        return html.replaceAll("(?s)<!--.*?-->", "");
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    @Test
    @DisplayName("Flowbite ya no se carga: el drawer es propio")
    void flowbiteIsGone() throws Exception {
        String html = renderRecipe();

        assertThat(html).doesNotContain("flowbite");
        // El contrato del drawer lo atiende ahora /js/admin.js.
        assertThat(html).contains("data-drawer-toggle=\"sidebar\"");
        assertThat(html).contains("data-drawer-backdrop=\"sidebar\"");
        // aria-expanded es lo que Flowbite no ponía y el lector de pantalla
        // necesita para anunciar el estado del menú.
        assertThat(html).contains("aria-expanded=\"false\"");
    }

    @Test
    @DisplayName("Las páginas sin layout también sirven el CSS compilado")
    void standalonePagesAlsoServeBuiltCss() throws Exception {
        // login y error/403 no decoran layout/main: renderizan sin sesión, y el
        // sidebar del layout necesita un usuario autenticado. Por eso cada una
        // tenía su propia copia del CDN y de la paleta.
        for (String url : List.of("/login", "/error/403")) {
            String html = withoutComments(
                    mockMvc.perform(get(url)).andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString());

            assertThat(html).as("CSS en %s", url).contains("/css/admin.css");
            assertThat(html).as("sin CDN en %s", url).doesNotContain("cdn.tailwindcss.com");
            assertThat(html).as("sin Flowbite en %s", url).doesNotContain("flowbite");
            assertThat(html).as("sin config inline en %s", url).doesNotContain("tailwind.config =");
        }
    }

    @Test
    @DisplayName("Ningún comentario de parser se escapa al HTML servido")
    void parserCommentsDoNotLeak() throws Exception {
        // Un comentario de nivel de parser de Thymeleaf termina en la PRIMERA
        // aparición de su secuencia de cierre. Si el texto del comentario la
        // contiene —por ejemplo al documentar la sintaxis— el bloque corta
        // antes de tiempo y el resto de la prosa sale impresa dentro del
        // <head>. Pasó exactamente eso al escribir el comentario del layout.
        for (String url : List.of("/login", "/error/403")) {
            String html = mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
            assertThat(html).as("fuga en %s", url).doesNotContain("*/-->");
        }
        assertThat(renderRecipe()).doesNotContain("*/-->");
    }

    // ---------- Modal nativo ----------

    @Test
    @DisplayName("products/recipe renderiza y usa <dialog> nativo")
    void recipeUsesNativeDialog() throws Exception {
        String html = renderRecipe();

        assertThat(html).contains("<dialog");
        assertThat(html).contains("id=\"editQuantityDialog\"");
        // El título accesible del diálogo: sin esto el lector anuncia "diálogo"
        // y nada más.
        assertThat(html).contains("aria-labelledby=\"editQuantityTitle\"");
        assertThat(html).contains("data-dialog-close");

        // El div artesanal que reemplaza.
        assertThat(html).doesNotContain("id=\"editModal\"");

        assertThat(html).contains("Queso crema");
        assertThat(html).contains("Cheesecake");
    }

    @Test
    @DisplayName("La URL de guardado la arma Thymeleaf, no el JS concatenando")
    void dialogActionComesFromThymeleaf() throws Exception {
        String html = withoutComments(renderRecipe());

        assertThat(html).contains("data-action=\"/products/7/recipe/ingredients/3/update\"");
        // El script inline construía la URL con '/products/' + productId, que
        // se rompe si la app se sirve bajo un context-path.
        assertThat(html).doesNotContain("th:inline");
        assertThat(html).contains("/js/recipe.js");
    }

    @Test
    @DisplayName("El form del modal lleva token CSRF pese a no tener th:action")
    void dialogFormCarriesCsrfToken() throws Exception {
        String html = withoutComments(renderRecipe());

        // Regresión: el form del modal es el único del panel sin th:action
        // (la URL la fija el JS), y es th:action lo que hace que Spring
        // Security inyecte el hidden. Sin el token explícito, guardar una
        // cantidad devolvía 403 en vez de guardar.
        assertThat(html).contains("name=\"_csrf\"");
    }

    @Test
    @DisplayName("products/recipe no deja JS inline: la CSP puede prohibirlo")
    void recipeHasNoInlineJavaScript() throws Exception {
        String html = withoutComments(renderRecipe());

        assertThat(html).doesNotContain("onclick=");
        assertThat(html).doesNotContain("onsubmit=");
        // El confirm de "quitar ingrediente" pasó al listener delegado.
        assertThat(html).contains("data-confirm=");
    }
}
