package com.luke.payment.task;

import com.luke.payment.entity.OrderInfo;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class WxPayTask {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("orderConfirm being executed...");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5);

        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();

            log.warn("超时订单 ===> {}", orderNo);

            // 核实订单状态，调用微信支付查单接口
            wxPayService.checkOrderStatus(orderNo);
        }
    }
}
