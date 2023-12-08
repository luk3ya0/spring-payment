package com.luke.payment.service.impl;

import com.google.gson.Gson;
import com.luke.payment.config.WxPayConfig;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.entity.RefundInfo;
import com.luke.payment.enums.OrderStatus;
import com.luke.payment.enums.wxpay.WxApiType;
import com.luke.payment.enums.wxpay.WxNotifyType;
import com.luke.payment.enums.wxpay.WxTradeState;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.service.PaymentInfoService;
import com.luke.payment.service.RefundInfoService;
import com.luke.payment.service.WxPayService;
import com.mysql.cj.util.StringUtils;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> nativePay(Long productId) throws Exception {
        log.info("生成订单");

        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);
        String codeUrl = orderInfo.getCodeUrl();
        if (orderInfo != null && !StringUtils.isNullOrEmpty(codeUrl)) {
            log.info("订单二维码已存在");

            HashMap<String, Object> strMap = new HashMap<>();
            strMap.put("codeUrl", codeUrl);
            strMap.put("orderNo", orderInfo.getOrderNo());

            return strMap;
        }

        log.info("调用统一下单API");

        // 调用统一下单API
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));

        // 请求body参数
        Gson gson = new Gson();
        Map<String, Object> paramsMap = composeParams(orderInfo);

        String jsonParams = gson.toJson(paramsMap);

        log.info("请求参数" + jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        // 完成签名并执行请求
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            String bodyAsString = handleResponse(response);

            HashMap<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);

            // qr code
            codeUrl = resultMap.get("code_url");

            // save qr code
            String orderNo = orderInfo.getOrderNo();
            orderInfoService.saveCodeUrl(orderNo, codeUrl);

            HashMap<String, Object> strMap = new HashMap<>();
            strMap.put("codeUrl", codeUrl);
            strMap.put("orderNo", orderInfo.getOrderNo());

            return strMap;
        }
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("处理订单");

        String plainText = decryptFromResource(bodyMap);

        // transform plainText to map
        Gson gson = new Gson();
        HashMap plainMap = gson.fromJson(plainText, HashMap.class);

        String orderNo = (String) plainMap.get("out_trade_no");

        // lock avoiding duplicating payment info records
        if (lock.tryLock()) {
            try {
                // 接口调用幂等性
                // handle duplicate notifications from WeChat payment callback
                String orderStatus = orderInfoService.getOrderStatus(orderNo);

                if (!orderStatus.equals(OrderStatus.NOTPAY.getType())) {
                    return;
                }

                // update status of the order by orderNo
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                // payment logging persistence
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void cancelOrder(String orderNo) throws IOException {
        // 调用微信支付的关单接口
        this.closeOrder(orderNo);

        // 更新商户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    @Override
    public String queryOrder(String orderNo) throws Exception {
        log.info("查单接口调用 ===> {}", orderNo);

        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = String.format(
                "%s%s?mchid=%s",
                wxPayConfig.getDomain(),
                url,
                wxPayConfig.getMchId());

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            return handleResponse(response);
        }
    }

    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        // 调用微信支付查单接口
        String result = this.queryOrder(orderNo);

        Gson gson = new Gson();
        Map resultMap = gson.fromJson(result, HashMap.class);

        String tradeState = (String) resultMap.get("trade_state");

        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付 ===> {}", orderNo);

            // 已支付，更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

            // 记录支付日志
            paymentInfoService.createPaymentInfo(result);
        }

        // 未支付，关闭订单，并更新商户端订单状态
        if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
            log.warn("核实订单未支付 ===> {}", orderNo);

            this.closeOrder(orderNo);

            // 更新本地订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(String orderNo, String reason) throws IOException {
        log.info("创建退款单记录");

        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        log.info("调用退款 API");

        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());

        HttpPost httpPost = new HttpPost(url);

        // Composing Params
        Gson gson = new Gson();

        Map paramsMap = new HashMap();
        paramsMap.put("out_trade_no", orderNo);
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", reason);
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map amountMap = new HashMap();
        amountMap.put("refund", refundInfo.getRefund());
        amountMap.put("total", refundInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);

        String jsonParams = gson.toJson(paramsMap);

        log.info("Requests Params ===> {}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("Succeed, refund result = {}", bodyAsString);
            } else if (statusCode == 204) {
                log.info("Succeed");
            } else {
                throw new RuntimeException(
                        "Refund Exception, Status Code = " + statusCode +
                                ", Refund Result = " + bodyAsString);
            }

            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

            refundInfoService.updateRefund(bodyAsString);
        }

    }

    @Override
    public String queryRefund(String refundNo) throws IOException {
        log.info("查询退款接口调用 ===> {}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功, 查询退款返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RemoteException(
                        "查询退款异常，响应码 = " + statusCode +
                                "查询退款返回结果 = " + bodyAsString);
            }

            return bodyAsString;
        }
    }

    @Override
    public void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("解析退款单");

        String plainText = decryptFromResource(bodyMap);

        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
                    return;
                }

                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

                refundInfoService.updateRefund(plainText);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public String queryBill(String billDate, String type) throws IOException {
        log.warn("申请账单接口调用 {}", billDate);

        String url = "";
        if ("tradebill".equals(type)) {
            url = WxApiType.TRADE_BILLS.getType();
        } else if ("fundflowbill".equals(type)) {
            url = WxApiType.FUND_FLOW_BILLS.getType();
        } else {
            throw new RuntimeException("不支持的账单类型");
        }

        url = wxPayConfig.getDomain().concat(url).concat("?bill_data").concat(billDate);

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功，申请账单返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException(
                        "申请账单异常，响应码 = " + statusCode +
                                ", 申请账单返回结果 = " + bodyAsString);
            }

            Gson gson = new Gson();
            Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);

            return resultMap.get("download_url");
        }
    }

    @Override
    public String downloadBill(String billDate, String type) throws IOException {
        log.warn("下载账单接口调用 {}, {}", billDate, type);

        String downloadUrl = this.queryBill(billDate, type);

        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功，下载账单返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException(
                        "下载账单异常，响应码 = " + statusCode +
                                ", 申请账单返回结果 = " + bodyAsString);
            }

            return bodyAsString;
        }
    }

    private String handleResponse(CloseableHttpResponse response) throws Exception {
        int statusCode = response.getStatusLine().getStatusCode();
        String bodyAsString = EntityUtils.toString(response.getEntity());
        if (statusCode == 200) {
            System.out.println("success return body = " + bodyAsString);
        } else if (statusCode == 204) {
            System.out.println("success");
        } else {
            System.out.println(
                    "failed, resp code = " + statusCode +
                            ", return body = " + bodyAsString);

            throw new IOException("request failed");
        }

        return bodyAsString;
    }

    private void closeOrder(String orderNo) throws IOException {
        log.info("关闭订单中，订单号 ===> {}", orderNo);

        HttpPost httpPost = getHttpPost(orderNo);

        // 完成签名并执行请求
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("success 200");
            } else if (statusCode == 204) {
                System.out.println("success 204");
            } else {
                System.out.println(
                        "failed, resp code = " + statusCode);

                throw new IOException("request failed");
            }
        }

    }

    private HttpPost getHttpPost(String orderNo) {
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);

        HttpPost httpPost = new HttpPost(url);

        // Compose json payload
        Gson gson = new Gson();
        HashMap<String, String> paramsMap = new HashMap<>();

        paramsMap.put("mchid", wxPayConfig.getMchId());
        // paramsMap.put("", "");

        String jsonParams = gson.toJson(paramsMap);

        // Setup params to request object
        StringEntity entity = new StringEntity(jsonParams, StandardCharsets.UTF_8);
        entity.setContentType("application/json");

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        return httpPost;
    }

    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("密文解密");

        Map<String, String> resourceMap = (Map<String, String>) bodyMap.get("resource");

        String ciphertext = resourceMap.get("ciphertext");
        String nonce = resourceMap.get("nonce");
        String associatedData = resourceMap.get("associated_data");


        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes());
        String plainText = aesUtil.decryptToString(
                associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        log.info("Cipher Text ===> {}", ciphertext);
        log.info("Plain Text ===> {}", plainText);

        return plainText;

    }

    private Map<String, Object> composeParams(OrderInfo orderInfo) {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);
        return paramsMap;
    }
}
