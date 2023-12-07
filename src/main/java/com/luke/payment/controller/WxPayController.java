package com.luke.payment.controller;

import com.google.gson.Gson;
import com.luke.payment.service.WxPayService;
import com.luke.payment.util.HttpUtils;
import com.luke.payment.util.WechatPay2Validator4Request;
import com.luke.payment.vo.R;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付 API")
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;

    @ApiOperation("调用统一下单 API, 生成支付二维码")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");

        Map<String, Object> map = wxPayService.nativePay(productId);

        return R.ok().setData(map);

    }

    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new Gson();

        // response map
        Map<String, String> map = new HashMap<>();

        try {
            // Handle notification params
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);

            String requestId = (String) bodyMap.get("id");

            log.info("Id of payment notification ===> {}", requestId);
            log.info("Complete data of payment notification ===> {}", body);

            // DONE: Verify Signature
            WechatPay2Validator4Request wechatPay2Validator4Request =
                    new WechatPay2Validator4Request(verifier, requestId, body);

            if (!wechatPay2Validator4Request.validate(request)) {

                // invalid response
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "failed to verified signature");

                return gson.toJson(map);
            }

            log.info("Verified signature successfully");

            // DONE: Handle Order Status etc.
            wxPayService.processOrder(bodyMap);

            // successful response
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "succeed");

            return gson.toJson(map);
        } catch (Exception e) {
            log.error(e.getMessage());

            // exception response
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "failed");

            return gson.toJson(map);
        }
    }
}
