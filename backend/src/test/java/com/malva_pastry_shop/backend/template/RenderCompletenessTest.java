package com.malva_pastry_shop.backend.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Fija la configuración que impide que una página se sirva cortada a la mitad.
 *
 * <h2>Qué pasaba</h2>
 * Thymeleaf emite la salida a medida que procesa la plantilla. Si una
 * expresión falla en el medio, la respuesta ya se envió y ya está committeada:
 * el status no se puede cambiar. El cliente recibe un <b>200 con media
 * página</b>.
 *
 * <p>Es el bug de {@code users/show} con {@code ${user.createdAt}}. Un barrido
 * que mira el status da esa página por buena, que fue exactamente lo que pasó.
 *
 * <h2>Medido, no supuesto</h2>
 * Reintroduciendo el bug y pidiendo {@code /users/1} contra la app real:
 *
 * <pre>
 * produce-partial-output-while-processing=true   status 200, 17.300 bytes, sin &lt;/html&gt;
 *                                                (curl corta con CURLE_PARTIAL_FILE)
 * produce-partial-output-while-processing=false  status 500, 0 bytes
 * </pre>
 *
 * <h2>Por qué no hay aserciones de &lt;/html&gt; en los tests de renderizado</h2>
 * Se midió también eso, y sobran: bajo MockMvc la excepción se propaga como
 * {@code ServletException} <b>con streaming o sin él</b>, porque no hay un
 * contenedor real que committee la respuesta. O sea que ningún test de
 * renderizado puede llegar a ver un 200 truncado, y afirmar
 * {@code contains("</html>")} sería copiar un gesto sin la causa.
 *
 * <p>La consecuencia real de la propiedad es cerrar la brecha al revés: hace
 * que el servidor de producción falle como ya fallaban los tests.
 */
@WebMvcTest(controllers = FragmentProbeController.class)
@DisplayName("Integridad del render completo")
class RenderCompletenessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ThymeleafProperties thymeleaf;

    @Test
    @DisplayName("Thymeleaf bufferea la página entera antes de escribirla")
    void thymeleafBuffersBeforeWriting() {
        assertThat(thymeleaf.getServlet().isProducePartialOutputWhileProcessing())
                .as("""
                        Volvió el streaming de Thymeleaf.

                        Con esto en true, una expresión que falla a mitad de plantilla encuentra
                        la respuesta ya committeada: el status queda en 200 y la página sale
                        cortada, sin </html>. Ningún test lo ve —bajo MockMvc no hay contenedor
                        que committee nada— así que el defecto llega entero a producción.

                        Se arregla en application.properties:
                            spring.thymeleaf.servlet.produce-partial-output-while-processing=false
                        """)
                .isFalse();
    }

    @Test
    @DisplayName("una expresión rota sale a la superficie en vez de dibujar media página")
    void aBrokenExpressionSurfaces() {
        // Control negativo de lo de arriba: si algún día un @ControllerAdvice
        // se traga las excepciones de render, la propiedad queda en false y no
        // sirve de nada, porque el fallo vuelve a convertirse en una página.
        // El nombre de la propiedad viaja en la causa raíz, no en el mensaje de
        // la ServletException, que solo dice "template parsing".
        assertThatThrownBy(() -> mockMvc.perform(get("/probe/broken")))
                .as("nada puede convertir un fallo de render en una página servida")
                .hasStackTraceContaining("createdAt");
    }
}
