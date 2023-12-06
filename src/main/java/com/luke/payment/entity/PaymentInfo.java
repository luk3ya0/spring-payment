package com.luke.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_payment_info")
@EqualsAndHashCode(callSuper = true)
public class PaymentInfo extends BaseEntity {

    // 商品订单编号
    private String orderNo;

    // 支付系统交易编号
    private String transactionId;

    // 支付类型
    private String paymentType;

    // 交易类型
    private String tradeType;

    // 交易状态
    private String tradeState;

    // 支付金额(分)
    private Integer payerTotal;

    // 通知参数
    private String content;
}
