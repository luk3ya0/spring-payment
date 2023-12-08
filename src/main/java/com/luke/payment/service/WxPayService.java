package com.luke.payment.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;

    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNo) throws IOException;

    String queryOrder(String orderNo) throws Exception;

    void checkOrderStatus(String orderNo) throws Exception;

    void refund(String orderNo, String reason) throws IOException;

    String queryRefund(String refundNo) throws Exception;

    void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException;

    String queryBill(String billDate, String type) throws IOException;

    String downloadBill(String billDate, String type) throws IOException;
}
