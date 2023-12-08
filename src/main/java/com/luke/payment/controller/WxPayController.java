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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "Website WeChat Payment API")
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;

    @ApiOperation("Invoke Creating Order API, Generate QR Code")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("Raise Payment Request");

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

    @GetMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws IOException {
        log.info("Canceling order");

        wxPayService.cancelOrder(orderNo);

        return R.ok().setMessage("Order has been cancelled");
    }

    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("Query Order");

        String result = wxPayService.queryOrder(orderNo);

        return R.ok().setMessage("Query successfully").data("result", result);
    }

    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws Exception {
        log.info("Request Refund");

        wxPayService.refund(orderNo, reason);

        return R.ok();
    }

    @ApiOperation("Query Refund，for Testing")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws Exception {
        log.info("Query Refund");

        String result = wxPayService.queryRefund(refundNo);

        return R.ok().setMessage("Query successfully").data("result", result);
    }

    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("Executing Refund Notification");

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();

        try {
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");

            log.info("Refund Notification id ===> {}", requestId);

            WechatPay2Validator4Request wechatPay2Validator4Request =
                    new WechatPay2Validator4Request(verifier, requestId, body);

            if (!wechatPay2Validator4Request.validate(request)) {
                log.error("Failed to verify Refund Notification");

                response.setStatus(500);

                map.put("code", "ERROR");
                map.put("message", "Failed to verify Refund Notification");

                return gson.toJson(map);
            }

            log.info("Succeed to verify Refund Notification");

            wxPayService.processRefund(bodyMap);

            response.setStatus(200);

            map.put("code", "SUCCESS");
            map.put("message", "Succeed");

            return gson.toJson(map);
        } catch (Exception e) {
            log.error(e.getMessage());

            response.setStatus(500);

            map.put("code", "ERROR");
            map.put("message", "Exception happends during handling refund notification");

            return gson.toJson(map);
        }
    }

    @ApiOperation("获取账单 url, 测试用")
    @GetMapping("/querybill/{billDate}/{type}")
    public R queryTradeBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {
        log.info("获取账单 url");

        String downloadUrl = wxPayService.queryBill(billDate, type);

        return R.ok().setMessage("获取账单 url 成功").data("downloadUrl", downloadUrl);
    }

    @ApiOperation("下载账单 API")
    @GetMapping("downloadbill/{billDate}/{type}")
    public R downloadBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {
        log.info("下载账单");

        String result = wxPayService.downloadBill(billDate, type);

        return R.ok().data("result", result);
    }
}
