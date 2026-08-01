/*
 * Comportamiento compartido del panel de administración.
 *
 * Todo por delegación sobre `document` y contratos data-*: no hay JS inline en
 * ninguna plantilla, así que el panel puede recibir una Content-Security-Policy
 * sin 'unsafe-inline'. Sin dependencias externas — este archivo reemplazó a
 * Flowbite, que se cargaba entero (CSS + JS por CDN) para un solo drawer.
 *
 * Contratos:
 *   [data-confirm="mensaje"]      en un <form>   -> confirma antes de enviar
 *   [data-drawer-toggle="id"]     en un <button> -> abre/cierra #id
 *   [data-drawer-backdrop="id"]   en un <div>    -> fondo clickeable de #id
 *   [data-dialog-close]           dentro de un <dialog> -> lo cierra
 */
(function () {
    'use strict';

    var FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), ' +
        'select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

    // ---------- Confirmaciones de borrado ----------
    //
    // Un único listener delegado atiende cualquier formulario con
    // [data-confirm], presente o agregado después. Reemplaza los
    // onsubmit="return confirm('...')" que estaban repetidos por template.

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        var message = form.dataset.confirm;
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });

    // ---------- Modales ----------
    //
    // El <dialog> nativo abierto con showModal() ya aporta role="dialog",
    // aria-modal, atrapado del foco, cierre con Escape y devolución del foco al
    // elemento que lo abrió: justo lo que le faltaba al modal artesanal de
    // products/recipe. Acá queda solo el cierre por botón; la apertura la hace
    // cada página, porque suele venir con datos que cargar.

    document.addEventListener('click', function (event) {
        var closer = event.target.closest('[data-dialog-close]');
        if (!closer) return;

        var dialog = closer.closest('dialog');
        if (dialog) dialog.close();
    });

    // ---------- Drawer del sidebar en mobile ----------

    function initDrawer(button) {
        var id = button.dataset.drawerToggle;
        var drawer = document.getElementById(id);
        if (!drawer) return;

        var backdrop = document.querySelector('[data-drawer-backdrop="' + id + '"]');

        function isOpen() {
            return button.getAttribute('aria-expanded') === 'true';
        }

        function open() {
            drawer.classList.remove('-translate-x-full');
            if (backdrop) backdrop.classList.remove('hidden');
            button.setAttribute('aria-expanded', 'true');

            // Mover el foco adentro: si no, el lector de pantalla se queda
            // parado en el botón y no anuncia que se abrió nada.
            var first = drawer.querySelector(FOCUSABLE);
            if (first) first.focus();
        }

        function close(returnFocus) {
            drawer.classList.add('-translate-x-full');
            if (backdrop) backdrop.classList.add('hidden');
            button.setAttribute('aria-expanded', 'false');
            if (returnFocus) button.focus();
        }

        button.addEventListener('click', function () {
            if (isOpen()) close(true); else open();
        });

        if (backdrop) {
            backdrop.addEventListener('click', function () { close(true); });
        }

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && isOpen()) close(true);
        });

        // A partir de lg el sidebar es fijo (lg:translate-x-0) y el fondo es
        // lg:hidden, así que "abierto" deja de significar nada. Sin este reset,
        // al volver a mobile reaparecería el fondo de un drawer ya cerrado.
        window.matchMedia('(min-width: 1024px)').addEventListener('change', function (event) {
            if (event.matches && isOpen()) close(false);
        });
    }

    document.querySelectorAll('[data-drawer-toggle]').forEach(initDrawer);
})();
