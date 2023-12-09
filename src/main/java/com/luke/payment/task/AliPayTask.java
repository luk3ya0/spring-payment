package com.luke.payment.task;

import com.luke.payment.entity.OrderInfo;
import com.luke.payment.enums.PayType;
import com.luke.payment.service.AliPayService;
import com.luke.payment.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class AliPayTask {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private AliPayService aliPayService;

    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("orderConfirm beging executed...");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5, PayType.ALIPAY.getType());

        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();

            log.warn("超市订单 ===> {}", orderNo);

            aliPayService.checkOrderStatus(orderNo);
        }
    }
}
