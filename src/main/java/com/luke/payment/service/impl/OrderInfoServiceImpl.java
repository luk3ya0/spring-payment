package com.luke.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.entity.Product;
import com.luke.payment.enums.OrderStatus;
import com.luke.payment.mapper.OrderInfoMapper;
import com.luke.payment.mapper.ProductMapper;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.util.OrderNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    /*
    @Resource
    private OrderInfoMapper;
    */

    @Override
    public OrderInfo createOrderByProductId(Long productId, String paymentType) {
        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // prevent composing duplicate order
        OrderInfo existingOrder = this.getNoPayOrderByProductId(productId, paymentType);
        if (existingOrder != null) {
            return existingOrder;
        }

        // Generate Order Info
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);

        // DONE: persis to database
        // orderInfoMapper.insert(orderInfo);
        // baseMapper refers to OrderInfoMapper Object
        baseMapper.insert(orderInfo);

        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        baseMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public List<OrderInfo> listOrderByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        log.info("Update status of the order ===> {}", orderStatus.getType());

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());

        baseMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public String getOrderStatus(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);

        // order has been deleted during waiting for payment notification
        if (orderInfo == null) {
            return null;
        }

        return orderInfo.getOrderStatus();
    }

    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes, String paymentType) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.eq("create_time", instant);
        queryWrapper.eq("payment_type", paymentType);

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>() ;
        queryWrapper.eq("order_no", orderNo);

        return baseMapper.selectOne(queryWrapper);
    }

    private OrderInfo getNoPayOrderByProductId(Long productId, String paymentType) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.eq("payment_type", paymentType);
        // TODO: user filter

        return baseMapper.selectOne(queryWrapper);
    }
}
