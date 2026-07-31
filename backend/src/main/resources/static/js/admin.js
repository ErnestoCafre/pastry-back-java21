/*
 * Comportamiento compartido del panel de administración.
 *
 * Reemplaza los onsubmit="return confirm('...')" inline que estaban
 * repetidos en 15 templates. Un único listener delegado en el documento
 * atiende cualquier formulario con [data-confirm], presente o agregado
 * después.
 *
 * Al no haber JS inline, el panel puede recibir una Content-Security-Policy
 * sin 'unsafe-inline' (hoy no tiene ninguna cabecera CSP configurada), y
 * cambiar confirm() por un <dialog> accesible es un cambio de este archivo.
 */
(function () {
    'use strict';

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        var message = form.dataset.confirm;
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });
})();
