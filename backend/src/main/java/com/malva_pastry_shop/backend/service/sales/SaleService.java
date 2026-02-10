package com.malva_pastry_shop.backend.service.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.ProductIngredient;
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.domain.sales.SaleIngredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.dto.request.SaleRequest;
import com.malva_pastry_shop.backend.repository.SaleIngredientRepository;
import com.malva_pastry_shop.backend.repository.SaleRepository;
import com.malva_pastry_shop.backend.service.storefront.ProductService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleIngredientRepository saleIngredientRepository;
    private final ProductService productService;

    public SaleService(SaleRepository saleRepository,
            SaleIngredientRepository saleIngredientRepository,
            ProductService productService) {
        this.saleRepository = saleRepository;
        this.saleIngredientRepository = saleIngredientRepository;
        this.productService = productService;
    }

    // ========== Consultas ==========

    public Page<Sale> findAll(Pageable pageable) {
        return saleRepository.findAll(pageable);
    }

    public Page<Sale> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return saleRepository.findBySaleDateBetween(startDate, endDate, pageable);
    }

    public Page<Sale> search(String productName, Pageable pageable) {
        return saleRepository.findByProductNameContainingIgnoreCase(productName, pageable);
    }

    @Transactional(readOnly = true)
    public Sale findById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + id));
    }

    /**
     * Obtiene una venta con todas sus relaciones inicializadas para la vista de
     * detalle.
     * Inicializa: registeredBy, saleIngredients
     */
    @Transactional(readOnly = true)
    public Sale findByIdWithDetails(Long id) {
        Sale sale = findById(id);
        // Forzar inicializacion de relaciones lazy
        if (sale.getRegisteredBy() != null) {
            sale.getRegisteredBy().getFullName(); // Inicializa User
        }
        // Inicializar saleIngredients para poder calcular costos en la vista
        sale.getSaleIngredients().size();
        return sale;
    }

    /**
     * Obtiene los ingredientes de una venta (snapshot).
     */
    @Transactional(readOnly = true)
    public List<SaleIngredient> getSaleIngredients(Long saleId) {
        // Verificar que la venta existe
        findById(saleId);
        return saleIngredientRepository.findBySaleId(saleId);
    }

    /**
     * Calcula el costo total de ingredientes de una venta.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalIngredientCost(Long saleId) {
        List<SaleIngredient> ingredients = getSaleIngredients(saleId);
        return ingredients.stream()
                .map(SaleIngredient::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== Crear Venta ==========

    /**
     * Crea una nueva venta con snapshot de ingredientes.
     */
    @Transactional
    public Sale create(SaleRequest request, User registeredBy) {
        // 1. Validar que el producto existe y esta activo
        Product product = productService.findById(request.getProductId());

        // 2. Crear Sale con snapshot del nombre del producto
        Sale sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setRegisteredBy(registeredBy);
        sale.setProduct(product);
        sale.setProductName(product.getName());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setNotes(request.getNotes());

        // Datos del cliente (opcionales)
        sale.setCustomerName(request.getCustomerName());
        sale.setCustomerDni(request.getCustomerDni());
        sale.setCustomerPhone(request.getCustomerPhone());

        // 5. Calcular totalAmount
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Validar que totalAmount no exceda 10 dígitos enteros
        if (totalAmount.precision() - totalAmount.scale() > 10) {
            throw new IllegalArgumentException(
                    "El monto total de la venta excede el límite permitido (máximo: 9,999,999,999.9999). " +
                            "Reduce la cantidad o el precio unitario.");
        }

        sale.setTotalAmount(totalAmount);

        // 3. Obtener ingredientes del producto (receta)
        List<ProductIngredient> productIngredients = productService.getProductIngredients(product.getId());

        // 4. Por cada ingrediente, crear SaleIngredient con snapshots
        for (ProductIngredient pi : productIngredients) {
            // quantityUsed = receta.quantity * cantidadVendida
            BigDecimal quantityUsed = pi.getQuantity()
                    .multiply(BigDecimal.valueOf(request.getQuantity()));

            // Validar que quantityUsed no exceda 10 dígitos enteros
            if (quantityUsed.precision() - quantityUsed.scale() > 10) {
                throw new IllegalArgumentException(
                        "La cantidad del ingrediente '" + pi.getIngredient().getName() +
                                "' excede el límite permitido. Reduce la cantidad de venta.");
            }

            BigDecimal unitCost = pi.getIngredient().getUnitCost();
            BigDecimal totalCost = quantityUsed.multiply(unitCost);

            // Validar que totalCost no exceda 10 dígitos enteros
            if (totalCost.precision() - totalCost.scale() > 10) {
                throw new IllegalArgumentException(
                        "El costo total del ingrediente '" + pi.getIngredient().getName() +
                                "' excede el límite permitido (máximo: 9,999,999,999.9999). " +
                                "Reduce la cantidad de venta o el costo unitario del ingrediente.");
            }

            SaleIngredient saleIngredient = new SaleIngredient(
                    sale,
                    pi.getIngredient(),
                    pi.getIngredient().getName(),
                    quantityUsed,
                    unitCost,
                    pi.getIngredient().getUnitOfMeasure().getDisplayName());

            sale.addSaleIngredient(saleIngredient);
        }

        // 6. Guardar Sale (cascade guarda SaleIngredient)
        return saleRepository.save(sale);
    }

    // ========== Filtros Combinados ==========

    public Page<Sale> findByProductNameAndDateRange(String productName, LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        return saleRepository.findByProductNameAndDateRange(productName, start, end, pageable);
    }

    // ========== Estadisticas ==========

    /**
     * Cuenta cuantas ventas tiene un producto.
     */
    public long countSalesByProduct(Long productId) {
        return saleRepository.countByProductId(productId);
    }

    public long countSalesInRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.countBySaleDateBetween(start, end);
    }

    public BigDecimal totalRevenueInRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.sumTotalAmountBySaleDateBetween(start, end);
    }

    // ========== Sumas para filtros ==========

    public BigDecimal sumTotalAmount() {
        return saleRepository.sumTotalAmount();
    }

    public BigDecimal sumTotalAmountByDateRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.sumTotalAmountByDateRange(start, end);
    }

    public BigDecimal sumTotalAmountByProductName(String productName) {
        return saleRepository.sumTotalAmountByProductNameContaining(productName);
    }

    public BigDecimal sumTotalAmountByProductNameAndDateRange(String productName, LocalDateTime start,
            LocalDateTime end) {
        return saleRepository.sumTotalAmountByProductNameAndDateRange(productName, start, end);
    }
}
