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
         * Admin Security Filter Chain - prioridad baja (Order 2).
         * Maneja todo lo que no sea /api/** con form login + sesión.
         */
        @Bean
        @Order(2)
        public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // Recursos estáticos públicos
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // Documentación OpenAPI pública
                                                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                                .permitAll()
                                                // Login público
                                                .requestMatchers("/login", "/login/**").permitAll()
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
