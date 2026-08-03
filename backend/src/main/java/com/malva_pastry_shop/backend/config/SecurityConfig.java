package com.malva_pastry_shop.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * API Security Filter Chain - prioridad alta (Order 1).
         * Maneja /api/** - endpoints públicos de solo lectura, sin autenticación.
         */
        @Bean
        @Order(1)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Catálogo público - solo lectura, sin autenticación
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/tags/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/sections/**").permitAll()
                                                // Denegar cualquier otro acceso a la API
                                                .anyRequest().denyAll())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((req, res, ex) -> {
                                                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        res.setContentType("application/json");
                                                        res.getWriter().write("{\"error\":\"No autenticado\"}");
                                                })
                                                .accessDeniedHandler((req, res, ex) -> {
                                                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        res.setContentType("application/json");
                                                        res.getWriter().write("{\"error\":\"Acceso denegado\"}");
                                                }));

                return http.build();
        }

        /**
         * Content-Security-Policy del panel.
         *
         * <p>Sin {@code 'unsafe-inline'}, que es lo que hace que valga la pena:
         * un {@code onclick} inyectado en el nombre de un producto no se
         * ejecuta. Es viable porque no queda JavaScript embebido en ninguna
         * plantilla —todo el comportamiento vive en {@code static/js/}
         * enganchado por atributos {@code data-*}— ni un solo atributo
         * {@code style} ni bloque {@code <style>}, así que {@code style-src}
         * puede ser igual de estricto. Las dos cosas las sostienen tests:
         * {@code inlineJavaScriptDoesNotSpread} y {@code noInlineStylesEither}.
         *
         * <p>{@code img-src} es la única directiva laxa, a propósito:
         * {@code Product.imageUrl} acepta cualquier URL de hasta 500 caracteres
         * y un admin puede pegar una externa. Una imagen no ejecuta nada, así
         * que el riesgo de permitir orígenes https no se parece al de permitir
         * scripts.
         */
        private static final String ADMIN_CSP = String.join("; ",
                        "default-src 'self'",
                        "script-src 'self'",
                        "style-src 'self'",
                        "img-src 'self' data: https:",
                        "font-src 'self'",
                        "connect-src 'self'",
                        "form-action 'self'",
                        "frame-ancestors 'none'",
                        "base-uri 'self'",
                        "object-src 'none'");

        /**
         * Swagger UI se sirve por esta misma cadena y arma su página con
         * scripts y estilos inline, así que la CSP de arriba la dejaría en
         * blanco. Queda exceptuada en vez de relajar la política de todo el
         * panel por una herramienta que en producción está apagada
         * ({@code SWAGGER_ENABLED:false}).
         */
        private static RequestMatcher swaggerPaths() {
                PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();
                return new OrRequestMatcher(
                                path.matcher("/swagger-ui/**"),
                                path.matcher("/swagger-ui.html"),
                                path.matcher("/api-docs/**"));
        }

        /**
         * Admin Security Filter Chain - prioridad baja (Order 2).
         * Maneja todo lo que no sea /api/** con form login + sesión.
         */
        @Bean
        @Order(2)
        public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .headers(headers -> headers.addHeaderWriter(
                                                new DelegatingRequestMatcherHeaderWriter(
                                                                new NegatedRequestMatcher(swaggerPaths()),
                                                                new ContentSecurityPolicyHeaderWriter(ADMIN_CSP))))
                                .authorizeHttpRequests(auth -> auth
                                                // Recursos estáticos públicos
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // Documentación OpenAPI pública
                                                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                                .permitAll()
                                                // Login público
                                                .requestMatchers("/login", "/login/**").permitAll()
                                                // Páginas de error: el forward de accessDeniedPage tiene
                                                // que poder renderizarse aunque no haya sesión, o el
                                                // usuario termina rebotado al login sin saber qué pasó.
                                                .requestMatchers("/error", "/error/**").permitAll()
                                                // Gestión de usuarios solo para ADMIN
                                                .requestMatchers("/users/**").hasRole("ADMIN")
                                                // Todo lo demás requiere ADMIN o EMPLOYEE
                                                .anyRequest().hasAnyRole("ADMIN", "EMPLOYEE"))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureUrl("/login?error=true")
                                                .usernameParameter("email")
                                                .passwordParameter("password")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(exceptions -> exceptions
                                                .accessDeniedPage("/error/403"));

                return http.build();
        }
}
