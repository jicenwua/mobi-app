package com.xcz.member.gateway.config;

import com.xcz.member.gateway.handler.CaptchaHandler;
import com.xcz.member.gateway.handler.SentinelFallbackHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GatewayConfig {

    /**
     * 允许浏览器读取续签后的 authorization 响应头（后台管理跨域场景）
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern(CorsConfiguration.ALL);
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);
        config.setAllowCredentials(true);
        config.addExposedHeader("authorization");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("role_permission");
        config.addExposedHeader("Role-Permission");
        config.addExposedHeader("X-Encrypted");
        config.addExposedHeader("X-IV");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
    @Resource
    private CaptchaHandler captchaHandler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction(){
        return RouterFunctions
                .route(
                        RequestPredicates.GET("/mobi/dashboard/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                        captchaHandler
                );
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelFallbackHandler sentinelFallbackHandler(){
        return new SentinelFallbackHandler();
    }
}
