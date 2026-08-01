/*
 * Configuracion de Tailwind para el panel de administracion.
 *
 * Antes vivia en un <script> inline de layout/main.html, junto al Play CDN.
 * Moverla a un archivo real es lo que permite:
 *   - servir CSS estatico en vez de compilar en el navegador en cada carga;
 *   - aplicar una Content-Security-Policy sin 'unsafe-inline';
 *   - que la paleta sea versionable y diffeable (ver A11: la divergencia con
 *     el storefront ahora es una decision escrita, no un accidente inline).
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
                primary: {
                    50: '#fdf2f8',
                    100: '#fce7f3',
                    200: '#fbcfe8',
                    300: '#f9a8d4',
                    400: '#f472b6',
                    500: '#ec4899',
                    600: '#db2777',
                    700: '#be185d',
                    800: '#9d174d',
                    900: '#831843',
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
