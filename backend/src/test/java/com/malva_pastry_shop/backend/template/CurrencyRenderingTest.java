package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.IngredientController;
import com.malva_pastry_shop.backend.controller.admin.SaleController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.domain.sales.SaleIngredient;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre los templates tocados al unificar el formato de moneda.
 *
 * <p>Existe por una razón concreta: esos 9 templates no tenían ninguna
 * cobertura de renderizado, y una edición masiva les metió expresiones rotas
 * que ni el compilador ni la suite detectaron —Thymeleaf solo falla al
 * renderizar—. Una prueba de humo que abra la página habría fallado al
 * instante.
 *
 * <p>Además fija la regla: los importes llevan símbolo, las cantidades no.
 */
@WebMvcTest(controllers = { IngredientController.class, SaleController.class })
@DisplayName("Renderizado de importes")
class CurrencyRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private SaleService saleService;

    @MockitoBean
    private ProductService productService;

    private User admin;

    @BeforeEach
    void setUp() {
        Role role = new Role(RoleType.ADMIN);
        role.setId(1L);

        admin = new User();
        admin.setId(1L);
        admin.setName("Ada");
        admin.setEmail("ada@malva.com");
        admin.setPasswordHash("irrelevante");
        admin.setEnabled(true);
        admin.setRole(role);
    }

    /**
     * El locale se fija a propósito. #numbers.formatDecimal resuelve el
     * separador decimal contra el locale de la petición y la app no configura
     * ninguno, así que el mismo importe sale "$12.50" en un navegador en
     * inglés y "$12,50" en uno en español. Sin fijarlo, estas aserciones
     * pasarían o fallarían según el locale por defecto de la máquina.
     */
    private String render(String url) throws Exception {
        return mockMvc.perform(get(url).locale(Locale.US).with(user(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DisplayName("ingredients/list muestra el costo unitario con símbolo")
    void ingredientListShowsCurrency() throws Exception {
        Ingredient harina = new Ingredient();
        harina.setId(1L);
        harina.setName("Harina 000");
        harina.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
        harina.setUnitCost(new BigDecimal("12.50"));

        when(ingredientService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(harina), PageRequest.of(0, 50), 1));

        String html = render("/ingredients");

        assertThat(html).contains("Harina 000");
        assertThat(html).contains("$12.50");
    }

    @Test
    @DisplayName("ingredients/show e ingredients/deleted muestran el costo con símbolo")
    void ingredientDetailAndTrashShowCurrency() throws Exception {
        Ingredient harina = new Ingredient();
        harina.setId(1L);
        harina.setName("Harina 000");
        harina.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
        harina.setUnitCost(new BigDecimal("12.50"));

        when(ingredientService.findById(1L)).thenReturn(harina);
        when(ingredientService.countProductsUsingIngredient(1L)).thenReturn(3L);
        assertThat(render("/ingredients/1")).contains("$12.50");

        harina.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
        when(ingredientService.findDeleted(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(harina), PageRequest.of(0, 50), 1));
        assertThat(render("/ingredients/deleted")).contains("$12.50");
    }

    /**
     * sales/show es el archivo con más importes (6) y fue uno de los que quedó
     * con expresiones corruptas tras la edición masiva. No tenía ninguna
     * cobertura, que es exactamente por qué la corrupción no saltó.
     */
    @Test
    @DisplayName("sales/show renderiza los seis importes y el margen")
    void saleDetailShowsCurrency() throws Exception {
        Sale sale = new Sale();
        sale.setId(7L);
        sale.setProductName("Torta de chocolate");
        sale.setQuantity(2);
        sale.setUnitPrice(new BigDecimal("30.00"));
        sale.setTotalAmount(new BigDecimal("60.00"));
        sale.setSaleDate(LocalDateTime.of(2026, 3, 14, 10, 30));
        // registered_by_id es NOT NULL en la base y @NotNull en la entidad, así
        // que la ficha lo desreferencia sin comprobar: el fixture debe traerlo.
        sale.setRegisteredBy(admin);

        SaleIngredient harina = new SaleIngredient();
        harina.setIngredientName("Harina 000");
        harina.setQuantityUsed(new BigDecimal("0.5000"));
        harina.setUnitCost(new BigDecimal("12.50"));
        harina.setUnitOfMeasure("kg");
        harina.setTotalCost(new BigDecimal("6.25"));

        when(saleService.findByIdWithDetails(7L)).thenReturn(sale);
        when(saleService.getSaleIngredients(7L)).thenReturn(List.of(harina));
        when(saleService.calculateTotalIngredientCost(7L)).thenReturn(new BigDecimal("6.25"));

        String html = render("/sales/7");

        assertThat(html).contains("$30.00");   // precio unitario
        assertThat(html).contains("$60.00");   // total de la venta
        assertThat(html).contains("$12.50");   // costo unitario del ingrediente
        assertThat(html).contains("$6.25");    // costo del ingrediente y total
        assertThat(html).contains("$53.75");   // margen = 60.00 - 6.25
        // La cantidad usada es una medida, no un importe.
        assertThat(html).contains("0.5000").doesNotContain("$0.5000");
    }

    @Test
    @DisplayName("sales/list muestra importes con símbolo")
    void saleListShowsCurrency() throws Exception {
        Sale sale = new Sale();
        sale.setId(7L);
        sale.setProductName("Torta de chocolate");
        sale.setQuantity(2);
        sale.setUnitPrice(new BigDecimal("30.00"));
        sale.setTotalAmount(new BigDecimal("60.00"));
        sale.setSaleDate(LocalDateTime.of(2026, 3, 14, 10, 30));

        when(saleService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sale), PageRequest.of(0, 50), 1));
        when(saleService.sumTotalAmount()).thenReturn(new BigDecimal("60.00"));

        String html = render("/sales");

        assertThat(html).contains("Torta de chocolate");
        assertThat(html).contains("$30.00");
        assertThat(html).contains("$60.00");
        // La cantidad es un conteo, no un importe: nunca lleva símbolo.
        assertThat(html).doesNotContain("$2<").doesNotContain("$2.00");
    }
}
