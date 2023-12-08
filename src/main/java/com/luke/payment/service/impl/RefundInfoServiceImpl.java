package com.luke.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.entity.RefundInfo;
import com.luke.payment.mapper.RefundInfoMapper;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luke.payment.util.OrderNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {
    @Resource
    OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    public void updateRefund(String bodyAsString) {
        Gson gson = new Gson();
        Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", resultMap.get("out_refund_no"));

        RefundInfo refundInfo = new RefundInfo();

        // Content of refund-query and refund-request
        if (resultMap.get("status") != null) {
            refundInfo.setRefundStatus(resultMap.get("status"));
            refundInfo.setContentReturn(bodyAsString);
        }

        // Content of refund-notify callback
        if (resultMap.get("refund_status") != null) {
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            refundInfo.setContentNotify(bodyAsString);
        }

        baseMapper.update(refundInfo, queryWrapper);
    }
}
