package com.luke.payment.service.impl;

import com.luke.payment.entity.OrderInfo;
import com.luke.payment.mapper.OrderInfoMapper;
import com.luke.payment.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
