package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.DashboardController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.controller.admin.SaleController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.sales.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de humo de las páginas con importes que no tenían ninguna cobertura
 * de renderizado: dashboard/index, products/list, products/show,
 * categories/products y sales/create.
 *
 * <p>Se escribió al centralizar el formato de importes, que tocó esas cinco
 * plantillas. Editar una plantilla sin test es exactamente cómo el proyecto se
 * comió una tanda de expresiones rotas antes: Thymeleaf no valida nada en
 * compilación, así que una expresión mal escrita compila, pasa la suite entera
 * y falla recién cuando alguien abre la página.
 *
 * <p>Las peticiones van con Locale.US y aun así se espera formato argentino:
 * eso prueba que el FixedLocaleResolver le gana al Accept-Language del
 * navegador, que era el defecto original (el mismo precio se veía distinto
 * según quién mirara).
 */
@WebMvcTest(controllers = { DashboardController.class, ProductController.class,
        CategoryController.class, SaleController.class })
@DisplayName("Renderizado de las páginas sin cobertura previa")
class UncoveredPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private SaleService saleService;

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

    private String render(String url) throws Exception {
        return mockMvc.perform(get(url).locale(Locale.US).with(user(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Un producto con precio de cinco cifras: el caso que exige agrupar. */
    private Product product() {
        Category tortas = new Category();
        tortas.setId(2L);
        tortas.setName("Tortas");

        Product product = new Product();
        product.setId(1L);
        product.setName("Cheesecake");
        product.setDescription("Con frutos rojos");
        product.setBasePrice(new BigDecimal("13132.00"));
        product.setCategory(tortas);
        product.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
        return product;
    }

    @Test
    @DisplayName("dashboard/index: los ingresos llevan símbolo y separador de miles")
    void dashboardRenders() throws Exception {
        when(productService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 50), 1));
        when(categoryService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        when(ingredientService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        when(saleService.countSalesInRange(any(), any())).thenReturn(3L);
        when(saleService.totalRevenueInRange(any(), any())).thenReturn(new BigDecimal("13132.00"));

        String html = render("/dashboard");

        assertThat(html).contains("$13.132,00");
        assertThat(html).doesNotContain("$13132,00");
    }

    @Test
    @DisplayName("products/list: el precio de la fila se agrupa")
    void productListRenders() throws Exception {
        when(productService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 50), 1));
        when(categoryService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        String html = render("/products");

        assertThat(html).contains("Cheesecake");
        assertThat(html).contains("$13.132,00");
    }

    @Test
    @DisplayName("products/show: el precio base se agrupa")
    void productDetailRenders() throws Exception {
        when(productService.findById(anyLong())).thenReturn(product());
        when(productService.getProductTags(anyLong())).thenReturn(List.of());

        String html = render("/products/1");

        assertThat(html).contains("Cheesecake");
        assertThat(html).contains("$13.132,00");
    }

    @Test
    @DisplayName("categories/products: el precio de cada producto se agrupa")
    void categoryProductsRenders() throws Exception {
        Category tortas = new Category();
        tortas.setId(2L);
        tortas.setName("Tortas");

        when(categoryService.findById(anyLong())).thenReturn(tortas);
        when(productService.findByCategoryId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 50), 1));

        String html = render("/categories/2/products");

        assertThat(html).contains("Cheesecake");
        assertThat(html).contains("$13.132,00");
    }

    /**
     * sales/create arma el texto de cada &lt;option&gt; concatenando nombre y
     * precio. Fue uno de los dos sitios que no seguían el patrón general y
     * hubo que migrarlos a mano.
     */
    @Test
    @DisplayName("sales/create: el precio va concatenado en el option del producto")
    void saleCreateRenders() throws Exception {
        when(productService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 50), 1));

        String html = render("/sales/new");

        assertThat(html).contains("Cheesecake - $13.132,00");
    }
}
