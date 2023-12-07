package com.luke.payment.service.impl;

import com.google.gson.Gson;
import com.luke.payment.entity.PaymentInfo;
import com.luke.payment.enums.PayType;
import com.luke.payment.mapper.PaymentInfoMapper;
import com.luke.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        log.info("Logging Payment Info");

        Gson gson = new Gson();
        HashMap plainMap = gson.fromJson(plainText, HashMap.class);

        String orderNo = (String) plainMap.get("out_trade_no");
        String transactionId = (String) plainMap.get("transaction_id");
        String tradeType = (String) plainMap.get("trade_type");
        String tradeState = (String) plainMap.get("trade_state");
        Map<String, Object> amount = (Map<String, Object>) plainMap.get("amount");
        Integer payerTotal = ((Double) amount.get("payer_total")).intValue();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);
    }
}
