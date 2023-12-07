package com.luke.payment.service;

import com.luke.payment.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDesc();
}
