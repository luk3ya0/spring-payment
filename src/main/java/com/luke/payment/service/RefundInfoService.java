package com.luke.payment.service;

import com.luke.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundByOrderNo(String orderNo, String reason);

    void updateRefund(String bodyAsString);
}
