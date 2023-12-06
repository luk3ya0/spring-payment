package com.luke.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class BaseEntity {

    // 主键
    // 定义主键策略: 跟随数据库的主键自增
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    // 创建时间
    private Date createTime;

    // 更新时间
    private Date updateTime;
}
