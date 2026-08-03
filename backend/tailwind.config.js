/*
 * Configuracion de Tailwind para el panel de administracion.
 *
 * Antes vivia en un <script> inline de layout/main.html, junto al Play CDN.
 * Moverla a un archivo real es lo que permite:
 *   - servir CSS estatico en vez de compilar en el navegador en cada carga;
 *   - aplicar una Content-Security-Policy sin 'unsafe-inline';
 *   - que la paleta sea versionable y diffeable, que es lo que permitio
 *     resolver A11: ver el comentario sobre `primary` mas abajo.
 *
 * Version 3.x a proposito: las 51 plantillas usan nomenclatura v3
 * (shadow-sm, rounded-sm, bg-opacity-75...). Tailwind 4 renombra varias de
 * esas utilidades, asi que migrar de version es un trabajo aparte, no un
 * efecto colateral de sacar el CDN.
 *
 * SIN PLUGINS a proposito. El Play CDN tampoco cargaba @tailwindcss/forms,
 * asi que los checkboxes y selects se ven nativos hoy. Agregar el plugin acá
 * cambiaria la apariencia de todos los formularios del panel sin que nadie
 * lo pidiera.
 */

/** @type {import('tailwindcss').Config} */
module.exports = {
    // El escaneo es textual sobre el fuente, no evaluacion. Por eso funcionan
    // los th:class con concatenacion de fragments/buttons.html: cada nombre de
    // clase aparece literal en el archivo. Si algun dia se arma un nombre por
    // partes ('bg-' + color + '-600'), Tailwind no lo va a generar.
    content: [
        './src/main/resources/templates/**/*.html',
        './src/main/resources/static/js/**/*.js',
    ],
    theme: {
        extend: {
            colors: {
                /*
                 * A11 resuelto: el panel usa la marca del storefront.
                 *
                 * Hasta aca `primary` era la escala `pink` de Tailwind sin una
                 * sola modificacion, o sea el default que quedo de la era del
                 * Play CDN. Nadie la eligio. El storefront declara
                 * `--primary: oklch(0.50 0.16 300)` -un violeta- y lo hace
                 * igual en los dos repos (pastry-web y pastry-front), asi que
                 * la divergencia eran 54 grados de hue por accidente. Malva es
                 * un violeta; el rosa no venia de ningun lado.
                 *
                 * La escala se derivo variando L y C sobre ese hue 300 fijo, y
                 * el 600 -el color de los botones- es exactamente el
                 * `--primary` del storefront. De paso el contraste del boton
                 * primario sobre blanco sube de 4.60:1 a 6.46:1; el anterior
                 * pasaba AA por 0.1.
                 *
                 * Ninguna plantilla cambia: todo el markup ya dice primary-*.
                 */
                primary: {
                    50: '#f7f4fe',   // oklch(0.972 0.014 300)
                    100: '#eee7fd',  // oklch(0.940 0.030 300)
                    200: '#dfd1fc',  // oklch(0.885 0.060 300)
                    300: '#c9b1f6',  // oklch(0.805 0.098 300)
                    400: '#a986e5',  // oklch(0.690 0.140 300)
                    500: '#8d63cc',  // oklch(0.590 0.158 300)
                    600: '#7347af',  // oklch(0.500 0.160 300) — el del storefront
                    700: '#5e3692',  // oklch(0.430 0.146 300)
                    800: '#492a74',  // oklch(0.365 0.122 300)
                    900: '#361f56',  // oklch(0.302 0.096 300)
                },
            },
            animation: {
                fadeInUp: 'fadeInUp 0.5s ease-out',
                logoFloat: 'logoFloat 2s ease-in-out infinite',
            },
            keyframes: {
                fadeInUp: {
                    '0%': { opacity: '0', transform: 'translateY(20px)' },
                    '100%': { opacity: '1', transform: 'translateY(0)' },
                },
                logoFloat: {
                    '0%, 100%': { transform: 'translateY(0)' },
                    '50%': { transform: 'translateY(-10px)' },
                },
            },
        },
    },
    plugins: [],
};
