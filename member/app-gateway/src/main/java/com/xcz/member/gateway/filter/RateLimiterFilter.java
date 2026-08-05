package com.xcz.member.gateway.filter;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import com.xcz.member.gateway.config.properties.BlackRequestProperties;
import com.xcz.member.gateway.config.properties.RateLimiterProperties;
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
 * 路由过滤拦截器，使用redis进行全局的拦截，防止黑名单只在本网关生效
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
        //监听其他服务出现的名单
        RTopic topic = redisson.getTopic(RATE_LIMITER_TOPIC);
        topic.addListener(String.class,new BlackIpListener());
    }

    /**
     * 监听使用的nacos配置文件，当配置文件进行修改的时候，进行刷新黑名单数据
     */
    @NacosConfigListener(
            dataId = "${spring.application.name}.${spring.cloud.nacos.config.file-extension:yaml}",
            groupId = "${spring.cloud.nacos.config.group:DEFAULT_GROUP}"
    )
    public void refresh(){
        blackIpList.clear();
        if (blackRequestProperties.getIps() != null) {
            blackIpList.addAll(blackRequestProperties.getIps());
        }
        RSet<String> redisBlackSet = redisson.getSet(RATE_LIMITER_BLACK);
        if (!redisBlackSet.isEmpty()) {
            blackIpList.addAll(redisBlackSet.readAll());
        }
    }



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String ip = getIPAddress(request);

        if (isWhiteList(path, ip)) {
            return chain.filter(exchange);
        }
        if (isBlackList(path, ip)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return ServletUtils.webFluxResponseWriter(response, "请求地址不允许");
        }
        if (!rateLimiterProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        RRateLimiterReactive rateLimiter = redisson.reactive().getRateLimiter(RATE_LIMITER_KEY + ip);
        return rateLimiter.trySetRate(RateType.OVERALL, rateLimiterProperties.getPermitsPerSecond(), Duration.ofSeconds(1)) //计算每秒的最大请求
                .flatMap(setResult -> rateLimiter.expire(Duration.ofHours(1))) // 一小时没有请求，则删除该ip令牌桶
                .then(rateLimiter.tryAcquire()) // 尝试获取令牌
                .flatMap(allowed -> {
                    //获取成功放行，失败则进行限流处理
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
                                .then(Mono.fromRunnable(() ->redisson.getTopic(RATE_LIMITER_TOPIC).publish("ADD:" + ip)))
                                .then(exchange.getResponse().setComplete()); // 结束 429 响应。
                    }

                    return expireMono.then(exchange.getResponse().setComplete());
                });
    }


    /**
     * 黑名单拦截
     *
     * @param path 请求路径
     * @param ip   客户端 IP
     * @return 是否处于黑名单
     */
    private boolean isBlackList(String path, String ip) {
        List<String> urls = blackRequestProperties.getUrls();
        if (urls != null && !urls.isEmpty()) {
            return StringUtils.matches(path, urls);
        }
        if (!blackIpList.isEmpty()) {
            return StringUtils.matches(ip, blackIpList);
        }
        return false;
    }

    /**
     * 检查是否白名单
     *
     * @param path 请求路径
     * @param ip   客户端 IP
     * @return 是否白名单
     */
    private boolean isWhiteList(String path, String ip) {
        List<String> whitelist = blackRequestProperties.getWhitelist();
        if (whitelist != null && !whitelist.isEmpty()) {
            return StringUtils.matches(ip, whitelist) || StringUtils.matches(path, whitelist);
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
