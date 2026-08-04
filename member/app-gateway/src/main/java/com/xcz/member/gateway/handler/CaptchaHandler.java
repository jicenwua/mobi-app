package com.xcz.member.gateway.handler;

import com.xcz.commons.core.web.vo.params.AjaxResult;
import com.xcz.member.gateway.service.CaptchaService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CaptchaHandler implements HandlerFunction<ServerResponse> {
    @Resource
    private CaptchaService captchaService;

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        AjaxResult ajax;

        try {
            ajax = captchaService.getCaptcha();
        } catch (IllegalAccessException e) {
            return Mono.error(e);
        }

        return ServerResponse.ok().bodyValue(ajax);
    }
}
