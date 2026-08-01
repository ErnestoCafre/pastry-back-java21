/*
 * Vista previa de la imagen en el formulario de producto.
 *
 * Antes eran dos manejadores inline —un oninput larguísimo en el campo y un
 * onerror en la <img>—. Sacarlos de los atributos es parte de poder servir una
 * Content-Security-Policy: los manejadores inline la necesitan tan permisiva
 * como los <script> embebidos.
 *
 * El estado inicial ya lo decide Thymeleaf según haya o no imagen cargada, así
 * que este archivo solo se ocupa de los cambios posteriores.
 */
(function () {
    'use strict';

    var input = document.getElementById('imageUrl');
    var box = document.getElementById('imagePreviewContainer');
    var img = document.getElementById('imagePreview');
    if (!input || !box || !img) return;

    input.addEventListener('input', function () {
        var value = input.value.trim();
        img.src = value;
        box.classList.toggle('hidden', value === '');
    });

    // Una ruta que no resuelve deja de mostrar el recuadro roto.
    img.addEventListener('error', function () {
        box.classList.add('hidden');
    });
})();
