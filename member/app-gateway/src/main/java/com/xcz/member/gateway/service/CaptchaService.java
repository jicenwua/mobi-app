package com.xcz.member.gateway.service;


import com.xcz.commons.core.web.vo.params.AjaxResult;
import org.springframework.stereotype.Service;

@Service
public interface CaptchaService {
    AjaxResult getCaptcha() throws IllegalAccessException;

    boolean validate(String uuid, String code);
}
