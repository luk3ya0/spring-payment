package com.luke.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.service.AliPayService;
import com.luke.payment.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class AliPayServiceImpl implements AliPayService {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private Environment config;

    @Transactional
    @Override
    public String tradeCreate(Long productId) {
        log.info("生成支付宝支付的订单");

        try {
            // 生成订单
            OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);

            // 调用支付宝支付发起接口
            // 调用支付宝接口
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            // 配置需要的公共请求参数
            // 支付完成后，支付宝向谷粒学院发起异步通知的地址
            request.setNotifyUrl(config.getProperty("alipay.notify-url"));
            // 支付完成后，我们想让页面跳转回谷粒学院的页面，配置returnUrl
            request.setReturnUrl(config.getProperty("alipay.return-url"));

            // 组装当前业务方法的请求参数
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            BigDecimal total = new BigDecimal(orderInfo.getTotalFee().toString()).divide(new BigDecimal("100"));
            bizContent.put("total_amount", total);
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(bizContent.toString());

            // 执行请求，调用支付宝接口
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                throw new RuntimeException("创建支付交易失败");
            }
        } catch (AlipayApiException e) {
            log.error(e.getMessage());
            throw new RuntimeException("创建支付交易失败");
        }

    }

    @Override
    public void processOrder(Map<String, String> params) {

    }

    @Override
    public void cancelOrder(String orderNo) {

    }

    @Override
    public String queryOrder(String orderNo) {
        return null;
    }

    @Override
    public void checkOrderStatus(String orderNo) {

    }

    @Override
    public void refund(String orderNo, String reason) {

    }

    @Override
    public String queryRefund(String orderNo) {
        return null;
    }

    @Override
    public String queryBill(String billDate, String type) {
        return null;
    }
}
