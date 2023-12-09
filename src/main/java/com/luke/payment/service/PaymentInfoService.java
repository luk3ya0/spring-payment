package com.luke.payment.service;

import java.util.Map;

public interface PaymentInfoService {

    void createPaymentInfo(String plainText);

    void createPaymentInfoForAlipay(Map<String, String> params);
}
