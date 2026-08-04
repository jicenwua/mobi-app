package com.xcz.member.feign.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 店铺 Feign 客户端配置（支持 multipart 文件上传）
 */
@Configuration
public class MobiShopFeignConfig {

    @Bean
    public Encoder MobiShopFeignEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
