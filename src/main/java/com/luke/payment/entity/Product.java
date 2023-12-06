package com.luke.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_product")
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

    // 商品名称
    private String title;

    // 价格（分）
    private Integer price;
}
