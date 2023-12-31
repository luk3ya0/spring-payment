package com.luke.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.entity.RefundInfo;
import com.luke.payment.enums.OrderStatus;
import com.luke.payment.enums.PayType;
import com.luke.payment.enums.pay.AliPayTradeState;
import com.luke.payment.service.AliPayService;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.service.PaymentInfoService;
import com.luke.payment.service.RefundInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class AliPayServiceImpl implements AliPayService {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private Environment config;

    @Resource
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String tradeCreate(Long productId) {
        log.info("生成支付宝支付的订单");

        try {
            // 生成订单
            OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.ALIPAY.getType());

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processOrder(Map<String, String> params) {
        log.info("处理订单");

        String orderNo = params.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);

                if (!orderStatus.equals(OrderStatus.NOTPAY.getType())) {
                    return;
                }

                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                paymentInfoService.createPaymentInfoForAlipay(params);
            } finally {
                lock.unlock();
            }
        }

    }

    @Override
    public void cancelOrder(String orderNo) {
        this.closeOrder(orderNo);

        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    private void closeOrder(String orderNo) {
        log.info("支付宝-关闭订单 ===> {}", orderNo);

        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                // throw new RuntimeException("关单接口调用失败");
            }
        } catch (AlipayApiException e) {
            log.error(e.getMessage());
            throw new RuntimeException("关单接口调用失败");
        }
    }

    /**
     * 查询订单
     * @param orderNo 订单编号
     * @return 订单查询结果，如果返回 null 则说明支付宝，未生成该编号所属的订单
     */
    @Override
    public String queryOrder(String orderNo) {
        log.info("支付宝-查单接口调用 ===> {}", orderNo);

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();

            bizContent.put("out_trade_no", orderNo);

            request.setBizContent(bizContent.toString());

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());

                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());

                return null;
            }
        } catch (AlipayApiException e) {
            log.error(e.getMessage());
            throw new RuntimeException("查单接口调用失败");
        }

    }

    /**
     * 根据订单号调用支付宝查单接口，核实订单状态
     * 如果订单未创建，则直接更新本地订单状态
     * 如果订单未支付，则调用关单接口关闭订单，并更新商户端订单状态
     * 如果订单已支付，则更新商户端订单状态，并记录支付日志
     * @param orderNo 订单号
     */
    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        String result = this.queryOrder(orderNo);

        if (result == null) {
            log.warn("订单未创建于支付宝处 ===> {}", orderNo);

            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }

        Gson gson = new Gson();
        HashMap<String, LinkedTreeMap> resultMap = gson.fromJson(result, HashMap.class);
        LinkedTreeMap alipayTradeQueryResponse = resultMap.get("alipay_trade_query_response");

        String tradeStatus = (String) alipayTradeQueryResponse.get("trade_status");
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("核实订单未支付 ===> {}", orderNo);

            this.closeOrder(orderNo);

            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }

        if (AliPayTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("核实订单已支付 ===> {}", orderNo);

            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

            paymentInfoService.createPaymentInfoForAlipay(alipayTradeQueryResponse);
        }
    }

    @Override
    public void refund(String orderNo, String reason) {
        try {
            log.info("调用退款 API");

            RefundInfo refundInfo = refundInfoService.createRefundByOrderNoForAliPay(orderNo, reason);

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            BigDecimal refund = new BigDecimal(refundInfo.getTotalFee().toString()).divide(new BigDecimal("100"));

            bizContent.put("refund_amount", refund);
            bizContent.put("refund_reason", reason);

            request.setBizContent(bizContent.toString());

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());

                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliPayTradeState.REFUND_SUCCESS.getType());
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());

                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);

                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliPayTradeState.REFUND_ERROR.getType());
            }

        } catch (AlipayApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String queryRefund(String orderNo) {
        try {
            log.info("查询退款接口调用 ===> {}", orderNo);

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();

            JSONObject bizContent = new JSONObject();

            bizContent.put("out_trade_no", orderNo);
            bizContent.put("out_request_no", orderNo);

            request.setBizContent(bizContent.toString());


            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());

                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());

                return null;
            }
        } catch (AlipayApiException e) {
            log.error(e.getMessage());

            return null;
        }
    }

    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();

            JSONObject bizContent = new JSONObject();

            bizContent.put("bill_type", type);
            bizContent.put("bill_date", billDate);

            request.setBizContent(bizContent.toString());

            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());

                Gson gson = new Gson();
                HashMap<String, LinkedTreeMap> resultMap = gson.fromJson(response.getBody(), HashMap.class);
                LinkedTreeMap billResponse = resultMap.get("alipay_data_dataservice_bill_downloadurl_query_response");

                return (String) billResponse.get("bill_download_url");
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());

                return null;
            }

        } catch (AlipayApiException e) {
            log.error(e.getMessage());

            return null;
        }
    }
}
