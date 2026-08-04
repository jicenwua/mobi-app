package com.xcz.member;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AppUserApplicationTests {

    @Resource
    private PasswordEncoder passwordEncoder;
    @Test
    void contextLoads() {

        System.err.println(passwordEncoder.encode("123456"));
    }

}
