package com.luke.payment.service.impl;

import com.luke.payment.entity.PaymentInfo;
import com.luke.payment.mapper.PaymentInfoMapper;
import com.luke.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
