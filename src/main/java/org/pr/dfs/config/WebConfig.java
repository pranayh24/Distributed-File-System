package org.pr.dfs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.interceptor.UserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering UserContextInterceptor...");
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")  // Apply to all paths
                .excludePathPatterns(
                        "/api/test/**",     // Exclude test endpoints
                        "/api/system/health", // Exclude health check
                        "/swagger-ui/**",   // Exclude swagger
                        "/v3/api-docs/**"   // Exclude api docs
                );
        log.info("UserContextInterceptor registered successfully!");
    }
}