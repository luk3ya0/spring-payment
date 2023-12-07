package com.luke.payment.service.impl;

import com.google.gson.Gson;
import com.luke.payment.config.WxPayConfig;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.enums.wxpay.WxApiType;
import com.luke.payment.enums.wxpay.WxNotifyType;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.service.WxPayService;
import com.mysql.cj.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private OrderInfoService orderInfoService;

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
