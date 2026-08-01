/*
 * Modal de "editar cantidad" de products/recipe.
 *
 * Vive en su propio archivo, no en admin.js, porque es comportamiento de una
 * sola página: admin.js aporta solo la parte compartida (cerrar por
 * [data-dialog-close]). Y no vive inline en la plantilla porque el JS inline
 * es lo que bloquea servir una CSP sin 'unsafe-inline'.
 *
 * El atrapado del foco, el cierre con Escape, aria-modal y la devolución del
 * foco al botón que abrió NO están acá: los da <dialog>.showModal() nativo.
 */
(function () {
    'use strict';

    var dialog = document.getElementById('editQuantityDialog');
    if (!dialog || typeof dialog.showModal !== 'function') return;

    var form = document.getElementById('editQuantityForm');
    var quantity = document.getElementById('editQuantity');
    var ingredientLabel = dialog.querySelector('[data-dialog-ingredient]');
    var unitLabel = dialog.querySelector('[data-dialog-unit]');

    document.querySelectorAll('.edit-btn').forEach(function (button) {
        button.addEventListener('click', function () {
            ingredientLabel.textContent = button.dataset.ingredientName;
            unitLabel.textContent = button.dataset.unit;
            quantity.value = button.dataset.quantity;

            // La URL viene armada por Thymeleaf en data-action (respeta el
            // context-path); antes se concatenaba '/products/' + id acá.
            form.action = button.dataset.action;

            if (!dialog.open) dialog.showModal();

            // showModal() enfoca el primer elemento focusable, que sería el
            // input igual; explicitarlo y seleccionar el valor deja la cantidad
            // lista para sobrescribir, que es a lo que se abre este modal.
            quantity.focus();
            quantity.select();
        });
    });
})();
