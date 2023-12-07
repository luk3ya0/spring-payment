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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    /*
    @Resource
    private OrderInfoMapper;
    */

    @Override
    public OrderInfo createOrderByProductId(Long productId) {
        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // prevent composing duplicate order
        OrderInfo existingOrder = this.getNoPayOrderByProductId(productId);
        if (existingOrder != null) {
            return existingOrder;
        }

        // 生成订单信息
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());

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

    private OrderInfo getNoPayOrderByProductId(Long productId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        // TODO: user filter

        return baseMapper.selectOne(queryWrapper);
    }
}
