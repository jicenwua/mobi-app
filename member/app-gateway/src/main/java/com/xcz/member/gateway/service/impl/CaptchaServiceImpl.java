package com.xcz.member.gateway.service.impl;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.uuid.IdUtils;
import com.xcz.commons.core.web.vo.params.AjaxResult;
import com.xcz.member.gateway.config.properties.CaptchaProperties;
import com.xcz.member.gateway.service.CaptchaService;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import jakarta.annotation.Resource;
import jodd.util.Base64;
import jodd.util.StringUtil;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServiceImpl implements CaptchaService {
    @Resource
    private DefaultKaptcha captchaProducer;
    @Resource
    private DefaultKaptcha captchaProducerMath;
    @Resource
    private CaptchaProperties captchaProperties;

    private final RedissonClient redisson = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);
    private static final String CAPTCHA_CODE = "captcha:code";


    @Override
    public AjaxResult getCaptcha() throws IllegalAccessException {
        boolean enabled = captchaProperties.isEnabled();
        if (!enabled) {
            throw new ServiceException("请配置验证码");
        }
        String code;
        String result;
        BufferedImage image;
        if ("char".equalsIgnoreCase(captchaProperties.getType())) {
            result = captchaProducer.createText();
            code = result;
            image = captchaProducer.createImage(code);
        } else {
            result = captchaProducerMath.createText();
            code = result.substring(0, result.lastIndexOf("@"));
            result = result.substring(result.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(code);
        }
        String uuid = IdUtils.fastSimpleUUID();
        redisson.getMapCache(CAPTCHA_CODE)
                .put(uuid, result, captchaProperties.getExpireTime(), TimeUnit.MILLISECONDS);

        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            return AjaxResult.error(e.getMessage());
        }

        AjaxResult ajaxResult = AjaxResult.success();
        ajaxResult.put("uuid", uuid);
        ajaxResult.put("img", Base64.encodeToByte(os.toByteArray(), false));

        return ajaxResult;
    }

    @Override
    public boolean validate(String uuid, String answer) {
        if (StringUtil.isEmpty(uuid)) {
            throw new IllegalArgumentException("uuid不能为空");
        }
        if (StringUtil.isEmpty(answer)) {
            throw new IllegalArgumentException("请输入验证码");
        }

        RMapCache<String, String> mapCache = redisson.getMapCache(CAPTCHA_CODE);
        String code = mapCache.remove(uuid);

        if(code == null){
            throw new IllegalArgumentException("验证码已过期");
        }
        if (!code.equalsIgnoreCase(answer)) {
            throw new IllegalArgumentException("验证码错误");
        }
        return true;
    }

}
