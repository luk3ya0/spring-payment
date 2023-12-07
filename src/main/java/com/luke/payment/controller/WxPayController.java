package com.luke.payment.controller;

import com.luke.payment.service.WxPayService;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付 API")
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @ApiOperation("调用统一下单 API, 生成支付二维码")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");

        Map<String, Object> map = wxPayService.nativePay(productId);

        return R.ok().setData(map);
    }
}
