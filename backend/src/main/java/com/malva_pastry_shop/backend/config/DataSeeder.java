package com.malva_pastry_shop.backend.config;

import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.inventory.ProductIngredient;
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.domain.sales.SaleIngredient;
import com.malva_pastry_shop.backend.domain.storefront.ProductTag;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSectionProduct;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.repository.CategoryRepository;
import com.malva_pastry_shop.backend.repository.IngredientRepository;
import com.malva_pastry_shop.backend.repository.ProductIngredientRepository;
import com.malva_pastry_shop.backend.repository.ProductRepository;
import com.malva_pastry_shop.backend.repository.ProductTagRepository;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.repository.SaleRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionProductRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionRepository;
import com.malva_pastry_shop.backend.repository.TagRepository;
import com.malva_pastry_shop.backend.repository.UserRepository;
import com.malva_pastry_shop.backend.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

        /**
         * Password de los usuarios demo. Es el mismo para los cuatro, igual que en
         * produccion (V2 y R__seed_demo_data.sql comparten un unico hash BCrypt),
         * para que las credenciales del README sirvan tanto en local como en la
         * demo desplegada.
         */
        private static final String DEMO_PASSWORD = "sysadmin123";

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final IngredientRepository ingredientRepository;
        private final TagRepository tagRepository;
        private final ProductTagRepository productTagRepository;
        private final StorefrontSectionRepository sectionRepository;
        private final StorefrontSectionProductRepository sectionProductRepository;
        private final ProductIngredientRepository productIngredientRepository;
        private final SaleRepository saleRepository;
        private final PasswordEncoder passwordEncoder;

        public DataSeeder(RoleRepository roleRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        ProductRepository productRepository,
                        IngredientRepository ingredientRepository,
                        TagRepository tagRepository,
                        ProductTagRepository productTagRepository,
                        StorefrontSectionRepository sectionRepository,
                        StorefrontSectionProductRepository sectionProductRepository,
                        ProductIngredientRepository productIngredientRepository,
                        SaleRepository saleRepository,
                        PasswordEncoder passwordEncoder) {
                this.roleRepository = roleRepository;
                this.userRepository = userRepository;
                this.categoryRepository = categoryRepository;
                this.productRepository = productRepository;
                this.ingredientRepository = ingredientRepository;
                this.tagRepository = tagRepository;
                this.productTagRepository = productTagRepository;
                this.sectionRepository = sectionRepository;
                this.sectionProductRepository = sectionProductRepository;
                this.productIngredientRepository = productIngredientRepository;
                this.saleRepository = saleRepository;
                this.passwordEncoder = passwordEncoder;
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                seedRoles();
                seedBasicUsers();
                seedIngredients();
                Map<String, Category> categories = seedCategories();
                seedProducts(categories);
                Map<String, Tag> tags = seedTags();
                seedProductTags(tags);
                Map<String, StorefrontSection> sections = seedSections();
                seedSectionProducts(sections);
                seedProductIngredients();
                seedSales();
        }

        private void seedRoles() {
                for (RoleType roleType : RoleType.values()) {
                        if (!roleRepository.existsByName(roleType)) {
                                Role role = new Role(roleType, getDescriptionFor(roleType));
                                roleRepository.save(role);
                                log.info("Rol creado: {}", roleType);
                        }
                }
        }

        private String getDescriptionFor(RoleType roleType) {
                return switch (roleType) {
                        case ADMIN -> "Administrador con acceso completo al sistema";
                        case EMPLOYEE -> "Empleado con acceso limitado a gestión de productos";
                        case USER -> "Usuario básico del sistema";
                };
        }

        private void seedBasicUsers() {
                String sysAdminEmail = "sysadmin@malva.com";
                if (userRepository.findByEmail(sysAdminEmail).isEmpty()) {
                        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                                        .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

                        User admin = new User();
                        admin.setName("Administrador");
                        admin.setLastName("Sistema");
                        admin.setEmail(sysAdminEmail);
                        admin.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                        admin.setEnabled(true);
                        admin.setSystemAdmin(true);
                        admin.setRole(adminRole);

                        userRepository.save(admin);
                        log.info("Usuario administrador creado: {}", sysAdminEmail);
                } else {
                        log.info("Usuario administrador ya existe: {}", sysAdminEmail);
                }

                String adminEmail = "admin@malva.com";
                if (userRepository.findByEmail(adminEmail).isEmpty()) {
                        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                                        .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

                        User admin = new User();
                        admin.setName("Administrador");
                        admin.setLastName("Sistema");
                        admin.setEmail(adminEmail);
                        admin.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                        admin.setEnabled(true);
                        admin.setSystemAdmin(false);
                        admin.setRole(adminRole);

                        userRepository.save(admin);
                        log.info("Usuario administrador creado: {}", adminEmail);
                } else {
                        log.info("Usuario administrador ya existe: {}", adminEmail);
                }

                String employeeEmail = "employee@malva.com";
                if (userRepository.findByEmail(employeeEmail).isEmpty()) {
                        Role employeeRole = roleRepository.findByName(RoleType.EMPLOYEE)
                                        .orElseThrow(() -> new RuntimeException("Rol EMPLOYEE no encontrado"));

                        User employee = new User();
                        employee.setName("Empleado");
                        employee.setLastName("Sistema");
                        employee.setEmail(employeeEmail);
                        employee.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                        employee.setEnabled(true);
                        employee.setSystemAdmin(false);
                        employee.setRole(employeeRole);

                        userRepository.save(employee);
                        log.info("Usuario empleado creado: {}", employeeEmail);
                } else {
                        log.info("Usuario empleado ya existe: {}", employeeEmail);
                }

                String userEmail = "user@malva.com";
                if (userRepository.findByEmail(userEmail).isEmpty()) {
                        Role userRole = roleRepository.findByName(RoleType.USER)
                                        .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

                        User user = new User();
                        user.setName("Usuario");
                        user.setLastName("Sistema");
                        user.setEmail(userEmail);
                        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                        user.setEnabled(true);
                        user.setSystemAdmin(false);
                        user.setRole(userRole);

                        userRepository.save(user);
                        log.info("Usuario usuario creado: {}", userEmail);
                } else {
                        log.info("Usuario usuario ya existe: {}", userEmail);
                }
        }

        private void seedIngredients() {
                if (ingredientRepository.count() > 0) {
                        log.info("Ya existen ingredientes en la base de datos. Saltando seed de ingredientes.");
                        return;
                }

                int totalIngredients = 0;

                // ========== HARINAS Y BASES ==========
                createIngredient("Harina de Trigo", "Harina todo uso para repostería",
                                new BigDecimal("25.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Harina Integral", "Harina de trigo integral para productos saludables",
                                new BigDecimal("32.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Harina de Almendra", "Harina de almendra para productos sin gluten",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Maicena", "Fécula de maíz para espesar y hornear",
                                new BigDecimal("35.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Harina de Avena", "Harina de avena para galletas saludables",
                                new BigDecimal("45.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Harina de Coco", "Harina de coco para recetas veganas",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Polvo para Hornear", "Polvo de hornear (Royal o similar)",
                                new BigDecimal("85.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Bicarbonato de Sodio", "Bicarbonato para levado y reacciones químicas",
                                new BigDecimal("40.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 8;

                // ========== AZÚCARES Y ENDULZANTES ==========
                createIngredient("Azúcar Blanca", "Azúcar refinada blanca estándar",
                                new BigDecimal("22.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Azúcar Morena", "Azúcar morena o mascabado",
                                new BigDecimal("28.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Azúcar Glass", "Azúcar glass (azúcar pulverizada) para decoración",
                                new BigDecimal("30.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Miel de Abeja", "Miel natural de abeja pura",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Jarabe de Maple", "Jarabe de arce natural",
                                new BigDecimal("280.00"), UnitOfMeasure.LITRO);
                createIngredient("Piloncillo", "Azúcar de caña sin refinar (panela)",
                                new BigDecimal("35.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Stevia", "Endulzante natural bajo en calorías",
                                new BigDecimal("450.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 7;

                // ========== LÁCTEOS ==========
                createIngredient("Leche Entera", "Leche de vaca entera pasteurizada",
                                new BigDecimal("18.00"), UnitOfMeasure.LITRO);
                createIngredient("Crema para Batir", "Crema de leche para batir (35% grasa)",
                                new BigDecimal("65.00"), UnitOfMeasure.LITRO);
                createIngredient("Mantequilla", "Mantequilla sin sal para repostería",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Queso Crema", "Queso crema estilo Philadelphia",
                                new BigDecimal("85.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Leche Condensada", "Leche condensada azucarada",
                                new BigDecimal("45.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Leche Evaporada", "Leche evaporada sin azúcar",
                                new BigDecimal("32.00"), UnitOfMeasure.LITRO);
                createIngredient("Leche de Almendra", "Leche vegetal de almendra para productos veganos",
                                new BigDecimal("55.00"), UnitOfMeasure.LITRO);
                createIngredient("Yogurt Natural", "Yogurt natural sin azúcar",
                                new BigDecimal("38.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 8;

                // ========== GRASAS ==========
                createIngredient("Aceite Vegetal", "Aceite vegetal neutro para hornear",
                                new BigDecimal("35.00"), UnitOfMeasure.LITRO);
                createIngredient("Aceite de Oliva", "Aceite de oliva extra virgen",
                                new BigDecimal("95.00"), UnitOfMeasure.LITRO);
                createIngredient("Manteca Vegetal", "Manteca vegetal para repostería",
                                new BigDecimal("42.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Aceite de Coco", "Aceite de coco virgen para productos veganos",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 4;

                // ========== HUEVOS Y PROTEÍNAS ==========
                createIngredient("Huevos", "Huevos frescos de gallina tamaño grande",
                                new BigDecimal("2.50"), UnitOfMeasure.UNIDAD);
                createIngredient("Clara de Huevo", "Clara de huevo pasteurizada líquida",
                                new BigDecimal("75.00"), UnitOfMeasure.LITRO);
                createIngredient("Yema de Huevo", "Yema de huevo pasteurizada",
                                new BigDecimal("95.00"), UnitOfMeasure.LITRO);
                totalIngredients += 3;

                // ========== CHOCOLATES ==========
                createIngredient("Chocolate Amargo", "Chocolate oscuro 70% cacao",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Chocolate de Leche", "Chocolate con leche premium",
                                new BigDecimal("150.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Chocolate Blanco", "Chocolate blanco de cobertura",
                                new BigDecimal("165.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Cacao en Polvo", "Cocoa en polvo sin azúcar",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Nutella", "Crema de avellanas con cacao",
                                new BigDecimal("85.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Chispas de Chocolate", "Chips de chocolate semi-amargo",
                                new BigDecimal("125.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 6;

                // ========== LEVADURAS Y AGENTES ==========
                createIngredient("Levadura Fresca", "Levadura fresca para panadería",
                                new BigDecimal("65.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Levadura Seca", "Levadura seca instantánea",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Cremor Tártaro", "Ácido para estabilizar merengues",
                                new BigDecimal("320.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 3;

                // ========== FRUTAS ==========
                createIngredient("Fresas Frescas", "Fresas frescas de temporada",
                                new BigDecimal("55.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Manzanas", "Manzanas rojas o verdes para hornear",
                                new BigDecimal("35.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Limones", "Limones frescos amarillos",
                                new BigDecimal("28.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Naranjas", "Naranjas frescas para jugo y ralladura",
                                new BigDecimal("25.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Frambuesas", "Frambuesas frescas o congeladas",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Moras", "Moras azules (blueberries)",
                                new BigDecimal("85.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Cerezas", "Cerezas frescas sin hueso",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Plátanos", "Plátanos maduros",
                                new BigDecimal("22.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Piña", "Piña fresca natural",
                                new BigDecimal("30.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Duraznos", "Duraznos en almíbar o frescos",
                                new BigDecimal("45.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 10;

                // ========== FRUTOS SECOS Y SEMILLAS ==========
                createIngredient("Nueces", "Nueces pecanas o de castilla",
                                new BigDecimal("220.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Almendras", "Almendras naturales sin piel",
                                new BigDecimal("185.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Avellanas", "Avellanas tostadas",
                                new BigDecimal("210.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Coco Rallado", "Coco deshidratado rallado",
                                new BigDecimal("75.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Pasas", "Pasas de uva sin semilla",
                                new BigDecimal("65.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Pistaches", "Pistaches sin cáscara",
                                new BigDecimal("350.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Cacahuates", "Cacahuates tostados sin sal",
                                new BigDecimal("55.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Semillas de Amapola", "Semillas de amapola para decoración",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 8;

                // ========== ESPECIAS Y SABORIZANTES ==========
                createIngredient("Extracto de Vainilla", "Extracto puro de vainilla",
                                new BigDecimal("420.00"), UnitOfMeasure.LITRO);
                createIngredient("Vainilla en Vaina", "Vainas de vainilla natural",
                                new BigDecimal("8.50"), UnitOfMeasure.UNIDAD);
                createIngredient("Canela en Polvo", "Canela molida de Ceilán",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Canela en Rama", "Ramas de canela entera",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Jengibre en Polvo", "Jengibre molido",
                                new BigDecimal("110.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Nuez Moscada", "Nuez moscada molida",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Sal", "Sal fina de mesa",
                                new BigDecimal("8.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Sal de Mar", "Sal marina en escamas para decoración",
                                new BigDecimal("45.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Esencia de Almendra", "Esencia artificial de almendra",
                                new BigDecimal("95.00"), UnitOfMeasure.LITRO);
                createIngredient("Azahar", "Agua de azahar para pan de muerto",
                                new BigDecimal("85.00"), UnitOfMeasure.LITRO);
                totalIngredients += 10;

                // ========== OTROS INGREDIENTES ==========
                createIngredient("Gelatina Sin Sabor", "Grenetina en polvo o láminas",
                                new BigDecimal("180.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Café Instantáneo", "Café soluble para saborizante",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Colorante Alimentario", "Colorantes vegetales variados",
                                new BigDecimal("15.00"), UnitOfMeasure.UNIDAD);
                createIngredient("Fondant", "Fondant para decoración de pasteles",
                                new BigDecimal("95.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Mermelada de Fresa", "Mermelada para rellenos",
                                new BigDecimal("55.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Caramelo", "Caramelo líquido o dulce de leche",
                                new BigDecimal("65.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Mazapán", "Mazapán para decoración",
                                new BigDecimal("85.00"), UnitOfMeasure.KILOGRAMO);
                createIngredient("Sprinkles", "Chispas de colores para decoración",
                                new BigDecimal("120.00"), UnitOfMeasure.KILOGRAMO);
                totalIngredients += 8;

                log.info("Seed de ingredientes completado. Total: {} ingredientes creados.", totalIngredients);
        }

        private void createIngredient(String name, String description, BigDecimal unitCost,
                        UnitOfMeasure unitOfMeasure) {
                if (ingredientRepository.findByNameIgnoreCase(name).isEmpty()) {
                        Ingredient ingredient = new Ingredient(name, unitCost, unitOfMeasure);
                        ingredient.setDescription(description);
                        ingredientRepository.save(ingredient);
                        log.debug("Ingrediente creado: {} - ${}/{}", name, unitCost, unitOfMeasure.getAbbreviation());
                } else {
                        log.debug("Ingrediente ya existe: {}", name);
                }
        }

        private Map<String, Category> seedCategories() {
                Map<String, Category> categoryMap = new HashMap<>();

                Object[][] categoriesData = {
                                { "Pasteles",
                                                "Pasteles tradicionales y personalizados para toda ocasión. Desde cumpleaños hasta bodas, elaborados con los mejores ingredientes." },
                                { "Cupcakes",
                                                "Deliciosos cupcakes artesanales con variedad de sabores y decoraciones únicas. Perfectos para eventos y antojos." },
                                { "Galletas",
                                                "Galletas horneadas diariamente con recetas tradicionales y creativas. Crujientes por fuera, suaves por dentro." },
                                { "Pan Dulce Mexicano",
                                                "Auténtico pan dulce mexicano: conchas, cuernos, orejas, polvorones y más. Tradición en cada bocado." },
                                { "Tartas y Pays",
                                                "Tartas frutales y pays caseros con masas crujientes y rellenos cremosos. Recetas de la abuela." },
                                { "Postres Individuales",
                                                "Postres gourmet en porciones individuales: mousse, tiramisú, cheesecake y más. Elegancia en cada cucharada." },
                                { "Panes Artesanales",
                                                "Panes especiales horneados con masa madre y técnicas artesanales. Baguettes, ciabattas y focaccias." },
                                { "Dulces y Confitería",
                                                "Dulces tradicionales mexicanos y confitería fina: mazapanes, cocadas, jamoncillos y trufas artesanales." },
                                { "Productos Veganos",
                                                "Opciones veganas sin sacrificar sabor. Pasteles, galletas y postres elaborados sin ingredientes de origen animal." },
                                { "Temporada y Festividades",
                                                "Productos especiales para Día de Muertos, Navidad, San Valentín y otras celebraciones. Edición limitada." }
                };

                for (Object[] data : categoriesData) {
                        String name = (String) data[0];
                        String description = (String) data[1];

                        var existingCategory = categoryRepository.findByNameIgnoreCase(name);
                        if (existingCategory.isEmpty()) {
                                Category category = new Category(name, description);
                                categoryRepository.save(category);
                                categoryMap.put(name, category);
                                log.info("Categoría creada: {}", name);
                        } else {
                                categoryMap.put(name, existingCategory.get());
                                log.info("Categoría ya existe: {}", name);
                        }
                }

                log.info("Total de categorías procesadas: {}", categoryMap.size());
                return categoryMap;
        }

        private void seedProducts(Map<String, Category> categories) {
                if (productRepository.count() > 0) {
                        log.info("Ya existen productos en la base de datos. Saltando seed de productos.");
                        return;
                }

                // Pasteles (7 productos)
                createProduct("Pastel de Chocolate Triple",
                                "Tres capas de bizcocho de chocolate con ganache, cubierto de chocolate belga", 3,
                                new BigDecimal("450.00"), categories.get("Pasteles"),
                                "/images/products/pastel-chocolate-triple.webp");
                createProduct("Pastel Red Velvet", "Suave bizcocho rojo aterciopelado con frosting de queso crema", 2,
                                new BigDecimal("420.00"), categories.get("Pasteles"),
                                "/images/products/pastel-red-velvet.webp");
                createProduct("Pastel de Vainilla Clásico", "Bizcocho esponjoso de vainilla con betún de mantequilla",
                                2,
                                new BigDecimal("380.00"), categories.get("Pasteles"),
                                "/images/products/pastel-vainilla-clasico.webp");
                createProduct("Pastel de Zanahoria", "Con nueces, pasas y frosting de queso crema con canela", 2,
                                new BigDecimal("400.00"), categories.get("Pasteles"),
                                "/images/products/pastel-zanahoria.webp");
                createProduct("Pastel de Fresas con Crema", "Bizcocho de vainilla con fresas frescas y crema chantilly",
                                1,
                                new BigDecimal("480.00"), categories.get("Pasteles"),
                                "/images/products/pastel-fresas-crema.webp");
                createProduct("Pastel Tres Leches", "Bizcocho bañado en tres tipos de leche con merengue italiano", 1,
                                new BigDecimal("350.00"), categories.get("Pasteles"),
                                "/images/products/pastel-tres-leches.webp");
                createProduct("Pastel Selva Negra", "Chocolate, cerezas y crema batida. Receta alemana tradicional", 3,
                                new BigDecimal("520.00"), categories.get("Pasteles"),
                                "/images/products/pastel-selva-negra.webp");

                // Cupcakes (5 productos)
                createProduct("Cupcake de Chocolate", "Base de chocolate intenso con frosting de chocolate", 1,
                                new BigDecimal("45.00"), categories.get("Cupcakes"),
                                "/images/products/cupcake-chocolate.webp");
                createProduct("Cupcake Red Velvet", "Cupcake rojo aterciopelado con queso crema", 1,
                                new BigDecimal("48.00"), categories.get("Cupcakes"),
                                "/images/products/cupcake-red-velvet.webp");
                createProduct("Cupcake de Limón", "Sabor cítrico refrescante con frosting de limón", 1,
                                new BigDecimal("45.00"), categories.get("Cupcakes"),
                                "/images/products/cupcake-limon.webp");
                createProduct("Cupcake de Nutella", "Relleno de Nutella con frosting de avellanas", 1,
                                new BigDecimal("55.00"), categories.get("Cupcakes"),
                                "/images/products/cupcake-nutella.webp");
                createProduct("Cupcake de Café Moka", "Para los amantes del café con toque de chocolate", 1,
                                new BigDecimal("50.00"), categories.get("Cupcakes"),
                                "/images/products/cupcake-cafe-moka.webp");

                // Galletas (5 productos)
                createProduct("Galletas de Chispas de Chocolate", "Clásicas con chips de chocolate semi-amargo. Docena",
                                1,
                                new BigDecimal("85.00"), categories.get("Galletas"),
                                "/images/products/galletas-chispas-chocolate.webp");
                createProduct("Galletas de Avena con Pasas", "Saludables y deliciosas con pasas y canela. Docena", 1,
                                new BigDecimal("75.00"), categories.get("Galletas"),
                                "/images/products/galletas-avena-pasas.webp");
                createProduct("Galletas de Mantequilla", "Tradicionales galletas danesas de mantequilla. Docena", 1,
                                new BigDecimal("70.00"), categories.get("Galletas"),
                                "/images/products/galletas-mantequilla.webp");
                createProduct("Galletas de Jengibre", "Especiadas y crujientes, perfectas con café. Docena", 1,
                                new BigDecimal("80.00"), categories.get("Galletas"),
                                "/images/products/galletas-jengibre.webp");
                createProduct("Galletas Snickerdoodle", "Con canela y azúcar, suaves por dentro. Docena", 1,
                                new BigDecimal("78.00"), categories.get("Galletas"),
                                "/images/products/galletas-snickerdoodle.webp");

                // Pan Dulce Mexicano (6 productos)
                createProduct("Concha de Vainilla", "Pan dulce tradicional con costra de azúcar sabor vainilla", 0,
                                new BigDecimal("18.00"), categories.get("Pan Dulce Mexicano"),
                                "/images/products/concha-vainilla.webp");
                createProduct("Concha de Chocolate", "Concha clásica con costra de chocolate", 0,
                                new BigDecimal("18.00"), categories.get("Pan Dulce Mexicano"),
                                "/images/products/concha-chocolate.webp");
                createProduct("Cuerno de Mantequilla", "Esponjoso cuerno bañado en mantequilla y azúcar glass", 0,
                                new BigDecimal("20.00"), categories.get("Pan Dulce Mexicano"),
                                "/images/products/cuerno-mantequilla.webp");
                createProduct("Oreja", "Pan hojaldrado caramelizado, crujiente y dulce", 0, new BigDecimal("22.00"),
                                categories.get("Pan Dulce Mexicano"),
                                "/images/products/oreja.webp");
                createProduct("Polvorón Rosa", "Galleta de mantequilla que se deshace en la boca", 0,
                                new BigDecimal("15.00"), categories.get("Pan Dulce Mexicano"),
                                "/images/products/polvoron-rosa.webp");
                createProduct("Garibaldi", "Panecillo cubierto de chochitos de colores", 0, new BigDecimal("16.00"),
                                categories.get("Pan Dulce Mexicano"),
                                "/images/products/garibaldi.webp");

                // Tartas y Pays (5 productos)
                createProduct("Pay de Manzana", "Manzanas caramelizadas con canela en masa crujiente", 2,
                                new BigDecimal("280.00"), categories.get("Tartas y Pays"),
                                "/images/products/pay-manzana.webp");
                createProduct("Pay de Limón", "Relleno cremoso de limón con merengue italiano", 2,
                                new BigDecimal("260.00"), categories.get("Tartas y Pays"),
                                "/images/products/pay-limon.webp");
                createProduct("Pay de Nuez", "Nueces caramelizadas en base de mantequilla", 2, new BigDecimal("320.00"),
                                categories.get("Tartas y Pays"),
                                "/images/products/pay-nuez.webp");
                createProduct("Tarta de Frutos Rojos", "Crema pastelera con frambuesas, moras y fresas", 1,
                                new BigDecimal("350.00"), categories.get("Tartas y Pays"),
                                "/images/products/tarta-frutos-rojos.webp");
                createProduct("Tarta Tatin", "Tarta francesa invertida de manzana caramelizada", 2,
                                new BigDecimal("300.00"), categories.get("Tartas y Pays"),
                                "/images/products/tarta-tatin.webp");

                // Postres Individuales (6 productos)
                createProduct("Tiramisú Individual", "Capas de mascarpone, café y bizcocho. Receta italiana", 1,
                                new BigDecimal("85.00"), categories.get("Postres Individuales"),
                                "/images/products/tiramisu-individual.webp");
                createProduct("Mousse de Chocolate", "Mousse sedoso de chocolate belga 70% cacao", 1,
                                new BigDecimal("75.00"), categories.get("Postres Individuales"),
                                "/images/products/mousse-chocolate.webp");
                createProduct("Cheesecake New York", "Cremoso cheesecake estilo americano con base de galleta", 2,
                                new BigDecimal("90.00"), categories.get("Postres Individuales"),
                                "/images/products/cheesecake-new-york.webp");
                createProduct("Panna Cotta", "Postre italiano de nata con coulis de frutos rojos", 1,
                                new BigDecimal("70.00"), categories.get("Postres Individuales"),
                                "/images/products/panna-cotta.webp");
                createProduct("Crème Brûlée", "Crema francesa con costra de caramelo crujiente", 1,
                                new BigDecimal("80.00"), categories.get("Postres Individuales"),
                                "/images/products/creme-brulee.webp");
                createProduct("Profiteroles", "Choux rellenos de crema con chocolate caliente", 1,
                                new BigDecimal("95.00"), categories.get("Postres Individuales"),
                                "/images/products/profiteroles.webp");

                // Panes Artesanales (4 productos)
                createProduct("Baguette Francesa", "Pan crujiente de masa madre fermentada 24 horas", 1,
                                new BigDecimal("45.00"), categories.get("Panes Artesanales"),
                                "/images/products/baguette-francesa.webp");
                createProduct("Ciabatta Italiana", "Pan rústico italiano con corteza crujiente y miga aireada", 1,
                                new BigDecimal("50.00"), categories.get("Panes Artesanales"),
                                "/images/products/ciabatta-italiana.webp");
                createProduct("Focaccia de Romero", "Pan plano italiano con aceite de oliva y romero fresco", 1,
                                new BigDecimal("65.00"), categories.get("Panes Artesanales"),
                                "/images/products/focaccia-romero.webp");
                createProduct("Pan de Masa Madre", "Hogaza artesanal fermentada naturalmente 48 horas", 2,
                                new BigDecimal("85.00"), categories.get("Panes Artesanales"),
                                "/images/products/pan-masa-madre.webp");

                // Dulces y Confitería (5 productos)
                createProduct("Mazapán de Almendra", "Dulce tradicional de almendra molida. Caja 12 piezas", 1,
                                new BigDecimal("120.00"), categories.get("Dulces y Confitería"),
                                "/images/products/mazapan-almendra.webp");
                createProduct("Cocadas", "Dulce de coco rallado caramelizado. Bolsa 250g", 1, new BigDecimal("65.00"),
                                categories.get("Dulces y Confitería"),
                                "/images/products/cocadas.webp");
                createProduct("Jamoncillo de Leche", "Dulce de leche tradicional mexicano. Caja 8 piezas", 1,
                                new BigDecimal("80.00"), categories.get("Dulces y Confitería"),
                                "/images/products/jamoncillo-leche.webp");
                createProduct("Trufas de Chocolate", "Trufas artesanales de chocolate belga. Caja 6 piezas", 1,
                                new BigDecimal("150.00"), categories.get("Dulces y Confitería"),
                                "/images/products/trufas-chocolate.webp");
                createProduct("Ate con Queso", "Dulce de membrillo artesanal con queso manchego. 300g", 1,
                                new BigDecimal("95.00"), categories.get("Dulces y Confitería"),
                                "/images/products/ate-queso.webp");

                // Productos Veganos (4 productos)
                createProduct("Pastel Vegano de Chocolate", "Sin huevo ni lácteos, igual de delicioso", 2,
                                new BigDecimal("420.00"), categories.get("Productos Veganos"),
                                "/images/products/pastel-vegano-chocolate.webp");
                createProduct("Cupcakes Veganos Variados", "Set de 4 cupcakes veganos de sabores surtidos", 1,
                                new BigDecimal("180.00"), categories.get("Productos Veganos"),
                                "/images/products/cupcakes-veganos.webp");
                createProduct("Galletas Veganas de Avena", "Sin huevo, con chips de chocolate vegano. Docena", 1,
                                new BigDecimal("95.00"), categories.get("Productos Veganos"),
                                "/images/products/galletas-veganas-avena.webp");
                createProduct("Brownie Vegano", "Brownie húmedo sin ingredientes animales", 1, new BigDecimal("55.00"),
                                categories.get("Productos Veganos"),
                                "/images/products/brownie-vegano.webp");

                // Temporada y Festividades (3 productos)
                createProduct("Pan de Muerto", "Tradicional pan de temporada con azahar. Disponible Oct-Nov", 1,
                                new BigDecimal("65.00"), categories.get("Temporada y Festividades"),
                                "/images/products/pan-de-muerto.webp");
                createProduct("Rosca de Reyes", "Con frutas cristalizadas y muñequito. Disponible en Enero", 2,
                                new BigDecimal("280.00"), categories.get("Temporada y Festividades"),
                                "/images/products/rosca-de-reyes.webp");
                createProduct("Tronco de Navidad", "Bûche de Noël con chocolate y crema de castañas", 3,
                                new BigDecimal("450.00"), categories.get("Temporada y Festividades"),
                                "/images/products/tronco-navidad.webp");

                log.info("Seed de productos completado. Total: 50 productos creados.");
        }

        private void createProduct(String name, String description, Integer preparationDays, BigDecimal basePrice,
                        Category category, String imageUrl) {
                Product product = new Product(name, basePrice);
                product.setDescription(description);
                product.setPreparationDays(preparationDays);
                product.setCategory(category);
                product.setImageUrl(imageUrl);
                product.setVisible(true);
                productRepository.save(product);
                log.debug("Producto creado: {} - ${}", name, basePrice);
        }

        // ========== Tags ==========

        private Map<String, Tag> seedTags() {
                Map<String, Tag> tagMap = new HashMap<>();

                Object[][] tagsData = {
                                { "Bestseller", "Productos más vendidos y favoritos de nuestros clientes" },
                                { "Nuevo", "Incorporaciones recientes a nuestro menú" },
                                { "Sin Gluten", "Productos elaborados sin gluten" },
                                { "Vegano", "Productos 100% libres de ingredientes de origen animal" },
                                { "Sin Lácteos", "Productos elaborados sin lácteos ni derivados" },
                                { "Bajo en Azúcar", "Opciones con contenido reducido de azúcar" },
                                { "Con Chocolate", "Productos que contienen chocolate en su preparación" },
                                { "Con Frutas", "Productos elaborados con frutas frescas o naturales" },
                                { "Clásico", "Recetas tradicionales que nunca pasan de moda" },
                                { "Premium", "Selección gourmet con ingredientes de primera calidad" },
                                { "Artesanal", "Elaborados a mano con técnicas artesanales" },
                                { "De Temporada", "Productos disponibles por tiempo limitado según la época del año" },
                                { "Para Compartir", "Porciones ideales para disfrutar en grupo" },
                                { "Individual", "Porciones perfectas para una persona" }
                };

                int created = 0;
                for (Object[] data : tagsData) {
                        String name = (String) data[0];
                        String description = (String) data[1];

                        var existingTag = tagRepository.findByNameIgnoreCase(name);
                        if (existingTag.isEmpty()) {
                                Tag tag = new Tag(name, description);
                                tag.setSlug(SlugUtil.generateSlug(name));
                                tagRepository.save(tag);
                                tagMap.put(name, tag);
                                created++;
                                log.info("Tag creado: {} ({})", name, tag.getSlug());
                        } else {
                                tagMap.put(name, existingTag.get());
                                log.debug("Tag ya existe: {}", name);
                        }
                }

                log.info("Seed de tags completado. {} tags nuevos creados, {} total procesados.", created,
                                tagMap.size());
                return tagMap;
        }

        // ========== Product-Tag associations ==========

        private void seedProductTags(Map<String, Tag> tags) {
                if (productTagRepository.count() > 0) {
                        log.info("Ya existen asociaciones producto-tag. Saltando seed.");
                        return;
                }

                int totalAssociations = 0;

                // Bestseller
                totalAssociations += assignTag(tags, "Bestseller",
                                "Pastel de Chocolate Triple", "Concha de Vainilla",
                                "Galletas de Chispas de Chocolate", "Pay de Manzana",
                                "Cupcake de Chocolate", "Pastel Tres Leches");

                // Nuevo
                totalAssociations += assignTag(tags, "Nuevo",
                                "Brownie Vegano", "Cupcakes Veganos Variados",
                                "Focaccia de Romero", "Crème Brûlée");

                // Sin Gluten
                totalAssociations += assignTag(tags, "Sin Gluten",
                                "Mousse de Chocolate", "Panna Cotta",
                                "Crème Brûlée", "Trufas de Chocolate",
                                "Cocadas", "Mazapán de Almendra");

                // Vegano
                totalAssociations += assignTag(tags, "Vegano",
                                "Pastel Vegano de Chocolate", "Cupcakes Veganos Variados",
                                "Galletas Veganas de Avena", "Brownie Vegano");

                // Sin Lácteos
                totalAssociations += assignTag(tags, "Sin Lácteos",
                                "Pastel Vegano de Chocolate", "Galletas Veganas de Avena",
                                "Brownie Vegano", "Cocadas");

                // Bajo en Azúcar
                totalAssociations += assignTag(tags, "Bajo en Azúcar",
                                "Galletas de Avena con Pasas", "Pan de Masa Madre",
                                "Baguette Francesa");

                // Con Chocolate
                totalAssociations += assignTag(tags, "Con Chocolate",
                                "Pastel de Chocolate Triple", "Cupcake de Chocolate",
                                "Galletas de Chispas de Chocolate", "Mousse de Chocolate",
                                "Trufas de Chocolate", "Pastel Selva Negra",
                                "Cupcake de Nutella", "Brownie Vegano",
                                "Concha de Chocolate");

                // Con Frutas
                totalAssociations += assignTag(tags, "Con Frutas",
                                "Pastel de Fresas con Crema", "Tarta de Frutos Rojos",
                                "Pay de Manzana", "Tarta Tatin",
                                "Pay de Limón", "Pastel de Zanahoria");

                // Clásico
                totalAssociations += assignTag(tags, "Clásico",
                                "Concha de Vainilla", "Concha de Chocolate",
                                "Cuerno de Mantequilla", "Oreja",
                                "Polvorón Rosa", "Pan de Muerto",
                                "Rosca de Reyes", "Pastel de Vainilla Clásico",
                                "Pastel Tres Leches", "Galletas de Mantequilla");

                // Premium
                totalAssociations += assignTag(tags, "Premium",
                                "Pastel Selva Negra", "Profiteroles",
                                "Tiramisú Individual", "Cheesecake New York",
                                "Tronco de Navidad", "Pay de Nuez",
                                "Trufas de Chocolate");

                // Artesanal
                totalAssociations += assignTag(tags, "Artesanal",
                                "Baguette Francesa", "Ciabatta Italiana",
                                "Focaccia de Romero", "Pan de Masa Madre",
                                "Mazapán de Almendra", "Cocadas",
                                "Jamoncillo de Leche");

                // De Temporada
                totalAssociations += assignTag(tags, "De Temporada",
                                "Pan de Muerto", "Rosca de Reyes",
                                "Tronco de Navidad");

                // Para Compartir
                totalAssociations += assignTag(tags, "Para Compartir",
                                "Rosca de Reyes", "Pay de Manzana",
                                "Pay de Nuez", "Tarta de Frutos Rojos",
                                "Pastel de Chocolate Triple");

                // Individual
                totalAssociations += assignTag(tags, "Individual",
                                "Cupcake de Chocolate", "Cupcake Red Velvet",
                                "Cupcake de Limón", "Cupcake de Nutella",
                                "Cupcake de Café Moka", "Tiramisú Individual",
                                "Mousse de Chocolate", "Panna Cotta",
                                "Crème Brûlée", "Profiteroles");

                log.info("Seed de asociaciones producto-tag completado. Total: {} asociaciones creadas.",
                                totalAssociations);
        }

        private int assignTag(Map<String, Tag> tags, String tagName, String... productNames) {
                Tag tag = tags.get(tagName);
                if (tag == null) {
                        log.warn("Tag no encontrado: {}", tagName);
                        return 0;
                }

                int count = 0;
                for (String productName : productNames) {
                        var productOpt = productRepository.findByNameIgnoreCase(productName);
                        if (productOpt.isPresent()) {
                                Product product = productOpt.get();
                                if (!productTagRepository.existsByProductIdAndTagId(product.getId(), tag.getId())) {
                                        productTagRepository.save(new ProductTag(product, tag));
                                        count++;
                                        log.debug("Asociación creada: {} -> {}", productName, tagName);
                                }
                        } else {
                                log.warn("Producto no encontrado para tag '{}': {}", tagName, productName);
                        }
                }
                return count;
        }

        // ========== Storefront Sections ==========

        private Map<String, StorefrontSection> seedSections() {
                Map<String, StorefrontSection> sectionMap = new HashMap<>();

                Object[][] sectionsData = {
                                { "Más Vendidos", "Los productos favoritos de nuestros clientes", 1 },
                                { "Novedades", "Las incorporaciones más recientes a nuestro menú", 2 },
                                { "Pasteles Destacados", "Nuestra selección de pasteles más especiales", 3 },
                                { "Postres Premium", "Postres gourmet elaborados con ingredientes de primera", 4 },
                                { "Pan Dulce Mexicano", "Auténtico pan dulce de tradición mexicana", 5 },
                                { "Para Compartir", "Porciones ideales para disfrutar en grupo", 6 }
                };

                int created = 0;
                for (Object[] data : sectionsData) {
                        String name = (String) data[0];
                        String description = (String) data[1];
                        Integer displayOrder = (Integer) data[2];

                        var existingSection = sectionRepository.findByNameIgnoreCase(name);
                        if (existingSection.isEmpty()) {
                                StorefrontSection section = new StorefrontSection(name, description);
                                section.setSlug(SlugUtil.generateSlug(name));
                                section.setDisplayOrder(displayOrder);
                                section.setVisible(true);
                                sectionRepository.save(section);
                                sectionMap.put(name, section);
                                created++;
                                log.info("Sección creada: {} ({})", name, section.getSlug());
                        } else {
                                sectionMap.put(name, existingSection.get());
                                log.debug("Sección ya existe: {}", name);
                        }
                }

                log.info("Seed de secciones completado. {} secciones nuevas creadas, {} total procesadas.", created,
                                sectionMap.size());
                return sectionMap;
        }

        private void seedSectionProducts(Map<String, StorefrontSection> sections) {
                if (sectionProductRepository.count() > 0) {
                        log.info("Ya existen asociaciones sección-producto. Saltando seed.");
                        return;
                }

                int totalAssociations = 0;

                // Más Vendidos
                totalAssociations += assignSection(sections, "Más Vendidos",
                                "Pastel de Chocolate Triple", "Concha de Vainilla",
                                "Galletas de Chispas de Chocolate", "Pay de Manzana",
                                "Cupcake de Chocolate", "Pastel Tres Leches");

                // Novedades
                totalAssociations += assignSection(sections, "Novedades",
                                "Brownie Vegano", "Cupcakes Veganos Variados",
                                "Focaccia de Romero", "Crème Brûlée",
                                "Tarta Tatin");

                // Pasteles Destacados
                totalAssociations += assignSection(sections, "Pasteles Destacados",
                                "Pastel de Chocolate Triple", "Pastel Red Velvet",
                                "Pastel de Fresas con Crema", "Pastel Tres Leches",
                                "Pastel Selva Negra", "Pastel de Zanahoria");

                // Postres Premium
                totalAssociations += assignSection(sections, "Postres Premium",
                                "Tiramisú Individual", "Mousse de Chocolate",
                                "Cheesecake New York", "Panna Cotta",
                                "Crème Brûlée", "Profiteroles",
                                "Trufas de Chocolate");

                // Pan Dulce Mexicano
                totalAssociations += assignSection(sections, "Pan Dulce Mexicano",
                                "Concha de Vainilla", "Concha de Chocolate",
                                "Cuerno de Mantequilla", "Oreja",
                                "Polvorón Rosa", "Garibaldi");

                // Para Compartir
                totalAssociations += assignSection(sections, "Para Compartir",
                                "Rosca de Reyes", "Pay de Manzana",
                                "Pay de Nuez", "Tarta de Frutos Rojos",
                                "Pastel de Chocolate Triple");

                log.info("Seed de asociaciones sección-producto completado. Total: {} asociaciones creadas.",
                                totalAssociations);
        }

        private int assignSection(Map<String, StorefrontSection> sections, String sectionName,
                        String... productNames) {
                StorefrontSection section = sections.get(sectionName);
                if (section == null) {
                        log.warn("Sección no encontrada: {}", sectionName);
                        return 0;
                }

                int count = 0;
                int order = 1;
                for (String productName : productNames) {
                        var productOpt = productRepository.findByNameIgnoreCase(productName);
                        if (productOpt.isPresent()) {
                                Product product = productOpt.get();
                                if (!sectionProductRepository.existsByStorefrontSectionIdAndProductId(section.getId(),
                                                product.getId())) {
                                        sectionProductRepository
                                                        .save(new StorefrontSectionProduct(section, product, order));
                                        count++;
                                        log.debug("Asociación sección creada: {} -> {} (orden {})", productName,
                                                        sectionName, order);
                                }
                        } else {
                                log.warn("Producto no encontrado para sección '{}': {}", sectionName, productName);
                        }
                        order++;
                }
                return count;
        }

        // ========== Recetas (Product-Ingredient) ==========

        /**
         * Recetas de los 50 productos. Cada fila es
         * { producto, "ingrediente:cantidad", ... }.
         * La cantidad se expresa SIEMPRE en la unidad de medida del ingrediente:
         * "Harina de Trigo:0.5" son 0.5 kg (KILOGRAMO) y "Huevos:6" son 6 piezas
         * (UNIDAD). Espejo exacto de la seccion 11 de R__seed_demo_data.sql.
         */
        private static final String[][] RECIPES = {
                        { "Pastel de Chocolate Triple", "Harina de Trigo:0.5", "Azúcar Blanca:0.4", "Chocolate Amargo:0.35", "Mantequilla:0.25", "Huevos:6", "Crema para Batir:0.3", "Cacao en Polvo:0.08" },
                        { "Pastel Red Velvet", "Harina de Trigo:0.45", "Azúcar Blanca:0.35", "Mantequilla:0.2", "Huevos:4", "Queso Crema:0.4", "Crema para Batir:0.2", "Colorante Alimentario:1", "Cacao en Polvo:0.03" },
                        { "Pastel de Vainilla Clásico", "Harina de Trigo:0.5", "Azúcar Blanca:0.4", "Mantequilla:0.3", "Huevos:5", "Leche Entera:0.25", "Azúcar Glass:0.2", "Extracto de Vainilla:0.02", "Polvo para Hornear:0.015" },
                        { "Pastel de Zanahoria", "Harina Integral:0.4", "Azúcar Morena:0.35", "Aceite Vegetal:0.25", "Huevos:4", "Nueces:0.15", "Pasas:0.1", "Queso Crema:0.35", "Canela en Polvo:0.01" },
                        { "Pastel de Fresas con Crema", "Harina de Trigo:0.45", "Azúcar Blanca:0.3", "Huevos:5", "Fresas Frescas:0.6", "Crema para Batir:0.5", "Mantequilla:0.15", "Azúcar Glass:0.1" },
                        { "Pastel Tres Leches", "Harina de Trigo:0.4", "Azúcar Blanca:0.3", "Huevos:6", "Leche Condensada:0.4", "Leche Evaporada:0.35", "Leche Entera:0.3", "Crema para Batir:0.25" },
                        { "Pastel Selva Negra", "Harina de Trigo:0.4", "Azúcar Blanca:0.3", "Chocolate Amargo:0.3", "Cerezas:0.4", "Crema para Batir:0.5", "Huevos:5", "Cacao en Polvo:0.05" },
                        { "Cupcake de Chocolate", "Harina de Trigo:0.05", "Azúcar Blanca:0.04", "Chocolate Amargo:0.03", "Mantequilla:0.03", "Huevos:1", "Cacao en Polvo:0.01" },
                        { "Cupcake Red Velvet", "Harina de Trigo:0.05", "Azúcar Blanca:0.04", "Queso Crema:0.05", "Mantequilla:0.025", "Huevos:1", "Colorante Alimentario:0.1", "Cacao en Polvo:0.005" },
                        { "Cupcake de Limón", "Harina de Trigo:0.05", "Azúcar Blanca:0.045", "Mantequilla:0.035", "Huevos:1", "Limones:0.08", "Azúcar Glass:0.04" },
                        { "Cupcake de Nutella", "Harina de Trigo:0.05", "Azúcar Blanca:0.035", "Nutella:0.06", "Mantequilla:0.03", "Huevos:1", "Avellanas:0.02" },
                        { "Cupcake de Café Moka", "Harina de Trigo:0.05", "Azúcar Blanca:0.04", "Café Instantáneo:0.01", "Chocolate de Leche:0.03", "Mantequilla:0.03", "Huevos:1" },
                        { "Galletas de Chispas de Chocolate", "Harina de Trigo:0.25", "Azúcar Morena:0.12", "Mantequilla:0.1", "Huevos:1", "Chispas de Chocolate:0.1", "Bicarbonato de Sodio:0.005" },
                        { "Galletas de Avena con Pasas", "Harina de Avena:0.2", "Harina de Trigo:0.1", "Azúcar Morena:0.1", "Mantequilla:0.09", "Huevos:1", "Pasas:0.08", "Canela en Polvo:0.005" },
                        { "Galletas de Mantequilla", "Harina de Trigo:0.25", "Mantequilla:0.15", "Azúcar Glass:0.08", "Huevos:1", "Extracto de Vainilla:0.005", "Sal:0.002" },
                        { "Galletas de Jengibre", "Harina de Trigo:0.25", "Piloncillo:0.1", "Mantequilla:0.1", "Jengibre en Polvo:0.01", "Canela en Polvo:0.008", "Huevos:1", "Bicarbonato de Sodio:0.005" },
                        { "Galletas Snickerdoodle", "Harina de Trigo:0.25", "Azúcar Blanca:0.15", "Mantequilla:0.12", "Huevos:1", "Canela en Polvo:0.01", "Cremor Tártaro:0.004" },
                        { "Concha de Vainilla", "Harina de Trigo:0.08", "Azúcar Blanca:0.03", "Mantequilla:0.02", "Huevos:0.5", "Levadura Fresca:0.003", "Extracto de Vainilla:0.001" },
                        { "Concha de Chocolate", "Harina de Trigo:0.08", "Azúcar Blanca:0.03", "Mantequilla:0.02", "Huevos:0.5", "Levadura Fresca:0.003", "Cacao en Polvo:0.008" },
                        { "Cuerno de Mantequilla", "Harina de Trigo:0.09", "Mantequilla:0.04", "Azúcar Glass:0.01", "Huevos:0.5", "Levadura Fresca:0.003", "Sal:0.001" },
                        { "Oreja", "Harina de Trigo:0.08", "Mantequilla:0.05", "Azúcar Blanca:0.04", "Sal:0.001" },
                        { "Polvorón Rosa", "Harina de Trigo:0.06", "Manteca Vegetal:0.03", "Azúcar Glass:0.03", "Colorante Alimentario:0.02", "Huevos:0.25" },
                        { "Garibaldi", "Harina de Trigo:0.06", "Azúcar Blanca:0.025", "Mantequilla:0.02", "Huevos:0.5", "Mermelada de Fresa:0.015", "Sprinkles:0.005" },
                        { "Pay de Manzana", "Harina de Trigo:0.35", "Mantequilla:0.2", "Azúcar Blanca:0.15", "Azúcar Morena:0.05", "Manzanas:0.8", "Canela en Polvo:0.01", "Huevos:1" },
                        { "Pay de Limón", "Harina de Trigo:0.3", "Mantequilla:0.18", "Leche Condensada:0.4", "Limones:0.35", "Huevos:4", "Azúcar Blanca:0.15" },
                        { "Pay de Nuez", "Harina de Trigo:0.3", "Mantequilla:0.18", "Nueces:0.35", "Azúcar Morena:0.2", "Huevos:3", "Miel de Abeja:0.05" },
                        { "Tarta de Frutos Rojos", "Harina de Trigo:0.25", "Mantequilla:0.15", "Frambuesas:0.25", "Moras:0.25", "Fresas Frescas:0.2", "Leche Entera:0.3", "Yema de Huevo:0.08", "Azúcar Blanca:0.12" },
                        { "Tarta Tatin", "Harina de Trigo:0.25", "Mantequilla:0.2", "Manzanas:0.9", "Azúcar Blanca:0.25", "Caramelo:0.1" },
                        { "Tiramisú Individual", "Harina de Trigo:0.05", "Queso Crema:0.12", "Café Instantáneo:0.01", "Huevos:2", "Azúcar Blanca:0.05", "Crema para Batir:0.08", "Cacao en Polvo:0.008" },
                        { "Mousse de Chocolate", "Chocolate Amargo:0.08", "Crema para Batir:0.12", "Huevos:2", "Azúcar Blanca:0.03", "Gelatina Sin Sabor:0.003" },
                        { "Cheesecake New York", "Queso Crema:0.2", "Azúcar Blanca:0.08", "Huevos:2", "Crema para Batir:0.1", "Mantequilla:0.04", "Harina de Trigo:0.06", "Extracto de Vainilla:0.004" },
                        { "Panna Cotta", "Crema para Batir:0.2", "Leche Entera:0.1", "Azúcar Blanca:0.05", "Gelatina Sin Sabor:0.005", "Frambuesas:0.06", "Vainilla en Vaina:0.5" },
                        { "Crème Brûlée", "Crema para Batir:0.22", "Yema de Huevo:0.06", "Azúcar Blanca:0.06", "Vainilla en Vaina:1" },
                        { "Profiteroles", "Harina de Trigo:0.08", "Mantequilla:0.06", "Huevos:3", "Leche Entera:0.15", "Chocolate Amargo:0.06", "Crema para Batir:0.12", "Azúcar Blanca:0.03" },
                        { "Baguette Francesa", "Harina de Trigo:0.4", "Levadura Fresca:0.006", "Sal:0.008" },
                        { "Ciabatta Italiana", "Harina de Trigo:0.4", "Aceite de Oliva:0.02", "Levadura Seca:0.004", "Sal:0.008" },
                        { "Focaccia de Romero", "Harina de Trigo:0.35", "Aceite de Oliva:0.06", "Levadura Seca:0.004", "Sal de Mar:0.006", "Sal:0.006" },
                        { "Pan de Masa Madre", "Harina de Trigo:0.5", "Harina Integral:0.15", "Levadura Fresca:0.004", "Sal:0.012" },
                        { "Mazapán de Almendra", "Almendras:0.25", "Azúcar Glass:0.18" },
                        { "Cocadas", "Coco Rallado:0.2", "Leche Condensada:0.15", "Azúcar Blanca:0.05" },
                        { "Jamoncillo de Leche", "Leche Entera:0.6", "Azúcar Blanca:0.2", "Leche Condensada:0.1", "Nueces:0.04" },
                        { "Trufas de Chocolate", "Chocolate Amargo:0.25", "Crema para Batir:0.1", "Mantequilla:0.03", "Cacao en Polvo:0.02" },
                        { "Ate con Queso", "Manzanas:0.4", "Azúcar Blanca:0.25", "Limones:0.05", "Gelatina Sin Sabor:0.004" },
                        { "Pastel Vegano de Chocolate", "Harina de Trigo:0.45", "Azúcar Morena:0.35", "Cacao en Polvo:0.1", "Aceite de Coco:0.15", "Leche de Almendra:0.4", "Chocolate Amargo:0.15", "Bicarbonato de Sodio:0.008" },
                        { "Cupcakes Veganos Variados", "Harina de Trigo:0.2", "Azúcar Morena:0.15", "Aceite de Coco:0.06", "Leche de Almendra:0.18", "Cacao en Polvo:0.03", "Chispas de Chocolate:0.05", "Polvo para Hornear:0.008" },
                        { "Galletas Veganas de Avena", "Harina de Avena:0.2", "Azúcar Morena:0.12", "Aceite de Coco:0.08", "Chispas de Chocolate:0.08", "Leche de Almendra:0.06", "Bicarbonato de Sodio:0.004" },
                        { "Brownie Vegano", "Harina de Trigo:0.06", "Azúcar Morena:0.05", "Cacao en Polvo:0.02", "Aceite de Coco:0.03", "Leche de Almendra:0.05", "Nueces:0.02" },
                        { "Pan de Muerto", "Harina de Trigo:0.25", "Mantequilla:0.08", "Azúcar Blanca:0.06", "Huevos:2", "Azahar:0.005", "Levadura Fresca:0.005", "Naranjas:0.05" },
                        { "Rosca de Reyes", "Harina de Trigo:0.5", "Mantequilla:0.2", "Azúcar Blanca:0.15", "Huevos:5", "Levadura Fresca:0.008", "Duraznos:0.1", "Naranjas:0.08", "Azahar:0.006" },
                        { "Tronco de Navidad", "Harina de Trigo:0.3", "Chocolate Amargo:0.25", "Crema para Batir:0.35", "Huevos:6", "Azúcar Blanca:0.2", "Avellanas:0.1", "Cacao en Polvo:0.04" }
        };

        private void seedProductIngredients() {
                if (productIngredientRepository.count() > 0) {
                        log.info("Ya existen recetas en la base de datos. Saltando seed de recetas.");
                        return;
                }

                int totalLines = 0;
                for (String[] recipe : RECIPES) {
                        String productName = recipe[0];
                        var productOpt = productRepository.findByNameIgnoreCase(productName);
                        if (productOpt.isEmpty()) {
                                log.warn("Producto no encontrado para receta: {}", productName);
                                continue;
                        }
                        Product product = productOpt.get();

                        for (int i = 1; i < recipe.length; i++) {
                                String[] parts = recipe[i].split(":");
                                var ingredientOpt = ingredientRepository.findByNameIgnoreCase(parts[0]);
                                if (ingredientOpt.isEmpty()) {
                                        log.warn("Ingrediente no encontrado para receta '{}': {}", productName,
                                                        parts[0]);
                                        continue;
                                }
                                productIngredientRepository.save(new ProductIngredient(product, ingredientOpt.get(),
                                                new BigDecimal(parts[1])));
                                totalLines++;
                        }
                }

                log.info("Seed de recetas completado. Total: {} lineas de receta creadas.", totalLines);
        }

        // ========== Ventas ==========

        /**
         * Historial de ventas demo. Cada fila es
         * { producto, cantidad, precioUnitario, diasAtras, hora, cliente, dni,
         * telefono, notas, emailVendedor }.
         * Las fechas son relativas al dia de arranque, por lo que el dashboard
         * siempre encuentra ventas de hoy y del mes en curso.
         * Espejo exacto de la seccion 12 de R__seed_demo_data.sql.
         */
        private static final Object[][] SALES = {
                        { "Pastel de Chocolate Triple", 1, "450.00", 0, 10, "María González Ruiz", "30125478", "55 1234 5601", "Entrega en sucursal centro", "employee@malva.com" },
                        { "Concha de Vainilla", 12, "18.00", 0, 11, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Cupcake de Chocolate", 6, "45.00", 0, 13, "Laura Méndez", "28994512", "55 1234 5602", "Pedido para oficina", "employee@malva.com" },
                        { "Galletas de Chispas de Chocolate", 2, "85.00", 0, 16, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Pastel Tres Leches", 1, "350.00", 1, 12, "Jorge Ramírez", "31450098", "55 1234 5603", "Cumpleaños familiar", "employee@malva.com" },
                        { "Pay de Manzana", 1, "280.00", 1, 15, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Cuerno de Mantequilla", 10, "20.00", 1, 17, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Cheesecake New York", 4, "90.00", 2, 11, "Patricia Soto", "27883421", "55 1234 5604", null, "employee@malva.com" },
                        { "Baguette Francesa", 6, "45.00", 2, 18, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Pastel Red Velvet", 1, "420.00", 3, 9, "Andrea Villalobos", "30012765", "55 1234 5605", "Decoración especial solicitada", "admin@malva.com" },
                        { "Cupcake Red Velvet", 12, "48.00", 3, 14, "Escuela Primaria Sol", "30559981", "55 1234 5606", "Festival escolar", "employee@malva.com" },
                        { "Trufas de Chocolate", 3, "150.00", 4, 16, "Ricardo Fuentes", "29334410", "55 1234 5607", null, "employee@malva.com" },
                        { "Concha de Chocolate", 20, "18.00", 4, 10, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Tarta de Frutos Rojos", 1, "350.00", 5, 13, "Sofía Herrera", "31220845", "55 1234 5608", "Aniversario", "employee@malva.com" },
                        { "Galletas de Avena con Pasas", 3, "75.00", 5, 17, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Pastel de Fresas con Crema", 1, "480.00", 6, 11, "Familia Ortega", "26778123", "55 1234 5609", "Recoger 18:00 hs", "admin@malva.com" },
                        { "Tiramisú Individual", 8, "85.00", 6, 15, "Café Aurora", "30991256", "55 1234 5610", "Pedido mayorista semanal", "employee@malva.com" },
                        { "Pan de Masa Madre", 4, "85.00", 8, 12, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Mousse de Chocolate", 6, "75.00", 9, 14, "Daniela Cruz", "31008877", "55 1234 5611", null, "employee@malva.com" },
                        { "Pastel Selva Negra", 1, "520.00", 10, 10, "Empresa Nexus SA", "30447712", "55 1234 5612", "Factura a nombre de la empresa", "admin@malva.com" },
                        { "Oreja", 15, "22.00", 11, 16, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Pay de Limón", 2, "260.00", 12, 13, "Marcos Iturbe", "28110934", "55 1234 5613", null, "employee@malva.com" },
                        { "Brownie Vegano", 10, "55.00", 13, 15, "Tienda Verde", "31556602", "55 1234 5614", "Pedido vegano recurrente", "employee@malva.com" },
                        { "Cupcake de Nutella", 6, "55.00", 14, 12, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Profiteroles", 5, "95.00", 15, 17, "Valeria Nava", "29887340", "55 1234 5615", null, "employee@malva.com" },
                        { "Pastel de Zanahoria", 1, "400.00", 16, 11, "Hugo Salazar", "30778215", "55 1234 5616", "Sin nueces en la cobertura", "employee@malva.com" },
                        { "Galletas de Mantequilla", 4, "70.00", 17, 14, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Panna Cotta", 6, "70.00", 18, 16, "Restaurante Bellavista", "30223391", "55 1234 5617", "Pedido para servicio de cena", "employee@malva.com" },
                        { "Polvorón Rosa", 24, "15.00", 19, 10, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Pastel de Vainilla Clásico", 1, "380.00", 20, 13, "Elena Paredes", "27665048", "55 1234 5618", "Cumpleaños infantil", "admin@malva.com" },
                        { "Ciabatta Italiana", 8, "50.00", 21, 18, "Bistró La Esquina", "31447790", "55 1234 5619", "Pedido mayorista", "employee@malva.com" },
                        { "Crème Brûlée", 4, "80.00", 23, 15, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Mazapán de Almendra", 2, "120.00", 25, 12, "Gabriel Lozano", "29556614", "55 1234 5620", null, "admin@malva.com" },
                        { "Pay de Nuez", 1, "320.00", 27, 11, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Cocadas", 6, "65.00", 29, 16, "Mercadito Artesanal", "30889945", "55 1234 5621", "Puesto de feria", "employee@malva.com" },
                        { "Focaccia de Romero", 5, "65.00", 31, 14, "Venta de mostrador", null, null, null, "admin@malva.com" },
                        { "Pastel Vegano de Chocolate", 1, "420.00", 33, 12, "Camila Duarte", "31770223", "55 1234 5622", "Sin trazas de lácteos", "employee@malva.com" },
                        { "Galletas de Jengibre", 3, "80.00", 35, 17, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Tarta Tatin", 2, "300.00", 38, 13, "Fernando Aguirre", "28993377", "55 1234 5623", null, "admin@malva.com" },
                        { "Jamoncillo de Leche", 4, "80.00", 40, 15, "Venta de mostrador", null, null, null, "employee@malva.com" },
                        { "Cupcakes Veganos Variados", 2, "180.00", 42, 11, "Tienda Verde", "31556602", "55 1234 5614", "Segundo pedido del mes", "employee@malva.com" },
                        { "Cupcake de Café Moka", 6, "50.00", 45, 16, "Venta de mostrador", null, null, null, "admin@malva.com" }
        };

        private void seedSales() {
                if (saleRepository.count() > 0) {
                        log.info("Ya existen ventas en la base de datos. Saltando seed de ventas.");
                        return;
                }

                int totalSales = 0;
                for (Object[] data : SALES) {
                        String productName = (String) data[0];
                        var productOpt = productRepository.findByNameIgnoreCase(productName);
                        if (productOpt.isEmpty()) {
                                log.warn("Producto no encontrado para venta: {}", productName);
                                continue;
                        }

                        String sellerEmail = (String) data[9];
                        var sellerOpt = userRepository.findByEmail(sellerEmail);
                        if (sellerOpt.isEmpty()) {
                                log.warn("Usuario no encontrado para venta de '{}': {}", productName, sellerEmail);
                                continue;
                        }

                        Product product = productOpt.get();
                        int quantity = (Integer) data[1];
                        BigDecimal unitPrice = new BigDecimal((String) data[2]);
                        LocalDateTime saleDate = LocalDate.now()
                                        .minusDays((Integer) data[3])
                                        .atTime((Integer) data[4], 0);

                        Sale sale = new Sale(saleDate, sellerOpt.get(), product, product.getName(), quantity,
                                        unitPrice);
                        sale.setCustomerName((String) data[5]);
                        sale.setCustomerDni((String) data[6]);
                        sale.setCustomerPhone((String) data[7]);
                        sale.setNotes((String) data[8]);

                        // Snapshot de insumos con la misma logica que SaleService.create()
                        for (ProductIngredient pi : productIngredientRepository.findByProductId(product.getId())) {
                                BigDecimal quantityUsed = pi.getQuantity().multiply(BigDecimal.valueOf(quantity));
                                sale.addSaleIngredient(new SaleIngredient(
                                                sale,
                                                pi.getIngredient(),
                                                pi.getIngredient().getName(),
                                                quantityUsed,
                                                pi.getIngredient().getUnitCost(),
                                                pi.getIngredient().getUnitOfMeasure().getDisplayName()));
                        }

                        // Cascade ALL persiste tambien los SaleIngredient
                        saleRepository.save(sale);
                        totalSales++;
                }

                log.info("Seed de ventas completado. Total: {} ventas creadas.", totalSales);
        }
}
