package com.malva_pastry_shop.backend.config;

import com.malva_pastry_shop.backend.format.MoneyFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActiveNavInterceptor activeNavInterceptor;

    public WebMvcConfig(ActiveNavInterceptor activeNavInterceptor) {
        this.activeNavInterceptor = activeNavInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Solo el panel: la API no renderiza vistas.
        registry.addInterceptor(activeNavInterceptor).excludePathPatterns("/api/**");
    }

    /**
     * Locale fijo al del negocio, ignorando el Accept-Language del navegador.
     *
     * <p>Sin esto, Spring usa AcceptHeaderLocaleResolver y todo lo que formatee
     * números en una plantilla cambia según con qué idioma esté configurado el
     * navegador de quien mira: el mismo precio salía {@code 13132.00} o
     * {@code 13132,00} en la misma pantalla según el usuario.
     *
     * <p>El panel es una herramienta interna con un único mercado, así que la
     * pregunta "¿qué idioma prefiere el navegador?" no es la que decide cómo se
     * escribe un precio. La decisión es del negocio y vive acá.
     *
     * <p>Los importes ya no dependen de esto —{@link MoneyFormatter} formatea
     * explícito—, pero sigue cubriendo cualquier {@code #numbers} o
     * {@code #temporals} suelto en las 36 plantillas todavía sin migrar.
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(MoneyFormatter.BUSINESS_LOCALE);
    }

    /**
     * Formateador de importes, accesible desde las plantillas como
     * {@code ${@money.format(x)}}.
     *
     * <p>Va acá y no como {@code @Component} por dos motivos. Uno práctico: un
     * componente suelto no entra en el slice de {@code @WebMvcTest} y todos los
     * tests de renderizado fallan con "No bean named 'money' available". Otro
     * de diseño: qué locale usa el panel y cómo se escribe un precio son la
     * misma decisión de negocio, y conviene leerlas juntas.
     */
    @Bean("money")
    public MoneyFormatter moneyFormatter() {
        return new MoneyFormatter();
    }
}
