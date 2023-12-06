package com.luke.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_order_info")
@EqualsAndHashCode(callSuper = true)
public class OrderInfo extends BaseEntity {

    //订单标题
    private String title;

    //商户订单编号
    private String orderNo;

    //用户id
    private Long userId;

    //支付产品id
    private Long productId;

    //订单金额(分)
    private Integer totalFee;

    //订单二维码连接
    private String codeUrl;

    //订单状态
    private String orderStatus;
}
