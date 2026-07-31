package com.malva_pastry_shop.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
}
