package com.xcz.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/*
用户信息管理，用户注册登录，权限，后台控制等信息
（
    还有一个店铺，会员管理，主要是会员积分的充值和使用，用户店铺的管理
    这个项目需要同故宫feign，获取店铺的信息审核等，以及消费记录
）
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.xcz.member.feign")
public class AppUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppUserApplication.class, args);
    }

}
