package com.xcz.member.gateway.filter;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.gateway.config.properties.BlackRequestProperties;
import com.xcz.member.gateway.config.properties.RateLimiterProperties;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 路由过滤拦截器
 */
@Slf4j
@Component
@Order(-900)
@RequiredArgsConstructor
public class RateLimiterFilter implements GlobalFilter {

    private final RedissonClient redisson = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);
    private static final String RATE_LIMITER_BLACK = "rate_limiter:black:";
    private static final String RATE_LIMITER_COUNT = "rate_limiter:count:";
    private static final String RATE_LIMITER_KEY = "rate_limiter:limiter:";
    private static final String RATE_LIMITER_TOPIC = "rate_limiter:topic";

    private final RateLimiterProperties rateLimiterProperties;
    private final BlackRequestProperties blackRequestProperties;
    private final List<String> blackIpList= new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init(){
        refresh();
        RTopic topic = redisson.getTopic(RATE_LIMITER_TOPIC);
        topic.addListener(String.class,new BlackIpListener());
    }

    @NacosConfigListener(
            dataId = "${spring.application.name}.${spring.cloud.nacos.config.file-extension:yaml}",
            groupId = "${spring.cloud.nacos.config.group:DEFAULT_GROUP}"
    )
    public void refresh(){
        if(blackRequestProperties.getIps() != null && !blackRequestProperties.getIps().isEmpty()){
            blackIpList.clear();
            blackIpList.addAll(blackRequestProperties.getIps());
            blackIpList.addAll(redisson.getList(RATE_LIMITER_BLACK));
        }
    }



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //进行黑名单拦截
        boolean isBlack = isBlackList(request);
        if(isBlack){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return ServletUtils.webFluxResponseWriter(response, "请求地址不允许");
        }
        if(!rateLimiterProperties.isEnabled()){
            return chain.filter(exchange);
        }
        //根据请求IP进行限流
        String ip = getIPAddress(request);
        RRateLimiterReactive rateLimiter = redisson.reactive().getRateLimiter(RATE_LIMITER_KEY + ip);
        return rateLimiter.trySetRate(RateType.OVERALL, rateLimiterProperties.getPermitsPerSecond(), Duration.ofSeconds(1))
                .flatMap(setResult -> rateLimiter.expire(Duration.ofHours(1))) // 设置过期时间
                .then(rateLimiter.tryAcquire()) // 尝试获取令牌
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        log.warn("请求被限流，IP: {}", ip);
                        return handleRateLimitExceeded(exchange,ip);
                    }
                });
    }

    /**
     * 处理黑名单ip
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String ip) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        RAtomicLongReactive atomicLong = redisson.reactive().getAtomicLong(RATE_LIMITER_COUNT + ip);
        return atomicLong.incrementAndGet()
                .flatMap(count ->{
                    Mono<Boolean> expireMono = Mono.just(true);
                    if (count == 1) {
                        expireMono = atomicLong.expire(Duration.ofMillis(rateLimiterProperties.getTimeout()));
                    }

                    if (count >= rateLimiterProperties.getBurstLimit()) {
                        return expireMono
                                .then(atomicLong.delete()) // 删除计数器
                                .then(redisson.reactive().getSet(RATE_LIMITER_BLACK).add(ip)) // 放入 Redis 黑名单
                                .then(Mono.fromRunnable(() -> blackIpList.add(ip)))
                                .then(exchange.getResponse().setComplete()); // 更新本地缓存
                    }

                    return expireMono.then(exchange.getResponse().setComplete());
                });
    }


    /**
     * 黑名单拦截
     * @param request   请求
     * @return  是否处于黑名单
     */
    private boolean isBlackList(ServerHttpRequest request) {
        List<String> urls = blackRequestProperties.getUrls();
        //判断是否黑名单url
        if(urls != null && !urls.isEmpty()){
            String url = request.getURI().getPath();
            if (StringUtils.matches(url, urls)){
                //如果是内部请求则不拦截，否则进行拦截
                String fromSource = request.getHeaders().getFirst(SecurityConstants.FROM_SOURCE);
                if(!SecurityConstants.INNER.equalsIgnoreCase(fromSource)){
                    return true;
                }
            }
        }
        //判断是否黑名单ip
        if(!blackIpList.isEmpty()){
            String ip = getIPAddress(request);
            return StringUtils.matches(ip, blackIpList);
        }
        return false;
    }


    /**
     * 获取用户请求ip
     * @param request   请求
     * @return  用户ip
     */
    private String getIPAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.isNotEmpty(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.isNotEmpty(xRealIp)) {
            return xRealIp;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "127.0.0.1";
    }

    /**
     * 订阅监听
     */
    private class BlackIpListener implements MessageListener<String> {
        @Override
        public void onMessage(CharSequence channel, String msg) {
            log.info("收到黑名单同步消息: {}", msg);
            if (msg.startsWith("ADD:")) {
                String ip = msg.substring(4);
                // 使用 Set 的 add 具有天然去重性
                if (!blackIpList.contains(ip)) {
                    blackIpList.add(ip);
                    log.info("⚡ 集群同步：已添加本地黑名单 IP: {}", ip);
                }
            } else if (msg.startsWith("REMOVE:")) {
                String ip = msg.substring(7);
                blackIpList.remove(ip);
                log.info("⚡ 集群同步：已移除本地黑名单 IP: {}", ip);
            }
        }
    }
}
