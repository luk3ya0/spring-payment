package com.luke.payment.controller;

import com.luke.payment.config.WxPayConfig;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "Test Controller")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    private WxPayConfig wxPayConfig;

    @GetMapping
    public R getWxPayConfig() {
        String mchId = wxPayConfig.getMchId();
        return R.ok().data("mchId", mchId);
    }
}
