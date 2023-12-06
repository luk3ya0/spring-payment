package com.luke.payment.service.impl;

import com.luke.payment.entity.RefundInfo;
import com.luke.payment.mapper.RefundInfoMapper;
import com.luke.payment.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

}
