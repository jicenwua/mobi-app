package com.xcz.member.feign.config;

import com.xcz.commons.core.security.ReactiveSecurityChainFilter;
import com.xcz.commons.core.security.SecurityChainFilter;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import com.xcz.member.feign.interceptor.FeignRequestInterceptor;
import com.xcz.member.feign.interceptor.InternalServiceAuthFilter;
import com.xcz.member.feign.interceptor.InternalServiceAuthWebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * Feign 自动配置：内部服务属性绑定、出站拦截与入站校验（Servlet / Reactive）。
 * <p>
 * 依赖 {@link InternalServiceProperties}（{@code security.internal-service.*}），
 * 在微服务间 Feign 调用时统一处理内部令牌与用户上下文透传。
 * </p>
 * <p>
 * 注册内容：
 * </p>
 * <ul>
 *   <li>启动校验 — 检查内部令牌是否配置、是否仍为默认值</li>
 *   <li>入站校验 — 对 Feign 管理端路径校验 {@code X-Internal-Service} 请求头（Servlet / Gateway 各一套）</li>
 *   <li>出站拦截 — Feign 调用时附加内部令牌并转发用户上下文请求头</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(InternalServiceProperties.class)
public class FeignAutoConfiguration {

    /**
     * 应用启动后校验 {@code security.internal-service.token}：
     * 未配置直接失败；生产环境使用默认值则失败；长度过短仅告警。
     */
    @Bean
    public InternalServiceStartupValidator internalServiceStartupValidator(
            InternalServiceProperties internalServiceProperties) {
        return new InternalServiceStartupValidator(internalServiceProperties);
    }

    /**
     * Servlet 环境入站内部服务认证过滤器。
     * <p>
     * 以 {@link Ordered#HIGHEST_PRECEDENCE} 注册到 {@link SecurityChainFilter} 链最前，
     * 对 {@link InternalServiceProperties#getProtectedPaths()} 中的路径校验内部令牌。
     * </p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityChainFilter internalServiceAuthChainFilter(
            InternalServiceProperties internalServiceProperties,
            Environment environment) {
        return () -> new InternalServiceAuthFilter(internalServiceProperties, environment);
    }

    /**
     * Reactive 环境（Gateway 等）入站内部服务认证过滤器。
     * <p>
     * 行为与 {@link #internalServiceAuthChainFilter} 一致，适配 WebFlux 请求链。
     * </p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveSecurityChainFilter internalServiceAuthReactiveChainFilter(
            InternalServiceProperties internalServiceProperties,
            Environment environment) {
        return () -> new InternalServiceAuthWebFilter(internalServiceProperties, environment);
    }

    /**
     * Feign 出站请求拦截器（仅 Servlet 业务服务）。
     * <p>
     * 调用下游时转发 {@code authorization}、用户 ID 等上下文头，
     * 并附加 {@link com.xcz.commons.core.constant.SecurityConstants#INTERNAL_SERVICE_HEADER} 内部令牌。
     * </p>
     */
    @Bean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FeignRequestInterceptor feignRequestInterceptor(
            InternalServiceProperties internalServiceProperties) {
        return new FeignRequestInterceptor(internalServiceProperties);
    }
}
