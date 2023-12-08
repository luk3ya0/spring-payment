package com.luke.payment.controller;

import com.luke.payment.entity.OrderInfo;
import com.luke.payment.enums.OrderStatus;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "Order Management")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {
    @Resource
    private OrderInfoService orderInfoService;

    @GetMapping("/list")
    public R list() {
        List<OrderInfo> list = orderInfoService.listOrderByCreateTimeDesc();

        return R.ok().data("list", list);
    }

    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo) {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            return R.ok().setMessage("Payment succeeded");
        }

        return R.ok().setCode(101).setMessage("Payment on going...");
    }
}
