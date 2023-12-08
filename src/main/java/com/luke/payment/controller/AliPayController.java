package com.luke.payment.controller;

import com.luke.payment.service.AliPayService;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@CrossOrigin
@RestController
@Api(tags = "网站支付宝支付")
@RequestMapping("/api/ali-pay")
public class AliPayController {
    @Resource
    AliPayService aliPayService;


    @ApiOperation("统一收单下单并支付页面接口的调用")
    @PostMapping("/trade/page/pay/{productId}")
    public R tradePagePay(@PathVariable Long productId) {
        log.info("统一收单下单并支付页面接口的调用");

        String formStr = aliPayService.tradeCreate(productId);

        return R.ok().data("formStr", formStr);
    }
}
