/*
 * Formulario de nueva venta: autocompleta el precio del producto elegido y
 * previsualiza el total.
 *
 * Estaba embebido en la plantilla. Era el último <script> inline del panel, y
 * mientras hubiera uno no se podía servir una Content-Security-Policy sin
 * 'unsafe-inline'.
 */
(function () {
    'use strict';

    var product = document.getElementById('productId');
    var quantity = document.getElementById('quantity');
    var unitPrice = document.getElementById('unitPrice');
    var preview = document.getElementById('totalPreview');
    var total = document.getElementById('totalAmount');
    if (!product || !quantity || !unitPrice || !preview || !total) return;

    /*
     * El total se muestra con el formato del negocio, igual que los importes
     * que arma el servidor. Antes usaba toFixed(2), que da punto decimal:
     * la previsualización decía "$12.50" y la venta guardada aparecía como
     * "$12,50" en la ficha, para el mismo número.
     */
    var money = new Intl.NumberFormat('es-AR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    /*
     * El precio sale de data-price, no de parsear el texto de la opción.
     * Antes se hacía text.match(/\$([0-9]+\.?[0-9]*)/) sobre la etiqueta ya
     * formateada, y ese formato depende del locale: con separador decimal
     * coma, "Torta - $12,50" capturaba "12" y la venta se registraba sin los
     * centavos.
     */
    product.addEventListener('change', function () {
        var price = product.options[product.selectedIndex].dataset.price;
        if (price) {
            unitPrice.value = price;
        }
        calculateTotal();
    });

    function calculateTotal() {
        var amount = (parseFloat(quantity.value) || 0) * (parseFloat(unitPrice.value) || 0);
        var visible = amount > 0;

        preview.classList.toggle('hidden', !visible);
        if (visible) {
            total.textContent = '$' + money.format(amount);
        }
    }

    quantity.addEventListener('input', calculateTotal);
    unitPrice.addEventListener('input', calculateTotal);

    // Por si el formulario vuelve con valores tras un error de validación.
    calculateTotal();
})();
